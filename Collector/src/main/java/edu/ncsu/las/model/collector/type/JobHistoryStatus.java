package edu.ncsu.las.model.collector.type;


public enum JobHistoryStatus {
		
	PROCESSING("processing"), // job currently running
	COMPLETE("complete"),     // job has processed successfully 
	CANCELLED("cancelled"),   // 
	KILLED("killed"),         // collector parent process was killed.
	STOPPED("stopped"),       // how different from cancelled???
	STOPPING("stopping"),       // user requested job to be stopped.
	INVALID_PARAMS("invalid_params"), // job was configured with invalid parameters
	RESTARTED("restarted"),    // job was re-configured, need to restart the processing.
	ERRORED("errored");       // error occurred while running
		
	private String _label;

	private JobHistoryStatus(String label) {
		_label = label;
	}
	
	public String toString() { return _label; }
	
	public static JobHistoryStatus getEnum(String label) {
		return JobHistoryStatus.valueOf(label.toUpperCase());
	}
}
