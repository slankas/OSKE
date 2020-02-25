package edu.ncsu.las.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.FileStorageStatusCode;


public class FileStorage implements StoreMechanismRaw {
	private static Logger logger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	
	public static boolean validateEnvironment(java.util.Collection<String> domainList) {
		boolean result = true;
		for (String domainName: domainList) {
			if (domainName.equals(Domain.DOMAIN_SYSTEM)) { continue; }
			for (FileStorageAreaType fsat: FileStorageAreaType.values()) {
				String basePath = Configuration.getConfigurationProperty(domainName, fsat, ConfigurationType.FILE_STORE);
				logger.log(Level.INFO, "File storage, ensuring directory exists: "+basePath);
				File f = new File(basePath);
				if (f.exists() == false) {
					boolean currResult = f.mkdirs();
					if (currResult == false) {
						logger.log(Level.SEVERE, "Unable to create directory: "+basePath);
						result = false;
					}
				}
			}
		}
		return result;
	}
	
	@Override
	public FileStorageStatusCode store(String domain, FileStorageAreaType area, String fileName, byte[] data, JSONObject metaData) {
		String basePath = Configuration.getConfigurationProperty(domain, area, ConfigurationType.FILE_STORE);
		Path path = Paths.get(basePath,fileName);
		
		try {
			Files.write(path, data);
			logger.log(Level.FINE, "saved file: "+fileName);
			return FileStorageStatusCode.SUCCESS;
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to save file: "+fileName);
			logger.log(Level.SEVERE, "   exception: "+e);
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
		
	}

	@Override
	public byte[] retrieve(String domain, FileStorageAreaType area, String fileName) {
		String basePath = Configuration.getConfigurationProperty(domain, area, ConfigurationType.FILE_STORE);
		Path path = Paths.get(basePath,fileName);
		
		byte[] data = null;
		try {
			data = Files.readAllBytes(path);
			logger.log(Level.FINE, "retrieved file: "+fileName);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to retreive file: "+fileName);
			logger.log(Level.SEVERE, "   exception: "+e);
		}
		return data;
	}

	@Override
	public FileStorageStatusCode delete(String domain, FileStorageAreaType area, String fileName) {
		String basePath = Configuration.getConfigurationProperty(domain, area, ConfigurationType.FILE_STORE);
		Path path = Paths.get(basePath,fileName);
		
		try {
			Files.delete(path);
			logger.log(Level.FINE, "deleted file: "+fileName);
			return FileStorageStatusCode.SUCCESS;
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to delete file: "+fileName);
			logger.log(Level.SEVERE, "   exception: "+e);
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
	}
	
	public static FileStorageStatusCode deleteDirectory(String domain, FileStorageAreaType area) {
		String basePath = Configuration.getConfigurationProperty(domain, area, ConfigurationType.FILE_STORE);
		//Path path = Paths.get(basePath);
		
		try {
			FileUtils.deleteDirectory(new File(basePath));
			//Files.delete(path);
			logger.log(Level.FINE, "deleted directory: "+basePath);
			return FileStorageStatusCode.SUCCESS;
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to delete directory: "+basePath);
			logger.log(Level.SEVERE, "   exception: "+e);
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
	}

	
	
	@Override
	public FileStorageStatusCode move(String domain, FileStorageAreaType srcArea, FileStorageAreaType destArea,	String fileName) {
		Path sourcePath = Paths.get( Configuration.getConfigurationProperty(domain, srcArea, ConfigurationType.FILE_STORE),fileName);
		Path destPath = Paths.get( Configuration.getConfigurationProperty(domain, destArea, ConfigurationType.FILE_STORE),fileName);
		
		try {
			Files.move(sourcePath,destPath, StandardCopyOption.REPLACE_EXISTING);
			logger.log(Level.FINE, "moved file: "+fileName);
			return FileStorageStatusCode.SUCCESS;
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to move file: "+fileName);
			logger.log(Level.SEVERE, "   exception: "+e);
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
		
		/*
		 * UnsupportedOperationException - if the array contains a copy option that is not supported
FileAlreadyExistsException - if the target file exists but cannot be replaced because the REPLACE_EXISTING option is not specified (optional specific exception)
DirectoryNotEmptyException - the REPLACE_EXISTING option is specified but the file cannot be replaced because it is a non-empty directory (optional specific exception)
AtomicMoveNotSupportedException - if the options array contains the ATOMIC_MOVE option but the file cannot be moved as an atomic file system operation.
IOException - if an I/O error occurs
SecurityException - In the case of the default provider, and a security manager is installed, the checkWrite method is invoked to check write access to both the source and target file.
		 */
	}

	
	
}
