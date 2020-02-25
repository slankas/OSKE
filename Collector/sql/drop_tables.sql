----- drop tables

drop table if exists job;
drop table if exists job_archive;
drop table if exists job_status_history;
drop table if exists Site_Crawl_Rule;
drop table if exists Visited_Pages;
drop table if exists system_user;
drop table if exists system_user_password;
drop table if exists system_user_option;
drop table if exists Job_History;
drop table if exists concepts;
drop table if exists concept_categories;
drop table if exists domain_discovery_session_execution;
drop table if exists domain_discovery_session;
drop table if exists domain;
drop table if exists user_agreement;
drop table if exists user_agreement_text;
drop table if exists search_alert;
drop table if exists search_alert_notification;
drop table if exists structural_extraction;

--- TODO: the collection tables are being replaced by the document_bucket tables.  Will need to remove when migration complete
drop table if exists collection;
drop table if exists collection_collaborator;

drop table if exists document_bucket;
drop table if exists document_bucket_collaborator;

drop table if exists discovery_index;
drop table if exists project;
drop table if exists project_document;
