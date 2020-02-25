--
-- Note: postgres requires the pgcrypto library to make random UUIDs for insert into 
-- From the command line of the postgress type, in the contrib directory.  Execute "gmake" and "gmake install" as root
-- For the database, execute "CREATE EXTENSION pgcrypto;"
--

DO $$DECLARE cnt INTEGER;
BEGIN
  SELECT COUNT(*) INTO cnt FROM pg_extension WHERE extname = 'pgcrypto';
  IF cnt = 0 THEN
    CREATE EXTENSION pgcrypto;
  END IF;
END$$;



create table domain (
	domain_instance_name	 character varying (15) not null,
	domain_status    character varying (10) not null,
	effective_ts	 timestamp with time zone not null,
	full_name        character varying (100) not null,
	description      character varying (1024) not null,
	primary_contact  character varying (512) not null,
	appearance_order integer not null,
	configuration    text not null,
	user_email_id	character varying(256) not null,
	insert_ts       timestamp with time zone not null,
	offline         boolean not null default false,
	PRIMARY KEY(domain_instance_name, effective_ts)
);

COMMENT ON TABLE domain IS 'Stores information - most notably, the system configuration for each domain within the system.';
COMMENT ON COLUMN domain.domain_instance_name IS  'this value is used to form elasticsearch index names, kafka queues, accumulo tables, and other items. Note: system is a reserved domain name.  The system domain stores the overall configuration is required to be created.';
COMMENT ON COLUMN domain.domain_status IS  'active/inactive';
COMMENT ON COLUMN domain.user_email_id IS 'who created this record'; 
COMMENT ON COLUMN domain.insert_ts IS 'when was this record created'; 

CREATE TABLE job(
	id				uuid not null PRIMARY KEY,
	domain_instance_name	 character varying (15) not null,
    url   			character varying(1024) not null,
	source_handler 	character varying(100) not null,
    priority        integer not null default 100,
   	status			character varying(50) not null,
   	status_dt 		timestamp with time zone not null,
   	latest_job_history_id	UUID,
   	latest_job_collector character varying(256) not null,
   	owner_email		character varying(256) not null,
   	config 		    text not null,
   	name			character varying(100) not null,
   	justification	character varying(4096),
   	cron_schedule   character varying(1024),
   	cron_next_run   timestamp with time zone,
   	adjudication_answers	text,
   	random_percent integer DEFAULT 0,
   	exportdata		boolean DEFAULT false
);
CREATE INDEX idx_job_name  ON job (name);
CREATE INDEX idx_job_domain ON job (domain_instance_name);

COMMENT ON TABLE job IS 'tracks jobs/configuration for collecting data from a wide variety of information sources on the internet';
COMMENT ON COLUMN job.domain_instance_name IS  'domain for this job';
COMMENT ON COLUMN job.url IS  'starting seed for the job or search criteria if the source handler is search-based';
COMMENT ON COLUMN job.source_handler IS  'handler used to collect data from the internet';
COMMENT ON COLUMN job.status IS  'status values: new - job created, but not yet run, processing - job currently running, complete - job has processed successfully and can run again, ready - job was on an error or hold status, but can now run, errored - error occured while running, hold - user has put the job on hold';  
COMMENT ON COLUMN job.status_dt IS  'date and time when the status was last changed';
COMMENT ON COLUMN job.config IS  'json configuration for the job';
COMMENT ON COLUMN job.cron_schedule IS  'cron-based schedule string that specifies the frequency when this job runs.  https://en.wikipedia.org/wiki/Cron';
COMMENT ON COLUMN job.cron_next_run IS  'date and time when this job is next scheduled to be run.  Computed every time a job completes.';
COMMENT ON COLUMN job.latest_job_collector IS  'which collector is/was processing the job'; 
COMMENT ON COLUMN job.adjudication_answers IS  'contains a json array of the FIPP(adjudicator) questions and answers completed by a user while creating/editing a job'; 
COMMENT ON COLUMN job.random_percent IS  'randomizes the start window to +/- a given percent'; 
COMMENT ON COLUMN job.exportdata IS  'allows user to toggle if the job retreived data is automatically zipped for export and transfer';

------------------------------------------------------------------------------------------

CREATE TABLE job_archive (
	id				uuid not null,
	domain_instance_name	 character varying (15) not null,
	update_dt       timestamp with time zone not null,
    user_email      character varying(256) not null,
	url   			character varying(1024) not null,
	source_handler 	character varying(100) not null,
    priority        integer not null default 100,
   	status			character varying(50) not null,
   	status_dt 		timestamp with time zone not null,
   	latest_job_history_id	UUID,
   	latest_job_collector character varying(256) not null,
   	owner_email		character varying(256) not null,
   	config 		    text not null,
   	name			character varying(100) not null,
   	justification	character varying(4096),
   	cron_schedule   character varying(1024),
   	cron_next_run   timestamp with time zone,
   	adjudication_answers	text, 
   	random_percent integer DEFAULT 0,
   	exportdata		boolean DEFAULT false,
   	PRIMARY KEY(id, update_dt)
);
COMMENT ON TABLE job_archive IS 'Tracks the history of a particular job.  Fields match those in job, but with the addition of update_dt and user_email to track changes and by whom';

-- table maintains the history that the job status values go through ...

CREATE TABLE job_status_history (
	id			uuid not null,
	status_dt 		timestamp with time zone not null,
	domain_instance_name	 character varying (15) not null,
   	status			character varying(50) not null,
   	operator_email		character varying(256) not null,
   	PRIMARY KEY(id, status_dt)
);
COMMENT ON TABLE job_status_history IS 'tracks the status values for a particular job';

-------

 
CREATE TABLE site_crawl_rule(
    domain_instance_name	 character varying (15) not null,
    site_domain_name  	character varying(1024) not null,
    flag 		character varying(100) not null, -- INCLUDE/EXCLUDE
   	PRIMARY KEY(domain_instance_name,site_domain_name)
);
COMMENT ON TABLE site_crawl_rule IS 'contains a list of host names that are primarily used to exclude them from being crawled.  Useful in situations where access may be granted to a site by an IP range, but that site usage license prohibits crawling.';

------------------------------------------------------------------------------------------

CREATE TABLE job_history(
    job_history_id		uuid  not null PRIMARY KEY,
    job_id	    uuid not null,
    domain_instance_name	 character varying (15) not null,
    job_name	character varying(100) not null,
    status		character varying(100) not null,
    startTime 	timestamp with time zone not null,
    endTime 	timestamp with time zone,
    comments 	character varying(4096),
   	job_collector   character varying(256) not null,
   	num_pages_visited       bigint not null,
   	total_page_size_visited bigint not null
 );

CREATE INDEX idx_job_history_job  ON job_history (job_id);
CREATE INDEX idx_job_history_starttime ON job_history (domain_instance_name,starttime DESC);

COMMENT ON TABLE job_history IS 'tracks the history of job executions/runs';
------------------------------------------------------------------------------------------

CREATE TABLE system_user (
    email_id	character varying(256) not null,
    name		character varying(100) not null,
    role		character varying(100) not null,
    domain_instance_name	 character varying (15) not null,
   	status			character varying(20) not null,
   	status_dt		timestamp with time zone not null,
   	changed_by_email_id	character varying(256) not null,
   	primary key (email_id,role,domain_instance_name)
);

COMMENT ON TABLE system_user IS 'tracks users access to specific domains. if a user has access to at least one domain, then they have access to the application';
COMMENT ON COLUMN system_user.email_id is 'user email address - must be valid as automated messages may be sent to this address';
COMMENT ON COLUMN system_user.name is 'name of the user';
COMMENT ON COLUMN system_user.domain_instance_name is 'domain in which the user has access for the specified role';
COMMENT ON COLUMN system_user.role is 'role values: administrator, collector, analyst, adjudicator';
COMMENT ON COLUMN system_user.status is 'status values: active, inactive, removed';
COMMENT ON COLUMN system_user.changed_by_email_id is 'who made this change to the record';
------------------------------------------------------------------------------------------

CREATE TABLE system_user_password (
    email_id	  character varying(256) not null,
    password	  character varying(100) not null,
    password_salt character varying(100) not null,
    temporary_access_token character varying (256) not null,
    must_change    boolean not null,
    password_changed_dt timestamp with time zone not null,
    account_locked_until_dt timestamp with time zone not null,
    account_suspended boolean not null,
    primary key (email_id)
);
COMMENT ON TABLE system_user_password is 'Tracks passwords if user local authentication is in use.  Password history and aging are NOT managed at this time';
COMMENT ON COLUMN system_user_password.email_id is 'which user does this record belong';
COMMENT ON COLUMN system_user_password.password is '256 bit value, base 64 encoded';
COMMENT ON COLUMN system_user_password.password_salt is '128 bit value, base 64 encocded';
COMMENT ON COLUMN system_user_password.temporary_access_token is 'Temporary token that can be emailed to the user to avoid entering there password.  They must change their password';
COMMENT ON COLUMN system_user_password.must_change is 'Is this user required to change to change their password on the next login?';
COMMENT ON COLUMN system_user_password.password_changed_dt is 'when was this password last changed?';
COMMENT ON COLUMN system_user_password.account_locked_until_dt is 'if the account is locked(eg, failed password attempts, then the user cannot access it until this date.';
COMMENT ON COLUMN system_user_password.account_suspended is 'is this account suspended?  if set to true, then the user should not be able to authenticate.';

------------------------------------------------------------------------------------------

CREATE TABLE system_user_option (
    email_id	character varying(256) not null,
    domain_instance_name	 character varying (15) not null,
   	option_name     character varying(50) not null,
   	option_value    text not null,
   	status_dt		timestamp with time zone not null,
   	constraint system_user_option_pkey primary key (email_id,domain_instance_name,option_name)
);

COMMENT ON TABLE system_user_option IS 'tracks user-specified options';
COMMENT ON COLUMN system_user_option.email_id is 'options for which user';
COMMENT ON COLUMN system_user_option.domain_instance_name is 'for what domain is this option. domains are independent (unlike configuration which uses system as the default and provides overrides)';
COMMENT ON COLUMN system_user_option.option_name is 'specific option';
COMMENT ON COLUMN system_user_option.option_value is 'value for that option - format is specific to the option';
COMMENT ON COLUMN system_user_option.status_dt is 'when was this option last updated';



-------------------------------------------------------------------------------------------
CREATE TABLE visited_pages(
	id				uuid  		not null PRIMARY KEY,
	job_history_id	uuid 		not null,
	job_id			uuid 		not null,
	domain_instance_name	 character varying (15) not null,
	url				character varying(4096),
	visited_ts		timestamp with time zone not null,
	mime_type 		character varying(100) not null,
	STORAGE_UUID	character varying(36),
	sha256_hash     character varying(64)  not null,
	status          character varying(20)  not null,
	related_id      uuid  not null,
   	full_text_send_result character varying(50) not null,
   	storage_area	character varying(20) not null,
	CONSTRAINT fk_jobId FOREIGN KEY (job_history_id)
      REFERENCES Job_History (job_history_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);
COMMENT ON TABLE visited_pages IS 'tracks all urls visted in the crawling process';
COMMENT ON COLUMN visited_pages.status IS 'new - first time seen, update - page was updated, unchanged - page was not changed, othersource - content matches record from another source, irrelevant - page was not relevant to the problemDomain ';
COMMENT ON COLUMN visited_pages.related_id is 'refers to previous record if the same URL, or if the hash existed previously, then that record';
COMMENT ON COLUMN visited_pages.storage_area is 'what is the storage area that this page was retreived: sandbox,normal,archive';

CREATE INDEX idx_visited_pages_jobhistoryID   ON visited_pages    (job_history_id);
CREATE INDEX idx_visited_pages_jobID   ON visited_pages    (job_id);
CREATE INDEX idx_visited_pages_sha  ON visited_pages  (sha256_hash COLLATE pg_catalog."default");
CREATE INDEX idx_visited_pages_storage_id  ON visited_pages(storage_uuid);
CREATE INDEX idx_visited_pages_time ON visited_pages (domain_instance_name,storage_area,visited_ts DESC NULLS LAST);

COMMENT ON INDEX idx_visited_pages_time IS 'Used for the most recent visited pages';
------------------------------------------------------------------------------------------
CREATE TABLE public.concept_categories (
  categoryid uuid NOT NULL,
  domain_instance_name	 character varying (15) not null,
  categoryname character varying(100),
  parentid uuid,
  CONSTRAINT concept_category_pk PRIMARY KEY (categoryid)
);
CREATE INDEX idx_concept_categories_parent ON public.concept_categories (parentid);

COMMENT ON TABLE concept_categories IS 'hierarchical groups for concepts';

CREATE TABLE public.concepts (
  id uuid NOT NULL,
  domain_instance_name	 character varying (15) not null,
  categoryid uuid,
  name character varying(100),
  type character varying(100),
  regex character varying(200),
  CONSTRAINT concept_pk PRIMARY KEY (id)
);

CREATE INDEX idx_concept_category ON public.concepts (categoryid);
COMMENT ON TABLE concepts IS 'regex based concepts for particular types of data/items'; 

------------------------------------------------------------------------------------------
CREATE TABLE public.domain_discovery_session
(
  session_id uuid NOT NULL,
  domain_instance_name	 character varying (15) not null,
  session_name character varying(256) NOT NULL,
  user_id character varying(256) NOT NULL,
  created_dt timestamp with time zone  NOT NULL,
  last_activity_dt timestamp with time zone  NULL,
  CONSTRAINT domain_discovery_session_pkey PRIMARY KEY (session_id)
);
CREATE INDEX idx_domain_discovery_session_domain ON domain_discovery_session (domain_instance_name,session_name);

COMMENT ON TABLE domain_discovery_session IS 'Domain discovery session tracks searches - whether finding new information on the internet or retrieving data from existing holdings/collected data';
COMMENT ON COLUMN domain_discovery_session.session_id IS 'primary key used track a session (e.g., searches on a particular topic).  This ID is also used to link to documentIndexes (back of the book) created for the entire session by the user';
COMMENT ON COLUMN domain_discovery_session.domain_instance_name IS 'do what domain does this session belong?';
COMMENT ON COLUMN domain_discovery_session.session_name IS 'name used for users to identify a particular session';
COMMENT ON COLUMN domain_discovery_session.user_id IS 'who created the session';
COMMENT ON COLUMN domain_discovery_session.created_dt IS 'when was the session created (first search)';
COMMENT ON COLUMN domain_discovery_session.last_activity_dt IS 'when was a search last performed for the session?';


CREATE TABLE public.domain_discovery_session_execution
(
  session_id uuid NOT NULL,
  domain_instance_name	 character varying (15) not null,
  execution_number integer NOT NULL,
  search_terms character varying(1024) NOT NULL,
  user_id character varying(256) NOT NULL,
  max_number_search_result integer,
  search_api character varying(50) NOT NULL,
  advanced_configuration character varying(4096),
  execution_start_dt timestamp with time zone NOT NULL,
  execution_end_dt timestamp with time zone,
  document_index_id uuid NULL,
  shouldtranslate boolean NOT NULL DEFAULT false,
  source_language character varying(4) NOT NULL DEFAULT 'none',
  search_terms_translated character varying(1024) NOT NULL DEFAULT 'none',
  CONSTRAINT pk_ddse PRIMARY KEY (session_id, execution_number)
);

COMMENT ON TABLE domain_discovery_session_execution IS '';
COMMENT ON COLUMN domain_discovery_session_execution.session_id IS '';
COMMENT ON COLUMN domain_discovery_session_execution.domain_instance_name IS '';
COMMENT ON COLUMN domain_discovery_session_execution.execution_number IS '';
COMMENT ON COLUMN domain_discovery_session_execution.search_terms IS '';
COMMENT ON COLUMN domain_discovery_session_execution.user_id IS '';
COMMENT ON COLUMN domain_discovery_session_execution.max_number_search_result IS '';
COMMENT ON COLUMN domain_discovery_session_execution.search_api IS '';
COMMENT ON COLUMN domain_discovery_session_execution.advanced_configuration IS '';
COMMENT ON COLUMN domain_discovery_session_execution.execution_start_dt IS '';
COMMENT ON COLUMN domain_discovery_session_execution.execution_end_dt IS '';
COMMENT ON COLUMN domain_discovery_session_execution.document_index_id IS  'link to the back of the book index created for this particular execution.';


---- Tracks the actual user agreements ------

CREATE TABLE user_agreement(
    email_id character varying(256) not null,
    agreement_timestamp timestamp with time zone not null,
    needs_new_acknowledgement boolean not null default FALSE,
    user_agreement_version integer not null,
    signature_text character varying(100) not null,
    signature_hash character varying(64) not null,
    agreement_version integer not null,
    status character varying(20) not null,
    status_timestamp timestamp with time zone not null,
    expiration_timestamp timestamp with time zone not null,
    answers text not null,
    user_organization character varying(100) not null,
    user_signature character varying(100) not null,
    digital_signature_hash character varying(64) not null,
    adjudicator_comments text,
    adjudicator_emailid character varying(64) not null,

    PRIMARY KEY(email_id, agreement_timestamp)
);

COMMENT ON TABLE user_agreement IS 'tracks the user signature of online usage agreements'; 
---- Stores the text(html) to be displayed to the user during the agreement process ----

CREATE TABLE user_agreement_text(
    version_number integer not null,
    version_date date not null,
    reading_text text not null,
    agreement_text text not null,
    questions text not null,

    PRIMARY KEY(version_number)
);
COMMENT ON TABLE user_agreement_text IS 'tracks the different versions of a user agreements that may be signed online.'; 

----------------------------------------------------------------------------------------------

CREATE TABLE search_alert (
    alert_id uuid not null,
	alert_name character varying(256) not null,
    source_handler character varying(100) not null,
    search_term character varying(1024) not null,
    number_of_result integer not null,
    owner_email_id character varying(256) not null,
    cron_schedule character varying(1024),
   	cron_next_run timestamp with time zone,
    date_created timestamp with time zone not null,
    date_last_run timestamp with time zone not null,
    state character varying(100) not null,
    current_collector character varying(100) not null,
    num_times_run integer not null,
    domain character varying(100) not null,

    PRIMARY KEY(alert_id)
);
COMMENT ON TABLE search_alert IS 'tracks searches that can be periodically run to identify new pages/items of interests'; 

CREATE TABLE search_alert_notification (
    alert_id uuid not null,
	result_url character varying(1024) not null,
    result_title character varying(1024) not null,
  	result_description character varying(1024) not null,
  	result_datetime timestamp with time zone not null,
    acknowledgement boolean not null,

    PRIMARY KEY(alert_id,result_url)
);
COMMENT ON TABLE search_alert IS 'specific items that have been found from executing searches'; 

CREATE TABLE structural_extraction (
	id				uuid not null PRIMARY KEY,
	domain_instance_name	 character varying (15) not null,
    hostname   		character varying(1024) not null,
	pathregex 		character varying(1024) not null,
	record_name		character varying(256)  not null,
	record_selector character varying(256)  not null,
	record_extract_by    character varying(20) not null default 'text',
	record_extract_regex character varying(256) not null default '',
	record_parent_id uuid  null,
	user_email_id	character varying(256) not null,
	last_database_ts       timestamp with time zone not null
);

CREATE INDEX idx_structural_extraction_domain ON structural_extraction (domain_instance_name);


COMMENT ON TABLE structural_extraction IS 'defines standard ways to extract content from a web page based upon css selectors';
COMMENT ON COLUMN structural_extraction.id IS  'primary key - ';
COMMENT ON COLUMN structural_extraction.domain_instance_name IS  ' ';
COMMENT ON COLUMN structural_extraction.hostname IS  'What host should this record be applied to?  Performed by looking at the end of DNS name.';
COMMENT ON COLUMN structural_extraction.pathregex IS  'if we need to limit to just part of the host, what is a regular expression that the path must match';
COMMENT ON COLUMN structural_extraction.record_name IS  'name to call the extracted data';
COMMENT ON COLUMN structural_extraction.record_extract_by IS  'text, html, text:regex, html:regex';
COMMENT ON COLUMN structural_extraction.record_extract_regex IS  'record_extract_regex';
COMMENT ON COLUMN structural_extraction.record_selector IS  'css selector (as defined by jSoup) for extracting the content.  Text content will be used';
COMMENT ON COLUMN structural_extraction.record_parent_id IS  'If this record is part of a group, what is the parent record?  The css selector will run from the extracted parent content';
COMMENT ON COLUMN structural_extraction.user_email_id IS  'Who last changed this record?';
COMMENT ON COLUMN structural_extraction.last_database_ts IS  'When was this record last changed?';

--- TODO: the collection tables are being replaced by the document_bucket tables.  Will need to remove when migration complete

CREATE TABLE collection (
	id				uuid not null PRIMARY KEY,
	domain_instance_name	 character varying (15) not null,
	collection_name character varying(100) not null,
	owner_email		character varying(256) not null,
	description     character varying(4096),
	notes           character varying(4096),
	date_created timestamp with time zone not null
);
CREATE INDEX idx_collection_domain ON collection (domain_instance_name);
CREATE INDEX idx_collection_domain_owner ON collection (domain_instance_name,owner_email);

COMMENT ON TABLE collection IS 'defines a group of related documents.  The actual collection IDs are mapped to documents in ElasticSearch';
COMMENT ON COLUMN collection.id IS  'primary key  ';
COMMENT ON COLUMN collection.domain_instance_name IS  'To what domain does this collection belong to';
COMMENT ON COLUMN collection.collection_name IS  'Name of the collection';
COMMENT ON COLUMN collection.owner_email IS  'who created/owns this collection';
COMMENT ON COLUMN collection.description IS  'What is this collection about?';
COMMENT ON COLUMN collection.notes IS  'Free-form comments about the collection';
COMMENT ON COLUMN collection.date_created IS  'When was this collection originally created?';


CREATE TABLE collection_collaborator (
	id					 uuid not null,
	domain_instance_name character varying (15) not null,
	collaborator_email	 character varying(256) not null,
	collaborator_name    character varying(4096) not null,
	primary key (id, domain_instance_name, collaborator_email, collaborator_name)
);

CREATE INDEX idx_collection_clb_domain_clb ON collection_collaborator (domain_instance_name,collaborator_email);

COMMENT ON TABLE collection_collaborator IS 'Defines the individuals that can view and edit a collection in addition to the owner';
COMMENT ON COLUMN collection_collaborator.id IS  'To what collection do these records belong to';
COMMENT ON COLUMN collection_collaborator.domain_instance_name IS  'To what domain does this collection belong to';
COMMENT ON COLUMN collection_collaborator.collaborator_email IS  'who can access this collection';
COMMENT ON COLUMN collection_collaborator.collaborator_name IS  'accessor''s name';


CREATE TABLE document_bucket (
	id				uuid not null PRIMARY KEY,
	domain_instance_name	 character varying (15) not null,
	document_bucket_tag character varying(30) not null,
	document_bucket_question character varying(256) not null,	
	owner_email		character varying(256) not null,
	description     character varying(4096),
	notes           character varying(4096),
	date_created timestamp with time zone not null
);
CREATE INDEX idx_document_bucket_domain ON document_bucket (domain_instance_name);
CREATE INDEX idx_document_bucket_domain_owner ON document_bucket (domain_instance_name,owner_email);

COMMENT ON TABLE document_bucket IS 'defines a group of related documents.  The actual document bucket IDs are mapped to documents in ElasticSearch';
COMMENT ON COLUMN document_bucket.id IS  'primary key  ';
COMMENT ON COLUMN document_bucket.domain_instance_name IS  'To what domain does this document bucket belong to';
COMMENT ON COLUMN document_bucket.document_bucket_tag IS  'tag (short identifier of the document_bucket';
COMMENT ON COLUMN document_bucket.document_bucket_question IS  'What question do documents in this bucket try to address?';
COMMENT ON COLUMN document_bucket.owner_email IS  'who created/owns this document bucket';
COMMENT ON COLUMN document_bucket.description IS  'What is this document_bucket about?';
COMMENT ON COLUMN document_bucket.notes IS  'Free-form comments about the document bucket';
COMMENT ON COLUMN document_bucket.date_created IS  'When was this document_bucket originally created?';


CREATE TABLE document_bucket_collaborator (
	id					 uuid not null,
	domain_instance_name character varying (15) not null,
	collaborator_email	 character varying(256) not null,
	collaborator_name    character varying(4096) not null,
	primary key (id, domain_instance_name, collaborator_email, collaborator_name)
);

CREATE INDEX idx_document_bucket_clb_domain_clb ON document_bucket_collaborator (domain_instance_name,collaborator_email);
COMMENT ON TABLE document_bucket_collaborator IS 'Defines the individuals that can view and edit a document bucket in addition to the owner';
COMMENT ON COLUMN document_bucket_collaborator.id IS  'To what document bucket do these records belong to';
COMMENT ON COLUMN document_bucket_collaborator.domain_instance_name IS  'To what domain does this document bucket belong to';
COMMENT ON COLUMN document_bucket_collaborator.collaborator_email IS  'who can access this document bucket';
COMMENT ON COLUMN document_bucket_collaborator.collaborator_name IS  'accessor''s name';



CREATE TABLE discovery_index (
	id				     uuid not null PRIMARY KEY,
	domain_instance_name character varying (15) not null,
	data                 json not null,
	name                 character varying(4096) not null,
	num_documents        integer not null,
	owner_email          character varying(256) not null,
	file_storage_area    character varying(30) not null,
	date_created         timestamp with time zone not null
);
CREATE INDEX idx_discovery_index ON discovery_index (domain_instance_name,file_storage_area);

COMMENT ON TABLE discovery_index IS 'Stores back of the book indices for domain discovery session';
COMMENT ON COLUMN discovery_index.id IS  'primary key  ';
COMMENT ON COLUMN discovery_index.domain_instance_name IS  'To what domain does this index belong to';
COMMENT ON COLUMN discovery_index.data IS  'index data, stored in json format';
COMMENT ON COLUMN discovery_index.name IS  'Name of the document index - only used for those outside of the sandbox';
COMMENT ON COLUMN discovery_index.num_documents IS  'how many documents are in this index';
COMMENT ON COLUMN discovery_index.owner_email IS  'individual created this index';
COMMENT ON COLUMN discovery_index.file_storage_area IS  'what was the filestorage area type? (normal/archive/sandbox)';
COMMENT ON COLUMN discovery_index.date_created IS  'When was this index last created/indexed?';


CREATE TABLE project (
	id				     uuid not null PRIMARY KEY,
	domain_instance_name character varying (15) not null,
	name                 character varying(256) not null,
	status               character varying (15) not null,
	purpose              character varying(4096) not null,
	key_questions         json not null,
	assumptions          json not null,
	related_urls         json not null,	
	date_created             timestamp with time zone not null,
	created_by_user_email_id character varying(256) not null,
	date_updated             timestamp with time zone not null,
    updated_by_user_email_id character varying(256) not null
);
CREATE INDEX idx_project_domain ON project (domain_instance_name);

COMMENT ON TABLE project IS 'Stores information about a given analytic task';
COMMENT ON COLUMN project.id IS  'primary key  ';
COMMENT ON COLUMN project.domain_instance_name IS  'To what domain does this project belong to';
COMMENT ON COLUMN project.name IS  'project name';
COMMENT ON COLUMN project.status IS  'active or inactive.';
COMMENT ON COLUMN project.purpose IS  'general description as to what this particular plan/project is seeking to accomplish';
COMMENT ON COLUMN project.key_questions IS  'json array of objects with two fields: question and tag.  Each object will create a new collection (tags will determine uniqueness)';
COMMENT ON COLUMN project.assumptions IS  'json array of strings describing the primary assumptions involved with this project';
COMMENT ON COLUMN project.related_urls IS  'json array of objects containing a link and title.  Used to point to external resources (mind maps, documents, sharepoint sites, etc.)';
COMMENT ON COLUMN project.date_created IS  'when was this project created?';
COMMENT ON COLUMN project.created_by_user_email_id IS  'who first created this project?';
COMMENT ON COLUMN project.date_updated IS  'when was the project last updated?';
COMMENT ON COLUMN project.updated_by_user_email_id IS  'who last updated the project?';


-- an alternate design for this table would be to store the timestamp as well - would allow for a history to be maintained

CREATE TABLE project_document (
	id				     uuid not null PRIMARY KEY,
	domain_instance_name character varying (15) not null,
	name                 character varying(256) not null,
	status               character varying (15) not null,
	contents             text,
	date_created             timestamp with time zone not null,
	created_by_user_email_id character varying(256) not null,
	date_updated             timestamp with time zone not null,
    updated_by_user_email_id character varying(256) not null
);
CREATE INDEX idx_project_document_domain ON project (domain_instance_name, name);

COMMENT ON TABLE project_document IS 'Scratchpad / document that an analytst can use to track information within the tool';
COMMENT ON COLUMN project_document.id IS  'primary key  ';
COMMENT ON COLUMN project_document.domain_instance_name IS  'To what domain does this scratchpad/document belong to';
COMMENT ON COLUMN project_document.name IS  'document name';
COMMENT ON COLUMN project_document.contents IS  'document contents';
COMMENT ON COLUMN project_document.date_created IS  'when was this scratchpad/document created?';
COMMENT ON COLUMN project_document.created_by_user_email_id IS  'who first created this scratchpad/document?';
COMMENT ON COLUMN project_document.date_updated IS  'when was the scratchpad/document last updated?';
COMMENT ON COLUMN project_document.updated_by_user_email_id IS  'who last updated the scratchpad/document?'; 