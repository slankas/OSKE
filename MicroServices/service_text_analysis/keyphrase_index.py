"""
code to retrieve the documents from elastic search extract the text data
and run different analytics on it to determine keyphrases, summary etc
"""

import json
import codecs
import keyphrase_textrank as kptr
import  collections
from nltk.stem.wordnet import WordNetLemmatizer
import requests
import urllib
import threading
from nltk.corpus import stopwords as nltk_stopwords
import nltk
import string
import logging
import re

lmtzr = WordNetLemmatizer()
punct = set(string.punctuation)
stop_words = set(nltk_stopwords.words('english'))
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

def getAllTextData(docs):
	"""
	extracting text data from the documents retrieved
	"""
	content = []
	for doc in docs:
		content.append(doc['text'])

	return content


def getBookBackIndexes(sessionID, content,docs):
	"""
	creating book back indexes
	(important keyphrases based on textrank algorithm from gensim package)
	"""
	allkeyphrases = set()
	logging.info(sessionID + " - start of getBackBook Indexs")

	for text in content:
		keyphrase = kptr.score_keyphrases_by_textrank(text, 0.05, 2000/len(content))
		for x in keyphrase:
			allkeyphrases.add(str(x[0]))
		#logging.info(sessionID + " - current keyphrase size: "+ str(len(allkeyphrases)))

	logging.info(sessionID + " - number of keyphrases: "+ str(len(allkeyphrases)))
	allkeyphrases = list(allkeyphrases)

	alldistinctwords = set()  # listing of all of the words from the KeyPhrases
	wordToKeyPhrase = {}      # dictionary of set.  For each word, what keyphrases was it found in?
	lemmatizedKeyPhrase = {}  # for each keyphrase, what is it's lemmatized version?
	regexedKeyPhrase = {}     # for each keyphrase, a regular expression that matches it.

	for phrase in allkeyphrases:
		distinctwords = phrase.split(" ")
		lemmatizedKeyPhrase[phrase] = lmtzr.lemmatize(phrase)
		regexedKeyPhrase[phrase] = re.compile(r'\b' + phrase  + r'\b',re.IGNORECASE)
		for word in distinctwords:
			word = lmtzr.lemmatize(word)
			if(len(word) > 2):
				alldistinctwords.add(word)
				if (word in wordToKeyPhrase):
					wordToKeyPhrase[word].add(phrase)
				else:
					wordToKeyPhrase[word] = { phrase }
	# not used, documentrecord = {}
	keyphraserecord = collections.OrderedDict()

	#just use Id when appending here, can add more information if needed

	logging.info(sessionID + " processing all documents now for keyphrases")
	iterCount = 0;
	for word in alldistinctwords:
		for k in wordToKeyPhrase[word]:
			keyphraseRegex = regexedKeyPhrase[k]    #re.compile(r'\b' + k + r'\b')
			# no longer needed because of the list used if re.search(r'\b' + word + r'\b', k):   # added 6/23/2018 to force match on complete word
			#if word in k:
			for doc in docs:
				iterCount += 1
				if (iterCount % 10000 == 0):
					logging.info(sessionID + " processing iterationCount: " + str(iterCount))
				if keyphraseRegex.search(doc['text']):   # removed .lower() as the regex is now case insensitive
					if word == k or word == lemmatizedKeyPhrase[k]:
						if word in keyphraserecord:
							keyphraserecord[word].append({"id":str(doc['id'])  })
						else:
							keyphraserecord[word] = [{"id":str(doc['id'])   }]
					elif (word+'|'+k) in keyphraserecord:
						keyphraserecord[word+'|'+k].append({"id":str(doc['id'])  })
					else:
						keyphraserecord[word+'|'+k] = [{"id":str(doc['id'])  }]

	logging.info(sessionID + " processed all documents now for keyphrases. Iteration count: "+ str(iterCount))


	output = []
	for key,value in keyphraserecord.items():
		#temp = collections.OrderedDict()
		temp = {}
		temp['keyphrase'] = key
		temp['documents'] = value
		output.append(temp)
		#print sorted(output)[:2]
	#TODO: Add more references for a document
	orderedoutput = sorted(output,key=lambda x:x['keyphrase'])
	return orderedoutput,len(alldistinctwords)


def getTitles(docs):
	"""
	retrieving titles for all documents
	"""
	documentData = []
	for d in docs:
		temp = {}
		temp['id'] = d['id']
		temp['title'] = d['title']
		temp['url'] = d['url']
		documentData.append(temp)
	return documentData,len(documentData)

def getConcepts(docs):
	conceptData = set()
	conceptNames = set()
	for d in docs:
		for concept in d['concepts']:
			conceptData.add(concept['fullName'])
			conceptNames.add(concept['name'])

	allConceptData = []
	valueCount = 0

	for concept in list(conceptData):

		tempConcept = {}
		tempDoc = []
		docPresent = False

		for d in docs:
			temp = {}
			valueList = set()
			isPresent = False

			for x in d['concepts'] :
				if x['fullName'] == concept:
					valueList.add(x['value'])
					isPresent = True
					docPresent = True
			if isPresent:
				temp['id'] = d['id']
				#temp['url'] = d['url']
				temp['values'] = list(valueList)
				tempDoc.append(temp)

		if docPresent:
			tempConcept['concept'] = str(concept)
			tempConcept['documents'] = tempDoc
			allConceptData.append(tempConcept)

	orderedConcepts = sorted(allConceptData,key=lambda x:x['concept'])
	return orderedConcepts,len(conceptNames)

def wordCounter(kw,text):
	logging.info("Counting instances of" + str(kw))
	count = 0
	words = [word.lower() for sent in nltk.sent_tokenize(text) for word in nltk.word_tokenize(sent) if word not in stop_words and not all(char in punct for char in word) and len(word) > 2]
	for w in words:
		if w == kw:
			count = count + 1
	logging.info("Counts of " + str(kw)+" = "+str(count))
	return count


def createTimeSeries(content,startTime):
	keyphraseCount = {}
	logging.info("Starting to extract KeyPhrases")
	for text in content:
		keyphrase = kptr.score_keyphrases_by_textrank(text)
		for x in keyphrase:
			w = x[0]
			count = wordCounter(w,text)

			if w in keyphraseCount.keys():
				keyphraseCount[w] = keyphraseCount[w] + count
			else:
				keyphraseCount[w] = count
	logging.info("KeyPharse Extraction/Counting Completed")
	return keyphraseCount





# Test Code

#sessionID = "00000157-b55e-9daf-c0a8-380100000192"
#docs = getJSONObjectsFromElasticSearch(sessionID)
'''
url = "http://10.26.0.53:9200/openke_sandbox/discoveryIndex/"+sessionID+"/_create"
alldocs = getDocsFromElasticSearch(sessionID)
textData = getAllTextData(alldocs)
indexes = getBookBackIndexes(textData,alldocs)

print indexes[:3]

insertsuccess = insertIntoElasticSearch(indexes,url)
print json.dumps(json.JSONDecoder().decode(insertsuccess.text))

sessionID = "00000157-b55e-9daf-c0a8-380100000192"
mymap[sessionID] = False
thread1 = myThread(sessionID)
thread1.start()



'''
