package edu.ncsu.las.persist.collector;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.UserAgreement;
import edu.ncsu.las.persist.DAO;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;


import java.sql.Timestamp;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * 
 */

public class UserAgreementDAO extends DAO {

    private static final Logger logger = Logger.getLogger(Collector.class.getName());

    private static final String SELECT_USER_AGREEMENT = "select email_id, agreement_timestamp, agreement_version, status, status_timestamp, expiration_timestamp, answers, user_organization, user_signature, digital_signature_hash, adjudicator_comments, adjudicator_emailid from user_agreement";

    private static final String SELECT_LATEST_USER_AGREEMENT_RECORDS =  SELECT_USER_AGREEMENT + " t where t.agreement_timestamp = (select max(agreement_timestamp) from user_agreement u where t.email_id = u.email_id)";

    private static final String SELECT_USER_AGREEMENT_FOR_USER = SELECT_USER_AGREEMENT + " where email_id=? ";
    
    private static final String SELECT_USER_AGREEMENT_FOR_USER_AND_TIMESTAMP = SELECT_USER_AGREEMENT + " where email_id=? and date_trunc('second', agreement_timestamp)= ?";

    private static final String INSERT_USER_AGREEMENT = "insert into user_agreement"+
                                                        "(email_id, agreement_timestamp, agreement_version, status, status_timestamp, expiration_timestamp, answers, user_organization, user_signature, digital_signature_hash, adjudicator_comments, adjudicator_emailid)"+
                                                        "values (?,?,?,?,?,?,?,?,?,?,?,?)" ;

    private static final String SELECT_MOST_RECENT_USER_AGREEMENT = SELECT_USER_AGREEMENT + " where email_id=? and agreement_timestamp in (select max(agreement_timestamp) from user_agreement where email_id=?)";

    private static final String UPDATE_USER_AGREEMENT_STATE = "Update user_agreement set status=?, status_timestamp=now(), adjudicator_comments=?, adjudicator_emailid=? where email_id=? and agreement_timestamp=?";

    private static final String CHANGE_USER_AGREEMENT_EXPIRATION_DATE = "Update user_agreement set expiration_timestamp=? where email_id=? and agreement_timestamp=?";

    private static final String SELECT_USER_AGREEMENT_APPROVED_AND_CURRENT = SELECT_USER_AGREEMENT + " where email_id=? and status='approved' and expiration_timestamp > now()";

    private static class UserAgreementRowMapper implements RowMapper<UserAgreement> {
        public UserAgreement mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new UserAgreement(rs.getString("email_id"), rs.getTimestamp("agreement_timestamp"), rs.getInt("agreement_version"), rs.getString("status"), rs.getTimestamp("status_timestamp"),rs.getTimestamp("expiration_timestamp"),rs.getString("answers"),rs.getString("user_organization"),rs.getString("user_signature"),rs.getString("digital_signature_hash"),rs.getString("adjudicator_comments"),rs.getString("adjudicator_emailid"));
        }
    }

    public boolean insertUserAgreement(UserAgreement ua){
        int numRows = this.getJDBCTemplate().update(INSERT_USER_AGREEMENT, ua.getEmailId(), ua.getAgreementTimestamp(),
                                                    ua.getAgreementVersion(), ua.getStatus(), ua.getStatusTimestamp(),
                                                    ua.getExpirationTimestamp(), ua.getAnswers(), ua.getUserOrganization(),
                                                    ua.getUserSignature(), ua.getDigitalSignatureHash(), ua.getAdjudicatorComments(),
                                                    ua.getAdjudicatorEmailID());
        return (numRows == 1);
    }

    public UserAgreement getMostRecentUserAgreement(String emailID){
        logger.log(Level.INFO, "checking if user has signed user agreement");
        return this.getJDBCTemplate().queryForObject(SELECT_MOST_RECENT_USER_AGREEMENT, new UserAgreementRowMapper(),emailID,emailID);
    }
    
    public UserAgreement  retrieveByIDAndTimestamp(String emailID, Timestamp agreementTimestamp){
        logger.log(Level.INFO, "checking if user has signed user agreement");
        return this.getJDBCTemplate().queryForObject(SELECT_USER_AGREEMENT_FOR_USER_AND_TIMESTAMP, new UserAgreementRowMapper(),emailID,agreementTimestamp);
    }

    public List<UserAgreement> getLatestAgreementsForAllUsers(){
        logger.log(Level.INFO, "getting all latest user agreements.");
        return this.getJDBCTemplate().query(SELECT_LATEST_USER_AGREEMENT_RECORDS, new UserAgreementRowMapper());
    }    
    
    public List<UserAgreement> getAllUserAgreements(){
        logger.log(Level.INFO, "getting all latest user agreements.");
        return this.getJDBCTemplate().query(SELECT_USER_AGREEMENT, new UserAgreementRowMapper());
    }

    public List<UserAgreement> getAllUserAgreementsForUser(String userEmailID){
        logger.log(Level.FINER, "getting all latest user agreements for "+userEmailID);
        return this.getJDBCTemplate().query(SELECT_USER_AGREEMENT_FOR_USER, new UserAgreementRowMapper(),userEmailID);
    }
    
    
    public boolean updateAgreementStatus(String emailID, Timestamp agreementTimestamp, String adjudicatorComments, String adjudicatorEmailID, String status){
        logger.log(Level.FINER, "setting user agreement status: "+status);
        return (this.getJDBCTemplate().update(UPDATE_USER_AGREEMENT_STATE,status,adjudicatorComments,adjudicatorEmailID,emailID,agreementTimestamp) == 1);
    }    
    
    
/*
    public int setAgreementStatusAsExpired(String email_id, Timestamp agreement_timestamp){
        String adjudicatorComments = "";
        String adjudicatorEmailId = "";
        logger.log(Level.INFO, "setting user agreement status as expired");
        return this.getJDBCTemplate().update(UPDATE_USER_AGREEMENT_STATE,"expired",adjudicatorComments,adjudicatorEmailId,email_id,agreement_timestamp);
    }
    */

    public boolean changeExpirationTimestampForUser( String emailID, Timestamp agreementTimestamp, Timestamp newExpirationTimestamp){
        logger.log(Level.INFO, "changing user agreement expiration for user");
        return (this.getJDBCTemplate().update(CHANGE_USER_AGREEMENT_EXPIRATION_DATE, newExpirationTimestamp,emailID,agreementTimestamp) ==1);
    }

    public UserAgreement getUserAgreementIfCurrentAndApproved(String emailID){
        logger.log(Level.INFO, "get user agreement expiration for user if approved");
        List<UserAgreement> results = this.getJDBCTemplate().query(SELECT_USER_AGREEMENT_APPROVED_AND_CURRENT, new UserAgreementRowMapper(),emailID);
        if (results.size() == 0) {
        	return null;
        } else {
        	return results.get(0);
        }
    }


}


