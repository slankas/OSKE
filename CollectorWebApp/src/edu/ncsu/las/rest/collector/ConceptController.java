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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import edu.ncsu.las.annotator.ConceptAnnotator;
import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.concept.Concept;
import edu.ncsu.las.model.collector.concept.ConceptCache;
import edu.ncsu.las.model.collector.concept.ConceptCategory;
import edu.ncsu.las.model.collector.concept.ConceptCategoryTable;
import edu.ncsu.las.model.collector.type.RoleType;

import java.util.Iterator;
import java.util.List;

/**
 * Handles requests for the Concept Extraction Page.
 */
@RequestMapping(value = "rest/{domain}/concepts",produces="application/json;charset=UTF-8")
@Controller
public class ConceptController extends AbstractRESTController {
	
	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());
	
	
	@RequestMapping(method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getAllConcepts(HttpServletRequest request, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.INFO, "ConceptController - get all concepts with their full name");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.ConceptController.getAllConcepts", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);
		
		java.util.Map<UUID, ConceptCategory> categories = ConceptCategory.getAllConceptsWithFullName(domainStr);
		JSONArray catArray = new JSONArray();
		
		List<Concept> conceptList = Concept.getAllConcepts(domainStr);
		for(Concept concept: conceptList){
			JSONObject jo = new JSONObject().put("id",   concept.getId().toString())
                                            .put("name", concept.getName())
                                            .put("type", concept.getType())
                                            .put("regex",concept.getRegex())
                                            .put("fullCategoryName", categories.get(concept.getCategoryId()).getFullCategoryName());
			catArray.put(jo);
		}
		
		JSONObject result = new JSONObject().put("status", "success")
                                            .put("concepts", catArray);
		return result.toString().getBytes("UTF-8");
	}	
	
	
	@RequestMapping(value = "/category", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getAllCategories(HttpServletRequest request, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.INFO, "ConceptController - get all categories");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.ConceptController.getAllCategories", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);
		
		List<ConceptCategory> conceptCategoryList = ConceptCategory.getAllConceptCategories(domainStr);

		JSONArray categories = new JSONArray(); 
		for(ConceptCategory cc : conceptCategoryList) {
			categories.put(cc.toJSON());
		}
		
		JSONObject result = new JSONObject().put("status", "success");
		result.put("conceptCategories", categories);
		return result.toString().getBytes("UTF-8");
	}	
	
		
	@RequestMapping(value = "/category", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody byte[] addCategory(HttpServletRequest request, HttpServletResponse response, @RequestBody String catStr, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.INFO, "ConceptController - addconceptCategory ");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		
		JSONObject catJSON = new JSONObject(catStr);
		
		String catname= catJSON.getString("name");	
		UUID catUUID = edu.ncsu.las.util.UUID.createTimeUUID();
		UUID parentuuid=UUID.fromString(catJSON.getString("parentid"));

		catJSON.put("id", catUUID.toString());
		//logger.log(Level.INFO, "new categ="+catJSON);

		this.instrumentAPI("edu.ncsu.las.rest.collector.ConceptController.addCategory", catJSON, System.currentTimeMillis(), null, request,domainStr);
		
		
		ConceptCategory newCategory = new ConceptCategory();
	    newCategory.setCategoryID(catUUID);
		newCategory.setCategoryName(catname);
		newCategory.setParentID(parentuuid);
		newCategory.setDomainInstanceName(domainStr);
	    
		if (newCategory.createConceptCategory()) {
		    JSONObject result = new JSONObject().put("status", "success")
		    		                            .put("concept", catJSON);
		    return result.toString().getBytes("UTF-8");
		}
		else {
			throw new ValidationException("Unable create concept category record",HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/category/{categoryUUID}", method = RequestMethod.DELETE, headers = "Accept=application/json")
	public @ResponseBody String deleteConceptCategory(HttpServletRequest request, @PathVariable("categoryUUID") UUID categoryUUID, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.INFO, "ConceptController - delete categroy concept"+categoryUUID);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.ConceptController.deleteCategory", new JSONObject().put("categoryID", categoryUUID), System.currentTimeMillis(), null, request,domainStr);
		
		ConceptCategory.deleteCategory(categoryUUID);
		
		JSONObject result = new JSONObject().put("status", "success")
				                            .put("action", "delete");
		
		return result.toString();
	}	
	
	@RequestMapping(value = "/category/{categoryID}/concept", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getConceptsForCategory(HttpServletRequest request,@PathVariable("categoryID") UUID categoryID, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.INFO, "ConceptController -getting concept with category id: "+categoryID);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.ConceptController.getConceptsForCategory",  new JSONObject().put("categoryID", categoryID), System.currentTimeMillis(), null, request,domainStr);
		
		List<Concept> l = Concept.getConceptWithCategoryId(categoryID);

		JSONArray ja = new JSONArray(); 
		l.stream().forEach( concept -> ja.put(concept.toJSONObject()));
		JSONObject result = new JSONObject().put("status", "success")
				                            .put("concepts", ja);
		
		return result.toString().getBytes("UTF-8");
	}
	
	
	@RequestMapping(value = "/concept/{conceptUUID}", method = RequestMethod.DELETE, headers = "Accept=application/json")
	public @ResponseBody String deleteSingleConcept(HttpServletRequest request, @PathVariable("conceptUUID") UUID conceptUUID, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.INFO, "ConceptController - deletesingleconcept: "+conceptUUID);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.ConceptController.deleteCategory", new JSONObject().put("conceptID", conceptUUID), System.currentTimeMillis(), null, request,domainStr);
		
		Concept.deleteConcept(conceptUUID);
		
		JSONObject result = new JSONObject().put("status", "success")
				                            .put("action", "delete")
				                            .put("id", conceptUUID.toString());
		return result.toString();
	}
	
	
	@RequestMapping(value = "/concept", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody String addConcept(HttpServletRequest request, @RequestBody String concStr, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		
		JSONObject concJSON = new JSONObject(concStr);
		concJSON.put("domainInstanceName",domainStr);
		concJSON.put("id", edu.ncsu.las.util.UUID.createTimeUUID().toString());
		logger.log(Level.FINER, "ConceptController - addconcept, "+concJSON);
		
		Concept newRow = new Concept(concJSON);
		
		JSONObject result = new JSONObject();
		if (newRow.getName().matches("^\\w*$") == false) {
			result.put("error","Invalid concept name.  Can only contain letters, digits, and underscores.");
		}
		else if ( Concept.validateRegex(newRow.getRegex()) == false) {
			result.put("error", "Regular expression was not valid.");
		}
		else  {
			newRow.insertConcept();
			result.put("concept", concJSON);
			this.instrumentAPI("edu.ncsu.las.rest.collector.ConceptController.addCategory", concJSON, System.currentTimeMillis(), null, request,domainStr);
		}
		
		return result.toString();
	}
	
	@RequestMapping(value = "/annotate", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody byte[] annotateTextWithConcept(HttpServletRequest request, @RequestBody String text, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.INFO, "ConceptController - AnnotatingConcept");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.ConceptController.annotateTextWithConcept", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);
		
		ConceptAnnotator annotate = new ConceptAnnotator();
		JSONArray resultArray= annotate.annotateConcepts(text,domainStr);
		
		JSONObject result = new JSONObject().put("status", "success");
		result.put("concepts", resultArray);
		return result.toString().getBytes("UTF-8");
	}
	
	/**
	 * Takes a file uploaded from the client.  Then processes that file to check for all concepts currently defined.
	 * Returns an array of those concepts back to the caller.
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "/testFile", method = RequestMethod.POST)
	public @ResponseBody byte[] annotateFileForConcepts(MultipartHttpServletRequest request, HttpServletResponse response,
		    @PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.INFO, "test concepts files");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.ConceptController.annotateFileForConcepts", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);
		
		JSONArray fileList = new  JSONArray();

		Iterator<String> itr = request.getFileNames();

		ConceptCache.CacheObject concepts = ConceptCache.createCacheObject(domainStr);
		while (itr.hasNext()) {
			MultipartFile mpf = request.getFile(itr.next());

			logger.log(Level.INFO,	"Concept test file: " + mpf.getOriginalFilename() + " (" + mpf.getContentType() + ") ");
			String text = TikaUtilities.extractText(mpf.getBytes());
			
			JSONArray conceptArray = Concept.annotateConcepts(text, domainStr, concepts.domainCategoryTable, concepts.domainConceptList);
			JSONObject fileResult = new JSONObject().put("name", mpf.getOriginalFilename())
					                                .put("contentType", mpf.getContentType())
					                                .put("size", mpf.getSize())
					                                .put("concepts", conceptArray);
			fileList.put(fileResult);
		}
	
		JSONObject result = new JSONObject();
		result.put("files", fileList);
		return result.toString().getBytes("UTF-8");
	}	
	
		
	/**
	 * import concepts
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public @ResponseBody String importConceptsFromFile(MultipartHttpServletRequest request, HttpServletResponse response,
		    @PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.INFO, "FileUploadController: POST files");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.ConceptController.importConceptsFromFile", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);
		
		JSONArray list = new  JSONArray();

		Iterator<String> itr = request.getFileNames();
		MultipartFile mpf;

		ConceptCategoryTable categoryTable = new ConceptCategoryTable(domainStr);
		JSONArray badRegexList = new JSONArray();
		JSONArray badNameList  = new JSONArray();
		boolean includedCategory = false; // was a category field included as part of the uploaded file or not?
		
		while (itr.hasNext()) {
			mpf = request.getFile(itr.next());
			String contentType = mpf.getContentType();
			String fileName = mpf.getOriginalFilename();
			int numRecordsAdded = 0;
			int numRecordsSkipped = 0;
			JSONArray addedRecords = new JSONArray();
			
			Long size = mpf.getSize();
			logger.log(Level.INFO,	"FileUploadController: POST file: " + mpf.getOriginalFilename() + " (" + contentType + ") ");

			Reader in = new StringReader(new String(mpf.getBytes(),StandardCharsets.UTF_8));
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
			for (CSVRecord record : records) {
				if (record.size() != 4) {
					numRecordsSkipped++;
					continue;
				}
				UUID categoryIDForRecord = categoryTable.getConceptCategoryByFullName(domainStr,record.get(0)).getCategoryID();
				String conceptName  = record.get(1).trim();
				String conceptType  = record.get(2).trim();
				String conceptRegEx = record.get(3).trim();
				
				if (conceptName.length() == 0 || conceptType.length() == 0 || conceptRegEx.length() ==0) { 
					numRecordsSkipped++;
					continue;
				}			
				
				if (conceptName.matches("^\\w*$") == false) {
					badNameList.put(conceptName);
					continue;
				}
				
				/*Check for Regex*/
				
				Concept newConcept = new Concept(categoryIDForRecord,conceptName,conceptType,conceptRegEx,domainStr);
				boolean isValid = Concept.validateRegex(conceptRegEx);
				
				if (isValid) {
					newConcept.insertConcept();
					addedRecords.put(newConcept.toJSONObject());
					numRecordsAdded++;
				}
				else {
					badRegexList.put(newConcept.toJSONObject());
				}
				
			}
			JSONObject file = new JSONObject();

			file.put("name", fileName);
			file.put("size", size);
			file.put("numRecordsAdded", numRecordsAdded);
			file.put("numRecordsSkipped", numRecordsSkipped);
			file.put("records", addedRecords);
			file.put("success", true);
			file.put("badRegexes", badRegexList);
			file.put("badNames", badNameList);

			// else: file.put("error", "ERROR message");

			list.put(file);
			//writeToDirectory(uploadPath, mpf);
		}
		
		JSONObject result = new JSONObject();
		result.put("files", list);
		result.put("includedCategory",includedCategory);
		//logger.log(Level.INFO, "Files : " + files);

		return result.toString();
	}
}