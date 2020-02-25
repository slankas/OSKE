package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.StructuralExtractionRecord;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.source.SourceHandlerInterface;
import edu.ncsu.las.util.InternetUtilities;
import edu.ncsu.las.util.json.JSONUtilities;

import java.util.Base64;
import java.util.Iterator;
import java.util.List;

/**
 * Handles requests for the Structural Annotation Configuration Page.
 */
@RequestMapping(value = "rest/{domain}/structuralExtraction",produces="application/json;charset=UTF-8")
@Controller
public class StructuralExtractionController extends AbstractRESTController {
	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());
	
	public static final String[] CSV_FIELD_NAMES = {"id","hostname","pathRegex","recordParentID","recordName","recordSelector","recordExtractBy","recordExtractRegex"};
	
	
	@RequestMapping(method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getAllContentExtractionRecords(HttpServletRequest request, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.FINER, "StructuralExtractionController - get all records");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.StructuralExtractionController.getAllContentExtractionRecords", new JSONObject(),System.currentTimeMillis(),null, request,domainStr);
		
		List<StructuralExtractionRecord> cerList = StructuralExtractionRecord.getRecordsForDomain(domainStr);
		JSONArray resultArray = new JSONArray();
		
		for(StructuralExtractionRecord cer: cerList){
			resultArray.put(cer.toJSON());
		}
		
		JSONObject result = new JSONObject().put("status", "success")
                                            .put("structuralExtractionRecords", resultArray);
		return result.toString().getBytes("UTF-8");
	}	
	
	
	@RequestMapping( method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody byte[] createNewContentExtractRecord(HttpServletRequest request, @RequestBody String postStr, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.FINER, "StructuralExtractionController - create record");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		
		User u = this.getUser(request);
		
		JSONObject newCERjson = new JSONObject(postStr);
		newCERjson.put("id", edu.ncsu.las.util.UUID.createTimeUUID().toString());
		newCERjson.put("userEmailID",u.getEmailID());
		newCERjson.put("domainInstanceName", domainStr);
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.StructuralExtractionController.createNewContentExtractRecord", newCERjson,System.currentTimeMillis(),null, request,domainStr);

		
		StructuralExtractionRecord cer = new StructuralExtractionRecord(newCERjson);		
		List<String> errors = cer.validate();
		if (errors.size() >0) {
			JSONObject result = new JSONObject().put("status", "failed")
					                            .put("errors", JSONUtilities.toJSONArrayAsString(errors))
                    							.put("message", "validation errors");
			return result.toString().getBytes("UTF-8");
		}

		
		if (cer.store()) {
			JSONObject result = new JSONObject().put("status", "success")
					                            .put("record", cer.toJSON());
			return result.toString().getBytes("UTF-8");
		}
		else {
			JSONObject result = new JSONObject().put("status", "failed")
                                                .put("message", "unknown error");
			return result.toString().getBytes("UTF-8");
		
		}
	}	
	
	@RequestMapping(value = "/{recordUUID}", method = RequestMethod.PUT, headers = "Accept=application/json")
	public @ResponseBody byte[] updateContentExtractRecord(HttpServletRequest request, @RequestBody String postStr, @PathVariable("recordUUID") UUID id, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.FINER, "StructuralExtractionController - update record");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		
		User u = this.getUser(request);
		
		JSONObject newCERjson = new JSONObject(postStr);
		newCERjson.put("id", id.toString());
		newCERjson.put("userEmailID",u.getEmailID());
		newCERjson.put("domainInstanceName", domainStr);

		this.instrumentAPI("edu.ncsu.las.rest.collector.StructuralExtractionController.updateContentExtractRecord", newCERjson,System.currentTimeMillis(),null, request,domainStr);
		
		StructuralExtractionRecord cer = new StructuralExtractionRecord(newCERjson);
		List<String> errors = cer.validate();
		if (errors.size() >0) {
			JSONObject result = new JSONObject().put("status", "failed")
					                            .put("errors", JSONUtilities.toJSONArrayAsString(errors))
                    							.put("message", "validation errors");
			return result.toString().getBytes("UTF-8");
		}
		
		if (cer.update()) {
			JSONObject result = new JSONObject().put("status", "success")
					                            .put("record", cer.toJSON());
			return result.toString().getBytes("UTF-8");
		}
		else {
			JSONObject result = new JSONObject().put("status", "failed")
                                                .put("message", "unknown error")
                                                .put("id", id.toString());
			return result.toString().getBytes("UTF-8");
		
		}
	}		
	
	
	@RequestMapping(value = "/{recordUUID}", method = RequestMethod.DELETE, headers = "Accept=application/json")
	public @ResponseBody byte[] deleteContentExtractionRecord(HttpServletRequest request, @PathVariable("recordUUID") UUID id, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.FINE, "StructuralExtractionController - delete contentExtractionRecord: "+id);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.StructuralExtractionController.deleteContentExtractionRecord", new JSONObject().put("recordID", id.toString()),System.currentTimeMillis(),null, request,domainStr);

		JSONObject result;
		StructuralExtractionRecord cer = StructuralExtractionRecord.retrieve(id);
		if (cer == null) {
			result =  new JSONObject().put("status", "failure")
					               .put("action", "delete")
			                       .put("id", id.toString())
			                       .put("message", "record not found");
		}
		else {
			if (cer.delete()) {
				result = new JSONObject().put("status", "success")
						                 .put("action", "delete")
						                 .put("id", id.toString());
			}
			else {
				result = new JSONObject().put("status", "failure")
						                 .put("action", "delete")
						                 .put("id", id.toString())
						                 .put("message", "unable to delete found recor");
			}
		}
		
		return result.toString().getBytes("UTF-8");
	}	
		
	
	@RequestMapping(value = "/extract/{url:.*}", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] annotateContentExtractionByURL(HttpServletRequest request, @PathVariable("domain") String domainStr,  @PathVariable("url") String urlToProcess,
			                                                   @RequestParam(value = "log", required=false,defaultValue = "false" ) boolean logResults) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		urlToProcess = new String(Base64.getDecoder().decode(urlToProcess.replace("$", "/")));
		logger.log(Level.FINER, "StructuralExtractionController - test extraction by url: " + urlToProcess);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		JSONObject result;
		try {
			String userAgent = SourceHandlerInterface.getNextUserAgent(domainStr);
			
			List<StructuralExtractionRecord> records = StructuralExtractionRecord.getRecordsForAnnotation(domainStr);
			InternetUtilities.HttpContent page = InternetUtilities.retrieveURL(urlToProcess, userAgent, 0, true);
			
			JSONObject contentData = StructuralExtractionRecord.annotateForStructuralExtraction(page, records,logResults);
			result = new JSONObject().put("status", "success")
                                                .put("content", contentData);
			if (contentData.has("_logMessages")) {
				result.put("_logMessages", contentData.get("_logMessages"));
				contentData.remove("_logMessages");
			}		
		}
		catch (Exception e) {
			result = new JSONObject().put("status", "failure")
					                            .put("message", e.toString());
		}
		this.instrumentAPI("edu.ncsu.las.rest.collector.StructuralExtractionController.annotateContentExtractionByURL", new JSONObject().put("url", urlToProcess),startTime,System.currentTimeMillis(), request,domainStr);

		return result.toString().getBytes("UTF-8");
		
		
	}
	
	/**
	 * Tests the given upload file to extract content from the html page.  The URL and path regex are ignored in this situation.
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "/testFile", method = RequestMethod.POST)
	public @ResponseBody byte[] annotateUploadedFile(MultipartHttpServletRequest request, HttpServletResponse response,
		    @PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.FINER, "test concepts files");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		JSONArray fileList = new  JSONArray();

		Iterator<String> itr = request.getFileNames();
		
		JSONArray filenames = new JSONArray();
		List<StructuralExtractionRecord> records = StructuralExtractionRecord.getRecordsForAnnotation(domainStr);
		while (itr.hasNext()) {
			MultipartFile mpf = request.getFile(itr.next());

			logger.log(Level.FINER,	"Concept test file: " + mpf.getOriginalFilename() + " (" + mpf.getContentType() + ") ");
			String htmlContent = new String(mpf.getBytes());
			
			JSONObject contentData = StructuralExtractionRecord.annotateForStructuralExtraction("",htmlContent,records,true);
			
			JSONObject fileResult = new JSONObject().put("name", mpf.getOriginalFilename())
					                                .put("contentType", mpf.getContentType())
					                                .put("size", mpf.getSize())
					                                .put("content", contentData);
			filenames.put(mpf.getOriginalFilename());
			if (contentData.has("_logMessages")) {
				fileResult.put("_logMessages", contentData.get("_logMessages"));
				contentData.remove("_logMessages");
			}
			fileList.put(fileResult);
		}
		this.instrumentAPI("edu.ncsu.las.rest.collector.StructuralExtractionController.annotateUploadedFile", new JSONObject().put("files", filenames),startTime,System.currentTimeMillis(), request,domainStr);
	
		JSONObject result = new JSONObject();
		result.put("files", fileList);
		return result.toString().getBytes("UTF-8");
	}	

	
	/**
	 * Import ContentExtractionRecords from a CSV file.  Each line must have at least 8 records defined:
	 * id	hostname	pathRegex	recordParentID	recordName	recordSelector	recordExtractBy	recordExtractRegex
     *
     * Any values for domain, userEmailId, and last database change are ignored...
     * 
     * The user is responsible for ensure ID's remain unique [provide help]
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "/importCSV", method = RequestMethod.POST)
	public String importContentExtraction(MultipartHttpServletRequest request, HttpServletResponse response,
		    @PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.FINER, "import files");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.StructuralExtractionController.importContentExtractionRecords", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);

		User u = this.getUser(request);
		
		JSONArray list = new  JSONArray();

		Iterator<String> itr = request.getFileNames();
		MultipartFile mpf;

		//JSONArray badRegexList = new JSONArray();
		//JSONArray badNameList  = new JSONArray();
		JSONArray errorMessages = new JSONArray();
				
		while (itr.hasNext()) {
			mpf = request.getFile(itr.next());
			String contentType = mpf.getContentType();
			String fileName = mpf.getOriginalFilename();
			int numRecordsAdded = 0;
			int numRecordsSkipped = 0;
			JSONArray addedRecords = new JSONArray();
			
			Long size = mpf.getSize();
			logger.log(Level.FINER,	" importing CER file: " + mpf.getOriginalFilename() + " (" + contentType + ") ");

			Reader in = new StringReader(new String(mpf.getBytes(),StandardCharsets.UTF_8));
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
			boolean fieldsChecked = false;
			for (CSVRecord record : records) {
				if (!fieldsChecked) {
					for (String fieldName: CSV_FIELD_NAMES) {
						if (record.isMapped(fieldName) == false) {
							errorMessages.put("Field not defined in CSV file: "+fieldName);
						}
					}
					if (errorMessages.length() >0) { 
						break;
					}
				}
				
				if (record.size()<8) {
					numRecordsSkipped++;
					continue;
				}
				
				UUID recordID = null;
				UUID recordParentID = null;

				try {
					recordID = UUID.fromString(record.get("id")); 
				}
				catch (Exception e) {
					String error = "Invalid UUID specified for the ID: "+record.get("id");
					errorMessages.put(error);
					logger.log(Level.WARNING,error );
				}
				
				try {
					String value = record.get("recordParentID").trim();
					if (value.length()>0) {
						recordParentID = UUID.fromString(value); 
					}
				}
				catch (Exception e) {
					String error = "Invalid UUID specified for the ID: "+record.get("id");
					errorMessages.put(error);
					logger.log(Level.WARNING,error );
				}				
				
				String hostname  = record.get("hostname");
				String pathRegex = record.get("pathRegex");
				String recordName= record.get("recordName");
				String recordSelector     = record.get("recordSelector");
				String recordExtractBy    = record.get("recordExtractBy");
				String recrodExtractRegex = record.get("recordExtractRegex");
				
				StructuralExtractionRecord newCER = new StructuralExtractionRecord(recordID, domainStr, hostname, pathRegex, recordName, recordSelector, recordExtractBy, recrodExtractRegex, recordParentID, u.getEmailID(), new java.sql.Timestamp(System.currentTimeMillis()));
				List<String> errors = newCER.validate();
				if (errors.size() == 0) {
					if (newCER.store()) {
						numRecordsAdded++;
						addedRecords.put(newCER.toJSON());
					}
					else {
						errorMessages.put(  newCER.toString()+": unable to store");
						numRecordsSkipped++;						
					}
				}
				else {
					errorMessages.put(  newCER.toString()+": " + String.join(",", errors));
					numRecordsSkipped++;
				}
								
			}
			JSONObject file = new JSONObject();

			file.put("name", fileName);
			file.put("size", size);
			file.put("numRecordsAdded", numRecordsAdded);
			file.put("numRecordsSkipped", numRecordsSkipped);
			file.put("records", addedRecords);
			file.put("success", true);
			list.put(file);
		}
		
		JSONObject result = new JSONObject();
		result.put("files", list);
		result.put("errors", errorMessages);
		request.setAttribute("importMessages", result);
		logger.log(Level.FINEST, result.toString());
		
		
		return "structuralExtraction";
	}		
	
}