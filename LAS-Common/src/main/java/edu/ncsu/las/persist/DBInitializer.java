package edu.ncsu.las.persist;
import java.util.Properties;

import org.json.JSONObject;


public class DBInitializer {

	public static void initialize(Properties prop, String connectionName) {
		
		org.apache.commons.dbcp2.BasicDataSource source = new org.apache.commons.dbcp2.BasicDataSource();
		
		
		source.setDriverClassName(prop.getProperty("driver"));
		source.setUrl(prop.getProperty("url"));
		source.setUsername(prop.getProperty("user"));
		source.setPassword(prop.getProperty("password"));
		source.setMaxTotal(Integer.parseInt(prop.getProperty("maxconnections")));

		DataSourceManager.getTheDataSourceManager().addDataSource(connectionName, source);
	}

	public static void initialize(JSONObject properties, String connectionName) {
		org.apache.commons.dbcp2.BasicDataSource source = new org.apache.commons.dbcp2.BasicDataSource();

		source.setDriverClassName(properties.getString("driver"));
		source.setUrl(properties.getString("url"));
		source.setUsername(properties.getString("user"));
		source.setPassword(properties.getString("password"));
		source.setMaxTotal(Integer.parseInt(properties.get("maxconnections").toString()));
		
		DataSourceManager.getTheDataSourceManager().addDataSource(connectionName, source);
	}
	
}