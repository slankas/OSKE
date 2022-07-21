package edu.ncsu.las.collector;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.document.DocumentHandler;
import edu.ncsu.las.document.DocumentRouter;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.Job;
import edu.ncsu.las.model.collector.JobHistory;
import edu.ncsu.las.model.collector.concept.Concept;
import edu.ncsu.las.model.collector.concept.ConceptCategory;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;
import edu.ncsu.las.model.collector.type.JobStatus;
import edu.ncsu.las.util.json.JSONComparator;
import edu.ncsu.las.util.json.JSONMismatch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;;

/**
 * Fake collector.
 *
 * Runs one file through the DocumentRouter, then prints resulting
 * JSON document.
 *
 */
public final class TestProcessor  {
    /**
     * Name of collector to register as.
     */
    public static final String COLLECTOR_NAME = "test_collector";

    /**
     * Priority to create fake job as.
     */
    public static final int JOB_PRIORITY = 100;

    /**
     * Amount to indent each JSON level by in output.
     */
    public static final int JSON_INDENT = 4;


    /**
     * Max length of domain names.
     */
    public static final int DOMAIN_NAME_LEN = 15;

    /**
     * Instance variable to track whether the collector has been initialized.
     */
    private boolean collectorReady = false;

    /**
     * Initializes collector.  Calls after the first are ignored.
     * @throws Exception
     */
    public void initCollector() throws Exception {
        //Only do this once
        if (collectorReady) {
            return;
        }

        //Set up the collector - needed for configuration
        Collector.initializeCollector(".",                      //String directory
                                      "system_properties.json", //String configurationFile
                                      false,                    //boolean validateProcessingQueues
                                      false, false);                   //boolean loadSiteRules
        
        collectorReady = true;
    }
    
    /**
     * Create an individual concept in the database
     * 
     * @param domain Domain to create in
     * @param category Parent category
     * @param data JSON to create - must contain, name, type, and regex
     * @throws Exception
     */
    public void createConcept(String domain, UUID category, JSONObject data) throws Exception {
    	if (!data.has("name") || !data.has("type") || !data.has("regex")) {
    		throw new Exception("Invalid JSON");
    	}
    	
    	Concept newConcept = new Concept(UUID.randomUUID(), 		//UUID id
    								     domain, 					//String domainInsatnceName
    								     category, 					//UUID categoryId
    								     data.getString("name"), 	//String name
    								     data.getString("type"), 	//String type
    								     data.getString("regex")); 	//String regex
    	
    	if (!Concept.validateRegex(newConcept.getRegex())) {
    		throw new Exception("Invalid RegEx");
    	}
    	
    	if (!newConcept.insertConcept()) {
    		throw new Exception("Could not create concept " + data.getString("name"));
    	}
    }
    
    /**
     * Create a category (for concepts) in the database.
     * 
     * @param domain Domain to create in
     * @param parent UUID of parent category
     * @param data JSON Object describing category.
     * @throws Exception
     */
    public void createCategory(String domain, UUID parent, JSONObject data) throws Exception {
    	if (!data.has("name")) {
    		throw new Exception("No name specified");
    	}
    	
    	UUID newCatId = UUID.randomUUID();
    	
    	ConceptCategory newCat = new ConceptCategory(newCatId, 					//UUID categoryID
    												 domain, 					//String domainInstanceName
    												 data.getString("name"), 	//String categoryName,
    												 parent); 					//UUID parentID
    	
    	if (!newCat.createConceptCategory()) {
    		throw new Exception("createConceptCategory failed for " + data.getString("name"));
    	}
    	
    	if (data.has("categories")) {
    		JSONArray cats = data.getJSONArray("categories");
    		for(int i = 0; i < cats.length(); i++) {
    			createCategory(domain, newCatId, cats.getJSONObject(i));
    		}
    	}
    	
    	if (data.has("concepts")) {
    		JSONArray concepts = data.getJSONArray("concepts");
    		for(int i = 0; i < concepts.length(); i++) {
    			createConcept(domain, newCatId, concepts.getJSONObject(i));
    		}
    	}
    }
    
    public void createCategories(String domain, JSONArray catData) throws Exception {
    	//Make sure that the collector has been created
        initCollector();
        
    	for(int i = 0; i < catData.length(); i++) {
    		createCategory(domain, ConceptCategory.ROOT_UUID, catData.getJSONObject(i));
    	}
    }

    public String createDomainFromCfg(String domainCfg) throws Exception {
        //Make sure that the collector has been created
        initCollector();

        //Generate dynamic domain-name
        String domainName = UUID.randomUUID().toString().substring(0, DOMAIN_NAME_LEN);

        // Create domain
        Domain tmpDomain = new Domain(domainName, //String domainInstanceName
                                      "active", //String domainStatus
                                      Timestamp.from(Instant.now()), //Timestamp effectiveTimestamp
                                      domainName, //String fullName
                                      "Test domain", //String description
                                      "test@test.asdf", //String primaryContact
                                      0, //int appearanceOrder
                                      domainCfg, //String configuration 
                                      "test@test.asdf", //String userEmailAddress,
                                      Timestamp.from(Instant.now()), false); //Timestamp insertTimestamp
        
        //Add to DB
        tmpDomain.create("test@test.asdf");

        //Update collector config
        Collector.getTheCollecter().refreshDomains(false, false);

        return domainName;
    }

    public JSONObject processDoc(File inputFile, String domainName) throws Exception {
        // Make sure that the collector is ready to go
        initCollector();

        // Create a fake job
        Timestamp tsNow = new Timestamp(System.currentTimeMillis());
        Job job = new Job(UUID.randomUUID(),
                          "http://www.test.com",
                          "web",
                          JobStatus.PROCESSING,
                          tsNow,
                          UUID.randomUUID(),
                          "",
                          new JSONObject("{}"),
                          "test_job",
                          TestProcessor.COLLECTOR_NAME,
                          "Test job",
                          JOB_PRIORITY,
                          null, //Defaults to Job.DEFAULT_SCHEDULE
                          25,
                          null, //Defaults to NOW
                          domainName,"[]",
                          false );

        // Create a fake JobHistory entry
        JobHistory jh = new JobHistory(UUID.randomUUID(),
                                       job.getID(),
                                       job.getName(),
                                       JobHistoryStatus.PROCESSING,
                                       "",
                                       COLLECTOR_NAME,
                                       job.getDomainInstanceName());

        //Create the router
        DocumentRouter router = new DocumentRouter(job.getDomainInstanceName(),
                                                   DocumentHandler.getDocumentHandlers(),
                                                   jh, job);

        //Create the document object
        JSONObject jobCfg = new JSONObject("{}");
        JSONObject jobSummary = new JSONObject("{}");
        Document doc = new Document(inputFile, jobCfg, jobSummary, System.currentTimeMillis(), domainName,jh);

        //Process the document - nothing hits the database
        router.processPage(doc, false, false);

        return doc.createJSONDocument();
    }

    /**
     * Entrypoint.
     * @param args Command line arguments.  Set by JRE.
     * @throws Exception If in doubt, bail.
     */
    public static void main(final String[] args) throws Exception {
        // Parse options
        Options opts = new Options();

        Option optInput = new Option("i", "input", true, "input file path");
        optInput.setRequired(true);
        opts.addOption(optInput);

        Option optDomain = new Option("d", "domain", true, "domain (must exist)");
        opts.addOption(optDomain);

        Option optDomainCfg = new Option("c", "domainCfg", true, "file to read domain config from");
        opts.addOption(optDomainCfg);

        Option optOutput = new Option("o", "output", true, "output file path");
        opts.addOption(optOutput);
        
        Option optValidate = new Option("v", "validate", true, "JSON to validate against");
        opts.addOption(optValidate);
        
        Option optConcepts = new Option("n", "concepts", true, "concept definition file");
        opts.addOption(optConcepts);

        CommandLineParser parser = new GnuParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(opts, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Test Collector", opts);

            System.exit(1);
            return;
        }

        TestProcessor processor = new TestProcessor();

        String inputFileName = cmd.getOptionValue("input");
        String domainCfgPath = cmd.getOptionValue("domainCfg");
        String domainName = cmd.getOptionValue("domain");

        if (domainName != null && domainCfgPath != null) {
            throw new Exception("Can't set domainCfg and domain");
        }

        //Select domain
        if (domainCfgPath != null) {
        	//Create domain using config file
            String domainCfgData = new String(Files.readAllBytes(Paths.get(domainCfgPath)), "UTF-8");
            domainName = processor.createDomainFromCfg(domainCfgData);
        } else if (domainName == null) {
        	//Use system domain
            domainName = "system";
        } else {
        	//Use specified domain
        }
        
        //Create categories and concepts if required
        String conceptPath = cmd.getOptionValue("concepts");
        if(conceptPath != null) {
        	if(domainCfgPath == null) {
        		System.err.println("WARNING: Adding concepts to existing domain");
        	}
        	
        	JSONArray categories = new JSONArray(new String(Files.readAllBytes(Paths.get(conceptPath)), "UTF-8"));
        	for(int i = 0; i < categories.length(); i++) {
        		processor.createCategories(domainName, categories);
        	}
        }
        		
        //Load data from file
        File inputFile = new File(inputFileName);

        //Process document
        JSONObject results = processor.processDoc(inputFile, domainName);

        //Get the results
        String outputFilePath = cmd.getOptionValue("output");
        if (outputFilePath == null) {
            System.out.println(results.toString(JSON_INDENT));
        } else {
            Files.write(Paths.get(outputFilePath), results.toString(JSON_INDENT).getBytes());
        }
        
        //Validate the results
        String validationFile = cmd.getOptionValue("validate");
        if(validationFile != null) {
        	JSONObject validJson = new JSONObject(new String(Files.readAllBytes(Paths.get(validationFile)), "UTF-8"));
        	List<JSONMismatch> issues = JSONComparator.run(validJson, results);
        	System.out.println("Issues found: " + issues.size());
        	for(JSONMismatch issue : issues) {
        		System.out.println(issue.toString());
        	}
        }
    }

    /**
     * Private constructor for utility class.
     */
    private TestProcessor() { }
}
