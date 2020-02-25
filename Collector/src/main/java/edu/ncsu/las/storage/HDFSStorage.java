package edu.ncsu.las.storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedExceptionAction;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.security.UserGroupInformation;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.FileStorageStatusCode;

public class HDFSStorage {
	private static Logger logger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	private static final org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
	static {
		conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName() );
		conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName() );
		conf.set("dfs.client.use.datanode.hostname", "true" );          // Required when the HDFS cluster may be running on a private network.
	}
	
	public FileStorageStatusCode store(FileStorageAreaType area, String domainInstanceName, String UUID, JSONObject data) {
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.SYSTEMCOMPONENT_ACCUMULO) == false) {
			return FileStorageStatusCode.SKIPPED;
		}
		if (Configuration.storeJSONInHDFS(domainInstanceName,area) == false) {
			return FileStorageStatusCode.SKIPPED;
		}
		String user = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.HDFS_USER);
		String uri  = Configuration.getConfigurationProperty(domainInstanceName, area, ConfigurationType.HDFS_STOREJSON_LOCATION);
				
		System.setProperty("HADOOP_USER_NAME", user);
		
		// for the URI, we need to add a YYYMMDD stamp, using GMT time.  then add the UUID after it. 
		ZonedDateTime zdt = ZonedDateTime.now(ZoneOffset.UTC);
		String date =zdt.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
		if (date.endsWith("Z")) { 
			date = date.substring(0, date.length()-1);
		}
		if (uri.endsWith("/") == false) { uri += "/"; }
		uri += date +"/" +UUID;
		final String toURI = uri;
		
		UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
		try {
			ugi.doAs(new PrivilegedExceptionAction<Void>() {
				public Void run() throws Exception {
					
					FileSystem fs = FileSystem.get(URI.create(toURI), conf);
					
					FSDataOutputStream out = fs.create(new Path(toURI)) ;
					out.write(data.toString().getBytes(StandardCharsets.UTF_8));
					out.close();
					
					return null;
				}
			});
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to save json record to HDFS: "+e);
		}
		return null;
	}
	
	public static FileStorageStatusCode deleteEntireDomainAndArea( String domainInstanceName,FileStorageAreaType area) {
		//Not sure about userGroupInforation
		String user = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.HDFS_USER);
		String uri  = Configuration.getConfigurationProperty(domainInstanceName, area, ConfigurationType.HDFS_STOREJSON_LOCATION);
		try {
			UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
			ugi.doAs(new PrivilegedExceptionAction<Void>() {
				public Void run() throws Exception {
					
					FileSystem fs = FileSystem.get(URI.create(uri), conf);
					fs.delete(new Path(uri),true);
					
					return null;
				}
			});
			
			
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to delete from HDFS: "+e);
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
		
		return FileStorageStatusCode.SUCCESS;
		
	}
	
	private static class MyInteger {
		int value = 0;
	}

	/**
	 * 
	 * 
	 * @param domainInstanceName
	 * @param area
	 * @param jobID
	 * @param storageIDs
	 * @return -1 on error, otherwise the number of records removed
	 */
	public static int purgeJobRecords(String domainInstanceName,FileStorageAreaType area, UUID jobID, java.util.Set<UUID> storageIDs) {
		//Not sure about userGroupInforation
		String user = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.HDFS_USER);
		
		final MyInteger recordsRemoved = new MyInteger();
		
		final String strJobID = jobID.toString();
		
		try {
			UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
			ugi.doAs(new PrivilegedExceptionAction<Void>() {
				public Void run() throws Exception {			

					String uri  = Configuration.getConfigurationProperty(domainInstanceName, area, ConfigurationType.HDFS_STOREJSON_LOCATION);
					
					FileSystem fs = FileSystem.get(URI.create(uri), conf);
					RemoteIterator<LocatedFileStatus> fileStatusListIterator  = fs.listFiles(new Path(uri), true);
					while(fileStatusListIterator.hasNext()){
				        LocatedFileStatus fileStatus = fileStatusListIterator.next();

				        if (fileStatus.getPath().getName().endsWith(".dat")) {
				        	recordsRemoved.value += HDFSStorage.removeJobRecordFromFile(strJobID, fs, fileStatus);
				        }
				        else {
				        	UUID testUUID = UUID.fromString(fileStatus.getPath().getName());
				        	if (storageIDs.contains(testUUID)) {
				        		fs.delete(fileStatus.getPath(),false);
				        		recordsRemoved.value++;
				        	}
				        }
				    }

					return null;			
				}
			});
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to delete from HDFS: "+e);
			return -1;
		}
		
		return recordsRemoved.value;
		
	}
	

	/**
	 * Examines a file (where each line is a json object).  If the JSON object came from a particular job, then
	 * it is removed from that file.
	 * 
	 * @param jobID
	 * @param fs pointer to the HDFS storage system
	 * @param originalDataFile file to check if a particular job record exists within it...
	 * @throws IOException
	 * @returns number of records removed from the file
	 */
	private static int removeJobRecordFromFile(String jobID, FileSystem fs, LocatedFileStatus originalDataFile) throws IOException {
		int numRecordsRemoved = 0;
		
		String uriNew = originalDataFile.getPath().toUri().toString() +"_new";
		
		boolean madeChanges = false;  // if we make change, delete the old .dat file and rename _new.  If no changes, delete _new file
		
		Path newOutputPath = new Path(uriNew);
		
		FSDataOutputStream out = fs.create(newOutputPath);
		BufferedReader in      = new BufferedReader(new InputStreamReader(fs.open(originalDataFile.getPath()),StandardCharsets.UTF_8));
		String line;
		
		while ( (line=in.readLine()) != null) {
			JSONObject jo = new JSONObject(line);
			boolean keepRecord=true;
			if (jo.has("provenance")) {
				JSONObject provObject= jo.getJSONObject("provenance");
				if (provObject.has("job")) {
					JSONObject jobObject = provObject.getJSONObject("job");
					if (jobObject.has("id") && jobObject.getString("id").equals(jobID)) {
						keepRecord = false;
						madeChanges = true;
						numRecordsRemoved++;
						logger.log(Level.FINEST, "From HDFS file, removing "+jo.toString());
					}
				}
			}
			if (keepRecord) {
				out.write(line.getBytes(StandardCharsets.UTF_8));
			}
		}
		in.close();
		out.close();
		
		if (madeChanges) {
			fs.delete(originalDataFile.getPath(),false);
			fs.rename(newOutputPath, new Path(originalDataFile.getPath().toUri()));
		}
		else {
			fs.delete(newOutputPath,false);
		}
		
		return numRecordsRemoved;

	}
	
}
