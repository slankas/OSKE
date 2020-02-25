package edu.ncsu.las.storage.citation;

/**
 * Simple data record that maintains a list of the PMIDs and associated versions that 
 * should be removed from a pub med repository
 * 
 *
 */
public class DeleteCitationRecord {

	private long _pmid;
	private int  _version;
	
	public DeleteCitationRecord(long pmid, int version) {
		_pmid = pmid;
		_version = version;
	}
	
	public long getPMID()   { return _pmid;    }
	public int getVersion() { return _version; }
	
	public String toString() {
		return _pmid + "\t" + _version;
	}
}
