package edu.ncsu.las.source;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.json.JSONObject;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.Instrumentation;
import edu.ncsu.las.model.collector.Job;
import edu.ncsu.las.model.collector.JobHistory;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.util.DateUtilities;

/**
 * 
 * 
 * Implementation notes:
 * - need to be wary of race conditions.  Windows will lock files in use (being copied)
 *   to prevent them from being deleted
 * - decided not to use the Watcher services as multiple events are generated
 * 
 */
public class DirectoryWatcher extends AbstractHandler implements SourceHandlerInterface, Runnable {
	private static ScheduledExecutorService _executorService = Executors.newScheduledThreadPool(1); 
	
	@Override
	/**
	 * 
	 * @param collector
	 * @param jobRecord
	 * @param job this value is ignored for this handler
	 */
	public void initialize(JobHistory jobRecord, Job job) {
		// create a job record for this
		UUID jobUUID = new UUID(0,0);
		UUID jobHistoryID = edu.ncsu.las.util.UUID.createTimeUUID();
		jobRecord = JobHistory.initiateJob(jobHistoryID, jobUUID, this.getSourceHandlerName(), JobHistoryStatus.PROCESSING, "", Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_ID),job.getDomainInstanceName());
		
		super.initialize(jobRecord, job);
	
		
		//Check that importDirectory is configured
		if (Configuration.getConfigurationProperty(job.getDomainInstanceName(),edu.ncsu.las.model.collector.type.ConfigurationType.IMPORT_DIRECTORY)==null) {
			throw new IllegalStateException("\"import.directory\" property has not been set");
		}
		if (Configuration.getConfigurationProperty(job.getDomainInstanceName(),edu.ncsu.las.model.collector.type.ConfigurationType.IMPORT_SLEEPTIMESEC)==null) {
			throw new IllegalStateException("\"import.sleepTimeSec\" property has not been set");
		}
		
		// Make import directories
		String directory = Configuration.getConfigurationProperty(this.getDomainInstanceName(),edu.ncsu.las.model.collector.type.ConfigurationType.IMPORT_DIRECTORY);
		srcLogger.log(Level.INFO,"Ensuring import (upload) driectory exists: "+ directory);
		Paths.get(directory).toFile().mkdirs();
	}

	@Override
	public void startService() {  
		_executorService.scheduleWithFixedDelay(this, 0, Configuration.getConfigurationPropertyAsLong(this.getDomainInstanceName(),edu.ncsu.las.model.collector.type.ConfigurationType.IMPORT_SLEEPTIMESEC), TimeUnit.SECONDS);
	}

	@Override
	public JSONObject getHandlerDefaultConfiguration() {
		return new JSONObject();
	}

	@Override
	public String getSourceHandlerName() {
		return "DirectoryWatcher";
	}

	@Override
	public String getDescription() {
		return "Service-based source handler that starts when the collector begins.  It listens for files and then processes those into the document router";
	}
	
	@Override
	public ParameterLabelType getPrimaryLabel() {
		return ParameterLabelType.NOT_APPLICABLE;
	}

	@Override
	public String getSampleConfiguration() {
		return "{}";
	}
	
	@Override
	public java.util.TreeMap<String, SourceParameter> getConfigurationParameters() {
		return new java.util.TreeMap<String,SourceParameter>();
	}

	@Override
	public boolean stop() {
		_executorService.shutdown();
		return true;
	}
	
	@Override
	public void forceShutdown() {
		_executorService.shutdownNow();
	}	
	
	
	public boolean isService() {
		return true;
	}	
	
	private static boolean isDirEmpty(final Path directory) throws IOException {
	    try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
	        return !dirStream.iterator().hasNext();
	    }
	}
	
	public void process() {
		throw new IllegalAccessError("Service-based threads utilize run()");
	}

	@Override
	public void run() {

		//srcLogger.log(Level.FINEST, "DirectoryWatcher starting");
		Path startingDir = Paths.get(Configuration.getConfigurationProperty(this.getDomainInstanceName(),edu.ncsu.las.model.collector.type.ConfigurationType.IMPORT_DIRECTORY));
		srcLogger.log(Level.INFO, "DirectoryWatcher starting: "+startingDir);
		long time = System.currentTimeMillis(); // use this to not process any new files
		
		try {
			Files.walkFileTree(startingDir,	new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException ioe) throws IOException  {
					if (dir != startingDir && isDirEmpty(dir)) {
						Files.delete(dir);
						srcLogger.log(Level.FINE, "DirectoryWatch removed empty directory after processing: "+dir);
					}
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (attrs.lastModifiedTime().toMillis() < time) {
						long startTime = System.currentTimeMillis();
						srcLogger.log(Level.INFO, "DirectoryWatch found file: "+file);
						markActivity();
						
						Document doc = new Document(file.toFile(), new JSONObject(), new JSONObject().put("name", "directoryWatcher"),attrs.lastModifiedTime().toMillis(),getDomainInstanceName(),DirectoryWatcher.this.getJobHistory());
						doc.addAnnotation("html_title", DateUtilities.getCurrentDateTimeISODateTimeFormat().substring(0, 13)+": "+file.toFile().getName()); 
						getDocumentRouter().processPage(doc,"");
						Files.delete(file);
						Instrumentation.createAndSendEvent(getDomainInstanceName(), "document.upload", startTime, System.currentTimeMillis(), new JSONObject().put("filename", file.toString()), getJobHistory().getJobHistoryID());
						srcLogger.log(Level.INFO, "DirectoryWatch removed file after processing: "+file);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException e) {
			srcLogger.log(Level.WARNING, "DirectoryWatch exception: "+e);
		}
			

	}

	@Override
	public String getSourceHandlerDisplayName() {
		return "Directory Watcher";
	}


	
	/**
	 * directory watcher really doesn't have any configuration
	 * throws illegal state exception as this should not be called
	 */
	public java.util.List<String> validateConfiguration(JSONObject configuration) {
		throw new IllegalStateException("validate configuration called on directory watcher");
	}

	/**
	 * directory watcher really doesn't have any configuration / limits as no jobs should be tied to this.
	 * throws illegalSTateException as this should not be called
	 */
	public java.util.List<String> validateInstantCount(Job job) {
		throw new IllegalStateException("validate instant called on directory watcher");
	}
	
	
}
