package edu.ncsu.las.image.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

/**
 * Extracts EXIF meta-data from a image utilizes the MetaData Extractor library
 * at https://github.com/drewnoakes/metadata-extractor
 * 
 *
 */
public class EXIFExtractor {
	
	/**
	 * Extract EXIF data from an InputStream
	 * 
	 * @param is
	 * @return JSONObject contain the EXIF data
	 * @throws ImageProcessingException
	 * @throws IOException
	 */
    public static JSONObject produceEXIF(InputStream is) throws ImageProcessingException, IOException {
    	JSONObject result = new JSONObject();
    	
		Metadata metadata = ImageMetadataReader.readMetadata(is);
		
		
		for (Directory directory : metadata.getDirectories()) {
		    for (Tag tag : directory.getTags()) {
		    	String dirName = tag.getDirectoryName();
		    	
		    	if (result.has(dirName) == false) {
		    		result.put(dirName, new JSONObject());
		    	}
		    	result.getJSONObject(dirName).put(tag.getTagName(), tag.getDescription());
		    }
		}

		return result;
    }

	/**
	 * Extract EXIF data from a byte array. 
	 * Note: a byteinputstream object is created and passed to produceEXIF(InputStream IS)
	 * 
	 * @param is
	 * @return JSONObject contain the EXIF data
	 * @throws ImageProcessingException
	 * @throws IOException
	 */

    public static JSONObject produceEXIF(byte[] data) throws ImageProcessingException, IOException {
    	return produceEXIF(new ByteArrayInputStream(data));			
    }

}
