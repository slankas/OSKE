# MicroServices
Series of small micro-services, generally running in Docker, that can be used across projects and examplars.

## Available services
This process will install the following services at the specified ports:

Service Name | Port | Description
--- | --- | ---
DBPedia-Spotlight | 2222 | Discovers links to dbpedia entries within unstructured text.
Exif Service | 9001 | Extracts camera and image metadata from images
GeoTagging Service | 9904 | Extracts locations from unstructured text
GeoCoding Service | 9002 | For a given address, returns the latitude and longitude coordinates
Google Search | 9015 | Implements the a slightly customized version googleapi from https://github.com/abenassi/Google-Search-API
Microformat Service | 9009 | Extracts microformat data from HTML pages. http://microformats.org/
NLP Service | 9006 | Extracts named entities and relations from unstructured text using Stanford CoreNLP and minIE
Scrapper Service | 9011 | Provides a simple wrapper around puppet to dynamically crawl an HTML page
Spacy Service | 9012 | Extracts named entities from unstructured text using the Spacy library service
Structured Extraction Service | 9010 | Extracts data from HTML pages based upon the page structure by using [CSS Selectors](https://jsoup.org/cookbook/extracting-data/selector-syntax).
Temporal Service | 9003 | Extracts time references from unstructured text.
Textrank Service | 8000 | Generates keywords, keyphrases, summaries, and discovery indexes
Topic Service | 9005 | Executes LDA (https://en.wikipedia.org/wiki/Latent_Dirichlet_allocation) to extract topics from a series of documents
WhoIS Service | 9008 | Retrieves WhoIS data for a domain
WordNet | 5679 | REST based WordNet Service - https://github.com/jacopofar/wordnet-as-a-service

## Docker Run-time Environment
To establish a Docker run-time enviroment:

```
yum install -y yum-utils   device-mapper-persistent-data   lvm2 git
yum-config-manager     --add-repo     https://download.docker.com/linux/centos/docker-ce.repo
yum install -y docker-ce
curl -L "https://github.com/docker/compose/releases/download/1.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
```

Docker cheatsheet: https://www.docker.com/sites/default/files/Docker_CheatSheet_08.09.2016_0.pdf

## Deploying Docker containers from this repo
1. Clone this repository
   ```
   cd <install directory>
   git clone git@github.com:NationalSecurityAgency/OSKE.git
   ```
1. Edit the docker-compose.yml in <install directory/OSKE/MicroServices, editing the following values: (The PostgreSQL values should point to the OSKE Database.)
   * IDENTENIFYING_EMAIL_ADDRESS
   * LOCATION_IQ_KEY (You can register for a free account at https://locationiq.com/register)
   * FULL_DOMAIN_NAME_POSTGRESQL
   * POSTGRESQL_PORT
   * POSTGRESQL_OPENKE_DB
   * POSTGRESQL_OPENKE_USER
   * POSTGRESQL_OPENKE_PASSWORD


3. Use docker-compose to build and start the services.  The initial run of this command will take over 30 minutes to complete the build, but subsequent runs will execute in under 30 seconds.
   ```
   cd <install directory>/OSKE/MicroServices
   docker-compose up -d
   ```

## Docker Swarm
You can utilize the docker-compose.yml file with Docker Swarm.  
However, you will want to first build all of the images, then remove the
"base" configuration from the file or remove that service once the swarm
starts.  Otherwise, the "base" service will constantly attempt to restart
itself. (The base image only exists as a building block for the other services.)

 View "[Deploy a stack to a swarm](https://docs.docker.com/engine/swarm/stack-deploy/)" for more information.
