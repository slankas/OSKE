package edu.ncsu.las.storage.export;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.Export;
import edu.ncsu.las.model.collector.type.Export.Format;
import edu.ncsu.las.util.StringUtilities;
import edu.ncsu.las.util.StringValidation;

public class OpenKETarArchiveEntry extends TarArchiveEntry {
	private static Logger logger =Logger.getLogger(OpenKETarArchiveEntry.class.getName());
	
	byte[] _content;
	public OpenKETarArchiveEntry(String name, byte[] content) {
		super(name);
		
		_content = content;
		this.setSize(content.length);
	}
	
	public byte[] getContent() { return _content; }
	
	
	
	public static  OpenKETarArchiveEntry createTarArchiveEntry(JSONObject record, Format format, String grouping, String naming, boolean eliminateNonSentences, boolean removeNonASCII, boolean stemText) {
		if (record.has("source_uuid") == false) { return null; }
		String name = record.getString("source_uuid");
		
		if (naming.equals("url")) {
			try {
				name = URLEncoder.encode(record.getString("url"), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				logger.log(Level.SEVERE, "Invalid encoding - UTF-8");
			} 		
		}
		else if (naming.equals("voyant")) {
			try {
				name = record.getString("url");
				
				URL url = new URL(name);
				name = record.optString("html_title", "") + url.getFile() + " - " + url.getHost() + " - " + record.getString("source_uuid");
				name = URLEncoder.encode(name, "UTF-8");
			} catch (Exception e) {
				logger.log(Level.SEVERE, "voyant naming: "+ e);
			} 		
		}
		if (grouping.equals("byDate")) {
            String crawledDate = record.getString("crawled_dt");
            String dateDir = crawledDate.substring(0,4)+crawledDate.substring(5,7)+crawledDate.substring(8,10);
            name = dateDir + "/"+name;
		}
		if (format == Export.Format.JSON_FILE || format == Export.Format.JSON_OBJ_LINE) {
			name += ".json";
		}
		else {
			name += ".txt";
		}
		
		name = StringValidation.removeNonASCII(name, "");
		
		String text = record.optString("text","");
		if (eliminateNonSentences) {
			text = StringUtilities.eliminateNonSentences(text);
		}
		if (removeNonASCII) {
			text = StringValidation.removeNonASCII(text, " ");
		}
		if (stemText) {
			text= StringUtilities.stemText(text);
		}
		
		byte[] content = null;
		if (format == Export.Format.IND_TEXT_ONLY) {
			content = text.getBytes(StandardCharsets.UTF_8);
		}
		else if (format == Export.Format.IND_TEXT_EXPANDED) {
            StringBuilder output = new StringBuilder();
            output.append(record.getString("source_uuid"));
            output.append("\n");
            output.append(record.optString("html_title"));
            output.append("\n");
            output.append(record.optString("url"));
            output.append("\n");
            output.append(record.getString("crawled_dt"));
            output.append("\n");
            output.append(text);
            output.append("\n");
            content = output.toString().getBytes(StandardCharsets.UTF_8);
		}
		else if (format == Export.Format.JSON_FILE || format == Export.Format.JSON_OBJ_LINE) {
			content = record.toString().getBytes(StandardCharsets.UTF_8);
		}
		else {
			logger.log(Level.SEVERE, "Invalid file format: "+format);
			return null;
		}
		
		OpenKETarArchiveEntry tae = new OpenKETarArchiveEntry(name,content);

		return tae;
	}
	
}