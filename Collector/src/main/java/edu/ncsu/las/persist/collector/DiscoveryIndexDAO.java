package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;
import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.DiscoveryIndex;
import edu.ncsu.las.persist.DAO;



public class DiscoveryIndexDAO extends DAO {
	private static String SELECT_NO_DATA = "SELECT id, domain_instance_name, '{}', name,num_documents,owner_email,file_storage_area, date_created FROM discovery_index";
	private static String SELECT_FULL = "SELECT id, domain_instance_name, data, name,num_documents,owner_email,file_storage_area, date_created FROM discovery_index";
	private static String SELECT_COLLECTION = SELECT_FULL + " WHERE id = ?";
	private static String SELECT_BY_AREA = SELECT_NO_DATA + " WHERE domain_instance_name = ? and file_storage_area = ?";
	private static String EXISTS         ="SELECT exists(SELECT 1 FROM discovery_index WHERE id=?)";
	private static String DELETE_BY_ID   = "DELETE FROM discovery_index WHERE id=? ";
	private static String DELETE_BY_DOMAIN = "DELETE FROM discovery_index WHERE domain_instance_name=? ";
	private static String INSERT_RECORD    = "INSERT INTO discovery_index (id, domain_instance_name,data, name,num_documents,owner_email, file_storage_area, date_created) VALUES (?,?,(to_json(?::json)),?,?,?,?,?)";
	
	public static class DiscoveryIndexRowMapper implements RowMapper<DiscoveryIndex> {
		public DiscoveryIndex mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new DiscoveryIndex (( java.util.UUID ) rs.getObject(1), rs.getString(2), new JSONObject(rs.getString(3)), rs.getString(4), rs.getInt(5), rs.getString(6), rs.getString(7), rs.getTimestamp(8));
		}
	}

	public DiscoveryIndex retrieve(UUID indexID) {
		List<DiscoveryIndex> indexes =  this.getJDBCTemplate().query(SELECT_COLLECTION, new DiscoveryIndexRowMapper(), indexID);
		if (indexes.size() == 0) { return null;}
		else { return indexes.get(0);}
	}

	public List<DiscoveryIndex> selectByDomainAndArea(String domain, String area) {
		return this.getJDBCTemplate().query(SELECT_BY_AREA, new DiscoveryIndexRowMapper(), domain, area);
	}
	
	public boolean exists(UUID indexID) {
		Boolean result  =   (Boolean)  this.getJDBCTemplate().queryForObject( EXISTS, new Object[] { indexID }, Boolean.class);
		return result.booleanValue();
	}
	
	
	public boolean delete(UUID uuid){
		return (this.getJDBCTemplate().update(DELETE_BY_ID,uuid) == 1);
	}

	public int deleteByDomain(String domain){
		return this.getJDBCTemplate().update(DELETE_BY_DOMAIN,domain);
	}

	
	public boolean insert(DiscoveryIndex di) {
		int numRows = this.getJDBCTemplate().update(INSERT_RECORD, di.getID(),di.getDomainInstanceName(),di.getIndexData().toString(),di.getName(), di.getNumDocuments(), di.getOwnerEmail(), di.getFileStorageArea(),di.getDateCreated());
		return (numRows == 1);
	}
	
}