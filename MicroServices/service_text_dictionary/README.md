# Dictionary Microservice
This API provides a mechanism to search one or more dictionary sources
to provide definitions for a particular word

To build the image, follow the buildnotes in the dockerfile.
To run the image, it is necessary to establish and pass the json parameters provided in the dockerbuild file.  
As necessary, you should update the user agent and keys.

## Data sources
This API uses the following sources

Name | Identifier | Description
-----|------------|------------
WordNet | wordnet |  
WordsAPI | wordsapi | https://www.wordsapi.com/
WordNik | wordnik | Provides definitions from several different sources (American Heritage Dictionary of the English Language, the Century Dictionary, Wiktionary, the GNU International Dictionary of English, and Wordnet). https://www.wordnik.com/
Urban Dictionary | urban | https://www.urbandictionary.com
Local | local | Custom CSV file to contain organization specific terms and definitions



Sample Executions:
```
http://serverNameOrIP:9020/dictionary/application.wadl
http://serverNameOrIP:9020/dictionary/v1lookup?term=searchTerm[&dictionary=identifier]
http://serverNameOrIP:9020/dictionary/v1/statistics
```
Identifier can be any of the identifiers in the table above, or "all". If not specified, wordnet is used.
