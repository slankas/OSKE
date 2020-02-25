package edu.ncsu.las.elastic.collection;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.Assert.fail;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.ncsu.las.collector.Collector;

/**
 * 
 *
 */
public class EditCollectionTest {

	private static Logger logger =Logger.getLogger(Collector.class.getName());

    @BeforeClass
    public void setUp() {
    	try {
    		File currentDirectory = new File(new File(".").getAbsolutePath());			
    		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
    		
			Collector.initializeCollector(currentWorkingDirectory,"sysstem_properties.json",true,true,false);
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
		int    port = (int) Collector.getTheCollecter().getConfigurationPropertyAsLong(ConfigurationType.JSONSTORE_ELASTIC_STORAGEPORT);
		String index = Collector.getTheCollecter().getConfigurationProperty(ConfigurationType.JSONSTORE_ELASTIC_NORMAL);
		String type = Collector.getTheCollecter().getConfigurationProperty(ConfigurationType.JSONSTORE_ELASTIC_COLLECTION_TYPE);
		String id = "2adf818b-9141-49bf-82b0-91a641eb67ec";
		String person_to_add = "add_during_edit_2@ncsu.edu";
		Client client;
		try {
			logger.log(Level.FINER, "Push to ElasticSearch  ");
			client = getClient(host, port);
			
			GetResponse response = client.prepareGet(index, type, id).execute().actionGet();
			
			System.out.println("Record: " + response.getId());
			System.out.println(response.getSourceAsMap());
			
			Map<String, Object> record = response.getSourceAsMap();
			
			System.out.println("Type of Shared with: " + record.get("shared_with").getClass());			

			@SuppressWarnings("unchecked")
			ArrayList<HashMap<String, String>> shared_with = new ArrayList<HashMap<String, String>>();
			shared_with = (ArrayList<HashMap<String, String>>) record.get("shared_with");
			
			System.out.println("Shared with : " + shared_with);			

			boolean exists = false;
			for (HashMap<String, String> p : shared_with) {
				if (p.values().contains(person_to_add)) {
					System.out.println(person_to_add + " already exists");
					exists = true;
					break;
				}
			}
			
			if (!exists) {
				HashMap<String, String> person = new HashMap<>();
				person.put("email", person_to_add);

				shared_with.add(person);

				UpdateRequest updateRequest = new UpdateRequest();
				updateRequest.index(index);
				updateRequest.type(type);
				updateRequest.id(id);
				updateRequest.doc("shared_with", shared_with);

				client.update(updateRequest).get();

				System.out.println("Successfully updated collection: ");

				response = client.prepareGet(index, type, id).execute().actionGet();

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
