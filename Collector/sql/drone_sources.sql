insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://www.gameofdrones.com/','web', 2880,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{ "limitToHost": true }','Game of Drones', '');

insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://www.protoquad.com/','web', 2880,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{ "limitToHost": true }','Proto-X (Quad)', '');

insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://knowbeforeyoufly.org/','web', 2880,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{ "limitToHost": true }','Know Before You Fly', '');

insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://www.aerialtronics.com/','web', 2880,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{ "limitToHost": true }','AerialTronics', '');

insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://www.airware.com','web', 2880,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{ "limitToHost": true }','Air Ware', '');

insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://www.precisionhawk.com/','web', 2880,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{ "limitToHost": true }','Precision Hawk', '');

insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://aeronavics.com/','web', 2880,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{ "limitToHost": true }','Aeronavics', '');


insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://www.faa.gov/uas/','web', 2880,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{ "limitToHost": true, "startsWithPath": "/uas/", "webCrawler" : { "maxDownloadSize" : 1000000000} }','FAA - UAS', '');


insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://www.uavexpertnews.com/','web', 2880,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{ "limitToHost": true, "webCrawler" : { "maxDownloadSize" : 1000000000} }','UAV Expert News', '');

-- Amazon Photography Drones
insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://www.amazon.com/s/?node=11910405011&page={p:%d[1-100]}','web', 10080,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{"limitToHost":true,"relevantRegExp":"drone|quadcopter","webCrawler":{"maxDepthOfCrawling":1,"politenessDelay":20000},"extractArea":[{"title":"Title","selector":"#productTitle"},{"title":"","selector":"#feature-bullets"},{"title":"Details","selector":"#prodDetails"},{"title":"Description","selector":"#productDescription"},{"title":"Details - Bullets","selector":"#detail-bullets"},{"title":"Manufacturer Info","selector":"#aplus-product-description_feature_div"},{"title":"Manufacturer Info","selector":"#aplusProductDescription"},{"title":"Technical Specifications","selector":"#technical-specs_feature_div"},{"title":"Amazon Specific","selector":".kmd-section-container"},{"title":"Book Description","selector":"#bookDescription_feature_div"}]}','Amazon - Photography Drones', '');

-- Amazon Industrial & Scientific : Robotics : Unmanned Aerial Vehicles (UAVs)
insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://www.amazon.com/b/?node=8498892011&page={p:%d[1-100]}','web', 10080,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{"limitToHost":true,"relevantRegExp":"drone|quadcopter","webCrawler":{"maxDepthOfCrawling":1,"politenessDelay":20000},"extractArea":[{"title":"Title","selector":"#productTitle"},{"title":"","selector":"#feature-bullets"},{"title":"Details","selector":"#prodDetails"},{"title":"Description","selector":"#productDescription"},{"title":"Details - Bullets","selector":"#detail-bullets"},{"title":"Manufacturer Info","selector":"#aplus-product-description_feature_div"},{"title":"Manufacturer Info","selector":"#aplusProductDescription"},{"title":"Technical Specifications","selector":"#technical-specs_feature_div"},{"title":"Amazon Specific","selector":".kmd-section-container"},{"title":"Book Description","selector":"#bookDescription_feature_div"}]}','Amazon - Industrial & Scientific - UAVs', '');

-- Amazon toys & games - Quadcopters & Multirotors
insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://www.amazon.com/s/?node=11608080011&page={p:%d[1-100]}','web', 10080,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{"limitToHost":true,"relevantRegExp":"drone|quadcopter","webCrawler":{"maxDepthOfCrawling":1,"politenessDelay":20000},"extractArea":[{"title":"Title","selector":"#productTitle"},{"title":"","selector":"#feature-bullets"},{"title":"Details","selector":"#prodDetails"},{"title":"Description","selector":"#productDescription"},{"title":"Details - Bullets","selector":"#detail-bullets"},{"title":"Manufacturer Info","selector":"#aplus-product-description_feature_div"},{"title":"Manufacturer Info","selector":"#aplusProductDescription"},{"title":"Technical Specifications","selector":"#technical-specs_feature_div"},{"title":"Amazon Specific","selector":".kmd-section-container"},{"title":"Book Description","selector":"#bookDescription_feature_div"}]}','Amazon toyes & games - Quadcopters & Multirotors', '');

-- Amazon toys & games - Hobbies: RC Vehicles and Parts
insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://www.amazon.com/s/?rh=n%3A165793011%2Cn%3A%21165795011%2Cn%3A276729011%2Cn%3A6925830011&page={p:%d[1-100]}','web', 10080,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{"limitToHost":true,"relevantRegExp":"drone|quadcopter","webCrawler":{"maxDepthOfCrawling":1,"politenessDelay":20000},"extractArea":[{"title":"Title","selector":"#productTitle"},{"title":"","selector":"#feature-bullets"},{"title":"Details","selector":"#prodDetails"},{"title":"Description","selector":"#productDescription"},{"title":"Details - Bullets","selector":"#detail-bullets"},{"title":"Manufacturer Info","selector":"#aplus-product-description_feature_div"},{"title":"Manufacturer Info","selector":"#aplusProductDescription"},{"title":"Technical Specifications","selector":"#technical-specs_feature_div"},{"title":"Amazon Specific","selector":".kmd-section-container"},{"title":"Book Description","selector":"#bookDescription_feature_div"}]}','Amazon toys & games - toys & games - Hobbies: RC Vehicles and Parts', '');

-- Parrot.com  - exclude foreign sites  TODO: need to refine this more
insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://parrot.com/','web', 2880,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{ "limitToHost": true, "excludeFilter": "/(uk|fr|es|de|nl|it|pt|no|sv|lv|da|fi|pl|hu|cs|gb|bf|at|ie|lu|bn)/" }','Parrot - Drone Maker', '');



--  Site appears highly relevant
insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://droneanalyst.com','web', 2880,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{ "limitToHost": true, "webCrawler" : { "maxDownloadSize" : 1000000000} }','Drone Analyst - Complete Site', 'Highly relevant content for drones');

-- Just processes the feed from drone analyst on a regular basis
insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(gen_random_uuid(), 'http://droneanalyst.com/feed/','feed', 60,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{ "limitToHost": true, "webCrawler" : { "maxDepthOfCrawling" : 0} }','Drone Analyst Feed', '');


INSERT INTO job VALUES ('00000151-b1c9-983f-0a11-0c0100000001', 'https://en.wikipedia.org/wiki/Unmanned_aerial_vehicle', 'web', 10080, 'new', '2015-12-23 08:37:18.756474', '00000151-cf0e-e5a4-0a11-0c010000d38a', '', 'testuser@test.com', '{"relevantRegExp":"drone|quadcopter","limitToDomain":true,"allowSingleHopFromReferrer":true,"webCrawler":{"maxDownloadSize":1000000000}}', 'Wikipedia_Drone',  'Researching general drone information.', 1);





-- uuid_generate_v1()

-- temporary queries to save


http://flying-drones.expert/

-- good test page look at embedded links...
-- http://www.amazon.com/DJI-Phantom-Standard-Quadcopter-Camera/dp/B013U0F6EQ/ref=lp_11910405011_1_7/176-5256055-9692153?ie=UTF8&qid=1447847012&s=photo&sr=1-7

-- Useful queries
delete from job where url like '%amazon%'

update job set status='ready' where url like '%amazon%'

insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(uuid_generate_v1(), 'http://www.amazon.com/Parrot-Bebop-Quadcopter-Controller-Bundle/dp/B00OOR90G0/ref=sr_1_2','web', 60,'new', '2015-10-24 00:10:33.28891',	'testuser@test.com','{"limitToHost":true,"relevantRegExp":"drone|quadcopter","webCrawler":{"maxDepthOfCrawling":1,"politenessDelay":15000},"extractArea":[{"title":"Title","selector":"#productTitle"},{"title":"","selector":"#feature-bullets"},{"title":"Details","selector":"#prodDetails"},{"title":"Description","selector":"#productDescription"},{"title":"Details - Bullets","selector":"#detail-bullets"},{"title":"Manufacturer Info","selector":"#aplus-product-description_feature_div"},{"title":"Manufacturer Info","selector":"#aplusProductDescription"},{"title":"Technical Specifications","selector":"#technical-specs_feature_div"},{"title":"Amazon Specific","selector":".kmd-section-container"},{"title":"Book Description","selector":"#bookDescription_feature_div"}]}','Amazon Test Page', '');

-- rough domain list / document crawl count
select substring(url from '[^\/\.\s]+\.[^\/\.\s]+\/'), count(*)
 from visited_pages
 group by substring(url from '[^\/\.\s]+\.[^\/\.\s]+\/')
 order by 2 desc
