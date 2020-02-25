package edu.ncsu.las.document;

import java.util.List;
import java.util.logging.Level;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import edu.ncsu.las.model.collector.type.MimeType;

/**
 * Used to process Office 2003 and old powerpoint files
 * 
 * TODO complete this Need to include possible configuration.
 * 
 */

public class RssHandler extends DocumentHandler {

	public static final String[] MIME_TYPE = { MimeType.APPLICATION_RSS_XML, MimeType.APPLICATION_ATOM_XML};

	@Override
	protected void processDocument() {
		
		try {
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(new java.io.ByteArrayInputStream(this.getCurrentDocument().getContentData())));
			List<SyndEntry> entries = feed.getEntries();
			for (SyndEntry entry: entries) {
				this.addURL(entry.getLink());
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to process XML / RSS Document: "+e);
		}

		logger.log(Level.FINER, "RSS XML Handler complete: " + this.getCurrentDocument().getURL());
	}

	@Override
	public String[] getMimeType() {
		return MIME_TYPE;
	}

	@Override
	public String getDocumentDomain() {
		return "";
	}

	@Override
	public String getDescription() {
		return "Used to process RSS feeds (atom / rss) as starting points for web crawls.  The entries form the initial seeds for a web crawl.  If depth = 0, only the entries in the feed would be retrieved.";
	}

	@Override
	public int getProcessingOrder() {

		return 100;
	}
}
