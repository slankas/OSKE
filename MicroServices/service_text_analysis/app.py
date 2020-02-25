# Basic Flask application to provide access to various textrank functions
# through a REST API

from flask import Flask,abort,jsonify,make_response,request, url_for
import logging
import gensim.summarization as genism
import keyphrase_textrank as kptr
import textacyutility as txtutility
import keyphrase_index as kpindex
import threading
import json
import datetime
import zulu
import redis
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

app = Flask(__name__,static_folder=None)


# need some location to share information across the worker processes
indexMapRedis = redis.StrictRedis(host='openke_redis', port=6379,db=0)

def setDiscoveryInProgess(id):
    indexMapRedis.set(str(id), "False")

def setDiscoveryIndex(id,discoveryIndex):
    indexMapRedis.set(str(id), json.dumps(discoveryIndex))


def getDiscoveryIndex(id):
    return indexMapRedis.get(str(id))

#to launch a separate thread for creating index
class myThread (threading.Thread):
    def __init__(self, sessionID,restEndPoint,sourceType,destinationType,query,title,maxNumResults, creatorID):
        threading.Thread.__init__(self)
        self.threadID = sessionID
        self.rest = restEndPoint
        self.sourceType = sourceType
        self.destinationType = destinationType
        self.query = query
        self.title = title
        self.maxNumResults = maxNumResults
        self.creatorID = creatorID

    def run(self):
        logging.info(self.threadID + " - thread started, query recieved: %s",str(self.query))
        logging.info(self.threadID + " REST endpoint: " + self.rest)

        sessionID = self.threadID
        #setDiscoveryIndex(sessionID)

        allJSONObjects = kpindex.getJSONObjectsFromElasticSearch(self.rest, self.sourceType,self.threadID,self.query,self.maxNumResults)
        logging.info(self.threadID + " -  data retrieved from Elastic search creating index now")

        textData = kpindex.getAllTextData(allJSONObjects)
        indexes,totalDistinctKeywords = kpindex.getBookBackIndexes(self.threadID,textData,allJSONObjects)
        logging.info(self.threadID +  " - Index creation done")
        documentData,totalDocuments = kpindex.getTitles(allJSONObjects)

        currentDateZulu = zulu.now()
        date = zulu.parse(currentDateZulu).isoformat()#datetime.datetime.now().isoformat()

        conceptData,totalConcepts=kpindex.getConcepts(allJSONObjects)
        metadata = {"totalDocuments":totalDocuments,"dateCreated":date,"totalKeywords":totalDistinctKeywords,"totalConcepts":totalConcepts,"title":self.title, "creator": self.creatorID}
        url = self.rest + self.destinationType +"/"+sessionID
        #insertsuccess = kpindex.insertIntoElasticSearch(indexes,documentData,conceptData,metadata,url)

        completedIndex = {"bookback": indexes,"allDocumentData":documentData,"metadata": metadata,"conceptIndex":conceptData}
        setDiscoveryIndex(sessionID,completedIndex)
        logging.info(self.threadID +  "Index stored, thread completed")



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
    """ Summarizes text based upon gensim's implementation of textrank

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
        summary = genism.summarize(text, split=True,ratio = r);
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
    """ Retrieves the top keywords based upon gensim's implementation of textrank
    By default, words are lemmatized to reduce duplicates.

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
          "keyword": [
            {
              "score": 0.30112727244135246,
              "word": "testValue"
            },
            {
              "score": 0.2880290370874553,
              "word": "testValue2"
            }
	  ]
	}
    Raises:
        None
    """

    text = request.get_json(silent = True)['text']
    keywordlist = genism.keywords(text,ratio = r,scores=True,split=True,lemmatize=True)
    result = [{ "word": str(k[0]), "score": k[1] } for k in keywordlist]
    return jsonify({'keywords':result});

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
    setDiscoveryInProgess(sessionID)
    restEndPoint = data["urlAndIndex"]
    sourceType = data["type"]
    destinationType = "discoveryIndex"
    query = data["query"]
    title = data["title"] if "title" in data else ""
    creatorID = data["creatorID"] if "creatorID" in data else "unknown"
    maxNumResults = data["maxNumResults"] if "maxNumResults" in data else -1

    #url = str(elasticServer)+str(indexES)+"/"+str(typeES)+"/"+str(sessionID)+"/_create"
    createIndexThread = myThread(sessionID,restEndPoint,sourceType,destinationType,query,title,maxNumResults,creatorID)
    createIndexThread.start()

    return jsonify({"status":"creating"})


@app.route('/textrank/index/status/<id>',methods=['GET'])
def checkCreateIndexStatus(id):
    index = getDiscoveryIndex(id)

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


@app.errorhandler(404)
def not_found(error):
    return make_response(jsonify({'error': 'Not found'}), 404)


if __name__ == '__main__':
    app.run(debug=True,host='0.0.0.0')
