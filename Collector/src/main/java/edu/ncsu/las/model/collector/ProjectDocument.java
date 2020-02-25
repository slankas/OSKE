package edu.ncsu.las.model.collector;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.persist.collector.ProjectDocumentDAO;
import edu.ncsu.las.util.DateUtilities;
import edu.ncsu.las.util.StringValidation;

/**
 * ProjectDocument represents a scratchpad
 * 
 */
public class ProjectDocument {	
	//private static Logger logger = Logger.getLogger(Project.class.getName());
	
    private UUID _id;    // primary key
    private String _domainInstanceName;  //To what domain does this index belong to
    private String _name;
    private String _status;
    private String _contents;
    private Timestamp _dateCreated;	
    private Timestamp _dateUpdated;	
    private String _createUserID; // who created this project?
    private String _lastUpdateUserID; // who last updated this project
    
	public ProjectDocument(UUID id, String domainInstanceName,  String name, String status,  String contents,  Timestamp dateCreated,  Timestamp dateUpdated,  String createUserID,  String lastUpdateUserID) {
		_id = id;
		_domainInstanceName = domainInstanceName;
		_name               = name;
		_status             = status;
		_contents           = contents;
		_dateCreated        = dateCreated;
		_dateUpdated        = dateUpdated;
		_createUserID       = createUserID;
		_lastUpdateUserID   = lastUpdateUserID;
	}
	
	/** used in in create from JSONObject */
    private ProjectDocument() { ;	}//no statements

	public UUID getID() { return _id; }
    public String getDomainInstanceName() { return _domainInstanceName; }
    public String getName () { return _name;}
    public String getStatus () { return _status;}  
    public String getContents () { return _contents;}  
    public Timestamp getDateCreated() { return _dateCreated;}	
    public Timestamp getdateUpdated() { return _dateUpdated;}	
    public String getCreateUserID() { return _createUserID;} 
    public String getLastUpdateUserID() { return _lastUpdateUserID;} 

    public void setID(UUID newValue) {  _id = newValue; }
    public void setDomainInstanceName(String newValue) {  _domainInstanceName = newValue; }
    public void setName (String newValue) {  _name = newValue;}
    public void setStatus (String newValue) {  _status = newValue;}  
    public void setContents (String newValue) {  _contents = newValue;}  
    public void setDateCreated(Timestamp newValue) {  _dateCreated = newValue;}	
    public void setDateUpdated(Timestamp newValue) {  _dateUpdated = newValue;}	
    public void setCreateUserID(String newValue) {  _createUserID = newValue;} 
    public void setLastUpdateUserID(String newValue) {  _lastUpdateUserID = newValue;}    

    /* List of fields that must be present in the JSON object to create */
    private static String[] REQUIRED_JSON_FIELDS = {  "name", "status", "contents"};
    
    /**
     *
     * @param source
     * @param domain Caller need to validate that this is valid for the user.
     * @param userID
     * @return project document object.  note: this object is not saved to the database.
     * @throws ValidationException 
     */
    public static ProjectDocument create(JSONObject source, String domain, String userID) throws ValidationException {
    	ProjectDocument p = new ProjectDocument();
    	
    	List<String> errors = new java.util.ArrayList<String>();
    	try {
	    	if (source.has("id")) { p.setID(UUID.fromString(source.getString("id"))); }
	    	else { p.setID(edu.ncsu.las.util.UUID.createTimeUUID()); }
    	}
    	catch (IllegalArgumentException e ) {
    		errors.add("Invalid id format.  must be valid UUID.");
    	}
    	
    	//Validate all required fields are present
    	for (String field: REQUIRED_JSON_FIELDS) {
    		if (!source.has(field)) {
    			errors.add(field +" must be present");
    		}
    	}
    	
   		p.setDomainInstanceName(domain);

    	if (source.has("name")) {
    		String name = source.getString("name").trim();
    		if (name.length() > 0 && name.length() < 256 && !StringValidation.containsHTML(name)) {
    			p.setName(name);
    		} else {
    			errors.add("The name field must be present, less than 256 characters, and no HTML.");
    		}
    	}
    	
    	String contents = source.optString("contents");
    	contents = StringValidation.removeNonStandardHTMLFromString(contents);  //custom  policy needs to exist such that we can maintain certain meta-elements in the HTML editor.
    	p.setContents(contents);

    	if (source.has("status")) {
    		String status = source.getString("status").trim();
    		if (status.equals("active") || status.equals("inactive")) {
    			p.setStatus(status);
    		}
    		else {
    			errors.add("The status value can only be 'active' or 'inactive'.");
    		}
    	}   	
    	
    	long currentTimeMillis = System.currentTimeMillis();
    	p.setDateCreated(new java.sql.Timestamp(currentTimeMillis));
    	p.setDateUpdated(new java.sql.Timestamp(currentTimeMillis));
    	
    	p.setCreateUserID(userID);
    	p.setLastUpdateUserID(userID);
    	
    	if (errors.size() > 0) {
    		// create an exception and throw...
    		throw new ValidationException("Unable to create project document - errors", errors);
    	}
    	
    	return p;
    }
    
    public JSONObject toJSON() {
    	return new JSONObject().put("id", _id.toString())
    			               .put("domain", _domainInstanceName)
    			               .put("name", _name)
    			               .put("status", _status)
    			               .put("contents", _contents)
    			               .put("dateCreated", DateUtilities.getDateTimeISODateTimeFormat(_dateCreated.toInstant()))
    	    			       .put("dateUpdated", DateUtilities.getDateTimeISODateTimeFormat(_dateUpdated.toInstant()))
    	    			       .put("createUserID",_createUserID)
    			               .put("lastUpdateUserID",_lastUpdateUserID);
    }
    
	public static ProjectDocument retrieve(UUID projectID) {
		return (new ProjectDocumentDAO()).retrieve(projectID);
	}
	
	public boolean create() {
		return (new ProjectDocumentDAO()).insert(this);
	}

	public boolean update(String userID) {
		this.setLastUpdateUserID(userID);
		this.setDateUpdated(new java.sql.Timestamp(System.currentTimeMillis()));
		return (new ProjectDocumentDAO()).update(this);
	}
	
	public boolean delete() {
		return ProjectDocument.deleteProject(this.getID());
	}
	
	public static boolean deleteProject(UUID id) {
		return (new ProjectDocumentDAO()).delete(id);
	}
	
	public static boolean updateStatus(UUID projectID, String newStatus, String userID) {
		return (new ProjectDocumentDAO()).updateStatus(projectID, newStatus, userID);
	}	
	
	/**
	 * Removes all projects records for this domain from the database
	 * 
	 * @param domainInstanceName
	 * @return
	 */
	public static int purgeDomain(String domainInstanceName) {
		return (new ProjectDocumentDAO()).deleteByDomain(domainInstanceName);
	}

	public static List<ProjectDocument> retrieveAvailableProjects(String domainStr) {
		return (new ProjectDocumentDAO()).selectByDomain(domainStr);
	}

	public static List<ProjectDocument> retrieveAllProjectsByName(String domainStr, String name) {
		return (new ProjectDocumentDAO()).selectByDomainAndName(domainStr,name);
	}
	
	public static JSONArray retrieveAvailableProjectsAsJSONArray(String domainStr) {
		List<ProjectDocument> documents = ProjectDocument.retrieveAvailableProjects(domainStr);
	    JSONArray jsonArray = new JSONArray();

	    documents.stream().forEach(document -> jsonArray.put(document.toJSON()));
	    return jsonArray;
	}


	
	
}
