package edu.ncsu.las.model.collector;

import java.util.HashMap;

import edu.ncsu.las.persist.collector.SiteCrawlRuleDAO;

/**
 * Should a given domain be crawlable or not?
 * 
 * Created to avoid crawling sites that the library has content licenses with in
 * which the resources are protected by IP ranges.
 */
public class SiteCrawlRule {

	private String _domainInstanceName;
	private String _siteDomainName;
	private String _flag;
	
	public SiteCrawlRule(String domainInstanceName, String siteDomainName, String flag){
		_domainInstanceName = domainInstanceName;
		_siteDomainName = siteDomainName;
		_flag = flag;
	}
	
	public String getDomainInstanceName() {
		return _domainInstanceName;
	}
	
	public String getSiteDomainName() {
		return _siteDomainName;
	}
	
	public String getFlag() {
		return _flag;
	}
	
	public static HashMap<String,HashMap<String,SiteCrawlRule>> getAllSiteRules() {
		java.util.List<SiteCrawlRule> tempResults = (new SiteCrawlRuleDAO()).getAllSiteCrawlRules();
		
		HashMap<String,HashMap<String,SiteCrawlRule>> results = new HashMap<String,HashMap<String,SiteCrawlRule>>();
		
		String lastDomainSeen = "";
		java.util.HashMap<String, SiteCrawlRule> domainRules = null;
		for (SiteCrawlRule scr: tempResults) {
			if (scr.getDomainInstanceName().equals(lastDomainSeen) == false) {
				if (domainRules != null) {
					results.put(lastDomainSeen, domainRules);
				}
				domainRules = new java.util.HashMap<String, SiteCrawlRule>();
			}
			domainRules.put(scr.getSiteDomainName(), scr);
			lastDomainSeen = scr.getDomainInstanceName();
		}
		if (lastDomainSeen.length() >0 && domainRules != null) {
			results.put(lastDomainSeen, domainRules);
		}
		
		if (results.containsKey(Domain.DOMAIN_SYSTEM) == false) {
			results.put(Domain.DOMAIN_SYSTEM, new java.util.HashMap<String, SiteCrawlRule>());
		}
		
		return results;
	}
	
	/**
	 * Copies all of the site rules from one domain, into another.
	 * 
	 * @param srcDomain
	 * @param destDomain
	 * @return
	 */
	public static int copySiteRules(String srcDomain, String destDomain) {
		return (new SiteCrawlRuleDAO()).copySiteRules(srcDomain, destDomain);
	}
	
}
