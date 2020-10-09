# Google Search API
Provides a REST-based interface to the googleapi at https://github.com/abenassi/Google-Search-API

This version uses https://github.com/slankas/Google-Search-API as the source,
which implements a small change to provide a more refined version of the title.

## Client Usage
The service only accepts a post request. The "text" attribute is required. "showAll" is optional.  Default is false.  If set to true, then the following entities are also returned: DATE, TIME, PERCENT, QUANTITY, ORDINAL, CARDINAL

````
GET http://serverName:port/google/search/search+terms
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
See comments in Dockerfile
