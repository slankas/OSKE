package edu.ncsu.las.model.collector;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.FileStorageStatusCode;
import edu.ncsu.las.persist.collector.VisitedPageDAO;
import edu.ncsu.las.util.DateUtilities;

/**
 * 
 *
 */
public class VisitedPage {

	private UUID _id;
	private UUID _jobHistoryID;
	private UUID _jobID;
	private Instant _visitedTimeStamp;
	private String _mimeType;
	private String _storageUUID;
	private String _sha256Hash;
	private String _status;
	private UUID _relatedID;
	private String _url;
	private String _fullTextSendResult;
	private String _domainInstanceName;
	private String _storageArea;
	
    public static enum ElasticSearchResultCode {
        SENT, 
        ALREADY_PRESENT, 
        FAILURE, 
        NOT_SENDING_COMPRESSED_FILE,
        NO_CONFIGURATION,               //The application parameters do not have a full-text engine specified
        NOT_APPLICABLE,
        NOT_SENDING;

		public static String getCode(FileStorageStatusCode jsonResult) {
			switch (jsonResult) {
				case SKIPPED:         return NOT_SENDING.toString(); 
				case SUCCESS:         return SENT.toString(); 
				case UNKNOWN_FAILURE: return FAILURE.toString(); 
				case FILE_NOT_FOUND:  return FAILURE.toString();
				case INVALID_CONTENT: return NOT_SENDING_COMPRESSED_FILE.toString();
				case NOT_EXECUTED:    return FAILURE.toString();
				case ALREADY_PRESENT: return ALREADY_PRESENT.toString();
			}
			return "vp_unknown";
		}
    } 	
	
	
	public VisitedPage() {

	}

	public VisitedPage(UUID id, UUID jobHistoryID, UUID jobID,  String url, Timestamp visitedTS,  String mimeType, String storageUUID, String sha256Hash, String status, UUID relatedID, String fullTextSendResult, String domainInstanceName, String storageArea) {
		_id = id;
		_jobHistoryID = jobHistoryID;
		_jobID        = jobID;
		_url = url;
		_visitedTimeStamp = visitedTS.toInstant();
		_mimeType = mimeType;
		_storageUUID = storageUUID;
		_sha256Hash = sha256Hash;
		_status = status;
		_relatedID = relatedID;		
		_fullTextSendResult = fullTextSendResult;
		_domainInstanceName = domainInstanceName;
		_storageArea        = storageArea;
	}
	
	public VisitedPage(UUID jobHistoryID, UUID jobID, String url, String mimeType, String storageUUID, String sha256Hash, String status, UUID relatedID, String fullTextSendResult, String domainInstanceName, String storageArea) {
		_id = edu.ncsu.las.util.UUID.createTimeUUID();
		_jobHistoryID = jobHistoryID;
		_jobID        = jobID;
		_url = url;
		_mimeType = mimeType;
		_storageUUID = storageUUID;
		_sha256Hash = sha256Hash;
		_status = status;
		_relatedID = relatedID;		
		_fullTextSendResult = fullTextSendResult;
		_domainInstanceName = domainInstanceName;
		_storageArea        = storageArea;
	}

	public UUID getID() {
		return _id;
	}

	public void setID(UUID id) {
		_id = id;
	}

	public UUID getJobHistoryID() {
		return _jobHistoryID;
	}

	public void setJobHistoryID(UUID jobID) {
		_jobHistoryID = jobID;
	}

	public UUID getJobID() {
		return _jobID;
	}

	public void setJobID(UUID jobID) {
		_jobID = jobID;
	}	
	
	public Instant getVisitedTimeStamp() {
		return _visitedTimeStamp;
	}

	public void setVisitedTimeStamp(java.sql.Timestamp visitedTimeStamp) {
		_visitedTimeStamp = visitedTimeStamp.toInstant();
	}

	public String getMimeType() {
		return _mimeType;
	}

	public void setMimeType(String mimeType) {
		_mimeType = mimeType;
	}

	public String getStorageUUID() {
		return _storageUUID;
	}

	public void setStorageUUID(String storageUUID) {
		_storageUUID = storageUUID;
	}

	public String getSHA256Hash() {	return _sha256Hash; }
	public void setSHA256Hash(String sha256Hash) {
		_sha256Hash = sha256Hash;
	}

	public String getStatus() {	return _status; }
	public void setStatus(String status) {
		_status = status;
	}

	public UUID getRelatedID() {	return _relatedID; }
	public void setRelatedID(UUID relatedID) {
		_relatedID = relatedID;
	}
	
	public String getURL() { return _url; }
	public void setURL(String url) {
		_url = url;
		
	}

	
	public String getFullTextSendResult() {
		return _fullTextSendResult;
	}
	
	public void setFullTextSendResult(String newFullTextSendResult) {
		_fullTextSendResult = newFullTextSendResult;
	}

	public String getDomainInstanceName() {
		return _domainInstanceName;
	}

	public void setDomainInstanceName(String domainInstanceName) {
		this._domainInstanceName = domainInstanceName;
	}
	
	public String getStorageArea() {
		return _storageArea;
	}
	
	public void setStorageArea(String area) {
		_storageArea = area;
	}

	public static VisitedPage findMostRecentMatch(String hashValue, String domain, FileStorageAreaType fileStorageAreaType) {
		return (new VisitedPageDAO()).findMostRecentRecord(hashValue, domain, fileStorageAreaType);
	}

	public boolean create() {
		return (new VisitedPageDAO()).insertRecord(this);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("VisitedPage {");
		sb.append(" id: "); sb.append(_id);
		sb.append(", jobID: "); sb.append(_jobHistoryID);
		sb.append(", url: "); sb.append(_url);
		sb.append(", visitTS: "); sb.append(_visitedTimeStamp);
		sb.append(", mimeType: "); sb.append(_mimeType);
		sb.append(", uuid: "); sb.append(_storageUUID);
		sb.append(", hash: "); sb.append(_sha256Hash);
		sb.append(", status: "); sb.append(_status);
		sb.append(", relatedID: "); sb.append(_relatedID);
		sb.append(" } ");
		
		return sb.toString();
	}

	public JSONObject toJSON() {
		JSONObject result = new JSONObject().put("id", this.getID().toString())
				                            .put("jobHistoryID", this.getJobHistoryID().toString())
				                            .put("jobID", this.getJobID().toString())
				                            .put("url", this.getURL())
				                            .put("mimeType", this.getMimeType())
				                            .put("storageUUID", this.getStorageUUID())
				                            .put("sha256Hash", this.getSHA256Hash())
				                            .put("status", this.getStatus())
				                            .put("relatedID", this.getRelatedID().toString())
				                            .put("fullTextSendResult", this.getFullTextSendResult());

		if (this.getVisitedTimeStamp() != null) {
			result.put("visitedTimeStamp", DateUtilities.getDateTimeISODateTimeFormat(this.getVisitedTimeStamp()));
		}
		else {
			result.put("visitedTimeStamp", "");
		}
		return result;
	}
	
	
	/**
	 * returns just the most recent records in the database
	 * 
	 * @return
	 */
	public static java.util.List<VisitedPage> getVisitedPages(String domain, String storageArea) {
		return (new VisitedPageDAO()).getVisitedPagesLimitRecords(domain,storageArea, 50);
	}
	
	
	public static java.util.List<VisitedPage> getVisitedPages(String domain, ZonedDateTime startDate, ZonedDateTime endDate, java.util.UUID jobID) {
		return (new VisitedPageDAO()).getVisitedPagesByDate(domain, startDate,endDate,jobID);
	}
	
	public static java.util.List<VisitedPage> getVisitedPages(java.util.UUID jobHistoryID) {
		return (new VisitedPageDAO()).getVisitedPagesByJobHistoryID(jobHistoryID);
	}

	public static Integer getNumberOfPagesVisited(String domainStr, String storageArea) {
		VisitedPageDAO dd = new VisitedPageDAO();
		Integer result = dd.getNumberOfPagesVisited(domainStr, storageArea);
		return result;
	}
	
	public static VisitedPage getVisitedPage(java.util.UUID id) {
		VisitedPageDAO dd = new VisitedPageDAO();
		return dd.getVisitedPageForId(id);
	}
	public static VisitedPage getVisitedPageByStorageID(java.util.UUID id) {
		VisitedPageDAO dd = new VisitedPageDAO();
		return dd.getVisitedPageForStorageId(id);
	}
	
	public static java.util.List<VisitedPage> getVisitedPages(String domain, java.util.UUID jobID) {
		return (new VisitedPageDAO()).getVisitedPagesByDate(domain, null, null,jobID);
	}
	
}
