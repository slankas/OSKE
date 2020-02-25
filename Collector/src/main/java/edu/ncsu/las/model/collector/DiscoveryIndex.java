package edu.ncsu.las.model.collector;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.persist.collector.DiscoveryIndexDAO;
import edu.ncsu.las.util.DateUtilities;

/**
 * DiscoveryIndex provides support for the back of the book index
 * that's available within the domain discovery sessions
 * 
 *
 */
public class DiscoveryIndex {
	private static Logger logger = Logger.getLogger(DiscoveryIndex.class.getName());
	
    private UUID _id;
    private String _domainInstanceName;
    private JSONObject _indexData;
    private String _name;
    private int _numDocuments;
    private String _ownerEmail;
    private String  _fileStorageArea;
    private Timestamp _dateCreated;	
    
	public DiscoveryIndex(UUID id, String domain, JSONObject indexData, String name, int numDocuments, String ownerEmail, String fileStorageArea, Timestamp timestamp) {
	    _id = id;
	    _domainInstanceName = domain;
	    _indexData       = indexData;
	    _name            = name;
	    _numDocuments    = numDocuments;
	    _ownerEmail      = ownerEmail;
	    _fileStorageArea =  fileStorageArea;
	    _dateCreated     = timestamp;	
	}
	
    public UUID getID() { return _id; }
    public String getDomainInstanceName() { return _domainInstanceName; }
    public JSONObject getIndexData()   { return _indexData; }
    public String getName()            { return _name; }
    public int getNumDocuments()       { return _numDocuments; }
    public String getOwnerEmail()      { return _ownerEmail; }
    public String getFileStorageArea() { return _fileStorageArea; }
    public Timestamp getDateCreated()  { return _dateCreated; }	

    public JSONObject toJSON() {
    	return new JSONObject().put("id", _id.toString())
    			               .put("domain", _domainInstanceName)
    			               .put("name", _name)
    			               .put("numDocuments", _numDocuments)
    			               .put("ownerEmail", _ownerEmail)
    			               .put("dateCreated", DateUtilities.getDateTimeISODateTimeFormat(_dateCreated.toInstant()));
    }
    
	public static boolean hasIndex(UUID indexID) {
		DiscoveryIndexDAO d = new DiscoveryIndexDAO();
		return d.exists(indexID);
	}
	
	public static JSONObject retreiveIndex(UUID indexID) {
		DiscoveryIndexDAO d = new DiscoveryIndexDAO();
		DiscoveryIndex di = d.retrieve(indexID);
		if (di != null) {
			return di._indexData;
		}
		else {
			return null;
		}
	}
	
	public boolean storeIndex() {
		DiscoveryIndexDAO d = new DiscoveryIndexDAO();
		d.delete(this.getID());
		return d.insert(this);
	}

	public boolean deleteIndex() {
		return deleteIndex(this.getID());
	}	
	
	public static boolean deleteIndex(UUID indexID) {
		DiscoveryIndexDAO d = new DiscoveryIndexDAO();
		return d.delete(indexID);
	}
	
	/**
	 * Removes all discovery index records for this domain from the database
	 * 
	 * @param domainInstanceName
	 * @return
	 */
	public static int purgeDomain(String domainInstanceName) {
		DiscoveryIndexDAO d = new DiscoveryIndexDAO();
		return d.deleteByDomain(domainInstanceName);
	}

	public static JSONArray retrieveAvailableIndexes(String domainStr, FileStorageAreaType area) {
		DiscoveryIndexDAO d = new DiscoveryIndexDAO();
		List<DiscoveryIndex> indexes = d.selectByDomainAndArea(domainStr,area.getLabel());
		JSONArray result = new JSONArray();
		for (DiscoveryIndex di: indexes) {result.put(di.toJSON()); }
		return result;
	}
	
	//TODO: Probably need to re-think this process, especially given that we will be creating a ton of these 
	//      indexes going forward.
	
	/**
	 * Runs in the background, polling for the index creation to be completed by the microservice.
	 * Once the completed index is received, the index is stored and the thread completes.
	 *  
	 * @param domain
	 * @param ownerEmail
	 * @param indexTitle
	 * @param documentIndexID
	 * @param documentArea
	 */
	public static void waitForCompletion(String domain, String ownerEmail, String indexTitle, String documentIndexID, 	String documentArea) {
		// query the python service to check if the process is complete for creating an index.  If so, store the document.
		Runnable runTask = new Runnable() {
			@Override
			public void run() {
				for (int i=0; i<200; i++) { // only check for 200 iterations before quiting.  
					try {
						logger.log(Level.INFO, "checking if discovery index has been created: "+documentIndexID);
						HttpResponse<JsonNode> jsonResponse = Unirest.get(Configuration.getConfigurationProperty(domain,ConfigurationType.TEXTRANK_API)+"index/status/"+documentIndexID.toString()).header("accept", "application/json").asJson();
						JSONObject result = jsonResponse.getBody().getObject();

						if (result.has("discoveryIndex")) {
							JSONObject discoveryIndex = result.getJSONObject("discoveryIndex");
							int numDocuments = discoveryIndex.getJSONObject("metadata").getInt("totalDocuments"); 
							DiscoveryIndex di = new DiscoveryIndex(UUID.fromString(documentIndexID), domain, discoveryIndex, indexTitle, numDocuments,  ownerEmail, documentArea, new Timestamp(System.currentTimeMillis()));
							di.storeIndex();
							break; // no need to check any more, we've got it ...
						}
					} catch (UnirestException ue) {
						logger.log(Level.SEVERE, "Unable to contact textrank service to get API", ue);
					}
					try {
						TimeUnit.SECONDS.sleep(5);
					} catch (InterruptedException e) {
						logger.log(Level.WARNING, "Unable to sleep", e);
					}
				}
				
			}
		};
		Collector.getTheCollecter().runTask(runTask);
	}		
}
