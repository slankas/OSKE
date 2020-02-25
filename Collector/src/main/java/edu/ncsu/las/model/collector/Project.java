package edu.ncsu.las.model.collector;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.persist.collector.ProjectDAO;
import edu.ncsu.las.util.DateUtilities;
import edu.ncsu.las.util.StringUtilities;
import edu.ncsu.las.util.StringValidation;
import edu.ncsu.las.util.json.JSONUtilities;
import edu.ncsu.las.util.json.JSONUtilities.JSONType;

/**
 * Projects contain the outline (purpose, key questions, assumptions, and external links) of particular analysis task.
 * 
 *
 */
public class Project {	
	private static Logger logger = Logger.getLogger(Project.class.getName());
	
    private UUID _id;    // primary key
    private String _domainInstanceName;  //To what domain does this index belong to
    private String _name;
    private String _status;  // is this project active or inactive?
    private String _purpose;  //general description as to what this particular plan/project is seeking to accomplish
    private JSONArray _keyQuestions; //json array of objects with two fields: question and tag.  Each object will create a new collection (tags will determine uniqueness)';
    private JSONArray _assumptions;  //json array of strings describing the primary assumptions involved with this project
    private JSONArray _relatedURLs;  //json array of objects containing a link and title.  Used to point to external resources (mind maps, documents, sharepoint sites, etc.)
    private Timestamp _dateCreated;	
    private Timestamp _dateUpdated;	
    private String _createUserID; // who created this project?
    private String _lastUpdateUserID; // who last updated this project
    
	public Project(UUID id, String domainInstanceName,  String name, String status,  String purpose, JSONArray keyQuestions,
			       JSONArray assumptions, JSONArray relatedURLs,  Timestamp dateCreated,  Timestamp dateUpdated,  String createUserID,  String lastUpdateUserID) {
		_id = id;
		_domainInstanceName = domainInstanceName;
		_name               = name;
		_status             = status;
		_purpose            = purpose;
		_keyQuestions       = keyQuestions;
		_assumptions        = assumptions;
		_relatedURLs        = relatedURLs;
		_dateCreated        = dateCreated;
		_dateUpdated        = dateUpdated;
		_createUserID       = createUserID;
		_lastUpdateUserID   = lastUpdateUserID;
	}
	
	/** used in in create from JSONObject */
    private Project() { ;	}//no statements

	public UUID getID() { return _id; }
    public String getDomainInstanceName() { return _domainInstanceName; }
    public String getName () { return _name;}
    public String getStatus () { return _status;}  
    public String getPurpose () { return _purpose;}  
    public JSONArray getKeyQuestions () { return _keyQuestions;} 
    public JSONArray getAssumptions () { return _assumptions;}  
    public JSONArray getRelatedURLs () { return _relatedURLs;}  
    public Timestamp getDateCreated() { return _dateCreated;}	
    public Timestamp getdateUpdated() { return _dateUpdated;}	
    public String getCreateUserID() { return _createUserID;} 
    public String getLastUpdateUserID() { return _lastUpdateUserID;} 

    public void setID(UUID newValue) {  _id = newValue; }
    public void setDomainInstanceName(String newValue) {  _domainInstanceName = newValue; }
    public void setName (String newValue) {  _name = newValue;}
    public void setStatus (String newValue) {  _status = newValue;}  
    public void setPurpose (String newValue) {  _purpose = newValue;}  
    public void setKeyQuestions (JSONArray newValue) {  _keyQuestions = newValue;} 
    public void setAssumptions (JSONArray newValue) {  _assumptions = newValue;}  
    public void setRelatedURLs (JSONArray newValue) {  _relatedURLs = newValue;}  
    public void setDateCreated(Timestamp newValue) {  _dateCreated = newValue;}	
    public void setdateUpdated(Timestamp newValue) {  _dateUpdated = newValue;}	
    public void setCreateUserID(String newValue) {  _createUserID = newValue;} 
    public void setLastUpdateUserID(String newValue) {  _lastUpdateUserID = newValue;}    

    /* List of fields that must be present in the JSON object to create */
    private static String[] REQUIRED_JSON_FIELDS = {  "name", "status", "purpose", "keyQuestions", "assumptions", "relatedURLs" };
    
    /**
     *
     * @param source
     * @param domain Caller need to validate that this is valid for the user.
     * @param userID
     * @return
     * @throws ValidationException 
     */
    public static Project create(JSONObject source, String domain, String userID) throws ValidationException {
    	Project p = new Project();
    	
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

    	if (source.has("status")) {
    		String status = source.getString("status").trim();
    		if (status.equals("active") || status.equals("inactive")) {
    			p.setStatus(status);
    		}
    		else {
    			errors.add("The status value can only be 'active' or 'inactive'.");
    		}
    	}

    	if (source.has("purpose")) {
    		String purpose = source.getString("purpose").trim();
    		if ( purpose.length() < 4096 && !StringValidation.containsHTML(purpose) ) {
    			p.setPurpose(purpose);
    		} else {
    			errors.add("The purpose must be less than 4,096 characters. HTML is not allowed.");
    		}
    	}
    	
    	if (source.has("assumptions")) {
    		if (JSONUtilities.getJSONType(source.get("assumptions")) == JSONType.JSON_ARRAY) {
	    		// need to validate that each is a string.  CSS attack potential
	    		JSONArray a = source.getJSONArray("assumptions");
	    		for (int i=0; i <a.length(); i++) {
	    			JSONType jt = JSONUtilities.getJSONType(a.get(i));
	    			if (jt.equals(JSONType.JSON_OBJECT) || jt.equals(JSONType.JSON_ARRAY)) {
	    				errors.add("Invalid assumption - must be a string: "+a.get(i));
	    			}
	    			else if (StringValidation.containsHTML(a.getString(i))) {
						errors.add("Assumption can not contain HTML: "+a.get(i));
					}
	    		}
	    		p.setAssumptions(source.getJSONArray("assumptions"));
    		}
    		else {
    			errors.add("assumptions must be a JSONArray of strings");
    			p.setAssumptions(new JSONArray());
    		}
    	}
    	
    	if (source.has("keyQuestions")) {
    		// need to validate that each is a string.  CSS attack potential
    		// double check the format of the key questions.  JSONobject, 2 fields: question and tag. 
    		if (JSONUtilities.getJSONType(source.get("keyQuestions")) == JSONType.JSON_ARRAY) {
	    		JSONArray a = source.getJSONArray("keyQuestions");
	    		for (int i=0; i <a.length(); i++) {
	    			JSONType jt = JSONUtilities.getJSONType(a.get(i));
	    			if (!jt.equals(JSONType.JSON_OBJECT)) {
	    				errors.add("Invalid key question - not a valid JSONObject with two fields (question and tag): "+a.get(i));
	    			}
	    			else {
	    				JSONObject jo = a.getJSONObject(i);
	    				if (jo.length() != 2 || !jo.has("question") || !jo.has("tag")) {
	    					errors.add("Invalid key question - not a valid JSONObject with two fields (question and tag): "+a.get(i));
	    				}
	
	    				if (StringValidation.containsHTML(jo.getString("question")) || StringValidation.containsHTML(jo.getString("tag"))) {
	    					errors.add("Questions and tags can not contain HTML: "+a.get(i));
	    				}	    				
	    				if (jo.getString("tag").length() > 30) {
	    					errors.add("The bucket tag is invalid.  Must be between 1 and 30 characters and composed of characters, numbers, spaces, commas, periods, colons and semi-colons only.");
	    				}
	    				if (!DocumentBucket.isValidTag(jo.getString("tag"))) {
	    					errors.add("The bucket tag is invalid.  Must be between 1 and 30 characters and composed of characters, numbers, spaces, commas, periods, colons and semi-colons only.");
	    				}
	    				if (jo.getString("question").length() > 255) {
	    					errors.add("Questions can only be up to 255 characters long: "+jo.getString("question"));
	    				}
	    			}
	    		}
	    		p.setKeyQuestions(a);
    		} else {
    			errors.add("keyQuestions must be a JSON array.");
    			p.setKeyQuestions(new JSONArray());
    		}
    	}

    	if (source.has("relatedURLs")) {
    		if (JSONUtilities.getJSONType(source.get("relatedURLs")) == JSONType.JSON_ARRAY) {
	    		JSONArray a = source.getJSONArray("relatedURLs");
	    		for (int i=0; i <a.length(); i++) {
	    			JSONType jt = JSONUtilities.getJSONType(a.get(i));
	    			if (!jt.equals(JSONType.JSON_OBJECT)) {
	    				errors.add("Invalid URL object - not a valid JSONObject with two fields (link and title): "+a.get(i));
	    			}
	    			else {
	    				JSONObject jo = a.getJSONObject(i);
	    				if (jo.length() != 2 || !jo.has("link") || !jo.has("title")) {
	    					errors.add("Invalid URL object - not a valid JSONObject with two fields (link and title): "+a.get(i));
	    				}
	
	    				if (StringValidation.containsHTML(jo.getString("title"))) {
	    					errors.add("Titles can not contain HTML: "+a.get(i));
	    				}
	    				
	    				if (!StringValidation.isValidURL(jo.getString("link"))) {
	    					errors.add("Links can only contain a hyperlink: "+a.get(i));
	    				}
	    			}
	    		}   		
	    		
	    		
	    		p.setRelatedURLs(a);
    		}
    		else {
    			errors.add("relatedURLs must be a JSONArray");
    			p.setRelatedURLs(new JSONArray());
    		}
    	}
    	
    	
    	long currentTimeMillis = System.currentTimeMillis();
    	p.setDateCreated(new java.sql.Timestamp(currentTimeMillis));
    	p.setdateUpdated(new java.sql.Timestamp(currentTimeMillis));
    	
    	p.setCreateUserID(userID);
    	p.setLastUpdateUserID(userID);
    	
    	if (errors.size() > 0) {
    		// create an exception and throw...
    		throw new ValidationException("Unable to create project - errors", errors);
    	}
    	
    	return p;
    }
    
    public JSONObject toJSON() {
    	return new JSONObject().put("id", _id.toString())
    			               .put("domain", _domainInstanceName)
    			               .put("name", _name)
    			               .put("status", _status)
    			               .put("purpose", _purpose)
    			               .put("keyQuestions", _keyQuestions)
    			               .put("assumptions", _assumptions)
    			               .put("relatedURLs", _relatedURLs)
    			               .put("dateCreated", DateUtilities.getDateTimeISODateTimeFormat(_dateCreated.toInstant()))
    	    			       .put("dateUpdated", DateUtilities.getDateTimeISODateTimeFormat(_dateUpdated.toInstant()))
    	    			       .put("createUserID",_createUserID)
    			               .put("lastUpdateUserID",_lastUpdateUserID);
    }
    
	public static Project retrieve(UUID projectID) {
		return (new ProjectDAO()).retrieve(projectID);
	}
	
	public boolean create() {
		return (new ProjectDAO()).insert(this);
	}

	public boolean update() {
		return (new ProjectDAO()).update(this);
	}

	/**
	 * Creates documentBuckets, a default discovery session by name, discovery sessions by question, and a document by name
	 * @param domainStr
	 * @param userID
	 * @returns a json object that contains corresponding fields pointing to the other items created
	 * 
	 */
	public JSONObject createAncillaryObjects(String domainStr, String userID) {
		JSONObject results = new JSONObject();
		results.put("documentBuckets",   this.createDocumentBuckets(domainStr, userID));
		results.put("discoverySessions", this.createCorrespondingDiscoverySessions(domainStr, userID));
		results.put("projectDocuments",  this.createCorrespondingDocument(domainStr, userID));
		return results;
	}
		
	
	/**
	 * From a given project plan, create document buckets for the key questions.
	 * 
	 * @param domain
	 * @param userID
	 * @return false if any create operation fails
	 */
	public JSONArray createDocumentBuckets(String domain, String userID) {
		java.util.List<DocumentBucket> buckets = DocumentBucket.getAllBuckets(domain);
		java.util.Map<String, DocumentBucket> bucketMap = buckets.stream().collect(Collectors.toMap(DocumentBucket::getTag, b -> b));
		
		JSONArray result = new JSONArray();
		for (int i=0;i<_keyQuestions.length();i++) {
			JSONObject kq = _keyQuestions.getJSONObject(i);
			
			String tag = kq.getString("tag");
			if (!bucketMap.containsKey(tag)) {
				DocumentBucket db = new DocumentBucket(domain, tag, kq.getString("question"), userID, "", "", new java.sql.Timestamp(System.currentTimeMillis()));
				db.create();
				result.put(db.toJSONObject());
				bucketMap.put(tag, db);				
			}
			else {
				DocumentBucket db = bucketMap.get(tag);
				if (db.getQuestion() == null || db.getQuestion().equals("")) {  //bucket was created on a search result - claim this question to it.
					db.setQuestion(kq.getString("question"));
					db.update();
				}
			}
		}
		
		return result;
	}
	
	/**
	 * From a given project plan, create an overall discovery session, as well as ones for each question
	 * 
	 * @param domain
	 * @param userID
	 * @return false if any create operation fails
	 */
	public JSONArray createCorrespondingDiscoverySessions(String domain, String userID) {
		java.util.List<DomainDiscoverySession> sessions = DomainDiscoverySession.getAllSessionsForDomain(domain);
		java.util.Set<String> sessionNames = sessions.stream().map( s -> s.getSessionName()).collect(Collectors.toSet());
		
		JSONArray result = new JSONArray();
		String name = this.getName().replaceAll(StringUtilities.IMPROPER_NAME_REGEX, "");
		if (name.length() > 0 && !sessionNames.contains(name)) {
			DomainDiscoverySession dds = DomainDiscoverySession.createSession(domain, name, userID);
			result.put(dds.toJSON());
			sessionNames.add(name);
		}
		
		for (int i=0;i<_keyQuestions.length();i++) {
			JSONObject kq = _keyQuestions.getJSONObject(i);
			String question = kq.getString("question").replaceAll(StringUtilities.IMPROPER_NAME_REGEX, "");
			if (question.length() > 0) {
				String possibleDiscoverySessionName = question;
				if (name.length() > 0) {
					possibleDiscoverySessionName = name+": "+question;
				}
				if (!sessionNames.contains(possibleDiscoverySessionName)) {
					DomainDiscoverySession dds = DomainDiscoverySession.createSession(domain, possibleDiscoverySessionName, userID);
					result.put(dds.toJSON());
					sessionNames.add(possibleDiscoverySessionName);
				}
			}
		}
		
		return result;
	}	
	
	
	/**
	 * From a given project plan, create a project document if the name does not already exist
	 * 
	 * @param domain
	 * @param userID
	 * @return false if any create operation fails
	 */
	public JSONArray createCorrespondingDocument(String domain, String userID) {
		java.util.List<ProjectDocument> documents = ProjectDocument.retrieveAllProjectsByName(domain, this.getName());
		JSONArray result = new JSONArray();
		
		if (documents.size() == 0) {
			JSONObject blankProjectDocument = new JSONObject().put("name", this.getName()).put("status", "active").put("contents", "");
			try {
				ProjectDocument pd = ProjectDocument.create(blankProjectDocument, domain, userID);
				pd.create();
				result.put(pd.toJSON());
			}
			catch (ValidationException ve) {
				logger.log(Level.SEVERE,"Unable to create default project - possible code issue",ve);
			}
		}
		else {
			result.put(documents.get(0).toJSON());
		}
		return result;
	}	
	
	
	public boolean delete() {
		return Project.deleteProject(this.getID());
	}
	
	public static boolean deleteProject(UUID id) {
		return (new ProjectDAO()).delete(id);
	}
	
	public static boolean updateStatus(UUID projectID, String newStatus, String userID) {
		return (new ProjectDAO()).updateStatus(projectID, newStatus, userID);
	}	
	
	/**
	 * Removes all projects records for this domain from the database
	 * 
	 * @param domainInstanceName
	 * @return
	 */
	public static int purgeDomain(String domainInstanceName) {
		return (new ProjectDAO()).deleteByDomain(domainInstanceName);
	}

	public static List<Project> retrieveAvailableProjects(String domainStr) {
		return (new ProjectDAO()).selectByDomain(domainStr);
	}

	public static JSONArray retrieveAvailableProjectsAsJSONArray(String domainStr) {
		List<Project> projects = Project.retrieveAvailableProjects(domainStr);
	    JSONArray jsonArray = new JSONArray();

	    projects.stream().forEach(project -> jsonArray.put(project.toJSON()));
	    return jsonArray;
	}



}
