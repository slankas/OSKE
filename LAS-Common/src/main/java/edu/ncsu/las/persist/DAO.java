package edu.ncsu.las.persist;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Simple class to automatically set the JDBC template for NCBP Connections
 */
public class DAO {

	    private JdbcTemplate jdbcTemplate;

	    protected DAO() {
	    	this.jdbcTemplate = DataSourceManager.getTheDataSourceManager().getJDBCTemplate(DBConstants.CONNECTION_AW);
	    }
	    
	    public JdbcTemplate getJDBCTemplate() {
	    	return jdbcTemplate;
	    }
}
