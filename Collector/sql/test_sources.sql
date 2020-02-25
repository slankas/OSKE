-- Olympian data
insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(uuid_generate_v1(), 'http://www.greekmythology.com/Olympians/olympians.html','web', 60,'new', '2015-10-24 00:10:33.28891',	'test@ncsu.edu','{"limitToHost":true,"startsWithPath": "/olympians/","webCrawler":{"maxDepthOfCrawling":1},"extractArea":[{"title":"","selector":".main-post-title"},{"title":"---------------","selector":".main-post-content"}]}','Greek Methodology Test','');

-- FAROO
insert into job (id, url, source_handler, frequency_min, status, status_dt, owner_email, config,name, latest_job_collector) values
(uuid_generate_v1(), 'quadcopter drones','faroo', 10880,'new', '2015-10-24 00:10:33.28891',	'test@ncsu.edu','{"limitToHost":true,"faroo" : { "source": "web", "length": 30, "key" : "" },"webCrawler":{"maxDepthOfCrawling":0}}','Faroo - web drone and quadcopter', ');
