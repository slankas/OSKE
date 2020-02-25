# Open Source Knowledge Enrichment (OSKE)
OSKE is an Open Source (OS) Knowledge Enrichment environment to manage the complexities of capturing, formatting, manipulating and making sense of Big Data.
The  platform hosts techniques and analytics designed to be semi-automated and allow for configuration to scan a wide range of structured and unstructured data sources.  

The system helps capture the "right" (relevant) information from the "right" (reliable) sources, and the system provides capabilities to holistically analyze big data within an internet-connected environment and integrate data of value into the mission environment.

## Getting Started
The quickest way to evaluate OSKE is to provision a server in the cloud and install [OSKE-Lite](tree/master/OSKE-Lite).  Then review the quick start guide in the documentation folder.

## Contributing
You can contribute to OSKE by providing new source handlers, document handlers, and analytics.

## System Components
OSKE utilizes many different components provide a wide variety of capabilities to discover, capture, recall, and manager open source information.  While these components may all run on a single server, you should consider spreading the components across multiple servers to provide the necessary performance considerations your situation may require.

### External components
External components are either necessary infrastructure components (e.g., database server) or applications that have been developed by others.  At a minimum, Elasticsearch and PostgreSQL must be utilized.  Other components can be utilized by your organization needs.

|Name |Required |Version	|Purpose
| --- | --- | --- | ---
|Cerebro |	No|	0.8.3	|Web administration tool for ElasticSearch <br>https://github.com/lmenezes/cerebro
|Elasticsearch |Yes|7.x+	|Stores crawled data from a variety of information sources.  Requires at least 7.0.0 due to [Elasticsearch’s removal of types](https://www.elastic.co/guide/en/elasticsearch/reference/current/removal-of-types.html).<br>https://www.elastic.co/products/elasticsearch |
|Kibana |	No |	Match Elasticsearch	|Provide visualization and monitoring capabilities of the system.  Also provides a rest-based query page. <br>https://www.elastic.co/products/kibana |
|NGINX	| No |	1.16.0 |	Reverse proxy for access various web interfaces, provides SSL support, provides authentication with oauth2 and JWT support.
|PostgreSQL |	Yes	| 9.4+, 10+, 11+ |	Provides the management database for the application |
| Voyant |	No|	2.2+ |	Provides text analysis tools <br> https://github.com/sgsinclair/VoyantServer|

### Internal Elements
Internal components are the actual OSKE system components.  These components must be installed.

|Name |	Description
| --- | ---
|Daemon |	Java-based service that continually runs to scrap open-source content from the Internet.
|Microservices | Variety of micro-services running as docker containers that provide additional ways to augment information for the system.
|Web Application |	End-user interface to explore new information, establish retrieval jobs, and search for information.

## Repository Layout
|Folder |	Description
| --- | ---
|Collector |	Java code for OSKE - contains most of the business logic and persistance code.  Also contains the code for the daemon that collects information from the Internet
|CollectorWebApp | Java code for the OSKE web application layer.  Relies heavy upon code in the Collector directory.
|LAS-Common-NLP | Java code for natural language processing.  This code is primarily a layer around [Stanford's CoreNLP project.](https://stanfordnlp.github.io/CoreNLP/)
|LAS-Common | Java code that is not necessarily specific to OSKE, but used within this project.
|MicroServices | Source code and Docker files to establish a variety of microservics to provide additional functionality for the system.
|OSKE-Lite | Instructions to establish a minimal installation of OSKE along with the necessary configuration files.
|development | Instructions and scripts to establish a development environment
|documentation| OSKE's documentation. Contains a quick start guide, user's guide, installation manual, and administrator's guide.
|externalProjects|Copies of external GitHub repositories placed here primarily for convenience.
