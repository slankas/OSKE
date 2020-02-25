package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.concept.Concept;
import edu.ncsu.las.persist.DAO;

public class ConceptDAO extends DAO {

	private static String SELECT = "SELECT id,domain_instance_name,categoryid,name,type,regex FROM concepts";
	private static String SELECT_ALL_FOR_DOMAIN = SELECT + " WHERE domain_instance_name = ?";
	private static String SELECT_CONCEPTS = SELECT + " WHERE categoryid = ?";
	private static String DELETE_BY_CATEGORY_ID = "DELETE FROM concepts WHERE categoryid=? ";
	private static String DELETE_BY_CONCEPT_ID  = "DELETE FROM concepts WHERE id=? ";
	private static String INSERT_ROW = "INSERT INTO concepts (id,domain_instance_name,categoryid,name,type,regex) VALUES (?,?,?,?,?,?)";
	
	
	public static class ConceptRowMapper implements RowMapper<Concept> {
		public Concept mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new Concept(( java.util.UUID ) rs.getObject(1), rs.getString(2), ( java.util.UUID ) rs.getObject(3),rs.getString(4),rs.getString(5),rs.getString(6));
		}
	}
	
	public boolean insert(Concept c){
		int numRows = this.getJDBCTemplate().update(INSERT_ROW,c.getId(), c.getDomainInstanceName(), c.getCategoryId(),c.getName(),c.getType(),c.getRegex());
		return (numRows == 1);
	}
	
	public List<Concept> selectConceptsByCategoryID(UUID id) {
		return this.getJDBCTemplate().query(SELECT_CONCEPTS, new ConceptRowMapper(),id);

	}
	
	public List<Concept> selectAll(String domainInstanceName){
		return this.getJDBCTemplate().query(SELECT_ALL_FOR_DOMAIN, new ConceptRowMapper(), domainInstanceName);
	}
	
	public int deleteByCategoryID(UUID uuid){
		return this.getJDBCTemplate().update(DELETE_BY_CATEGORY_ID,uuid);
	}
	
	public int delete(UUID uuid){
		return this.getJDBCTemplate().update(DELETE_BY_CONCEPT_ID,uuid);
	}
	
	
	
	
	
	
}