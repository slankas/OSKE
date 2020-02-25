# Open Source Knowledge Enrichment (OSKE) Development

## Install the Development Environment
For the development environment, you will need to install Elasticsearch and PostgresSQL at a minimum.  The OSKE daemon and web application will generally run within the context of your development environment.  With the exception of the TextRank service (which produces the back of the book index and must be capable of running queries against the Elasticsearch instance), these microservices are independent.  Multiple instance may be shared among different environments and developers.  The microservices can also be executed locally for development and testing.

As an alternative, you can run an instance of OSKE-Lite for your back-end services for the system.


### Pre-requisities:
Running Docker will be the easiest way to make the different applications available.  (The primary issue will be running Docker on Windows where it may interfere with running VirtualBox or other VM software.  You may want to run docker within a linux image on VirtualBox.).  For authentication to the web application, the install process will put the user into "singleuser" mode.

* 8gb RAM minimum, 16gb preferable.  Will need additional RAM based upon the various services utilized.
* Java 1.8 installed.  We have not tested against later versions, but no compatibility problems are expected.  Versions less than 1.8 are not supported.
* Docker and docker-compose
   * [Docker for Mac](https://docs.docker.com/docker-for-mac/)
   * [CentOS instructions](https://docs.docker.com/engine/installation/linux/centos/#install-using-the-repository)
   * [Windows](https://docs.docker.com/docker-for-windows/)
* Use of either Eclipse or IntelliJ
* Determine the directory on your workstation/laptop where the code should reside.  *CodeDirectory*
* [pgadmin](https://www.pgadmin.org/) or some other utility to execute SQL queries against a PostgreSQL database.
*  [ModHeader](https://chrome.google.com/webstore/detail/modheader/idgpnmonknjnojddfkpgkljpfnnfcklj) add-in extension for Google Chrome.  This extension allow you to modify HTTP Headers and can be used to insert specific users for testing. (You will need to use "header" as the authentication mode for this situation.)
* Tomcat 8.5+ to execute the development environment locally

### Install steps:
1. Change to the directory where you'd like to install the code.
   ```bash
   cd "CodeDirectory"
   mkdir database
   mkdir elasticsearch
   mkdir data
   ```
1. Clone the OSKE Repository: (same directory as *CodeDirectory*)
   ```bash
   git clone git@github.com:NationalSecurityAgency/OSKE.git
   ```
1. Change the value for POSTGRES_PASSWORD in OSKE/development/docker-compose.yml
  from MUST_CHANGE_PASSWORD to some other value.

1. In OSKE/Collector/system_properties.json.development change
   the parameter for "password" (under "database") from REQUIRED_PASSWORD_CHANGE
   to the same password utilized in the prior step.

1. Execute the following commands:
   ```bash
   cd OSKE/development
   ./startDev.sh
   ```
   This will deploy Docker containers that run PostgreSQL and Elasticsearch. (These images are pulled from the official Docker repository.) The data directories are assumed to be up two levels in the "database" and "elasticserch" directories created in the first step. (You can adjust this location in the docker-compose-dev.yml file.) The first time you execute start.sh, the database will be initialized.  Subsequent calls will skip over the initialization.  You can re-initialize the system by clearing out those two directories.

1. Import the following projects into the IDE (use "Existing Maven Projects" for Eclipse)
    * Collector
    * CollectorWebApp
    * externalProjects/boilerpipe
    * externalProjects/crawler4j (not the parent project)
    * externalProjects/minie
    * LAS-Common
    * MicroServices/service_image_exit
    * MicroServices/service_internet_siteinfo
    * MicroServices/service_whois
    * MicroServices/service_text_contentExtraction
    * MicroServices/service_geo_coding
    * MicroServices/service_geo_tagging
    * MicroServices/service_lda
    * MicroServices/service_nlp
    * MicroServices/service_temporal

  Within Eclipse, you will need to turn-off validation on Collector/system_properties.json The file contains comments, which are removed by a minifier.  Select project properties -> validation.  Under the settings ("...") for the JSON Validator, add an exclude group and then a rule under that to ignore this file.

  Additional, you should execute maven-install on the following projects: boilerpipe, Collector, crawler4j, LAS-Common

1. Configure the IDE to run the CollectorWebApplication
   * Eclipse
     * Setup the Tomcat server and deploy.
     * Under the Apache Tomcat run configruationn, change the working directory to "Collector".  (If this is not done, you will receive a NoSuchFileException.)
   * IntelliJ
     * Add `tomcat7:deploy` goal to the Maven build string.  This will
       automatically deploy the finished WAR to Tomcat via the HTTP API.
     * Under project structure, change the CollectorWebApp to also contain the classes of boilerpipe,collector, crawler4j, and LAS-Common.

### MicroServices
Look at the docker files within the OSKE-Lite to setup and run these services as you need.  The system gracefully degrades when services are unavailable.

If you run the "textrank" service, you will need to alter the configuration for the elastic.restEndPoint to use you computer's IP address rather than the localhost IP (127.0.0.1).  The "textrank" service uses this address to query Elasticsearch to retrieve documents when producing the "back of the book" index.

### Database access
Once the Docker containers have been started, you can directly access database with these parameters:

| Parameter | Value |
|-|-|
| database | OpenKE_db
| user | OpenKE_user
| password | *value defined in step #3*
| JDBC URL | jdbc:postgresql://127.0.01:5432/OSKE_db

You can find these parameters in Collector/system_properties.json

### OSKE-Lite Development environment
First follow the instructions in the OSKE-Lite folder.  Once the environment is running, you will need to update the ports for the various services to match their original external mappings. (If you run ps-a, you can see those mappings.)  You will also need to add "restEndPointInternal" as a configuration property under ElasticSearch.  You will also need to setup SSH tunnels to the back-end services.

You may also want to remove the collector daemon and webapp from the docker-compose file.  The configuration change below assumes that you will be performing development on a machine other than the docker-compose environment and using those services eternally. (Note: You must run the daemon at least once to initialize the system.)

The following shows an except of the configuration changes:
```
{
    "geoTagAPI": "http://localhost:9004/geoTagger/",
    "textrankAPI": "http://localhost:8000/textrank/",
    "whoisAPI": "http://localhost:9008/whois/",
    "siteinfoAPI": "http://siteinfo-service:9001/siteinfo/",
    "nlpAPI": "http://localhost:9006/nlp/",
    "microformatAPI": "http://localhost:9009/microformat/",
    "dbpediaSpotlightAPI": "http://localhost:2222/rest/annotate",
    "spacyAPI": "http://localhost:9012/spacy/",
    "elastic": {
        "restEndPoint": "http://localhost:9200/",
        "restEndPointInternal" : "http://elasticsearch:9200/"
    },
    "wordnetAPI": {
        "specializedUrl": "http://localhost:5679/hyponym/1/",
        "generalizedUrl": "http://localhost:5679/hypernym/1/"
    },
    "topicModelAPI": "http://localhost:9005/topicmodel/",
}
```


The following snippet can be adapted for use within an SSH config file.  With this present, you can execute "ssh OSKE-dev" to establish the necessary SSH tunnels.
```
Host OSKE-dev
 ProxyCommand ssh bastionServerIP -W OSKE-devServer:22
 Localforward 5432 OSKE-devServer:5432
 Localforward 5601 OSKE-devServer:5601
 Localforward 9200 OSKE-devServer:9200
 Localforward 2222 OSKE-devServer:2222
 Localforward 8000 OSKE-devServer:8000
 Localforward 9004 OSKE-devServer:9004
 Localforward 9005 OSKE-devServer:9005
 Localforward 9006 OSKE-devServer:9006
 Localforward 9007 OSKE-devServer:9007
 Localforward 9008 OSKE-devServer:9008
 Localforward 9009 OSKE-devServer:9009
 Localforward 9012 OSKE-devServer:9012
 Localforward 5679 OSKE-devServer:5679
 Localforward 5679 OSKE-devServer:8800
 ForwardAgent yes
 User userID
```
