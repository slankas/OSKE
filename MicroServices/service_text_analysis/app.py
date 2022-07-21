# Basic Flask application to provide access to various textrank functions
# through a REST API

from flask import Flask,abort,jsonify,make_response,request, url_for
import logging
import keyphrase_textrank as kptr
import textacyutility as txtutility
import keyphrase_index as kpindex
import lda_topic
import threading
import json
import datetime
import zulu
import redis
import gensim
import summarization
from summarization.keywords import keywords

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

app = Flask(__name__,static_folder=None)


# need some location to share information across the worker processes
indexMapRedis = redis.StrictRedis(host='openke_redis', port=6379,db=0)

def setAnalyticInProgress(id):
    indexMapRedis.set(str(id), "False")

def setAnalyticResult(id,result):
    indexMapRedis.set(str(id), json.dumps(result))


def getAnalyticResult(id):
    return indexMapRedis.get(str(id))

#to launch a separate thread for creating index
class myThread (threading.Thread):
    def __init__(self, sessionID,title,creatorID, docs):
        threading.Thread.__init__(self)
        self.threadID = sessionID
        self.title = title
        self.creatorID = creatorID
        self.documents = docs

    def run(self):
        logging.info(self.threadID + " - thread started")

        sessionID = self.threadID
        #setAnalyticResult(sessionID)

        #allJSONObjects = kpindex.getJSONObjectsFromElasticSearch(self.rest, self.sourceType,self.threadID,self.query,self.maxNumResults)
        #logging.info(self.threadID + " -  data retrieved from Elastic search creating index now")

        textData = kpindex.getAllTextData(self.documents)
        indexes,totalDistinctKeywords = kpindex.getBookBackIndexes(self.threadID,textData,self.documents)
        logging.info(self.threadID +  " - Index creation done")
        documentData,totalDocuments = kpindex.getTitles(self.documents)

        currentDateZulu = zulu.now()
        date = zulu.parse(currentDateZulu).isoformat()#datetime.datetime.now().isoformat()

        conceptData,totalConcepts=kpindex.getConcepts(self.documents)
        indexMetaData = {"totalDocuments":totalDocuments,"dateCreated":date,"totalKeywords":totalDistinctKeywords,"totalConcepts":totalConcepts,"title":self.title, "creator": self.creatorID}

        completedIndex = {"bookback": indexes,"allDocumentData":documentData,"metadata": indexMetaData,"conceptIndex":conceptData}
        setAnalyticResult(sessionID,completedIndex)
        logging.info(self.threadID +  "Index stored, thread completed")

#to launch a separate thread for generating the LDA topics and assigning documents
class ldaThread (threading.Thread):
    def __init__(self, session_id,docs, num_topics, stop_words):
        threading.Thread.__init__(self)
        self.session_id = session_id
        self.documents = docs
        self.num_topics = num_topics
        self.stop_words = stop_words

    def run(self):
        logging.info(self.session_id + " - LDA thread started")

        topics = lda_topic.produce_lda(self.documents, num_topics=self.num_topics, additional_stop_words=self.stop_words)
        result =  { "topics" : topics, "records" : self.documents }
        setAnalyticResult(self.session_id,result)
        logging.info(self.session_id +  "LDA stored, thread completed")


@app.route('/')
def index():
    """Lists the available REST endpoints for the application.

    Args:
        none

    Returns:
        A json array containing a JSON array indexed at "endPoints".
        Each element in the array contains a JSON object with ...

	Example:
        {
          "code": 200,
          "endPoints": [
          {
            "methods": "GET,OPTIONS,HEAD",
            "rule": "/"
          },
          {
            "methods": "OPTIONS,POST",
            "rule": "/textrank/keyphrase/<float:r>"
          }
          ]
        }

    Raises:
        None
    """
    routes = []
    for rule in app.url_map.iter_rules():
        myRule = {}
        myRule["rule"] = rule.rule
        myRule["methods"] = ",".join(list(rule.methods))
        #myRule["function"] = rule.endpoint
        routes.append(myRule)

    return jsonify(code=200, endPoints=routes)

@app.route('/textrank/summary/<float:r>',methods=['POST'])
def getSummary(r):
    """ Summarizes text based upon textrank

    Args:
        r - what % of the text should be provided as a summary
        post data - json object with a string attribute called "text"
                    which contains the text to be summarized.

    Returns:
        A json object with a single element "summary" that is a json
        array of the sentences to be used as the summary.  Each element
        is a string

        Example:
        {
          "summary": [
             "sentence 1",
             "sentence 2",
             ...
             "sentence n"
          ]
        }

    Raises:
        None
    """
    try:
        contentJSON = request.json
        text = contentJSON['text']
        summary = summarization.summarize(text, split=True,ratio = r);
        return jsonify({'summary':summary});
    except:
        return jsonify({'error': sys.exc_info()[0], 'value':  sys.exc_info()[1]});

#keyphrase extraction
@app.route('/textrank/keyphrase/<float:r>',methods=['POST'])
def getKeyphrases(r):
    """ Retrieves the top keyphrases

    Args:
        r - what % of the top keywords should be provided.
            The 1.0 would return all keywords
        post data - json object with a string attribute called "text"
                    which contains the text to have the keywords returned.

    Returns:
        A json object with a single element "keywords" that is a json
        array of

        Example:
        {
          "keyphrase": [
            {
              "score": 0.30112727244135246,
              "phrase": "testValue"
            },
            {
              "score": 0.2880290370874553,
              "phrase": "testValue2"
            }
          ]
        }
    Raises:
        None
    """

    text = request.get_json(silent = True)['text']
    keyphrase = kptr.score_keyphrases_by_textrank(text,r)
    #print(keyphrase)
    result = [{ "phrase": k[0], "score": k[1] } for k in keyphrase]
    return jsonify({'keyphrase': result});

@app.route('/textrank/keyword/<float:r>',methods=['POST'])
def getKeyWords(r):
    """ Retrieves the top keywords based upon pytextrank's implementation of textrank
    By default, words are lemmatized to reduce duplicates.

    Args:
        r - what % of the top keywords should be provided.
            The 1.0 would return all keywords
        post data - json object with a string attribute called "text"
                    which contains the text to have the keywords returned.

    Returns:
        A JSON array of the keywords and their score

        Example:
           [
            {
              "score": 0.30112727244135246,
              "word": "testValue"
            },
            {
              "score": 0.2880290370874553,
              "word": "testValue2"
            }
	       ]

    Raises:
        None
    """

    text = request.get_json(silent = True)['text']
    keywordlist = keywords(text,ratio = r,scores=True,split=True,lemmatize=True)
    result = [{ "word": str(k[0]), "score": k[1] } for k in keywordlist]
    return jsonify(result);

@app.route('/textrank/sgrank/<int:num>',methods=['POST'])
def getSgrank(num):
    text = request.get_json(silent = True)['text']
    doc = txtutility.textacy.Doc(text)
    resultlist = txtutility.mySgRank(doc,n_keyterms=num)
    result = [(str(k[0]),k[1]) for k in resultlist]
    return jsonify({'KeyWords by Sgrank':result});

@app.route('/textrank/index',methods=['POST'])
def createAndStoreIndexes():
    data =  json.loads(request.data.decode("utf-8"))
    sessionID = data["sessionID"]
    setAnalyticInProgress(sessionID)
    title = data["title"] if "title" in data else ""
    creatorID = data["creatorID"] if "creatorID" in data else "unknown"
    docs = data["documents"]


    createIndexThread = myThread(sessionID,title,creatorID,docs)
    createIndexThread.start()

    return jsonify({"status":"creating"})


@app.route('/textrank/index/status/<id>',methods=['GET'])
def checkCreateIndexStatus(id):
    index = getAnalyticResult(id)

    if index is None:
        return  jsonify({"status": False,"message":"key not found"})

    index = index.decode("utf-8")
    if index == "False":
        return jsonify({"status":False,"message":"in progress"})
    else:
        indexMapRedis.delete(str(id))
        return jsonify({"status": True, "discoveryIndex": json.loads(index)})


@app.route('/textrank/index/timeseries',methods=['POST'])
def createTimeSeries():
    data =  json.loads(request.data.decode("utf-8"))
    startTime = data['startTime']
    textData = data['text']
    res = kpindex.createTimeSeries(textData,startTime)

    return jsonify(res)


@app.route('/lda',methods=['POST'])
def produceLDATopics():
    data =  json.loads(request.data.decode("utf-8"))
    #sessionID = data["sessionID"]
    #setAnalyticInProgress(sessionID)

    docs = data["records"]
    stop_words = data["stopWords"]  if "stopWords" in data else []
    num_topics  = data["numTopics"] if "numTopics" in data else 7
    session_id  = data["sessionID"]

    setAnalyticInProgress(session_id)
    lda_thread = ldaThread(session_id,docs, num_topics, stop_words)
    lda_thread.start()
    return jsonify({"status":"creating"})

@app.route('/lda/<id>',methods=['GET'])
def checkLDAStatus(id):
    index = getAnalyticResult(id)

    if index is None:
        return  jsonify({"status": False,"message":"key not found"})

    index = index.decode("utf-8")
    if index == "False":
        return jsonify({"status":False,"message":"in progress"})
    else:
        indexMapRedis.delete(str(id))
        index = json.loads(index)
        index["status"] = True
        return jsonify(index)








@app.errorhandler(404)
def not_found(error):
    return make_response(jsonify({'error': 'service url not found'}), 404)


if __name__ == '__main__':
    app.run(debug=True,host='0.0.0.0')
