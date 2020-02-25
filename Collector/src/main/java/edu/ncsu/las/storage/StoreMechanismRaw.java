package edu.ncsu.las.storage;

import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.FileStorageStatusCode;

/**
 * Classes that can store content collected through this system 
 * must implement this interface.  This is used to store the "raw" data that
 * was crawled ...
 * 
 */
public interface StoreMechanismRaw {

	/**
	 * Stores the file data based upon the configuration based.
	 * 
	 * @param domain   
	 * @param area     What area to store the data in .....
	 * @param fileName Name of the file to be saved
	 * @param data     Data to put into the file
	 * @param metaData Additional data to be stored.  This is optional. Most cases will be empty
	 * @return whether the operation succeeeded or not
	 */
	public FileStorageStatusCode store(String domain, FileStorageAreaType area, String fileName, byte[] data, JSONObject metaData);
	
	
	/**
	 * retrieves the file data based upon the configuration based.
	 * 
	 * @param config   JSON object that defines the storage parameters
	 * @param area     What area does the data exist in .....
	 * @param fileName Name of the file to be saved
	 * return byte array of data from the stored file.
	 */
	public byte[] retrieve(String domain, FileStorageAreaType area,String fileName);	
	
	
	/**
	 * Removes the file data based upon the configuration based.
	 * 
	 * @param area     What area to remove the data from
	 * @param config   JSON object that defines the storage parameters
	 * @param fileName Name of the file to be saved
	 * return whether or no the deletion was successful.
	 */
	public FileStorageStatusCode delete(String domain, FileStorageAreaType area, String fileName);	
	
	
	/**
	 * Moves the file data based upon the configuration based into the specified archive location
	 * from within the configuration
	 * 
	 * @param config   JSON object that defines the storage parameters
	 * @param srcArea   what area to move the data from
	 * @param destArea  what area to move the data to
	 * @param fileName Name of the file to be saved
	 * 
	 * return whether or no the deletion was successful.
	 */
	public FileStorageStatusCode move(String domain, FileStorageAreaType srcArea, FileStorageAreaType destArea, String fileName);	
	
}
