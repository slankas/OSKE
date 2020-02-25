package edu.ncsu.las.topicmodel.model;

import scala.Tuple2;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.clustering.DistributedLDAModel;
import org.apache.spark.mllib.clustering.LDA;
import org.apache.spark.mllib.linalg.Matrix;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;


import java.util.ArrayList;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.spark.SparkConf;
import org.apache.spark.ml.feature.CountVectorizer;
import org.apache.spark.ml.feature.CountVectorizerModel;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.json.JSONObject;

import java.io.Serializable;

import org.apache.spark.ml.feature.RegexTokenizer;
import org.apache.spark.ml.feature.StopWordsRemover;




public class LDASession implements Serializable, Runnable {
	private static final long serialVersionUID = 1L;
	
	
	public static class Document implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private String _uuid;
		private String _url;
		private String _text;
		
		public Document(String uuid, String url, String text) {
			_uuid = uuid;
			_url  = url;
			_text = text;
		}
		
		public String getUUID() { return _uuid; }
		public String getURL()  { return _url;  }
		public String getText() { return _text; }
		
	}
	
	public static class DocumentResult implements Serializable {
		private static final long serialVersionUID = 1L;

		public long documentNumber;
		public String documentUUID;
		public String documentURL;
		public double score;
		
		public JSONObject toJSON() {
			return new JSONObject().put("documentNumber",documentNumber)
					               .put("documentUUID",  documentUUID)
					               .put("documentURL", documentURL)
					               .put("score", score);
		}
	}
	
	
	static final Logger srcLogger =Logger.getLogger(LDASession.class.getName());
	
	private String _status = "";   // awaitingDocuments,awaitingExecution,executing,complete

	
	private List<String>[] topicDistributionMap;
	private DistributedLDAModel myModel;
	
	//private List<Document> _documents;
	private List<Row> _rowsForAnalyis;
	private List<String> _documentsWithNoTextByUUID =  new ArrayList<String>();
	
	private ArrayList<String> _documentsUUID;  //maps the document to the document's UUID;
	private ArrayList<String> _documentsURL;   //maps the document (by position) to the document's URL;
	
	private int _numberOfTopics   = 10;
	private int _numberOfKeywords = 10;
	private int _maxNumberOfIterations    = 100;

	private boolean _stemWords = true;
	
	private long _initialTime;  // used to help calculate the wait time before running.  May get "extra time" include for initialization, but most of that should be minor
	
	public LDASession() {
		_status = "awaitingDocuments";
		_initialTime = System.currentTimeMillis();
	}



	public int getNumberOfTopics() {
		return _numberOfTopics;
	}



	public void setNumberOfTopics(int numberOfTopics) {
		if (numberOfTopics < 1) {
			throw new IllegalArgumentException("Invalid value for the number of topics: "+numberOfTopics);
		}
		this._numberOfTopics = numberOfTopics;
	}

	public int getNumberOfKeywords() {
		return _numberOfKeywords;
	}

	public void setNumberOfKeywords(int numberOfKeywords) {
		if (numberOfKeywords < 1) {
			throw new IllegalArgumentException("Invalid value for the number of keywords: "+numberOfKeywords);
		}
		this._numberOfKeywords = numberOfKeywords;
	}

	public int getMaxNumberOfIterations() {
		return _maxNumberOfIterations;
	}

	public void setMaxNumberOfIterations(int maxNumberOfIterations) {
		if (maxNumberOfIterations < 1) {
			throw new IllegalArgumentException("Invalid value for the maximim number of iterations: "+maxNumberOfIterations);
		}		
		this._maxNumberOfIterations = maxNumberOfIterations;
	}



	public boolean getStemWords() {
		return _stemWords;
	}



	public void setStemWords(boolean stemWords) {
		_stemWords = stemWords;
	}



	public DistributedLDAModel getMyModel() {
		return myModel;
	}

	private void setMyModel(DistributedLDAModel myModel) {
		this.myModel = myModel;
	}

	List<Integer> getTopTopicsOfDocument(int k)
	{
		JavaPairRDD<Long,Vector> topicPerDocument=myModel.javaTopicDistributions();
		
		return null;
	}
	
	public int getDocumentCount() {
		return _rowsForAnalyis.size();
	}

	/**
	 * Set the documents to be analyzed.  Must be called before runLDA
	 * 
	 * @param docs
	 */
	public void setDocuments(List<Document> docs) {
		convertDocumentsForProcessing(docs);
	}
	
	private void convertDocumentsForProcessing(List<Document> allDocs) {
		List<Row> content = new ArrayList<Row>(); 
		_documentsUUID    = new ArrayList<String>();
		_documentsURL     = new ArrayList<String>();   //maps the document (by position) to the document's URL;
		for(Document doc : allDocs)	{    
			String text = doc.getText();
			
			if (text == null) {
				_documentsWithNoTextByUUID.add(doc.getUUID());
				continue;
			}
			
			
			text = StringUtilities.eliminateNonSentences(text);
			if (this.getStemWords()) {
				text = StringUtilities.stemText(text);
			}

			content.add(RowFactory.create(text));
			_documentsUUID.add(doc.getUUID());
			_documentsURL.add(doc.getURL());
		}
		
		_rowsForAnalyis = content;
	}
	
	

	
	/**
	 * Returns a list of document UUIDs that have no text (e.g., they are excluded from the topic analysis as
	 * we can't process them.
	 * 
	 * @return
	 */
	public List<String> getDocumentsNotInAnalysis() {
		return _documentsWithNoTextByUUID;
	}

	
	public List<String> getWordForTopic(int n)
	{
		
		
		return null;
	}
	
	public HashMap<String,List<String>> getResults(DistributedLDAModel model,String[] vocab, int totalTopics, int top_n)
	{
		 
		    return null;
	}
	
	
	
	public List<String>[] getTopicDistributionMap() {
		return topicDistributionMap;
	}

	private void setTopicDistributionMap(List<String>[] topicDistributionMap) {
		this.topicDistributionMap = topicDistributionMap;
	}
	

	public  void runLDA() {
		this.runLDA(_rowsForAnalyis);
	}
	
	private  void runLDA(List<Row> content) {
		SparkConf conf = null;
	    JavaSparkContext sc = null;
	    SQLContext sqlContext = null;
	    JavaRDD<Row> jrdd = null;
	    Dataset<Row> df = null;
	    Dataset<Row> wordsDataFrame = null;
	    Dataset<Row> rem = null;
	    JavaRDD<String> countVectors = null;
	    JavaRDD<Vector> parsedData = null;
	    JavaPairRDD<Long, Vector> corpus = null;
	    JavaPairRDD<Long, Vector> topicDistributionPerDocument = null;
	    
		try {
			/*Setting up spark configuration and spark context*/
			conf = new SparkConf().setAppName("LDA Example").setMaster("local");
		    sc = new JavaSparkContext(conf);
		    sqlContext = new org.apache.spark.sql.SQLContext(sc);
			
			/*Creating RDD and then dataframe of the content extracted*/
			jrdd = sc.parallelize(content);
			StructType schema = new StructType(new StructField [] {
					  new StructField("text", DataTypes.StringType, false, Metadata.empty())
					});
			df = sqlContext.createDataFrame(jrdd, schema);
	
			//create tokens first
			//Tokenizer tokenizer = new Tokenizer().setInputCol("text").setOutputCol("rawtokens");
			RegexTokenizer regexTokenizer = new RegexTokenizer().setInputCol("text")
					                                            .setOutputCol("rawtokens")
					                                            .setPattern("[a-zA-Z]{4,}+")
					                                            .setGaps(false);
			
			wordsDataFrame = regexTokenizer.transform(df);
			//wordsDataFrame.show();
					
			//Remove unwanted words using stopwordsremover
			StopWordsRemover remover = new StopWordsRemover().setInputCol("rawtokens")
					                                         .setOutputCol("tokens");
			rem = remover.transform(wordsDataFrame);
					
			//count vectorizer to get data in correct format
			CountVectorizer cv = new CountVectorizer().setInputCol("tokens")
					                                  .setOutputCol("features")
					                                  .setVocabSize(5000) // at most these many unique words.
					                                  .setMinDF(1); // unique words must be present in at least these many documents.
			CountVectorizerModel m = cv.fit(rem);
				
			/*get the word count vector in form of string RDD*/
			countVectors = m.transform(rem)
				              .select("features").toJavaRDD()
				              .map(new Function<Row, String>() {
				                public String call(Row row) throws Exception {
				                    double[] temp =  ((org.apache.spark.ml.linalg.SparseVector) row.get(0)).toArray();
				                    //System.out.println(temp.toString());
				                    StringBuilder sb = new StringBuilder();
				                    for(double d : temp)
									{
										sb.append( Double.toString(d));
										sb.append(" ");
									}
				                    
				                    return sb.toString().trim();
				                }
				              });
					
			//Retrieve the vocabulary		
			String[] vocab = m.vocabulary();
			/*
			for(String s : vocab) {
				System.out.println(s);
			}
			*/
			/*converting in required format*/	
			parsedData = countVectors.map(
						        new Function<String, Vector>() {
						          public Vector call(String s) {
						            String[] sarray = s.trim().split(" ");
						            double[] values = new double[sarray.length];
						            for (int i = 0; i < sarray.length; i++)
						              values[i] = Double.parseDouble(sarray[i]);
						            return Vectors.dense(values);
						          }
						        }
						    );
					
			//parsedData.collect().forEach(System.out::println);
					
			corpus = JavaPairRDD.fromJavaRDD(parsedData.zipWithIndex().map(
					        new Function<Tuple2<Vector, Long>, Tuple2<Long, Vector>>() {
					          public Tuple2<Long, Vector> call(Tuple2<Vector, Long> doc_id) {
					            return doc_id.swap();
					          }
				  
							
							
					        }
					    ));
			corpus.cache();
			corpus.collect(); //.forEach(System.out::println);
					   
			// Cluster the documents into topics using LDA
			DistributedLDAModel ldaModel = (DistributedLDAModel) new LDA().setK(_numberOfTopics).setMaxIterations(_maxNumberOfIterations).run(corpus);
					  
	
			// Output topics. Each is a distribution over words (matching word count vectors)
			Matrix topics = ldaModel.topicsMatrix();
			this.setMyModel(ldaModel);
	
			topicDistributionPerDocument=ldaModel.javaTopicDistributions();
					    
			 /*Output results*/
			//HashMap<Integer,List<String>> topicToKeywordsMap = new  HashMap<Integer,List<String>>();
			
			List<String>[] topicToKeywordsMap = new ArrayList[_numberOfTopics];
			//System.out.println("Learned topics (as distributions over vocab of " + ldaModel.vocabSize()  + " words):");
			for (int topic = 0; topic < _numberOfTopics; topic++) {
				//System.out.print("Topic " + topic + ":");
						    
				 //map the vocab words with their respective distribution in descending order wrt distribution
				 TreeMap<Double, String> treeMap1 = new TreeMap<Double, String>(Collections.reverseOrder());
				 for (int word = 0; word < ldaModel.vocabSize(); word++) { 
					 treeMap1.put(topics.apply(word, topic),vocab[word]);
				 }
	
				 // compute the top keywords for each topic
				 List<String> topKeywordsForTopic = new ArrayList<String>();			 
				 for (Map.Entry<Double, String> entry : treeMap1.entrySet()) { 	
					 // System.out.println(entry.getValue()+" <-> "+entry.getKey()+" ");   // This is the keyword and the corresponding vlaue
					 if (!entry.getValue().equals("") && !entry.getValue().equals(" ")) {
						 topKeywordsForTopic.add(entry.getValue());
						 if (topKeywordsForTopic.size() >= _numberOfKeywords) {
							 break;
						 }
					 }
				 }
				 topicToKeywordsMap[topic] =  topKeywordsForTopic;
			}
			
			this.setTopicDistributionMap(topicToKeywordsMap);
			
			
			scala.Tuple2<long[],double[]>[] documentsPerTopic = ldaModel.topDocumentsPerTopic(10);
			_topDocumentsForTopics = new ArrayList[documentsPerTopic.length];
			
			System.out.println("length (same as number of topics): " +documentsPerTopic.length);
			for (int i=0;i<documentsPerTopic.length;i++) {
				scala.Tuple2<long[],double[]> s = documentsPerTopic[i];
				
				_topDocumentsForTopics[i] = new ArrayList<DocumentResult>();
				for (int j=0; j < s._1.length; j++) {
					DocumentResult dr = new DocumentResult();
					dr.documentNumber = s._1[j];
					dr.documentUUID   = _documentsUUID.get((int)dr.documentNumber);
					dr.documentURL    = _documentsURL.get((int) dr.documentNumber);
					dr.score          = s._2[j];
					_topDocumentsForTopics[i].add(dr);
				}
			}
			topicDistributionPerDocument.unpersist();
			corpus.unpersist();
			parsedData.unpersist();
			countVectors.unpersist();
			rem.unpersist();
			wordsDataFrame.unpersist();
			df.unpersist();
			jrdd.unpersist();
	
			sqlContext.clearCache();
			sc.close();  

		}
		catch (Exception e) {
			srcLogger.log(Level.WARNING, "Spark LDA Processing Exception", e);
			this.setLDAPrococessingError(e.toString());
		}
		finally {
			if (topicDistributionPerDocument != null) { try {topicDistributionPerDocument.unpersist(); } catch (Exception e) {; } }
			if (corpus != null) { try { corpus.unpersist(); } catch (Exception e) {; }}
			if (parsedData != null) {try {parsedData.unpersist(); } catch (Exception e) {; } }
			if (countVectors != null) { try {countVectors.unpersist(); } catch (Exception e) {; } }
			if (rem != null) { try { rem.unpersist(); } catch (Exception e) {; } }
			if (wordsDataFrame != null) { try {wordsDataFrame.unpersist(); } catch (Exception e) {; } }
			if (df != null) {try {df.unpersist(); } catch (Exception e) {; } }
			if (jrdd != null) {try {jrdd.unpersist(); } catch (Exception e) {; } }
	
			if (sqlContext != null) { try { sqlContext.clearCache(); } catch (Exception e) {; } }
			if (sc != null) { try {sc.close(); } catch (Exception e) {; }}
		}
	}

	
	private String _ldaProcessingError = null;
	private void setLDAPrococessingError(String exceptionString) {
		_ldaProcessingError = exceptionString;		
	}
	public String getLDAProcessingError() { return _ldaProcessingError; }

	ArrayList<DocumentResult>[] _topDocumentsForTopics;
	
	public List<DocumentResult>[] getTopDocumentsForTopics() {
		return _topDocumentsForTopics;
	}
	
	/**
	 * returns the status as to whether or not LDA has completed running
	 * 
	 * @return awaitingDocuments,awaitingExecution,executing,complete
	 */
	public String getStatus() {
		return _status;
	}
	
	public static void main(String[] args) {
		
		List<Row> content = new ArrayList<Row>();
		content.add(RowFactory.create("Test lda sentence first"));
		content.add(RowFactory.create("Test lda sentence second"));
		content.add(RowFactory.create("some random text"));
		
		LDASession m = new LDASession();
		m._rowsForAnalyis = content;
		m._numberOfTopics = 5;
		
		m._documentsUUID    = new ArrayList<String>();
		m._documentsUUID.add("document 0");
		m._documentsUUID.add("document 1");
		m._documentsUUID.add("document 2");
		
		m._documentsURL     = new ArrayList<String>();
		m._documentsURL.add("document 0 (URL)");
		m._documentsURL.add("document 1 (URL)");
		m._documentsURL.add("document 2 (URL)");		
		m.runLDA(); 
	}

	
	private static long minimumWaitTimeMS = Long.MAX_VALUE;
	private static long maximumWaitTimeMS = Long.MIN_VALUE;
	private static long totalWaitTimeMS   = 0;
	
	private static long minimumResponseTimeMS = Long.MAX_VALUE;
	private static long maximumResponseTimeMS = Long.MIN_VALUE;
	private static long lastResponseTimeMS    = -1;
	private static long totalResponseTimeMS   = 0;
	private static long totalRequestCount = 0;
	


	@Override
	public void run() {
		_status = "executing";
		try {
			long startTime = System.currentTimeMillis();
			this.runLDA();
			long processingTime = System.currentTimeMillis() - startTime;
			long waitTime = startTime -_initialTime;
			_status = "complete";
			synchronized (LDASession.class) {  
				minimumResponseTimeMS = Math.min(processingTime, minimumResponseTimeMS);
				maximumResponseTimeMS = Math.max(processingTime, maximumResponseTimeMS);
				totalResponseTimeMS  += processingTime;
				lastResponseTimeMS    = processingTime;
				
				minimumWaitTimeMS = Math.min(waitTime, minimumWaitTimeMS);
				maximumWaitTimeMS = Math.max(waitTime, maximumWaitTimeMS);
				totalWaitTimeMS  += waitTime;
				
				totalRequestCount++;
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
			_status = "failure";
		}
		
	}
	
	public static JSONObject getProcessStatistics() {
    	double averageTime     = ((double) totalResponseTimeMS) / Math.max((double)totalRequestCount, 1.0);
    	double averageWaitTime = ((double) totalWaitTimeMS)     / Math.max((double)totalRequestCount, 1.0);
		JSONObject processStats = new JSONObject().put("minimumResponseTimeMS", minimumResponseTimeMS)
				               .put("maximumResponseTimeMS", maximumResponseTimeMS)
				               .put("lastResponseTimeMS", lastResponseTimeMS)
				               .put("totalResponseTimeMS", totalResponseTimeMS)
				               .put("averageTimeMS", averageTime)
				               .put("minimumWaitTimeMS", minimumWaitTimeMS)
				               .put("maximumWaitTimeMS", maximumWaitTimeMS)
				               .put("totalWaitTimeMS", totalWaitTimeMS)
				               .put("averageWaitTimeMS", averageWaitTime)
				               .put("totalRequests", totalRequestCount);
		return processStats;
	}

}
