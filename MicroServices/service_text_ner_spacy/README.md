# spaCy Named Entity Recognition Service
Provides a REST-based interface to find named enities in a text document.

The spaCy named entity feature is described at https://spacy.io/usage/linguistic-features#section-named-entities

The available entity types are https://spacy.io/usage/linguistic-features#entity-types

## Client Usage
The service only accepts a post request. The "text" attribute is required. "showAll" is optional.  Default is false.  If set to true, then the following entities are also returned: DATE, TIME, PERCENT, QUANTITY, ORDINAL, CARDINAL

````
POST http://serverName:port/spacy/ner
Content-type: application/json
{
  "showAll": false,
  "text": "sample text to extract named entities"
}
````

Sample response
````
{
"entities": [  {
    "endPos": 10,
    "startPos": 1,
    "text": "text matched",
    "type": "ENTITY TYPE"
    }, ...
],
}
````

## Build and run
See comments in docker/Dockerfile_app
