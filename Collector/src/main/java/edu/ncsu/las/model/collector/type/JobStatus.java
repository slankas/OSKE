package edu.ncsu.las.model.collector.type;


public enum JobStatus {
		
	NEW("new",true),                   // job created, but not yet run. Legacy state. Now goes to adjudication and then run
	PROCESSING("processing",false),    // job currently running
	COMPLETE("complete",true),         // job has processed successfully and can run again
	SCHEDULED("scheduled",true),       // job has been approved by the adjudicator and is awaiting the next run time.  Also used when errored/hold job is moved off of that status.
	READY("ready",true),               // job was on an error or hold status, but can now run
	ERRORED("errored",true),           // error occurred while running
	STOPPING("stopping",false),        // user has elected to stop a running job
	HOLD("hold",true),                 // user has put the job on hold
	INACTIVE("inactive",true),         // either a source adjudicator has disapproved a job, or a user has marked it as now longer being active
	DRAFT("draft",true),               // newly created job.  needs to be submitted (which will then go into the adjudication phase)
	ADJUDICATION("adjudication",true), // job has just been created or edited.  Needs to be approved
	PURGING("purging",true);           // job is in the process of being purged will be deleted after this.
	
	private String _label;
	private boolean _editable;  // can jobs in this state be edited

	private JobStatus(String label, boolean editable) {
		_label = label;
		_editable = editable;
	}
	
	public String toString() { return _label; }
	
	public boolean isEditable() { return _editable; }
	
	public static JobStatus getEnum(String label) {
		return JobStatus.valueOf(label.toUpperCase());
	}
}
