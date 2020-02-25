package edu.ncsu.las.document;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.ncsu.las.annotator.Annotator;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.storage.StorageProcessor;

/**
 * This class allows us to process certain annotations after the initial processing is complete.
 * Additionally, this class manages a linked list of the items to be processed so we can report
 * 
 *
 */
public class DocumentRouterSecondaryProcessor {

	
	private ExecutorService _taskPool = Executors.newFixedThreadPool(5);  //TODO: Make this is configuration parameter.

	private List<String> _queue = Collections.synchronizedList(new LinkedList<String>()); // What entries are currently.  These are really UUIDs
	
	private static DocumentRouterSecondaryProcessor _theSecondaryProcessor = new DocumentRouterSecondaryProcessor();
	private DocumentRouterSecondaryProcessor() {
	}
	
	public static final DocumentRouterSecondaryProcessor getTheSecondaryProcessor() {
		return _theSecondaryProcessor;
	}
	
	public void submitDocumentForSecondaryProcessing(Document currentDocument) {
		CallableTask ct = new CallableTask(currentDocument);
		_queue.add(currentDocument.getUUID());
		_taskPool.submit(ct);
	}
	
	public int getDocumentPositioninProcessingQueue(UUID uuid) {
		return _queue.indexOf(uuid.toString());
	}
	
	public int getProcessingQueueSize() {
		return _queue.size();
	}
	
	
	private  class CallableTask implements Runnable {
		private final Document _currentDocument;
		
		CallableTask(Document doc) {
			this._currentDocument = doc;
		}

		public void run() {
			Annotator.annotate(AnnotatorExecutionPoint.SECONDARY, _currentDocument);				
			
			StorageProcessor.getTheStorageProcessor().updateJSONData(_currentDocument);
			_currentDocument.markDocumentSaved();

			
			_queue.remove(_currentDocument.getUUID());
		  }

	}	
	
	
}
