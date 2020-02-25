package edu.ncsu.las.model.collector;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.persist.collector.DocumentBucketDAO;
import edu.ncsu.las.util.DateUtilities;

/** 
 * Represents a collection of retrieved documents from the web.  
 * This is analagous to "tagging" documents.
 * 
 */
public class DocumentBucket {

	
	public static class Collaborator {
		private String _name;
		private String _email;
		
		public Collaborator(String name, String email) {
			_name  = name;
			_email = email; 
		}
		
		public Collaborator(JSONObject obj) {
			_name  = obj.getString("name");
			_email = obj.getString("email"); 
		}
		
		public String getName() { return _name;}
		public String getEmail() { return _email;}
		public JSONObject toJSON() { return new JSONObject().put("name", _name).put("email", _email); }
	}

	public static String TAG_INVALID_CHARACTERS_REGEX = "[^a-zA-Z0-9,.:; _-]";
	private static String TAG_VALIDATION_REGEX = "^[a-zA-Z0-9,.:; _-]*$"; // the tag must only consist of these characters.  TODO: add this logic to check in Project
    public static Pattern TAG_VALIDATION_PATTERN  = Pattern.compile(TAG_VALIDATION_REGEX);

    private UUID _id;
    private String _domainInstanceName;
	private String _tag;
	private String _question;
	private String _description;
	private String _personalNotes;
	private String _ownerEmail;
    private Timestamp _dateCreated;	
	
    private java.util.List<Collaborator> _collaborators  = new java.util.ArrayList<DocumentBucket.Collaborator>();
    
	
    public DocumentBucket(UUID id, String domainInstanceName, String bucketTag, String bucketQuestion, String ownerEmail, String description, String personalNotes, Timestamp dateCreated) {
        _id = id;
        _domainInstanceName = domainInstanceName;
    	_tag            = bucketTag;
    	_question       = bucketQuestion;
    	_description    = description;
    	_personalNotes  = personalNotes;
    	_ownerEmail     = ownerEmail;
    	_dateCreated    = dateCreated;
    }
	
	
    public DocumentBucket (String domainInstanceName, String bucketTag, String bucketQuestion, String ownerEmail, String description, String personalNotes, Timestamp dateCreated) {
    	_id             =  edu.ncsu.las.util.UUID.createTimeUUID();
    	_tag            = bucketTag;
    	_question       = bucketQuestion;
        _description    = description;
        _personalNotes  = personalNotes;
        _domainInstanceName = domainInstanceName;
        _ownerEmail         = ownerEmail;
    	_dateCreated        = dateCreated;

    }
    
    public DocumentBucket(JSONObject collectionObject, UUID collectionID, String email, String domain) {
    	if (collectionID == null) { collectionID = edu.ncsu.las.util.UUID.createTimeUUID();}
    	_id             = collectionID;
        _tag            = collectionObject.getString("tag");
        _question       = collectionObject.getString("question");
        _description    = collectionObject.getString("description");
        _personalNotes  = collectionObject.getString("personalNotes");
        _dateCreated    = new Timestamp(System.currentTimeMillis());
        _ownerEmail     = email;
        _domainInstanceName = domain;
        
        JSONArray collaborators = collectionObject.optJSONArray("collaborators");
        if (collaborators != null) {
	        for (int i=0;i< collaborators.length(); i++) {
	        	_collaborators.add(new Collaborator(collaborators.getJSONObject(i)));
	        }
        }
    }
    
    public UUID getID() {
    	return _id;
    }

    public String getDomainInstanceName() {
		return _domainInstanceName;
	}
	public void setDomainInstanceName(String name) {
		_domainInstanceName = name;
	}
    
    public String getTag() {
		return _tag;
	}
	public void setTag(String newTag) {
		_tag = newTag;
	}

    public String getQuestion() {
		return _question;
	}
	public void setQuestion(String newQuestion) {
		_question = newQuestion;
	}
	
	
	public String getDescription() {
		return _description;
	}
	public void setDescription(String description) {
		_description = description;
	}
	public String getPersonalNotes() {
		return _personalNotes;
	}
	public void setPersonalNotes(String personalNotes) {
		_personalNotes = personalNotes;
	}
	
	public String getOwnerEmail() {
		return _ownerEmail;
	}
	
	public void setOwnerEmail(String email) {
		_ownerEmail = email;
	}
	
	public Timestamp getDateCreated( ){ return _dateCreated ; }
	
	public List<Collaborator> getCollabotors() {
		return new java.util.ArrayList<Collaborator>(_collaborators);
	}

	public void addCollaborator(DocumentBucket.Collaborator collaborator) {
		_collaborators.add(collaborator);
	}
	
	public static boolean isValidTag(String tag) {
		Matcher nameMatchesRegex = TAG_VALIDATION_PATTERN.matcher(tag);
		return nameMatchesRegex.matches();
	}
	
	/**
	 * Performs the necessary validation on all fields.  If an error occurs, an error message is returned.
	 * 
	 * @return the array will be empty on no errors, otherwise the appropriate error messages are added.
	 */
	public java.util.ArrayList<String> validate() {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();
		
        if (this.getTag() == null || this.getTag().length() < 1 || this.getTag().length() > 30 || !isValidTag(this.getTag())) {
            errors.add("The bucket tag is invalid.  Must be between 1 and 30 characters and composed of characters, numbers, spaces, commas, periods, colons and semi-colons only.");
        }
        if (this.getDescription() == null || this.getDescription().length() > 2000) {
            errors.add("Description cannot be longer than 2,000 characters.");
        }
        if (this.getPersonalNotes() == null || this.getPersonalNotes().length() > 10000) {
            errors.add("Personal notes cannot be longer than 10,000 characters.");
        }
		
        //TODO: need to validate collaborators are valid users.
        
		return errors;
	}
		
	public JSONObject toJSONObject() {	
		JSONArray collaboratorArray = new JSONArray();
		
		for (Collaborator c: _collaborators) {
			collaboratorArray.put(c.toJSON());
		}
		
		JSONObject result = new JSONObject().put("id",_id)
				                            .put("domain", _domainInstanceName)
				                            .put("tag", _tag)
				                            .put("question", _question)
				                            .put("description", _description)
				                            .put("personalNotes", _personalNotes)
				                            .put("owner", _ownerEmail)
				                            .put("dateCreated", DateUtilities.getDateTimeISODateTimeFormat(_dateCreated.toInstant()))
				                            .put("collaborators", collaboratorArray);
		return result;
	}
	
	/*
	public static java.util.List<DocumentCollection> getCollectionsOwned(String domain, String ownerEmail) {
		CollectionDAO cd = new CollectionDAO();
		return cd.selectByOwner(domain, ownerEmail);
	}
	*/
	
	/**
	 * Returns the collections available for a particular user. If the access control is turned on for domain, 
	 * then users must be assigned.  Otherwise, all collections will be available for the user.
	 * 
	 * @param domain
	 * @param email
	 * @return
	 */
	public static java.util.List<DocumentBucket> getAvailableCollections(String domain, String email) {
		DocumentBucketDAO cd = new DocumentBucketDAO();
		if (Configuration.getConfigurationPropertyAsBoolean(domain, ConfigurationType.DOMAIN_COLLECTION_ACCESS_REQUIRED)) {
			return cd.selectAvailableCollections(domain, email);
		}
		else {
			return cd.selectAll(domain);
		}
	}
	
	/**
	 * Returns the collections available for a particular user. If the access control is turned on for domain, 
	 * then users must be assigned.  Otherwise, all collections will be available for the user.
	 * 
	 * @param domain
	 * @param email
	 * @return
	 */
	public static java.util.List<DocumentBucket> getAllBuckets(String domain) {
		DocumentBucketDAO cd = new DocumentBucketDAO();
		return cd.selectAll(domain);
	}	
	
	public static boolean delete(UUID collectionID) {
		DocumentBucketDAO cd = new DocumentBucketDAO();
		cd.deleteCollaborators(collectionID);
		return cd.delete(collectionID);
	}
	
	/**
	 * Removes all document collection records for this domain from the database
	 * 
	 * @param domainInstanceName
	 * @return
	 */
	public static int purgeDomain(String domainInstanceName) {
		DocumentBucketDAO d = new DocumentBucketDAO();
		return d.deleteByDomain(domainInstanceName);
	}
	
	public static DocumentBucket retrieve(UUID collectionID) {
		DocumentBucketDAO cd = new DocumentBucketDAO();
		DocumentBucket collection = cd.getDocumentCollection(collectionID);
		if (collection != null) {
			collection._collaborators = cd.selectCollaborators(collectionID);
		}
		
		return collection;
	}

	/** 
	 * Method will only return a record if the user is the owner or listed as a collaborator 
	 * 
	 * @param collectionID
	 * @param email
	 * @return
	 */
	public static DocumentBucket retrieve(UUID collectionID, String email) {
		DocumentBucketDAO cd = new DocumentBucketDAO();
		DocumentBucket collection = cd.getDocumentCollection(collectionID, email);
		if (collection != null) {
			collection._collaborators = cd.selectCollaborators(collectionID);
		}
		return collection;
	}

	
	public boolean create() {
		DocumentBucketDAO cd = new DocumentBucketDAO();
		boolean result =  cd.insert(this);
		cd.storeAllCollaborators(this);
		return result;
	}
	
	public boolean update() {
		DocumentBucketDAO cd = new DocumentBucketDAO();
		boolean result = cd.update(this);
		cd.deleteCollaborators(this.getID());
		cd.storeAllCollaborators(this);
		return result;
	}

}
