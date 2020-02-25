package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.concept.Concept;
import edu.ncsu.las.model.collector.concept.ConceptCategory;
import edu.ncsu.las.persist.DAO;

public class ConceptCategoryDAO extends DAO {
	
	private static String INSERT = "INSERT INTO concept_categories (categoryid,domain_instance_name,categoryname,parentid) values (?,?,?,?)";
	private static String SELECT_CATEGORY = "SELECT categoryid,domain_instance_name,categoryname,parentid FROM concept_categories WHERE parentid = ? order by categoryname";
	private static String SELECT = "SELECT categoryid,domain_instance_name,categoryname,parentid FROM concept_categories ";
	private static String SELECT_ALL_FOR_DOMAIN = SELECT + " where domain_instance_name=? order by parentid,categoryname";

	private static String DELETE = "DELETE FROM concept_categories WHERE categoryid=? ";
	//private static String DELETE_PARENT = "DELETE FROM concept_categories WHERE parentid=? ";
	
	public static class ConceptCategoryRowMapper implements RowMapper<ConceptCategory> {
		public ConceptCategory mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new ConceptCategory(( java.util.UUID ) rs.getObject(1),rs.getString(2), rs.getString(3),( java.util.UUID ) rs.getObject(4));
		}
	}
	
	public boolean createConceptCategory(ConceptCategory c){
		int numRow=this.getJDBCTemplate().update(INSERT,c.getCategoryID(),c.getDomainInstanceName(), c.getCategoryName(),c.getParentID());
		return (numRow ==1);
	}
	
	public  ConceptCategory selectByParentID(String parentID){
		List<ConceptCategory> l = this.getJDBCTemplate().query(SELECT_CATEGORY,new ConceptCategoryRowMapper(), parentID);
		return l.get(0);
	}
	
	public List<ConceptCategory> selectAll(String domain){
		return this.getJDBCTemplate().query(SELECT_ALL_FOR_DOMAIN,new ConceptCategoryRowMapper(),domain);
	
	}
	public int delete(UUID uuid){
		return this.getJDBCTemplate().update(DELETE,uuid);
	}
		
	public void recursiveDelete(List<ConceptCategory> l){
		for(ConceptCategory c : l){
			List<ConceptCategory> lc = this.getJDBCTemplate().query(SELECT_CATEGORY,new ConceptCategoryRowMapper(), c.getCategoryID());
			
			Concept.deleteConceptByCategoryID(c.getCategoryID());
			this.delete(c.getCategoryID());
			recursiveDelete(lc);
		}
	}
	
	public void deleteCategory(UUID uuid){
		List<ConceptCategory> l = this.getJDBCTemplate().query(SELECT_CATEGORY,new ConceptCategoryRowMapper(), uuid);
		
		Concept.deleteConceptByCategoryID(uuid);
		this.delete(uuid);
		recursiveDelete(l);
		
	}
}