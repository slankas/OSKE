import json
import pandas as pd
import re 
import nltk
import gensim
from gensim.utils import simple_preprocess
import gensim.corpora as corpora
from nltk.corpus import stopwords

def sent_to_words(sentences):
    for sentence in sentences:
        # deacc=True removes punctuations
        yield(gensim.utils.simple_preprocess(str(sentence), deacc=True))

def remove_stopwords(texts, stop_words):
    return [[word for word in simple_preprocess(str(doc)) if word not in stop_words] for doc in texts]
             
             
def findTopTopic(topicList):
    topTopic = 0
    topScore = 0.0
    for tuple in topicList:
        if tuple[1] > topScore:
            topTopic = tuple[0]
            topScore = tuple[1]
    return (topTopic,topScore)
    
def produce_lda(docs, num_topics=7, additional_stop_words=[]):
    """
    
    docs - list of dictionary objects
    """
    df = pd.DataFrame(docs)
    
    # clean text
    df['text_processed'] = df['text'].map(lambda x: re.sub('[,\.!?\[\]:;\n]', ' ', x))
    df['text_processed'] = df['text_processed'].map(lambda x: re.sub('spk_\d', ' ', x))
    df['text_processed'] = df['text_processed'].map(lambda x: re.sub('\d+', ' ', x))
    df['text_processed'] = df['text_processed'].map(lambda x: x.lower())

    data = df.text_processed.values.tolist()
    data_words = list(sent_to_words(data))
    
    # remove stop words
    stop_words = stopwords.words('english')
    stop_words.extend(additional_stop_words)
    data_words = remove_stopwords(data_words,stop_words)
    
    # Create dictionary (mapping of word IDs to their actual values
    id2word = corpora.Dictionary(data_words)

    # Create Corpus
    texts = data_words

    # Term Document Frequency
    corpus = [id2word.doc2bow(text) for text in texts]

    # Build LDA model
    lda_model = gensim.models.LdaModel(corpus=corpus,id2word=id2word,num_topics=num_topics)
    
    topicList = lda_model.show_topics(num_topics = num_topics,num_words=5,formatted=False)
    
    myTopicList = []
    for topic in topicList:
        keywords = []
        for tuple in topic[1]:
            keywords.append(tuple[0])
        topicObject = {  "keywords": keywords,  "records": []  }
        myTopicList.append(topicObject)

    # now map the documents to their top topic
    for idx,doc in enumerate(corpus):
        docTopics = lda_model[doc]
        topTopic = findTopTopic(docTopics)
        myTopicList[topTopic[0]]["records"].append({"_id": df["_id"][idx], "score": float(topTopic[1]) } )
    
    for topicObject in myTopicList:
        topicObject["records"]= sorted(topicObject["records"], key=lambda x:(-x["score"],x["_id"]))

    return myTopicList
    