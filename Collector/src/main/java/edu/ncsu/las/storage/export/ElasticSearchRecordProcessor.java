package edu.ncsu.las.storage.export;

import java.io.IOException;

/**
 * Serves as a call-back function for a class to implement to 
 * process records from an ElasticSearch search.
 *
 */
public interface ElasticSearchRecordProcessor {

	public void processRecord(ElasticSearchRecord record) throws IOException;
}
