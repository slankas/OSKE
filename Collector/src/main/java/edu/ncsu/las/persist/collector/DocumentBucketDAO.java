package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.DocumentBucket;
import edu.ncsu.las.persist.DAO;

public class DocumentBucketDAO extends DAO {
	private static String SELECT = "SELECT id, domain_instance_name, document_bucket_tag,  document_bucket_question, owner_email, description, notes,date_created FROM document_bucket";
	private static String SELECT_COLLECTION     = SELECT + " WHERE id = ?";
	private static String SELECT_COLLECTION_ACCESSIBLE = SELECT + " c WHERE id = ? and (owner_email = ? or exists (select 1 from document_bucket_collaborator cc where c.id=cc.id and cc.collaborator_email=?)) order by document_bucket_tag";
	private static String SELECT_ALL_FOR_DOMAIN = SELECT + " WHERE domain_instance_name = ? order by document_bucket_tag";
	private static String SELECT_BY_DOMAIN_OWNER = SELECT + " WHERE domain_instance_name = ? and owner_email = ?  order by document_bucket_tag";
	private static String SELECT_BY_DOMAIN_AND_ACCESS = SELECT + " c WHERE domain_instance_name = ? and (owner_email = ? or exists (select 1 from document_bucket_collaborator cc where c.id=cc.id and cc.collaborator_email=?))  order by document_bucket_tag";
	private static String DELETE_BY_ID  = "DELETE FROM document_bucket WHERE id=? ";
	private static String DELETE_BY_DOMAIN  = "DELETE FROM document_bucket WHERE domain_instance_name=? ";
	private static String INSERT_RECORD = "INSERT INTO document_bucket (id, domain_instance_name, document_bucket_tag, document_bucket_question, owner_email, description, notes, date_created) VALUES (?,?,?,?,?,?,?,?)";
	private static String UPDATE_RECORD = "UPDATE document_bucket set document_bucket_tag =?,  document_bucket_question = ?, description = ?, notes = ? where id=?";
	
	private static String DELETE_COLLAB_RECORD = "delete from document_bucket_collaborator where id=?";
	private static String INSERT_COLLAB_RECORD = "insert into document_bucket_collaborator (id, domain_instance_name, collaborator_email, collaborator_name) values (?,?,?,?)";
	private static String SELECT_COLLABORATORS = "select collaborator_email, collaborator_name from  document_bucket_collaborator where id = ?";
	
	
	public static class CollectionRowMapper implements RowMapper<DocumentBucket> {
		public DocumentBucket mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new DocumentBucket (( java.util.UUID ) rs.getObject(1), rs.getString(2), rs.getString(3), rs.getString(4),rs.getString(5),rs.getString(6), rs.getString(7),rs.getTimestamp(8));
		}
	}
	
	public boolean insert(DocumentBucket c){
		int numRows = this.getJDBCTemplate().update(INSERT_RECORD,c.getID(), c.getDomainInstanceName(), c.getTag(), c.getQuestion(),
				                                               c.getOwnerEmail(), c.getDescription(), c.getPersonalNotes(), c.getDateCreated());
		return (numRows == 1);
	}
	
	public List<DocumentBucket> selectAll(String domainInstanceName){
		return this.getJDBCTemplate().query(SELECT_ALL_FOR_DOMAIN, new CollectionRowMapper(), domainInstanceName);
	}
	
	public List<DocumentBucket> selectByOwner(String domainInstanceName, String ownerEmail){
		return this.getJDBCTemplate().query(SELECT_BY_DOMAIN_OWNER, new CollectionRowMapper(), domainInstanceName, ownerEmail);
	}	
	
	public List<DocumentBucket> selectAvailableCollections(String domainInstanceName, String email){
		return this.getJDBCTemplate().query(SELECT_BY_DOMAIN_AND_ACCESS, new CollectionRowMapper(), domainInstanceName, email, email);
	}	
	
	public boolean delete(UUID uuid){
		//need to delete from document_bucket_collaborator first
		return (this.getJDBCTemplate().update(DELETE_BY_ID,uuid) == 1);
	}
	
	public int deleteByDomain(String domain){
		return this.getJDBCTemplate().update(DELETE_BY_DOMAIN,domain);
	}
	
	public boolean update(DocumentBucket c) {
		int numRows = this.getJDBCTemplate().update(UPDATE_RECORD, c.getTag(),  c.getQuestion(), c.getDescription(), c.getPersonalNotes(), c.getID());
		return (numRows == 1);
	}
	
	public DocumentBucket getDocumentCollection(UUID bucketUUID) {
		return  this.getJDBCTemplate().queryForObject(SELECT_COLLECTION, new CollectionRowMapper(), bucketUUID);
	}
	
	public DocumentBucket getDocumentCollection(UUID bucketUUID, String email) {
		try {
			return  this.getJDBCTemplate().queryForObject(SELECT_COLLECTION_ACCESSIBLE, new CollectionRowMapper(), bucketUUID, email,email);
		}
		catch (org.springframework.dao.EmptyResultDataAccessException e) {
			return null;
		}
	}	
	
	public static class CollectionCollaboratorRowMapper implements RowMapper<DocumentBucket.Collaborator> {
		public DocumentBucket.Collaborator mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new DocumentBucket.Collaborator(rs.getString(1),rs.getString(2));
		}
	}
	

	public int deleteCollaborators(UUID uuid) {
		return this.getJDBCTemplate().update(DELETE_COLLAB_RECORD,uuid);
	}
	
	public List<DocumentBucket.Collaborator> selectCollaborators(UUID bucketUUID){
		return this.getJDBCTemplate().query(SELECT_COLLABORATORS, new CollectionCollaboratorRowMapper(), bucketUUID);
	}
	
	public boolean insertCollaborator(DocumentBucket dc, DocumentBucket.Collaborator collaborator) {
		int numRows = this.getJDBCTemplate().update(INSERT_COLLAB_RECORD,dc.getID(), dc.getDomainInstanceName(),collaborator.getEmail(),collaborator.getName());
		return (numRows == 1);		
	}

	public void storeAllCollaborators(DocumentBucket documentCollection) {
		java.util.List<DocumentBucket.Collaborator> collabotors = documentCollection.getCollabotors();
		for (DocumentBucket.Collaborator c: collabotors) {
			this.insertCollaborator(documentCollection, c);
		}
		
	}
	
}