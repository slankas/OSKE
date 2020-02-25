package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.RoleType;

/**
* upload files into the system for processing..
*/

/**
 * Handles requests for the files upload page.
 */
@RequestMapping
@Controller
public class FileUploadController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(FileUploadController.class.getName());

	/**
	 * Gets the file uploaded at client.  This will place it into a directory in which the directorywatcher handler will kickoff the processing of the file
	 * 
	 * @throws IOException
	 * @throws ValidationException 
	 */
	@RequestMapping(value = "{domain}/upload", method = RequestMethod.POST)
	public @ResponseBody String upload(MultipartHttpServletRequest request, 
			HttpServletResponse response, 
			@PathVariable("domain") String domainStr, 
			HttpServletRequest httpRequest)
			throws IOException, ValidationException {
		logger.log(Level.INFO, "FileUploadController: POST files, uriInfo = "+request.getRequestURL().toString());
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);

		long startTime = System.currentTimeMillis();
		
		JSONArray files = new JSONArray();
		
		Iterator<String> itr = request.getFileNames();
		MultipartFile mpf;

		String uploadPath = Configuration.getConfigurationProperty(domainStr, ConfigurationType.IMPORT_DIRECTORY);	
		logger.log(Level.INFO, "Writing files to: " + uploadPath);

		while (itr.hasNext()) {
			mpf = request.getFile(itr.next());
			String contentType = mpf.getContentType();
			String fileName = mpf.getOriginalFilename();
			Long size = mpf.getSize();
			logger.log(Level.INFO,	"FileUploadController: POST file: " + mpf.getOriginalFilename() + " (" + contentType + ") ");

			JSONObject fileObject = new JSONObject().put("name", fileName)
					                                .put("size", size)
					                                .put("success", true);
			files.put(fileObject);
			writeToDirectory(uploadPath, mpf);
		}
		
		JSONObject result = new  JSONObject().put("files", files);
		logger.log(Level.FINEST, "Files : " + files.toString());

		
		this.instrumentAPI("edu.ncsu.las.rest.collector.FileUploadController.upload", result, startTime, System.currentTimeMillis(), request,domainStr);	
		return result.toString();
	}
	
	
	
	@RequestMapping(value = "rest/{domain}/upload/zip/{filename}", method = RequestMethod.POST)
	public @ResponseBody String uploadZip(@RequestBody String data, 
			HttpServletResponse response, 
			@PathVariable("domain") String domainStr, 
			@PathVariable("filename") String fileName, 
			HttpServletRequest httpRequest )
			throws IOException, ValidationException {
		
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		
		String prefix = "openke_"+domainStr;
		String suffix = nowAsISO();
		String fullFileName = prefix+"_"+fileName +"_"+suffix;

		String uploadPath = Configuration.getConfigurationProperty(domainStr, ConfigurationType.EXPORT_EXTERNAL_SYSTEM_PATH);	
		logger.log(Level.INFO, "Writing "+fullFileName+" to: " + uploadPath);

		writeZipToDirectory(uploadPath, fullFileName, data);
		
		return "Writing "+fullFileName+" to: " + uploadPath;
	}
	
	
	private String nowAsISO() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());
		
		return nowAsISO;
	}
	
	

	/**
	 * Saves the file to directory
	 * 
	 * TODO: this functionality probably already exists in a file utility class...
	 * 
	 * @throws IOException 
	 */
	private void writeToDirectory(String uploadPath, MultipartFile mpf) {
		String uuid = edu.ncsu.las.util.UUID.createTimeUUID().toString();
	
		Path path = Paths.get(uploadPath,uuid+"_"+mpf.getOriginalFilename());
		
		try {
			Paths.get(uploadPath).toFile().mkdirs();   //ensure parent directories exist
			Files.write(path, mpf.getBytes());
			logger.log(Level.FINE, "saved file: "+path.toString());
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to save file: "+path.toString());
			logger.log(Level.SEVERE, "   exception: "+e);
		}
		
	}
	
	
	private void writeZipToDirectory(String uploadPath, String filename, String data) throws IOException {
		String uuid = edu.ncsu.las.util.UUID.createTimeUUID().toString();
		Path path = Paths.get(uploadPath,filename+".zip");
		byte[] zip = Base64.decodeBase64(data);
		
		try {
			Paths.get(uploadPath).toFile().mkdirs();   //ensure parent directories exist
			Files.write(path, zip);
			logger.log(Level.INFO, "wrote file: "+path.toString());
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to save file: "+path.toString());
			logger.log(Level.SEVERE, "   exception: "+e);
		}
		
	}

}