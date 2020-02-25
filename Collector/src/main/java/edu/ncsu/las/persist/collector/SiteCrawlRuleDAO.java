package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.SiteCrawlRule;
import edu.ncsu.las.persist.DAO;


public class SiteCrawlRuleDAO extends DAO {
	private static final Logger logger =Logger.getLogger(Collector.class.getName());
	
	private String SELECT = "select domain_instance_name,site_domain_name,flag from site_Crawl_rule";
	private String SELECT_DOMAIN_SITE_CRAWL_RULES = SELECT + " where domain_instance_name=?";
	
	private String INSERT_FROM_EXISTING_DOMAIN  = "insert into site_crawl_rule ( domain_instance_name,site_domain_name,flag) select ?,site_domain_name,flag from site_crawl_rule  where domain_instance_name=?";
	
	
	
	private static class SiteCrawlRuleMapper implements RowMapper<SiteCrawlRule> {
		public SiteCrawlRule mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new SiteCrawlRule(rs.getString(1), rs.getString(2), rs.getString(3));
		}
	}
	
	public List<SiteCrawlRule> getSiteCrawlRulesFromDomain(String domain){		
		logger.log(Level.INFO, "grabbing all site rules");
		return this.getJDBCTemplate().query(SELECT_DOMAIN_SITE_CRAWL_RULES, new SiteCrawlRuleMapper(),domain);
	}
	
	public List<SiteCrawlRule> getAllSiteCrawlRules(){		
		logger.log(Level.INFO, "grabbing all site rules");
		return this.getJDBCTemplate().query(SELECT, new SiteCrawlRuleMapper());
	}
	
	public int copySiteRules(String srcDomain, String destDomain) {
		int count = this.getJDBCTemplate().update(INSERT_FROM_EXISTING_DOMAIN, destDomain,srcDomain);
			
		return count;
	}
	
}