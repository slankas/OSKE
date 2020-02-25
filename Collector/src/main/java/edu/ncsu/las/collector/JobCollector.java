package edu.ncsu.las.collector;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.collector.util.ApplicationConstants;
import edu.ncsu.las.model.collector.SearchAlert;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.Instrumentation;
import edu.ncsu.las.model.collector.Job;
import edu.ncsu.las.model.collector.JobHistory;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;
import edu.ncsu.las.source.AbstractHandler;
import edu.ncsu.las.storage.ElasticSearchDomainDiscoveryQuery;
import edu.ncsu.las.storage.export.ExportAssistant;
import edu.ncsu.las.util.crypto.AESEncryption;


/**
 * The "JobCollector" is a daemon-based process that can crawl a variety of internet-based
 * resources to bring back relevant data.
 * 
 * Two property files are used:
 * - "application_properties.json"
 * - "local_properties.json" - used to provide local overrides as necessary.
 * 
 * The property files are located in the current working directory unless the
 * environment variable COLLECTOR_CONFIG_DIR is set - in which case it uses that directory location
 *
 * Command-line arguments:
 * --cleanup    Application will read configuration, check for orphaned records, and then exit.
 * --help       Prints a message about how to start the application.
 * 
 * This class implements runnable so that the main loop run on a periodic basis being executed by
 * a ScheduledFixedThreadPool on a FixedDelay basis. 
 * 
 *
 */
public class JobCollector implements Runnable {
	private static Logger logger =Logger.getLogger(JobCollector.class.getName());
	
	private java.util.ArrayList<AbstractHandler> _currentJobs = new java.util.ArrayList<AbstractHandler>();
	
	private static JobCollector _theCollector;
	
	public static JobCollector getTheCollecter() {
		if (_theCollector == null) {
			_theCollector = new JobCollector();
		}
		return _theCollector;
	}	

	private JobCollector()  {
	}

	private void printCurrentJobStatus() {
		logger.log(Level.INFO, "Current Job Status");
		logger.log(Level.INFO, "  Active job count: "+_currentJobs.size());
		logger.log(Level.INFO, "  name,handler,ms since last activity, num activities");
		long currentTime=System.currentTimeMillis();
		for (AbstractHandler shi: _currentJobs) {
			Job j = shi.getJob();
			String name = shi.getSourceHandlerName();
			if (j != null) { // Services don't have jobs
				name= j.getName();
			}
			logger.log(Level.INFO, "  "+name+","+shi.getSourceHandlerName()+","+(currentTime-shi.getLastActivityTime())+","+shi.getNumberOfActivities());
		}
	}
	
	/**
	 * This routine checks to see if the daemon should be restart
	 * if there are spurious concept annotations that have not been completed
	 * (ie, these threads are caught in regex backtrack hell),
	 * Then kill the daemon.  
	 * We'll rely upon systemd or some other functionality (eg, docker restart policy)
	 * to start the daemon.  
	 */
	private void checkToRestartDaemon() {
		if (_currentJobs.size() == 0) {
			boolean foundConceptAnnotator = false;
			
			java.util.Map<Thread,StackTraceElement[]> traces = Thread.getAllStackTraces();

			for (Thread t: traces.keySet()) {
				for (StackTraceElement e: traces.get(t)) {
					String fullMethodName = e.getClassName()+"."+e.getMethodName();
					if (fullMethodName.contains("edu.ncsu.las.model.collector.concept.Concept")) {
						foundConceptAnnotator = true;
						logger.log(Level.SEVERE, "ConceptAnnotator found in spurious thread: "+ t.getName());
						logger.log(Level.SEVERE, "Stack trace: " + Arrays.asList(traces.get(t)).stream().map(Objects::toString).collect(Collectors.joining("\n")));
						break;
					}
				}
			}
			
			if (foundConceptAnnotator) {
				logger.log(Level.SEVERE, "Shutting down the collector");
				System.exit(5);
			}
		}
	}
	
	
	private void checkForInactiveJobs() {
		logger.log(Level.INFO, "Check for inactive jobs:");
		
		long maxInactiveTimeAllowed = Configuration.getConfigurationPropertyAsLong(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_IDLEKILLTIMESEC) * 1000;
		
		long currentTime=System.currentTimeMillis();
		for (int i=_currentJobs.size()-1; i>=0; i--) {
			AbstractHandler shi = _currentJobs.get(i);
			if (shi.isService()) { continue; } // services (such as directory watcher) are excluded from this check.
			
			long timeSinceLastActivity = currentTime-shi.getLastActivityTime();
			if (timeSinceLastActivity > maxInactiveTimeAllowed) {
				JobHistory jh = shi.getJobHistory();
				Job job = shi.getJob();
				jh.updateStatusAndComments(JobHistoryStatus.KILLED,"no activity for "+ timeSinceLastActivity+"ms");
				job.markErrored(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_ID));
				shi.forceShutdown(); 
				shi.getExecutorService().shutdownNow();
				_currentJobs.remove(i);
				logger.log(Level.SEVERE, " -- removed inactive job -  "+job.getName()+", inactive for "+ timeSinceLastActivity+"ms");
				
				long startTime = shi.getJobHistory().getStartTime().getTime();
				long endTime = System.currentTimeMillis();
				Instrumentation.createAndSendEvent(job.getDomainInstanceName(), "daemon.job.stop-inactive", "jobData", startTime, endTime, job.toJSON(), shi.getJobHistory().getJobHistoryID());
			}
		}
	}	
	
	
	private void checkForJobsToStop() {
		java.util.List<Job> jobsToStop = Job.getJobsToStop();
		logger.log(Level.INFO, " jobs to stop - "+jobsToStop.size());
		
		for (Job job: jobsToStop) {
			AbstractHandler shi = this.getCorrespondingSourceHandler(job.getLatestJobID());
			if (shi == null) {
				logger.log(Level.INFO, "Job to stop does not exist on this collector: "+job);
			}
			else {
				logger.log(Level.INFO, "Stopping job: "+job);
				shi.stop();
				long startTime = shi.getJobHistory().getStartTime().getTime();
				long endTime = System.currentTimeMillis();
				Instrumentation.createAndSendEvent(job.getDomainInstanceName(), "daemon.job.stop-requested", "jobData",startTime, endTime, job.toJSON(), shi.getJobHistory().getJobHistoryID());
			}
			
		}		
	}
	
	private void processAvailableJobs() {
		java.util.List<Job> jobs = Job.getJobsAvailableForProcessing();
		logger.log(Level.INFO, "jobs available for processing - "+jobs.size());

		String collectorID = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_ID);
		
		for (Job job: jobs) {
			if (Collector.getTheCollecter().getDomain(job.getDomainInstanceName()).isOffline()) {
				logger.log(Level.INFO, "Domain offline ("+job.getDomainInstanceName()+"), skipping "+job.getName());
				continue;
			}
			
			AbstractHandler shi = AbstractHandler.getSourceHandler(job.getSourceHandler());
			if (shi == null) {
				logger.log(Level.SEVERE, "No SourceHandlerInterface found for - "+job.getSourceHandler());
				job.markErrored(collectorID);
				continue;
			}
			if (shi.isService()) { continue; }
			long maxPoolSize = Configuration.getConfigurationPropertyAsLong(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_JOB_POOLSIZE);
			if (this._currentJobs.size() >= maxPoolSize) {
				logger.log(Level.INFO, "unable to process pending job (queue full): "+job);
				continue;
			}
			//claim job, initiate a new job record, process, and mark complete
			UUID jobHistoryID = edu.ncsu.las.util.UUID.createTimeUUID();
			if (job.claimJobToRun(jobHistoryID,collectorID) == false) { // we were not able to claim the job as another collector has it
				logger.log(Level.INFO, "another collector has claimed Job to process - "+job);
				continue;
			}
			
			JobHistory jh = JobHistory.initiateJob(jobHistoryID, job.getID(), job.getName(), JobHistoryStatus.PROCESSING, "", collectorID,job.getDomainInstanceName());
					
			shi.initialize(jh, job);

			_currentJobs.add(shi);
			
			ExecutorService es = Executors.newSingleThreadExecutor();
			es.execute( new Runnable() {
				public void run() {
					long startTime = shi.getJobHistory().getStartTime().getTime();
					Instrumentation.createAndSendEvent(job.getDomainInstanceName(), "daemon.job.start", "jobData", startTime, null, job.toJSON(), shi.getJobHistory().getJobHistoryID());

					shi.process();
					
					// see if we need to export the job's data for transfer
					String jobID = shi.getJobHistory().getJobHistoryID().toString();
					System.out.println("job processed, id = "+jobID);
					if (job.getExportData()) {
						System.out.println("*** job export data is "+job.getExportData());
						
						String domainStr = job.getDomainInstanceName();
						
						User user = new User(job.getOwnerEmail());
						
						JSONObject optionObject = new JSONObject();
						optionObject.put("destination", "directory");
						optionObject.put("format", "indJSON");
						optionObject.put("naming", "uuid");
						optionObject.put("currPage", job.getName());
						
						JSONObject queryClause = ElasticSearchDomainDiscoveryQuery.createQueryClauseForJob(jobID);
						try {
							ExportAssistant.processDirectory(domainStr, FileStorageAreaType.SANDBOX, user, queryClause, optionObject);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					long endTime = System.currentTimeMillis();
					

					JSONObject eventDetails = job.toJSON();
					eventDetails.put("jobHistory", shi.getJobHistory().toJSON());
					eventDetails.put("blockedURLs", shi.getBlockedURLs());
					eventDetails.put("blockedProcessURLs", shi.getBlockedProcessURLs());
					int blockedURLCount = 0;
					for (String key: eventDetails.getJSONObject("blockedURLs").keySet()) {
						JSONArray a = eventDetails.getJSONObject("blockedURLs").getJSONArray(key);
						blockedURLCount += a.length();
					}
					eventDetails.put("blockedURLCount", blockedURLCount);
					
					Instrumentation.createAndSendEvent(job.getDomainInstanceName(), "daemon.job.stop-normal", "jobData", startTime, endTime, eventDetails, shi.getJobHistory().getJobHistoryID());

				}
			});
			shi.setExecutorService(es);  // ties the job to the execution within it executes.  allows for "killing" of that service
		}		
	}
	
	/**
	 * Primary application loop/processing.  (loop is driven in the main process through an Executor)
	 */
	public void run() {
		long startTime = System.currentTimeMillis();

		try {
			Instrumentation.createAndSendEvent(Domain.DOMAIN_SYSTEM, "daemon.processLoop.start", startTime, null, null, null);

			logger.log(Level.INFO, "Start process loop");
						
			Collector collector = Collector.getTheCollecter();
			
			logger.log(Level.FINER, " site rules count: "+ collector.getSiteRules(Domain.DOMAIN_SYSTEM).size());
			logger.log(Level.FINER, " source handler count:  "+ collector.getSourceHandlers().size());
			logger.log(Level.FINER, " document handler count: "+ collector.getDocumentHandlers().size());
			
			this.checkForDomainRefresh();
			this.printCurrentJobStatus();
			this.checkToRestartDaemon();
			this.checkForInactiveJobs();
			this.checkForJobsToStop();
			this.processAvailableJobs();
			SearchAlert.executeWaitingAlerts();
			ExportAssistant.deleteTempExportFiles();
			
			logger.log(Level.INFO, "End process loop");
			Instrumentation.createAndSendEvent(Domain.DOMAIN_SYSTEM, "daemon.processLoop.end", startTime, System.currentTimeMillis(), null, null);
		}
		catch (Throwable t) {
			logger.log(Level.SEVERE, "Unable able to process",t);
			Instrumentation.createAndSendEvent(Domain.DOMAIN_SYSTEM, "daemon.processLoop.exception", System.currentTimeMillis(), null, (new JSONObject()).put("exception",t.toString()), null);
			//Let's just exit the run-loop.  This may cause the error to appear every x minutes, but this temporary communication
			//problems won't be impacted.
		}
	}
	
	/**
	 * Checks if the domains should be re-freshed.
	 * Currently, this is done every hour regardless if any change has been made to the domains
	 * 
	 */
	private void checkForDomainRefresh() {
		long lastRefreshTime = Collector.getTheCollecter().getLastDomainRefreshTime();
		long currentTime = System.currentTimeMillis();
		
		if (currentTime > (lastRefreshTime + ( 1000 * 60 *60))) {           // has it been an hour since we last refreshed the domains ....
			Collector.getTheCollecter().refreshDomains();
			Instrumentation.createAndSendEvent(Domain.DOMAIN_SYSTEM, "daemon.domain.refresh", currentTime, System.currentTimeMillis(), null, null);
		}
		
	}


	/**
	 * Runs the initializes and processes any source handlers marked as services.
	 * 
	 * In most cases, these source handlers will initiate their own threads and then return.
	 */
	public void startSourceServices() {
		if ( Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_ALLOW_SERVICES) == false) {
			logger.log(Level.INFO, "Services not allowed for this instance - skipping startSourceService()");
			return;
		}
		for (Domain domain: Collector.getTheCollecter().getAllDomains().values()) {
			if (domain.isOffline()) {
				logger.log(Level.INFO, "Domain offline, not establishing source services: "+domain.getDomainInstanceName());
				continue;
			}
			
			domain.startSourceServices();
		}		
	}
	
	/**
	 * Runs the initializes and processes any source handlers marked as interactive.
	 * Generally speaking, the daemon process won't call this - only the web application.
	 * 
	 * In most cases, these source handlers will initiate their own threads and then return.
	 */
	public void startInteractiveServices() {
		for (Domain domain: Collector.getTheCollecter().getAllDomains().values()) {
			if (domain.isOffline()) {
				logger.log(Level.INFO, "Domain offline, not establishing interactives services: "+domain.getDomainInstanceName());
				continue;
			}
			domain.startInteractiveServices();
		}
	}
	
	
	public void cleanOrphanedRecords() {
		logger.log(Level.INFO, "checking for orphaned job and job history records");
		String collectorID = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_ID);
		logger.log(Level.INFO, "cleaned orphaned job history records: " + JobHistory.updateOrphanedRecords(collectorID));
		logger.log(Level.INFO, "cleaned orphaned job records: " + Job.updateOrphanedJobs(collectorID));
	}
	
	/**
	 * SourceHandlers call this method when they are finished processing.  
	 * 
	 * @param shi
	 * @param status
	 * @param message
	 */
	public void sourceHandlerCompleted(AbstractHandler shi, JobHistoryStatus status, String message) {
		_currentJobs.remove(shi);
		
		JobHistory jh = shi.getJobHistory();
		Job job = shi.getJob();
		
		String id = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_ID);
		
		jh.updateStatusAndComments(status,message);
		switch (status) {
		case INVALID_PARAMS:
		case ERRORED:	job.markErrored(id);  break;
		case STOPPED:	job.markForHold(id);  break;
		case COMPLETE:	job.markComplete(id); 
		                job.updateNextRun();
		                break;
		default: 		logger.log(Level.WARNING, " unhandeled job status code - "+status);
		}

		
	}
	
	public AbstractHandler getCorrespondingSourceHandler(java.util.UUID jobID) {
		for (AbstractHandler shi: _currentJobs) {
			Job j = shi.getJob();
			if (j==null) {continue;} // this is a service-based handler (ie, DirectoryWatcher)
			if (j.getLatestJobID().equals(jobID)) {
				return shi;
			}
		}
		
		return null;
	}
		
	public static void printUsageStatement(java.io.PrintWriter pw) {
		pw.println("usage: java edu.ncsu.las.collector.JobCollector [--help][--cleanup]");
		pw.println("  --help displays this message and exits");
		pw.println("  --cleanup checks for any orphaned records, updates those with an appropriate status message, ");
		pw.println("            and then exits.");
		pw.println("");
		pw.println("Configuration: The collector system requires two configuration files: ");
		pw.println("        application_properties.json - used for generally configuration across multiple instances");
		pw.println("        local_properties.json       - used for specific instances");
		pw.println("    The property files either need to be in the current working directory when the application starts");
		pw.println("    or specified in the directory pointed at by the COLLECTOR_CONFIG_DIR environment variable.");
		pw.println("    While comments technically are not allowed within json file, the application does allow comments ");
		pw.println("    to be present.");
		pw.println();
	}
	
	
	
	public static void main(String[] args) {

		
		
		try {
			File currentDirectory = new File(new File(".").getAbsolutePath());
			String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
						
			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
			
			logger.log(Level.INFO,"JobCollector starting....");
			logger.log(Level.INFO,"Validating unlimited strength policy files....");
			if (AESEncryption.hasUnlimitedStrengthPolicy() == false) {
				logger.log(Level.SEVERE, "JobCollector halting: Unlimited Strength Jurisdiction Policy Files are not installed for Java.");
				System.exit(1);
			}
			
			logger.log(Level.INFO,"Starting initialization....");
			Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",true,true,true);
			JobCollector jobCollector = JobCollector.getTheCollecter();
			
			//MRK: We may want to refactor to use "https://commons.apache.org/proper/commons-cli/"
			if (args.length >0) {
				for (String arg: args) {
					if (arg.equals("--help")) {
						JobCollector.printUsageStatement(new java.io.PrintWriter(System.out));
						System.exit(0);
					}
				}				
				for (String arg: args) {
					if (arg.equals("--cleanup")) {
						jobCollector.cleanOrphanedRecords();
						System.exit(0);
					}
				}
			}
			
			logger.log(Level.INFO,"JobCollector initialized");

			Instrumentation.createAndSendEvent(Domain.DOMAIN_SYSTEM, "daemon.start", System.currentTimeMillis(), null, null, null);
			
			jobCollector.cleanOrphanedRecords();
			jobCollector.startSourceServices();

			logger.log(Level.INFO,"JobCollector cleanup complete, scheduling ongoing deamon");
			
			Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(jobCollector, 0, Configuration.getConfigurationPropertyAsLong(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_SLEEPTIMESEC), TimeUnit.SECONDS);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
		
}
