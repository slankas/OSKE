package edu.ncsu.las.storage;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.IteratorSetting.Column;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.hadoop.io.Text;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.FileStorageStatusCode;

public class AccumuloStorage implements StoreMechanismRaw {
	private static Logger logger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());

	private static Connector _accumuloConnector;
	private static HashMap<String, BatchWriter> _batchWriters = new HashMap<String, BatchWriter>();
	
	private static String META_DATA_COLUMN_FAMILY = "metadata";
	private static String RAW_DATA_COLUMN_FAMILY = "data";
	private static String RAW_DATA_COLUMN_NAME   = "rawdata";
	
	private static synchronized Connector  getAccumuloConnector() {
		if (_accumuloConnector == null) {
			Instance accumuloInstance = new ZooKeeperInstance(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.ACCUMULO_INSTANCE_NAME),
					                                          Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.ACCUMULO_ZOOKEEPERS));
			try {
				_accumuloConnector = accumuloInstance.getConnector(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.ACCUMULO_USERNAME), new PasswordToken(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.ACCUMULO_PASSWORD)));

			} catch (JSONException e) {
				logger.log(Level.SEVERE, "Inproper configuration for accumulo storage: "+Configuration.getConfigurationObject(Domain.DOMAIN_SYSTEM, ConfigurationType.ACCUMULO),e);
			} catch (AccumuloException e) {
				logger.log(Level.SEVERE, "Unable to access accumulo storage: "+Configuration.getConfigurationObject(Domain.DOMAIN_SYSTEM, ConfigurationType.ACCUMULO),e);
			} catch (AccumuloSecurityException e) {
				logger.log(Level.SEVERE, "Inproper security credentials for accumulo storage: "+Configuration.getConfigurationObject(Domain.DOMAIN_SYSTEM, ConfigurationType.ACCUMULO),e);
			}
		}
		return _accumuloConnector;
	}
	

	//according to the Accummulo book, BatchWriters are thread-sadfe.  Might think about the affect of isolation, but shouldn't be a problem for us..
	private static synchronized BatchWriter getBatchWriter(String table) {
		if (_batchWriters.containsKey(table) == false) {
			try {
				BatchWriter bw = getAccumuloConnector().createBatchWriter(table, new BatchWriterConfig());
				_batchWriters.put(table, bw);
			} catch (TableNotFoundException e) {
				logger.log(Level.SEVERE, "Unable to find accumulo table: "+table);  //shouldn't occur because of logic in setting up the connection.
				return null;
			}
			
		}
		return _batchWriters.get(table);
	}
	
	public static void validateEnvironment(java.util.Collection<String> domainList) {
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.SYSTEMCOMPONENT_ACCUMULO) == false) {
			logger.log(Level.INFO, "System configured to not use Accumulo - not validating environment");
			return;
		}
		
		for (String domainName: domainList) {
			if (domainName.equals(Domain.DOMAIN_SYSTEM)) { continue; }
			validateEnvironment(domainName);
		}
	}
	
	private static void validateEnvironment(String domain) {
		try {
			createTable(Configuration.getDomainAndArea(domain, FileStorageAreaType.REGULAR));  //ensures that the tables are created.
			createTable(Configuration.getDomainAndArea(domain, FileStorageAreaType.ARCHIVE));
			createTable(Configuration.getDomainAndArea(domain, FileStorageAreaType.SANDBOX));
		}
		catch (AccumuloException e) {
			logger.log(Level.SEVERE, "Unable to access accumulo storage: "+Configuration.getConfigurationObject(Domain.DOMAIN_SYSTEM, ConfigurationType.ACCUMULO),e);
		}
		catch (AccumuloSecurityException e) {
			logger.log(Level.SEVERE, "Inproper authorization credentials for accumulo storage: "+Configuration.getConfigurationObject(Domain.DOMAIN_SYSTEM, ConfigurationType.ACCUMULO),e);
		}
	}
	

	private static void createTable(String table) throws AccumuloException, AccumuloSecurityException {
		Connector conn = getAccumuloConnector();
		
		if (conn.tableOperations().exists(table) == false) { 
			try {
				conn.tableOperations().create(table);
				
				Set<Text> contentGroup = new HashSet<Text>();
				contentGroup.add(new Text(RAW_DATA_COLUMN_FAMILY));
				
				Set<Text> metadataGroup = new HashSet<>();
				metadataGroup.add(new Text(META_DATA_COLUMN_FAMILY));

				Map<String, Set<Text>> groups = new HashMap<String, Set<Text>>();
				groups.put("contentGroup", contentGroup);
				groups.put("metadataGroup", metadataGroup);

				conn.tableOperations().setLocalityGroups(table, groups);
			} catch (TableExistsException e) {
				logger.log(Level.SEVERE, "Table exists: "+table); // this shouldn't happen because of our check.
			}
			catch (TableNotFoundException e) {
				logger.log(Level.SEVERE, "Table does not exist: "+table); // this shouldn't happen because we just created it....
			}
			logger.log(Level.INFO, "Created table in accummulo: "+table);
		}
	}
	
	
	@Override
	public FileStorageStatusCode store(String domain, FileStorageAreaType area, String UUID, byte[] data, JSONObject metaData) {
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.SYSTEMCOMPONENT_ACCUMULO) == false) {
			return FileStorageStatusCode.SKIPPED;
		}
		
		String tableName =  Configuration.getDomainAndArea(domain, area);
		BatchWriter bw = getBatchWriter(tableName);
		
		logger.log(Level.INFO, "Saving to accumulo: "+tableName);
		String authorizationLabel = "";
		try {
			Mutation m = new Mutation(new Text(UUID));
	        m.put(new Text(RAW_DATA_COLUMN_FAMILY), new Text(RAW_DATA_COLUMN_NAME), new ColumnVisibility(authorizationLabel), new Value(data));
	        bw.addMutation(m);
	        for (String key: metaData.keySet()) {
		        m = new Mutation(new Text(UUID));
		        m.put(new Text(META_DATA_COLUMN_FAMILY), new Text(key), new ColumnVisibility(authorizationLabel), new Value(metaData.getString(key).getBytes(StandardCharsets.UTF_8)));
		        bw.addMutation(m);
	        }
	        bw.flush();
		}
		catch(MutationsRejectedException e) {
			logger.log(Level.SEVERE, "Unable to save to accumulo: "+e);
			try {
				bw.close(); // per the javadoc, this needs to be closed and subsequently re-opened when this exception occurs
			}
			catch (MutationsRejectedException ex) {
				//ignore silently
			}
			_batchWriters.remove(tableName);
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
		return FileStorageStatusCode.SUCCESS;
	}

	@Override
	public byte[] retrieve(String domain, FileStorageAreaType area, String UUID) {
		byte[] result = null;
		
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.SYSTEMCOMPONENT_ACCUMULO) == false) {
			return null;
		}
		
		try {
			String tableName = Configuration.getDomainAndArea(domain, area);
			Authorizations authorizations = getAccumuloConnector().securityOperations().getUserAuthorizations(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.ACCUMULO_USERNAME));
				
			Scanner scanner = getAccumuloConnector().createScanner(tableName, authorizations);
			scanner.clearColumns();
			scanner.fetchColumn(new Column(RAW_DATA_COLUMN_FAMILY, RAW_DATA_COLUMN_NAME));
			scanner.setRange(new Range(UUID));
		    for (java.util.Map.Entry<Key,Value> entry : scanner) {
		    	result = entry.getValue().get();
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to retrieve raw data from accumulo: "+e);
		}
		return result;
	}

	@Override
	public FileStorageStatusCode delete(String domain, FileStorageAreaType area, String UUID) {
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.SYSTEMCOMPONENT_ACCUMULO) == false) {
			return FileStorageStatusCode.SKIPPED;
		}
		
		String tableName = Configuration.getDomainAndArea(domain, area);
		try {
			_accumuloConnector.tableOperations().deleteRows(tableName, new Text(UUID), new Text(UUID));
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to delete record from from accumulo("+UUID+"): "+e);
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
		return FileStorageStatusCode.SUCCESS;
	}

	
	public static FileStorageStatusCode deleteTable(String domain, FileStorageAreaType area) {
		String tableName = Configuration.getDomainAndArea(domain, area);
		
		try {
			_accumuloConnector.tableOperations().delete(tableName);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to delete table from accumulo domain: "+domain+" Area: "+area.toString()+e);
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
		return FileStorageStatusCode.SUCCESS;
	}

	@Override
	public FileStorageStatusCode move(String domain, FileStorageAreaType srcArea, FileStorageAreaType destArea,	String UUID) {
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.SYSTEMCOMPONENT_ACCUMULO) == false) {
			return FileStorageStatusCode.SKIPPED;
		}
		
		// To move, need to read all of the data from the old record, creating a mutation record for the new one, then need to delete the old one.
		String destTableName = Configuration.getDomainAndArea(domain, destArea);
		BatchWriter bw = getBatchWriter(destTableName);
		
		try {
			String tableName = Configuration.getDomainAndArea(domain, srcArea);
			
			Authorizations authorizations = getAccumuloConnector().securityOperations().getUserAuthorizations(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.ACCUMULO_USERNAME));
				
			Scanner scanner = getAccumuloConnector().createScanner(tableName, authorizations);
			scanner.clearColumns();
			scanner.fetchColumn(new Column(RAW_DATA_COLUMN_FAMILY, RAW_DATA_COLUMN_NAME));
			scanner.setRange(new Range(UUID));
			
			
			String authorizationLabel = "";			
			
		    for (java.util.Map.Entry<Key,Value> entry : scanner) {
		    	Mutation m = new Mutation(entry.getKey().getRow());
		        m.put(entry.getKey().getColumnFamily(), entry.getKey().getColumnQualifier(), new ColumnVisibility(authorizationLabel), entry.getValue());
		        bw.addMutation(m);
			}
		    bw.flush();	
		    
		    return this.delete(domain, srcArea, UUID);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to move record("+UUID+") in accumulo: "+e);
			try {
				bw.close(); // per the javadoc, this needs to be closed and subsequently re-opened when this exception occurs
			}
			catch (MutationsRejectedException ex) {
				//ignore silently
			}
			_batchWriters.remove(destTableName);			
			
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
	}
}
