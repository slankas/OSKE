package edu.ncsu.las.source;

import static java.lang.reflect.Modifier.isAbstract;

import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.Reflections;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.collector.JobCollector;
import edu.ncsu.las.document.DocumentRouter;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Job;
import edu.ncsu.las.model.collector.JobHistory;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;


public abstract class AbstractHandler implements SourceHandlerInterface {
	
	
	private DocumentRouter _documentRouter;
	public DocumentRouter getDocumentRouter() {
		return _documentRouter;
	}
	
	private JobHistory _jobHistory;
	public void setJobHistory(JobHistory newJobHistory) { _jobHistory = newJobHistory; }
	public JobHistory getJobHistory() {
		return _jobHistory;
	}
	
	private String _domainInstanceName;
	public String getDomainInstanceName() {
		return _domainInstanceName;
	}
	
	// This allows us to set the user agent value when running in a stand-alone mode by explicitly setting it versus
	// having to retrieve it from the Configuration loaded in the database.
	
	private String _userAgent = null;
	
	public void setUserAgent(String newValue) { _userAgent = newValue; }
	
	public String getUserAgent() {
		if (_userAgent == null) {
			return SourceHandlerInterface.getNextUserAgent(this.getDomainInstanceName());
		}
		else {
			return _userAgent;
		}
	}
	
	/**
	 * What is the maximum number of search results that this handler can produce?
	 * 
	 * @return if not a search-based handler, -1  should be returned
	 */
	public int getMaximumNumberOfSearchResults() {
		return -1;
	}
	
	/** 
	 * Used to setup a SourceHandlerInterface.  By having this separate from a constructor, it's easier to create
	 * the initial object.
	 * 
	 * @param jobCollector
	 * @param jobRecord
	 * @param job
	 */
	public void initialize(JobHistory jobRecord, Job job) {		
		_jobHistory = jobRecord;
		_documentRouter = new DocumentRouter(job.getDomainInstanceName(), Collector.getTheCollecter().getDocumentHandlers(),jobRecord,job);
		_documentRouter.setSourceHandler(this);
		_domainInstanceName = job.getDomainInstanceName();
		
		this.setJob(job);
		
		//TODO:  need to set the relevancy pattern on othe document router at some point.  This is on processInternal now 
		//_router.setRelevancyPattern(_relevancyPattern);
		
	}
	
	
	private SourceParameterRepository _sourceParameters;

	@Override
	public SourceParameterRepository getSourceParameterRepository() {
		if (_sourceParameters == null) {
			_sourceParameters = new SourceParameterRepository(this.getConfigurationParameters());
		}

		return _sourceParameters;
	}	
	
	
	/**
	 * By default, we will check to make sure all required parameters are
	 * present. Any parameters present will have their types checked as well.
	 *
	 * @return list of possible issues. Array will be empty if no issues found.
	 */
	@Override
	public List<String> validateConfiguration(String domain, String primaryField, JSONObject configuration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();

		errors.addAll(this.getSourceParameterRepository().validateConfiguration(configuration));

		if (this.getPrimaryLabel().equals(ParameterLabelType.URL)) {
			try {
				new URL(primaryField);
			}
			catch (Exception e) {
				errors.add("Valid URL must be specified.");
			}
		}
		else if (this.getPrimaryLabel().equals(ParameterLabelType.SEARCH_TERMS)) {
			if (primaryField.trim().length() == 0) {
				errors.add("Search terms must be entered.");
			}
		}
		
		return errors;
	}	
	
	//Link to our job
	private Job _job;
	
	/**
	 * What's the job that's currently associated with this SourceHandler?
	 */
	public Job getJob() { return _job; }
	
	public void setJob(Job job) { _job = job; } 
	
	/**
	 * Gets the job's configuration for this handler.
	 * @return
	 */
	public  JSONObject getJobConfiguration() {
		return this.getJob().getConfiguration();
	}
	
	
	//Metrics and current status
	private java.util.concurrent.atomic.AtomicLong _numActivities = new java.util.concurrent.atomic.AtomicLong(0);
	private long _lastActivityTime = System.currentTimeMillis();
	private JobHistoryStatus _jobHistoryStatus = JobHistoryStatus.PROCESSING;
	
	public void markActivity() {
		_numActivities.incrementAndGet();
		_lastActivityTime = System.currentTimeMillis();
	}
	
	/**
	 * How many pages have been retrieved?
	 * 
	 * @return 
	 */
	public final long getNumberOfActivities() {
		return _numActivities.get();
	}
	
	/** 
	 * When was the last page retrieved for this SourceHandler?
	 * 
	 * @return time in milliseconds (as generated from System.currentTimeMillis).
	 */
	public final long getLastActivityTime() {
		return _lastActivityTime;
	}
	
	public final void setJobHistoryStatus(JobHistoryStatus status) { _jobHistoryStatus = status; }
	
	/**
	 * What is the current status of the Source?
	 * 
	 * @return
	 */
	public final JobHistoryStatus getJobHistoryStatus() { return _jobHistoryStatus; }
	
	
	
	
	private JSONObject _blockedURLs = new JSONObject(); //  fields are arrays of the URL.  The field names are the blocked reasons.  These are URLs blocked during the crawling
	
	private JSONObject _blockedProcessURLs = new JSONObject(); //  fields are arrays of the URL.  The field names are the blocked reasons.  URLs blocked from saving/processing
	
	
	
	/**
	 * Returns a JSON object containing arrays (indexed by reason) as to why the URL was not stored/processed
	 * 
	 * @return
	 */
	public JSONObject getBlockedProcessURLs() {
		return _blockedProcessURLs;
	}
	
	public void addBlockedProcessURL(String reason, String url) {
		if (_blockedProcessURLs.has(reason) == false) {
			_blockedProcessURLs.put(reason, new JSONArray());
		}
		JSONArray a = _blockedProcessURLs.getJSONArray(reason);
		
		boolean contains = false;  // let's only track the URL once per reason
		for (int i=0;i<a.length();i++) {
			if (a.getString(i).equals(url)) {
				contains = true;
				break;
			}
		}
		if (contains == false) {
			a.put(url);
		}
	}	
	
	public JSONObject getBlockedURLs() {
		return _blockedURLs;
	}
	
	public void addBlockedURL(String reason, String url) {
		if (_blockedURLs.has(reason) == false) {
			_blockedURLs.put(reason, new JSONArray());
		}
		JSONArray a = _blockedURLs.getJSONArray(reason);
		
		boolean contains = false;  // let's only track the URL once per reason
		for (int i=0;i<a.length();i++) {
			if (a.getString(i).equals(url)) {
				contains = true;
				break;
			}
		}
		if (contains == false) {
			a.put(url);
		}
	}
	
	
	/** 
	 * Used to setup a SourceHandlerInterface.  By having this separate from a constructor, it's easier to create
	 * the initial object.
	 * 
	 * @param collector
	 * @param jobRecord
	 * @param job
	 */
	public void initializeInteractiveService(JobCollector collector, JobHistory jobRecord, Job job) { 
		throw new IllegalStateException("initializeInteractiveServices can only be called on services marked as interactive");
	}
	
	
	// By default jobs are executed within the context of an ExecutorService
	// from the JobCollector Daemon.  By associated the executorService with a 
	// specific source handler, we can use that reference to force a job to shutdown
	ExecutorService _executor;
	public void setExecutorService(ExecutorService es) {
		_executor = es;
	}
	
	public ExecutorService getExecutorService() {
		return _executor;
	}
	
	
	/**
	 * Convenience method to get the job collector
	 * 
	 * @return
	 */
	public JobCollector getJobCollector() {
		return JobCollector.getTheCollecter();
		
	}
	
	

	
	/**
	 * Used to perform "ad-hoc" crawls of links that may be found within source content. 
	 * 
	 * a list of the last 1,000 (MAX_ENTRIES) urls that have been visited is maintained.  If a link is within that set, it is skipped.
	 * The list resets for each new job or when the server is restarted.
	 * 
	 * @param domainName
	 * @param router
	 * @param jobRecord
	 * @param urlsToVisit
	 * @throws Exception
	 */
	public void crawlAdHocURLs(String domainName, DocumentRouter router, Job jobRecord, java.util.Set<String> urlsToVisit) throws Exception {
		for (String visitURL: urlsToVisit) {
			if (!visitedURLs.contains(visitURL)) {
				visitedURLs.add(visitURL);
				
				try {
					edu.ncsu.las.document.Document entityDocument = new edu.ncsu.las.document.Document(edu.ncsu.las.util.UUID.createTimeUUID(),visitURL,jobRecord.getConfiguration(),jobRecord.getSummary(), domainName, this.getJobHistory());
					router.processPage(entityDocument, "");
				}
				catch (Throwable t) {
					if (t instanceof java.io.FileNotFoundException) {
						srcLogger.log(Level.INFO, "Unable to adhoc URL not found: "+visitURL);
					}
					else {
						srcLogger.log(Level.WARNING, "Unable to parse adhoc URL: "+visitURL, t);
					}
				}
			}
		}
	}	
	
	public boolean isOnline() {
		return true;
	}
	
	
	//Track whether or not we have manually stopped a job
	private boolean _jobManuallyStopped = false;
	
	/**
	 * Used to stop a source handler from continued execution.
	 * 
	 * @return
	 */
	public boolean stop() {
		_jobManuallyStopped = true;
		
		return false;   //what are we doing with ths???
 	}
	
	public boolean isManuallyStopped() {
		return _jobManuallyStopped;
	}
	
	
	/**
	 * Used to help kill an inactive process...
	 * 
	 */
	public abstract void forceShutdown();	
	
	
	public static java.util.HashMap<String, AbstractHandler> getAllSourceHandlers() {
		Reflections reflections = new Reflections("edu.ncsu.las");    
		java.util.Set<Class<? extends AbstractHandler>> classes = reflections.getSubTypesOf(AbstractHandler.class);
	
		java.util.HashMap<String, AbstractHandler> result = new java.util.HashMap<String, AbstractHandler>();
		for (Class<? extends AbstractHandler> c: classes) {
			if (!isAbstract(c.getModifiers())) {
				try {
					AbstractHandler shi = c.newInstance();
					if (shi.isOnline()) {result.put(shi.getSourceHandlerName(), shi); }
				} catch (Exception e) {
					srcLogger.log(Level.SEVERE, "Unable to create instance of class: " + c.getName());
					srcLogger.log(Level.SEVERE, "Unable to create instance of class - exception: " + e);
				}
			}
		}
		
		return result;
	}	
	
	public static java.util.ArrayList<SourceHandlerInterface> getSourceHandlersForJobs(String domain) {
		Reflections reflections = new Reflections("edu.ncsu.las");    
		java.util.Set<Class<? extends SourceHandlerInterface>> classes = reflections.getSubTypesOf(SourceHandlerInterface.class);
	
		java.util.ArrayList<SourceHandlerInterface> result = new java.util.ArrayList<SourceHandlerInterface>();
		
		JSONArray array = Configuration.getConfigurationPropertyAsArray(domain, ConfigurationType.SOURCE_HANDLERS);
		java.util.HashSet<String> availableHandlers = new java.util.HashSet<String>();
		for (int i=0;i<array.length();i++) { availableHandlers.add(array.getString(i)); }
		
		for (Class<? extends SourceHandlerInterface> c: classes) {
			if (!isAbstract(c.getModifiers())) {
				try {
					SourceHandlerInterface shi = c.newInstance();
					if (shi instanceof DirectoryWatcher ||
						shi instanceof DomainDiscoveryHandler) { 
						continue; 
					}
					if (!availableHandlers.contains(shi.getSourceHandlerName())) { continue;}
					if (shi.isOnline()) {result.add(shi); }
				} catch (Exception e) {
					srcLogger.log(Level.SEVERE, "Unable to create instance of class: "+ c.getName());
					srcLogger.log(Level.SEVERE, "Unable to create instance of class - exception: "+ e);
				}
			}
		}
		result.sort((SourceHandlerInterface o1, SourceHandlerInterface o2)-> o1.getSourceHandlerName().compareTo(o2.getSourceHandlerName()));
		return result;
	}
	
	public static java.util.ArrayList<SourceHandlerInterface> getSourceHandlersForDomainDiscovery(String domain) {
		java.util.ArrayList<SourceHandlerInterface> result = getSourceHandlersForJobs(domain);
		
		for (int i= result.size()-1; i >= 0; i--) {
			if (result.get(i).supportsDomainDiscovery() == false) {  // sourceHandlerForJobs already tests for being online so that check isn't added here.
				result.remove(i);
			}
		}
		return result;
	}		
		
	
	/**
	 * Searches for a SourceHandler with a given name and returns an instance of that source handler.
	 * Null is returned if the source handler is not found.
	 * 
	 * @param name
	 * @return
	 */
	public static AbstractHandler getSourceHandler(String name) {
		Reflections reflections = new Reflections("edu.ncsu.las");    
		java.util.Set<Class<? extends AbstractHandler>> classes = reflections.getSubTypesOf(AbstractHandler.class);
	
		for (Class<? extends AbstractHandler> c: classes) {
			if (!isAbstract(c.getModifiers())) {
				try {
					AbstractHandler shi = c.newInstance();
					if (shi.getSourceHandlerName().equals(name)){
						return shi;
					}
				} catch (Exception e) {
					srcLogger.log(Level.SEVERE, "Unable to create instance of class: "+ c.getName());
					srcLogger.log(Level.SEVERE, "Unable to create instance of class - exception: "+ e);
				}
			}
		}
		
		return null;
	}	
	
}
