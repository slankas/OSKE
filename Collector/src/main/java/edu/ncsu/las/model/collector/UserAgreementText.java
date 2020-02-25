package edu.ncsu.las.model.collector;

import edu.ncsu.las.persist.collector.UserAgreementTextDAO;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;

/**
 * Tracks the information needed on a user agreement form.
 * 
 * agreementText: string of html content to be displayed on the page as a form.
 * readingText: JSONArray of JSONObjects. Each object of the form:
 *              { "hyperlink" : "hyperlinkValue",
 *                "name"      : "display value to the user"}
 *              Readings are shown to the user in the order listed.
 *              If there are no readings, then this should be blank json array (e.g., [])
 * questions: 
				[
				{ "questionName": "shortStringIdentifier",
				  "questionText": "full text of the question",
				  "questionType": “textarea/textfield",
				  "length": x     // width of a textfield
				}, …
				]

 * 
 */
public class UserAgreementText {

    private static Logger logger = Logger.getLogger(Job.class.getName());

    private int _versionNumber;
    private Date _versionDate;
    private String _readingText;
    private String _agreementText;
    private String _questions;

    private JSONArray _questionArray;
    //private JSONObject _questionHash;

    private JSONArray _readingArray;
    
    public UserAgreementText(int versionNumber, Date versionDate, String readingText, String agreementText, String questions) {
        _versionNumber = versionNumber;
        _versionDate = versionDate;
        _readingText = readingText;
        _agreementText = agreementText;
        _questions = questions;

        _readingArray = new JSONArray(readingText);
        
        _questionArray = new JSONArray(questions);

    }

    public UserAgreementText(){
    }

    public void setVersionNumber(int versionNumber){ _versionNumber = versionNumber ; }

    public void setVersionDate(Date versionDate){ _versionDate = versionDate ; }

    public void setReadingText(String readingText) { 
    	_readingText = readingText ;
    	_readingArray = new JSONArray(readingText);
    }

    public void setAgreementText(String agreementText) { _agreementText = agreementText ; }

    public void setQuestion(String questions) { 
    	_questions = questions;
    	_questionArray = new JSONArray(questions);
    }

    public int getVersionNumber(){ return _versionNumber ; }

    public Date getVersionDate(){ return _versionDate ; }

    public String getReadingText(){ return _readingText ; }

    public String getAgreementText(){ return _agreementText ; }

    public String getQuestions(){ return _questions ; }

    public JSONObject toJSON()  {
        JSONObject result = new JSONObject().put("versionNumber", _versionNumber)
                .put("versionDate", _versionDate)
                .put("readingText", _readingArray)
                .put("agreementText", _agreementText)
                .put("questions", _questionArray);
//                .put("questionHash", _questionHash);


        return result;
    }

    /**
     * Retrieves that latest version of the user agreement text to be completed by a user.
     * 
     * @return
     */
    public static UserAgreementText getLatestUserAgreementForm(){
        logger.log(Level.FINER, "UserAgreementText: getting user agreement to be signed by user.");
        UserAgreementTextDAO utdd = new UserAgreementTextDAO();
        return utdd.getLatestUserAgreementText();
    }    
    
    /**
     * Retrieves a specific user agreement form based upon the version number
     * 
     * @param versionNumber
     * @return
     */
    public static UserAgreementText getUserAgreementForm(int versionNumber){
        logger.log(Level.INFO, "UserAgreementText: getting specific user agreement for user.");

        return new UserAgreementTextDAO().getSpecificUserAgreementForm(versionNumber);
    }    
    
    
    public static java.util.List<UserAgreementText> getLatestUserAgreementForUser(String emailId){
        logger.log(Level.INFO, "UserAgreementText: getting latest user agreement for user.");
        UserAgreementTextDAO utdd = new UserAgreementTextDAO();
        return utdd.getLatestUserAgreementForUser(emailId);
    }



    public static java.util.List<UserAgreementText> getUserAgreementsForUser(String emailId){
        logger.log(Level.INFO, "UserAgreementText: getting user agreement history for user.");
        UserAgreementTextDAO utdd = new UserAgreementTextDAO();
        return utdd.getUserAgreementsForUser(emailId);
    }




}