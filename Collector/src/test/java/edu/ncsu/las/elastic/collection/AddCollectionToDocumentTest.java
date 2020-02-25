package edu.ncsu.las.elastic.collection;


import static org.testng.Assert.fail;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.ncsu.las.collector.Collector;

/**
 * 
 */
public class AddCollectionToDocumentTest {

	private static Logger logger =Logger.getLogger(Collector.class.getName());

    @BeforeClass
    public void setUp() {
    	try {
    		File currentDirectory = new File(new File(".").getAbsolutePath());			
    		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
    		
			Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",true,true,false);
    	}
    	catch (Exception e) {
    		System.err.println("ElasticSearchExportTest setup failed: "+e);
    		
    		fail("unable to iniatilized Collector");
    	}
    }	
	
	/**
	 * Test method to Create a collection.
	 */
	@Test
	public void testExportQueryToFile() {
		logger.log(Level.SEVERE, "ElasticSearch - test collection");
		logger.setLevel(Level.ALL);
/*
		String host = Collector.getTheCollecter().getConfigurationProperty(ConfigurationType.JSONSTORE_ELASTIC_STORAGEHOST);
		int port = (int) Collector.getTheCollecter()
				.getConfigurationPropertyAsLong(ConfigurationType.JSONSTORE_ELASTIC_STORAGEPORT);
		String documentType = "_doc";
		String index = Collector.getTheCollecter().getConfigurationProperty(ConfigurationType.JSONSTORE_ELASTIC_NORMAL);
		String collectionType = Collector.getTheCollecter().getConfigurationProperty(ConfigurationType.JSONSTORE_ELASTIC_COLLECTION_TYPE);

		String collectionID = "2adf818b-9141-49bf-82b0-91a641eb67ec";
		String documentID = "00000153-4278-f166-0a11-0c01000bdf3a";
		
//		String documentID = "00000151-38fa-f1aa-0a7e-051400007ffa"; // 00000151-3ee2-e54c-c0a8-7a01000000d5
		
		Client client;
		try {
			logger.log(Level.FINER,
					"Adding collection 0f018b02-4252-456d-8247-73166ab2dd6f to document 00000151-3ee2-e54c-c0a8-7a01000000d5  ");
			client = getClient(host, port);

			GetResponse response = client.prepareGet(index, documentType, documentID).execute().actionGet();
			System.out.println("Record: " + response.getId());
			System.out.println(response.getSourceAsMap());

			Map<String, Object> fields = response.getSourceAsMap();

			System.out.println("map :" + fields.keySet());

			GetResponse collectionRecord = client.prepareGet(index, collectionType, collectionID).execute().actionGet();
			Map<String, Object> collectionRecordSource = collectionRecord.getSourceAsMap();

			System.out.println("Collection Record: " + collectionRecord.getId());
			System.out.println(collectionRecordSource);

//			ArrayList<HashMap<String, String>> shared_with = new ArrayList<HashMap<String, String>>();
//			shared_with = (ArrayList<HashMap<String, String>>) collectionRecordSource.get("shared_with");
//			HashMap<String, String> owner = new HashMap<String, String>();
//			owner.put("email", collectionRecordSource.get("owner").toString());
//			shared_with.add(owner);

			ArrayList<HashMap<String, Object>> user_collection = (ArrayList<HashMap<String, Object>>) response.getSource().get("user_collection");
					
			if (user_collection == null) {				
				user_collection = new ArrayList<HashMap<String, Object>>();;
			}
			
			boolean exists = false;
			for (HashMap<String, Object> collection : user_collection) {
				if (collection.get("collection_id").toString().equals(collectionID)) {
					System.out.println("Collection " + collectionID + " already exists in the document");
					exists = true;
					break;
				}
			}
			
			if (!exists) {

				String collection_name = collectionRecord.getSourceAsMap().get("name").toString();

				HashMap<String, Object> collection = new HashMap<String, Object>();
				collection.put("added_by", added_by);
				collection.put("collection_id", collectionID);
				collection.put("collection_name", collection_name);
				collection.put("date_added", Instant.now().toString());

				user_collection.add(collection);

				UpdateRequest updateRequest = new UpdateRequest();
				updateRequest.index(index);
				updateRequest.type(documentType);
				updateRequest.id(documentID);
				updateRequest.doc("user_collection", user_collection);

				UpdateResponse updateResponse = client.update(updateRequest).get();

				System.out.println("Successfully updated collection in document: ");
				response = client.prepareGet(index, documentType, documentID).execute().actionGet();
				System.out.println("Record: " + response.getId());
				System.out.println(response.getSourceAsMap());
			}
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Unable to save to ElasticSearch", e);
		} finally {
			destroyConnection(host, port);
		}
*/
	}


}
