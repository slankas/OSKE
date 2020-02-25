package edu.ncsu.las.persist;

import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

//import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DataSourceManager is a singleton that manages all of the
 * different connections used within a system.  Persist
 * objects calls this object to retrieve the connection.
 *
 * To use, the system must first get the connection manager, then register
 * the appropriate DataSources and/or DirectoryContexts into it.
 *
 * Example:
 *		DBConnectionManager dcm = DBConnectionManager.getDBConnectionManager();
 *		DBConnection dbc = new DBConnection(....);
 *      dcm.addDBConnection("POOL_NAME",dbc);
 *
 * Optionally, the system can also monitor the connections on a periodic
 * basis.  To start the monitoring, use
 * 		DBConnectionManager.getDBConnectionManager().startMonitoringConnections();
 * This starts up a new thread to check the connections every 5 minutes.  If
 * a connection is unavailable, it closes off that connection pool and attempts to
 * re-establish it.
 *
 *  
 */

public class DataSourceManager implements Runnable {
	private static DataSourceManager _DBConnectionManager;
	private java.util.HashMap<String, javax.sql.DataSource> _dataSources;
	private java.util.HashMap<String, JdbcTemplate> _jdbcTemplates;
	private java.util.HashMap<String, Hashtable<String, Object>> _ldapEnvironments;
	
	//protected static Logger logger = Logger.getLogger("persist");	
	


	/*  Thought with this is to periodically monitor and restore the connection pools if necessary
	private static int MONITOR_PERIOD_SLEEP_TIME = 300000; // 5 minutes in milliseconds
    */
/**
 * DBConnectionManager constructor comment.
 */
private DataSourceManager() {
	super();
	_dataSources = new java.util.HashMap<String, javax.sql.DataSource>();
	_jdbcTemplates = new java.util.HashMap<String, JdbcTemplate>();
	_ldapEnvironments = new java.util.HashMap<String, Hashtable<String, Object>>();
}

/**
 * Insert the method's description here.
 * Creation date: (2/15/01 11:05:18 AM)
 * @param conn com.fub.its.persist.DBConnection
 * @param key java.lang.String
 */
public void addDataSource(String key, javax.sql.DataSource dataSource) {
	_dataSources.put( key, dataSource);
	_jdbcTemplates.put(key, new JdbcTemplate(dataSource));
}
/**
 * Retrieves a connections out of a specific DBConnection datasource
 *
 * @param key DBConnection to use.
 */
public javax.sql.DataSource getDataSource(String key) {
	return _dataSources.get(key);		 
}

/**
 * Gets a JdbcTemplate Object for Spring.
 */
public JdbcTemplate getJDBCTemplate(String key) {
	return _jdbcTemplates.get(key);
}


/**
 * Add LDAP Environment
 */
public void addLDAPEnvironments(String key, Hashtable<String, Object> ldapEnvironment) {
	_ldapEnvironments.put( key, ldapEnvironment);
}

/**
 * 
 * @param key
 * @return
 */
public javax.naming.directory.DirContext getLDAPContext(String key) {
	try {
		return new InitialDirContext(_ldapEnvironments.get(key));
	}
	catch (NamingException ex) {
		//logger.trace("Unable to get LDAP Context: "+key);
		return null;
	}
}

/** doesn't have to be synchronized as only the initservlet is active when this is first called */
public static DataSourceManager getTheDataSourceManager() {
	if( _DBConnectionManager == null ) {
		_DBConnectionManager = new DataSourceManager();
	}
	return _DBConnectionManager;
			
}

public void run() {
	while (true) {
		try {
			Thread.sleep(300000); // sleep for 5 minutes
			//logger.trace("testing connections");
			//logger.trace("   NOT IMPLEMENTED!");
			//this.testConnectionPools();
			//logger.trace("Connection test complete");
			
		}
		catch (InterruptedException ie) {
		}
	}
}

/**
 * This starts a thread that could be used within certain applications
 * to monitor the different connections.
 *
 * In generally, it is not necessary because getConnection() will re-establish a
 * data source if it becomes invalid.
 */
public void startMonitoringConnections() {
	new Thread(this).start();
}

/**
 * Tests whether or not the various connections pools are up.
 * If a connection pool is not avaible, it closes the pool and
 * attempts to initialize it again.
 *
 * returns true if all connections are fine, false otherwise
 */
/*
public synchronized boolean testConnectionPools() {
	boolean result = true;
	for (DBConnection dbconn: _ConnectionPools.values() ) {
		if (dbconn.testDBConnection() == false) {
			this.logMessage("  Connection unavailable - "+dbconn.getURL());
			try {
				dbconn.initializeDataSource();
				this.logMessage("  --restored - "+dbconn.getURL());
			}
			catch (DBException e) {
				this.logMessage("  --still not available"+dbconn.getURL());
				result = false;
			}
		}
	}
	return result;
}
*/
}
