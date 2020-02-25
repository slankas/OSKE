package edu.ncsu.las.persist;

public class TestDAO extends DAO {

	    public String getPostgreSQLVersion() {
	    	String result = this.getJDBCTemplate().queryForObject("SELECT current_database()||' - '||version()||CURRENT_TIMESTAMP", String.class);
	    	return result;
	    }
}
