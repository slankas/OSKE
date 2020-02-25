package edu.ncsu.las.geo.model;

import java.util.List;

import com.bericotech.clavin.ClavinException;
import com.bericotech.clavin.GeoParser;
import com.bericotech.clavin.GeoParserFactory;
import com.bericotech.clavin.resolver.ResolvedLocation;

import com.bericotech.clavin.nerd.StanfordExtractor;

import java.io.IOException;


/*create Lucene Index first from geoTagged file*/

/**
 * 
 *
 */
public class Clavin {
	private static final ClavinPool CLAVIN_POOL = new ClavinPool(new ClavinPoolFactory());
		
	public static String INDEXDIRECTORY_PATH;
	
	public static void initialize(String configurationPath) {
		INDEXDIRECTORY_PATH = configurationPath;
	}
	
	public static ClavinPool getTheClavinPool() {
		return CLAVIN_POOL;
	}
	
	private GeoParser parser;

	public Clavin() throws ClassCastException, ClassNotFoundException, ClavinException, IOException {
		
		 // Explicitly specifying Stanford Extractor, if not specified ApacheExtractor will be used
		 parser = GeoParserFactory.getDefault(INDEXDIRECTORY_PATH, new StanfordExtractor(), 1, 1, false);
		
	}
	
	/*For Fully uppercase articles*/
    public List<ResolvedLocation> geoparseUppercaseArticle(String inputString) throws Exception {
        // Instantiate a CLAVIN GeoParser using the StanfordExtractor with "caseless" models
        GeoParser upperCaseParser = GeoParserFactory.getDefault(INDEXDIRECTORY_PATH, new StanfordExtractor("english.all.3class.caseless.distsim.crf.ser.gz", "english.all.3class.caseless.distsim.prop"), 1, 1, false);
        
        List<ResolvedLocation> resolvedLocations = upperCaseParser.parse(inputString);
        
        return resolvedLocations;
    }

    /**
     * 
     * @param text
     * @return
     * @throws Exception
     */
	public List<ResolvedLocation>  parseArticle(String text) throws Exception {
		 
		List<ResolvedLocation> resolvedLocations = parser.parse(text);
		 return resolvedLocations;
	}
	
}
