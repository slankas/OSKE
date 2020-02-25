package edu.ncsu.las.model.collector;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.EmailClient;
import edu.ncsu.las.persist.collector.UserAgreementDAO;
import edu.ncsu.las.util.DateUtilities;
import edu.ncsu.las.util.crypto.HMAC_SHA1Signature;

import org.json.JSONObject;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */

public class UserAgreement {
    private static Logger logger = Logger.getLogger(Job.class.getName());

    public static final String STATE_REVIEW   = "review";
    public static final String STATE_APPROVED = "approved";
    public static final String STATE_REVOKED  = "revoked";
    public static final String STATE_DENIED   = "denied";
    public static final String STATE_EXPIRED  = "expired";
    public static final String STATE_REWORK   = "rework";
    
    private String _emailId;
    private Timestamp _agreementTimestamp;
    private int _agreementVersion;
    private String _status;
    private Timestamp _statusTimestamp;
    private Timestamp _expirationTimestamp;
    private String _answers;
    private String _userOrganization;
    private String _userSignature;
    private String _digitalSignatureHash;
    private String _adjudicatorComments;
    private String _adjudicatorEmailID;

    public UserAgreement(String emailId, Timestamp agreementTimestamp, int agreementVersion, String status, Timestamp statusTimestamp, Timestamp expirationTimestamp, String answers, String userOrganization, String userSignature, String digitalSignatureHash, String adjudicatorComments, String adjudicatorEmailID){

        _emailId = emailId;
        _agreementTimestamp = agreementTimestamp;
        _agreementVersion = agreementVersion;
        _status = status;
        _statusTimestamp = statusTimestamp;
        _expirationTimestamp = expirationTimestamp;
        _answers = answers;
        _userOrganization = userOrganization;
        _userSignature = userSignature;
        _digitalSignatureHash = digitalSignatureHash;
        _adjudicatorComments = adjudicatorComments;
        _adjudicatorEmailID = adjudicatorEmailID;

    }

    public UserAgreement(){
        _emailId = "administratorOpenke";
        _agreementTimestamp = new Timestamp(System.currentTimeMillis());
        _agreementVersion = -1;
        _status = UserAgreement.STATE_APPROVED;
        _statusTimestamp = new Timestamp(System.currentTimeMillis());
        _expirationTimestamp = new Timestamp(System.currentTimeMillis());
        _answers = "{}";
        _userOrganization = "";
        _userSignature = "";
        _digitalSignatureHash = "";
        _adjudicatorComments = "";
        _adjudicatorEmailID = "";    	
    }
    

    /*
    public void setEmailId(String emailId){ this._emailId = emailId; }

    public void setAgreementTimestamp(Timestamp agreementTimestamp){ this._agreementTimestamp = agreementTimestamp; }

    public void setAgreementVersion(int agreementVersion){ this._agreementVersion = agreementVersion; }

    public void setStatus(String status){ this._status = status; }

    public void setStatusTimestamp(Timestamp statusTimestamp){ this._statusTimestamp = statusTimestamp; }

    public void setExpirationTimestamp(Timestamp expirationTimestamp){ this._expirationTimestamp = expirationTimestamp; }

    public void setAnswers(String answers){ this._answers = answers; }

    public void setUserOrganization(String userOrganization){ this._userOrganization = userOrganization; }

    public void setUserSignature(String userSignature){ this._userSignature = userSignature; }

    public void setDigitalSignatureHash(String digitalSignatureHash){ this._digitalSignatureHash = digitalSignatureHash; }

    public void setAdjudicatorComments(String adjudicatorComments){ this._adjudicatorComments = adjudicatorComments; }

    public void setAdjudicatorEmailid(String adjudicatorEmailid){ this._adjudicatorEmailid = adjudicatorEmailid; }
	*/
    
    public String getEmailId(){ return _emailId; }

    public Timestamp getAgreementTimestamp(){ return _agreementTimestamp; }

    public int getAgreementVersion(){ return _agreementVersion; }

    public String getStatus(){ return _status; }

    public Timestamp getStatusTimestamp(){ return _statusTimestamp; }

    public Timestamp getExpirationTimestamp(){ return _expirationTimestamp; }

    public String getAnswers(){ return _answers; }

    public String getUserOrganization(){ return _userOrganization; }

    public String getUserSignature(){ return _userSignature; }

    public String getDigitalSignatureHash(){ return _digitalSignatureHash; }

    public String getAdjudicatorComments(){ return _adjudicatorComments; }

    public String getAdjudicatorEmailID(){ return _adjudicatorEmailID; }


    public JSONObject toJSON()  {
        JSONObject result = new JSONObject().put("emailId", _emailId)
                .put("agreementTimestamp", DateUtilities.getDateTimeISODateTimeFormat(_agreementTimestamp.toInstant()))
                .put("agreementVersion", _agreementVersion)
                .put("status", _status)
                .put("statusTimestamp", DateUtilities.getDateTimeISODateTimeFormat(_statusTimestamp.toInstant()))
                .put("expirationTimestamp", DateUtilities.getDateTimeISODateTimeFormat(_expirationTimestamp.toInstant()))
                .put("answers", new JSONObject(_answers))
                .put("userOrganization", _userOrganization)
                .put("userSignature", _userSignature)
                .put("digitalSignatureHash", _digitalSignatureHash)
                .put("adjudicatorComments", _adjudicatorComments)
                .put("adjudicatorEmailID", _adjudicatorEmailID);

        return result;
    }


	public void signAgreement() {
        String agreementDocument = this.getEmailId()+":"+ this.getAgreementTimestamp() + ":" +this.getAnswers() + ":"+ this.getAgreementVersion() + ":" + this.getUserOrganization() + ":" + this.getUserSignature(); 
        
        this._digitalSignatureHash = HMAC_SHA1Signature.signToBase64(agreementDocument, this.getUserSignature());
		
	}

    public boolean submit(){
        logger.log(Level.FINER, "UserAgreement: inserting user agreement signed by user.");
        UserAgreementDAO uad = new UserAgreementDAO();
        
        return uad.insertUserAgreement(this);
    }
    
    public void notifyAdjudicatorsOfSubmission() {
        String adjudicators[] = User.getAdjudicatorEmails(Domain.DOMAIN_SYSTEM);
        
        String title = "OpenKE User Agreement signed by " + this.getUserSignature();
        
        StringBuilder body = new StringBuilder();
        body.append("User signed agreement with following details: ");
        body.append("<br>User: " + this.getUserSignature());
        body.append("<br>User Email Id: " + this.getEmailId());
        body.append("<br>User organization: " + this.getUserOrganization());
        body.append("<br>Answers:" + (new JSONObject(this.getAnswers())).toString(4) );
        
        //TODO: need to link the adjudidcators to the review page
        
        try {
            EmailClient ec = Collector.getTheCollecter().getEmailClient();
            ec.sendMessage(Arrays.asList(adjudicators),new java.util.ArrayList<String>(),new java.util.ArrayList<String>(), title, body.toString());
        } catch(Exception e) {
        	logger.log(Level.WARNING, "unable to send adjudicator email: "+e.toString());
        }
    }
    
    /**
     * Called when an adjudicator denies a user's agreement form
     * 
     * @param adjudicatorEmailId
     * @param adjudicatorComments
     */
	public boolean deny(String adjudicatorEmailId, String adjudicatorComments) {
	    if ((new UserAgreementDAO()).updateAgreementStatus(this.getEmailId(), this.getAgreementTimestamp(),adjudicatorComments,adjudicatorEmailId,STATE_DENIED)) {
	        String title = "OpenKE: User Agreement Request Denied";
	        StringBuilder body = new StringBuilder();
	        body.append("Adjudicator has given following comments:");
	        body.append("<br>Adjudicator Email Id: " + adjudicatorEmailId);
	        body.append("<br>Adjudicator comments: " + adjudicatorComments);

	        try {
	        	Collector.getTheCollecter().getEmailClient().sendMessage(this.getEmailId(), title, body.toString());
	        } catch(Exception e){
	            logger.log(Level.WARNING, "Unable to send agreement denied to user("+this.getEmailId()+"): "+ e.toString());
	        }
	    	
	    	return true;
	    }
	    else {
	    	return false;
	    }
	}
    
    /**
     * Called when an adjudicator marks a user's agreement form as need to be "reworked" (which really amounts to a "deny" now
     * 
     * @param adjudicatorEmailId
     * @param adjudicatorComments
     */
	public boolean rework(String adjudicatorEmailID, String adjudicatorComments) {
	    if ((new UserAgreementDAO()).updateAgreementStatus(this.getEmailId(), this.getAgreementTimestamp(),adjudicatorComments,adjudicatorEmailID,STATE_REWORK)) {
	        String title = "OpenKE: User Agreement Request Needs Rework";
	        StringBuilder body = new StringBuilder();
	        body.append("Adjudicator has given following comments:");
	        body.append("<br>Adjudicator Email Id: " + adjudicatorEmailID);
	        body.append("<br>Adjudicator comments: " + adjudicatorComments);
	        body.append("<br>&nbsp;<br>&nbsp;");
	        body.append("<br>User: " + this.getUserSignature());
	        body.append("<br>User Email Id: " + this.getEmailId());
	        body.append("<br>User organization: " + this.getUserOrganization());
	        body.append("<br>Answers:" + (new JSONObject(this.getAnswers())).toString(4) );
	        

	        try {
	        	Collector.getTheCollecter().getEmailClient().sendMessage(this.getEmailId(), title, body.toString());
	        } catch(Exception e){
	            logger.log(Level.WARNING, "Unable to send agreement denied to user("+this.getEmailId()+"): "+ e.toString());
	        }
	    	
	    	return true;
	    }
	    else {
	    	return false;
	    }
	}	
	
    /**
     * Called when an adjudicator marks a user's agreement form to be revoked.
     * The expireation time is changed to now and the state is updated to be revoked.
     * 
     * @param adjudicatorEmailId
     * @param adjudicatorComments
     */
	public boolean revoke(String adjudicatorEmailID, String adjudicatorComments) {
	    if (((new UserAgreementDAO()).changeExpirationTimestampForUser(this.getEmailId(), this.getAgreementTimestamp(), new java.sql.Timestamp(System.currentTimeMillis()))) &&	
	    	(new UserAgreementDAO()).updateAgreementStatus(this.getEmailId(), this.getAgreementTimestamp(),adjudicatorComments,adjudicatorEmailID,STATE_REVOKED)) {
	        String title = "OpenKE: User Agreement Revoked";
	        StringBuilder body = new StringBuilder();
	        body.append("Adjudicator has given following comments:");
	        body.append("<br>Adjudicator Email Id: " + adjudicatorEmailID);
	        body.append("<br>Adjudicator comments: " + adjudicatorComments);
	        body.append("<br>&nbsp;<br>&nbsp;");
	        body.append("<br>User: " + this.getUserSignature());
	        body.append("<br>User Email Id: " + this.getEmailId());
	        body.append("<br>User organization: " + this.getUserOrganization());
	        body.append("<br>Answers:" + (new JSONObject(this.getAnswers())).toString(4) );
	        

	        try {
	        	Collector.getTheCollecter().getEmailClient().sendMessage(this.getEmailId(), title, body.toString());
	        } catch(Exception e){
	            logger.log(Level.WARNING, "Unable to send agreement denied to user("+this.getEmailId()+"): "+ e.toString());
	        }
	    	
	    	return true;
	    }
	    else {
	    	return false;
	    }
	}		

    /**
     * Called when an adjudicator changes the expiration time of a user agreement.
     * 
     * @param adjudicatorEmailId
     * @param newExpirationTime
     */
	public boolean changeExpirationTime(String adjudicatorEmailID, Timestamp newExpiration) {
	    if (((new UserAgreementDAO()).changeExpirationTimestampForUser(this.getEmailId(), this.getAgreementTimestamp(), newExpiration )) &&	
	    	(new UserAgreementDAO()).updateAgreementStatus(this.getEmailId(), this.getAgreementTimestamp(),this.getAdjudicatorComments(),adjudicatorEmailID,this.getStatus())) {
	        String title = "OpenKE: User Agreement Expiration Date Change";
	        StringBuilder body = new StringBuilder();
	        body.append("Your OpenKE User Agreement now expires at ");
	        body.append(DateUtilities.getDateTimeISODateTimeFormat(newExpiration.toInstant()));
	        body.append("<br>&nbsp;<br>&nbsp;");
	        body.append("<br>User: " + this.getUserSignature());
	        body.append("<br>User Email Id: " + this.getEmailId());
	        body.append("<br>User organization: " + this.getUserOrganization());
	        body.append("<br>Answers:" + (new JSONObject(this.getAnswers())).toString(4) );
	        

	        try {
	        	Collector.getTheCollecter().getEmailClient().sendMessage(this.getEmailId(), title, body.toString());
	        } catch(Exception e){
	            logger.log(Level.WARNING, "Unable to send expiration change email to user("+this.getEmailId()+"): "+ e.toString());
	        }
	    	
	    	return true;
	    }
	    else {
	    	return false;
	    }
	}	
	
	
	
	/**
	 * Called when the adjudicator approves an user agreement....
	 * 
	 * @param adjudicatorEmailID userID of the adjudicator who approved 
	 * @return
	 */
	public boolean markApproved(String adjudicatorEmailID) {
	    if ((new UserAgreementDAO()).updateAgreementStatus(this.getEmailId(), this.getAgreementTimestamp(),"",adjudicatorEmailID,STATE_APPROVED)) {
	        String title = "OpenKE: User Agreement Approved";
	        StringBuilder body = new StringBuilder();
	        body.append("Adjudicator has approved your user agreement");

	        try {
	        	Collector.getTheCollecter().getEmailClient().sendMessage(this.getEmailId(), title, body.toString());
	        } catch(Exception e){
	            logger.log(Level.WARNING, "Unable to send agreement approved message to user("+this.getEmailId()+"): "+ e.toString());
	        }
	    	
	    	return true;
	    }
	    else {
	    	return false;
	    }
	}

	
    /**
     * Checks if the current this user agreement will expire in the next x days or not
     * will also return true if it has already expired
     * @return 
     */
    public boolean willExpire(int days) {
    	Instant expiration = _expirationTimestamp.toInstant();
    	expiration = expiration.minus(days, ChronoUnit.DAYS);
    	
    	return (expiration.isBefore(Instant.now()));
    }

    
    /**
     *
     * @param emailId
     * @return null if the user agreement was not found, otherwise the agreement object is returned
     */
    public static UserAgreement getMostRecentUserAgreement(String emailID) {
        logger.log(Level.INFO, "UserAgreement: getting most recent user agreement for user. ");
        UserAgreementDAO uad = new UserAgreementDAO();
        try {
            return uad.getMostRecentUserAgreement(emailID);
        }
        catch (Exception e) {
            return null;   // assume no user agreement returned.
        }
    }

    /**
    *
    * @param emailId
    * @return null if the user agreement was not found, otherwise the agreement object is returned
    */
   public static List<UserAgreement> getAllUserAgreementsForUser(String emailID) {
       logger.log(Level.INFO, "UserAgreement: getting most recent user agreement for user. ");
       UserAgreementDAO uad = new UserAgreementDAO();
       
       return uad.getAllUserAgreementsForUser(emailID);
   }
    
    
    /**
     * Finds a current record that has not expired and is approved for the user identified by the emailID 
     * 
     * @param emailID
     * 
     * @return userAgreement record if it exists, null otherwise.
     */
    public static UserAgreement getUserAgreementIfCurrentAndApproved(String emailID){
        logger.log(Level.INFO, "UserAgreement: get user agreement expiration for user if approved.");
        UserAgreementDAO uad = new UserAgreementDAO();
        return uad.getUserAgreementIfCurrentAndApproved(emailID);
    }
    
    
	public static UserAgreement retrieve(String emailID, Timestamp agreementTimestamp) {
        UserAgreementDAO uad = new UserAgreementDAO();
        try {
            return uad.retrieveByIDAndTimestamp(emailID, agreementTimestamp);
        }
        catch (Exception e) {
            return null;   // assume no user agreement returned.
        }
	}    
    

    public static java.util.List<UserAgreement> getLatestAgreementsForAllUsers(){
        logger.log(Level.FINER, "UserAgreementText: getting all latest user agreements.");

        UserAgreementDAO uad = new UserAgreementDAO();
        return uad.getLatestAgreementsForAllUsers();
    }

    public static java.util.List<UserAgreement> getAllUserAgreements(){
        logger.log(Level.FINER, "UserAgreementText: getting all latest user agreements.");

        UserAgreementDAO uad = new UserAgreementDAO();
        return uad.getAllUserAgreements();
    }    
    
}
