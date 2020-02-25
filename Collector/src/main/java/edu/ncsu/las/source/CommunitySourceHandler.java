package edu.ncsu.las.source;

import static edu.ncsu.las.model.collector.type.SourceParameter.putSourceParameter;
import static edu.ncsu.las.model.collector.type.SourceParameterType.STRING;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.TreeMap;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;
import edu.ncsu.las.model.collector.type.SourceParameter;

/**
 * Functionality that is common for community based systems.
 *
 * Many discussion systems include common features such as users, threads,
 * posts, and replies. The {@link CommunitySourceHandler} provides basic
 * functionality that is common to a number of similar systems.
 *
 */
public abstract class CommunitySourceHandler extends AbstractHandler {
	public static class StopRequestedException extends RuntimeException {
		private static final long serialVersionUID = 3081260704171106557L;
		/*
		 * Simply a private exception used to support stopping a job.
		 * 
		 * This exception is thrown from markActivity if the job is in the
		 * "stopped" state.
		 */
	}

	private static final String SINCE_PROPERTY_KEY = "since";
	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject();

	/**
	 * Check parameters prior to job processing
	 *
	 * @return true if parameters are valid.
	 */
	protected abstract boolean checkParameters();

	/**
	 * Perform job processing
	 */
	protected abstract void doProcessing();

	/**
	 * Perform work needed to finalize the job processing.
	 */
	protected void finalizeProcessing() {
		// By default, nothing is needed
	}

	@Override
	public void forceShutdown() {
		this.stop();
	}

	@Override
	public TreeMap<String, SourceParameter> getConfigurationParameters() {
		TreeMap<String, SourceParameter> map = new TreeMap<>();
		putSourceParameter(map, SINCE_PROPERTY_KEY, "Date for the crawl start period.  Specify as an ISO-8601 timestamp.  Defaults to 24 hours ago.", false, "1971-04-22T17:22:56.526Z", false, STRING,false,false);
		return map;
	}

	@Override
	public final String getDescription() {
		return "Source handler for " + StringUtils.capitalize(this.getSourceHandlerName());
	}

	@Override
	public JSONObject getHandlerDefaultConfiguration() {
		return SOURCE_HANDLER_CONFIGURATION;
	}

	/**
	 * Get instant the source handler will use as it's starting point. (i.e.
	 * process all content "since" this instant).
	 * <p>
	 * If since is not defined we assume since 1 day prior to the current time.
	 */
	protected final Instant getSince() {
		String sinceString = Instant.now().minus(1, ChronoUnit.DAYS).toString();
		sinceString = this.getJobConfigurationFieldAsString(SINCE_PROPERTY_KEY, sinceString);
		return Instant.parse(sinceString);
	}


	@Override
	public final String getSourceHandlerDisplayName() {
		return StringUtils.capitalize(this.getSourceHandlerName()) + " Handler";
	}

	private boolean isStopRequested() {
		return this.isManuallyStopped();
	}

	/**
	 * Mark our handler data structure to indicate that activity occurred.
	 * <p>
	 * If activity occurs after a stop is requested the method will throw
	 * {@link StopRequestedException} and the handler data is not marked.
	 *
	 * @throws StopRequestedException
	 */
	public void markActivity() {
		if (this.isStopRequested()) { //TODO:  move this logic up
			throw new StopRequestedException();
		}
		super.markActivity();
	}

	/**
	 * Perform work needed to prepare for job processing
	 */
	protected void prepareForProcessing() {
		// By default, nothing is needed
	}

	@Override
	public final void process() {
		if (!this.checkParameters()) {
			String message = "Unable to process " + this.getJob().getName() + " - invalid parameters";
			srcLogger.log(Level.SEVERE, message);
			this.getJobCollector().sourceHandlerCompleted(this, JobHistoryStatus.INVALID_PARAMS, message);
			this.setJobHistoryStatus(JobHistoryStatus.INVALID_PARAMS);
			return;
		}
		srcLogger.log(Level.INFO, "Job processing starting: " + this.getJob().getName());
		JobHistoryStatus status;
		String message;
		try {
			this.prepareForProcessing();
			Instant newSince = Instant.now();
			this.doProcessing();

			srcLogger.log(Level.INFO, "Job processing completed: " + this.getJob().getName());

			status = JobHistoryStatus.COMPLETE;
			if (this.isManuallyStopped()) {
				status = JobHistoryStatus.STOPPED;
				message = "Job stopped upon request.";
			} else {
				message = "Completed " + this.getNumberOfActivities() + " activities";
			}
			this.finalizeProcessing();
			this.setSince(newSince);
			String id = Configuration.getConfigurationProperty(this.getDomainInstanceName(), ConfigurationType.COLLECTOR_ID);
			this.getJob().updateConfiguration(this.getJob().getConfiguration(), id);
		} catch (StopRequestedException sre) {
			status = JobHistoryStatus.STOPPED;
			message = "Job stopped upon request.";
		} catch (Throwable t) {
			status = JobHistoryStatus.ERRORED;
			message = t.getMessage();
			srcLogger.log(Level.SEVERE, "Job processing failed: " + this.getJob().getName(), t);
		}
		this.getJobCollector().sourceHandlerCompleted(this, status, message);
		this.setJobHistoryStatus(status);
		return;
	}

	private void setSince(Instant newSince) {
		this.getJob().getConfiguration().put(SINCE_PROPERTY_KEY, newSince.toString());
	}

}