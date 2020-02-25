package edu.ncsu.las.document;

import edu.ncsu.las.model.collector.JobHistory;
import edu.ncsu.las.model.collector.SearchRecord;

public interface DocumentCreatorInterface {
	public edu.ncsu.las.document.Document createDocument(SearchRecord ddr, String url, String domain, JobHistory jobHistory);
}
