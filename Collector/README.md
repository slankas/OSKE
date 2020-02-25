# Collector
This directory contains the primary application code for the application.

## build
To build this code, you must first ensure all of the necessary dependencies
have been built and installed locally.  Then the application can be built -

### Build crawler4j:
```
cd install_directory/OpenSourceKnowledgeEnrichment/externalProjects/crawler4j
mvn install
```

### Build Boilerpipe:
```
cd install_directory/OpenSourceKnowledgeEnrichment/externalProjects/boilerpipe/
mvn install
```
### Build minIE
```
cd install_directory/OpenSourceKnowledgeEnrichment/externalProjects/minIE
mvn install
```
### Build LAS-Common:
```
cd install_directory/OpenSourceKnowledgeEnrichment/LAS-Common
mvn install
```
### Build LAS-Common-NLP
```
cd install_directory/OpenSourceKnowledgeEnrichment/LAS-Common-NLP
mvn install
```
### Build the Collector
```
cd install_directory/OpenSourceKnowledgeEnrichment/Collector
mvn dependency:copy-dependencies package install
```
