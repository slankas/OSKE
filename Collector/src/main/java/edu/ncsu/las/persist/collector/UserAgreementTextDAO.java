package edu.ncsu.las.persist.collector;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.persist.DAO;

import java.util.logging.Logger;
import java.util.logging.Level;
import edu.ncsu.las.model.collector.UserAgreementText;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 
 */

public class UserAgreementTextDAO extends DAO{

    private static final Logger logger = Logger.getLogger(Collector.class.getName());

    private static final String SELECT_USER_AGREEMENTS_TEXT = "select version_number, version_date, reading_text, agreement_text, questions from user_agreement_text";

    private static final String SELECT_USER_AGREEMENT_TEXT_FOR_USER =  SELECT_USER_AGREEMENTS_TEXT + " where version_number in (Select agreement_version from user_agreement where email_id=?)";

    //private static final String SELECT_USER_AGREEMENT_TEXT_LATEST = SELECT_USER_AGREEMENTS_TEXT + "where version_number in (select max(version_number) from user_agreement_text) ";

    //private static final String INSERT_USER_AGREEMENT_TEXT = "insert into user_agreement_text (version_number, version_date, reading_text, agreement_text, questions) values (?,?,?,?,?)" ;

    private static final String SELECT_USER_AGREEMENT_BY_VERSION = SELECT_USER_AGREEMENTS_TEXT + " where version_number = ?";


    private static final String SELECT_LATEST_USER_AGREEMENT_TEXT = SELECT_USER_AGREEMENTS_TEXT  + " where version_number in (select max(version_number) from user_agreement_text where version_date < now())";


    private static final String GET_LATEST_USER_AGREEMENT_FOR_USER = SELECT_USER_AGREEMENTS_TEXT+" where version_number in " +
            "(Select agreement_version from user_agreement where email_id=? and agreement_timestamp in " +
            "(Select max(agreement_timestamp) from user_agreement where email_id=?))";

    private static class UserAgreementTextRowMapper implements RowMapper<UserAgreementText> {
        public UserAgreementText mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new UserAgreementText(rs.getInt("version_number"), rs.getDate("version_date"), rs.getString("reading_text"), rs.getString("agreement_text"),rs.getString("questions"));
        }
    }

    public List<UserAgreementText> getLatestUserAgreementForUser(String emailId){
        logger.log(Level.INFO, "getting latest user agreement signed by user.");
        return this.getJDBCTemplate().query(GET_LATEST_USER_AGREEMENT_FOR_USER, new UserAgreementTextRowMapper(), emailId, emailId);
    }

    public UserAgreementText getLatestUserAgreementText(){
        logger.log(Level.FINER, "getting latest user agreement which has to be signed by user.");
        return this.getJDBCTemplate().queryForObject(SELECT_LATEST_USER_AGREEMENT_TEXT, new UserAgreementTextRowMapper());
    }

    public UserAgreementText getSpecificUserAgreementForm(int versionNumber){
        logger.log(Level.INFO, "getting all user agreements signed by user.");
        return this.getJDBCTemplate().queryForObject(SELECT_USER_AGREEMENT_BY_VERSION, new UserAgreementTextRowMapper(),versionNumber);
    }    
    
    public List<UserAgreementText> getUserAgreementsForUser(String emailId){
        logger.log(Level.INFO, "getting all user agreements signed by user.");
        return this.getJDBCTemplate().query(SELECT_USER_AGREEMENT_TEXT_FOR_USER, new UserAgreementTextRowMapper(),emailId);
    }





}
