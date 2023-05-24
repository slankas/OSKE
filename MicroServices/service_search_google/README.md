# Google Search API
Provides a REST-based interface to the googlesearch-python API: https://github.com/Nv7-GitHub/googlesearch

## Creating local environment
```sh
python3 -m venv service_venv
source serverice_venv/bin/activate
pip install --upgrade pip setuptools wheel
pip install -r requirements.txt

flask run --port 8000 --reload --debugger
```



## Client Usage
The service only accepts a get request and returns an array of JSON Objects.
Each object contains title, url, and description.  Use a '+' symbol as speaces in the search terms.

````
GET http://serverName:port/google/search/search+terms/numResults
````

Sample response
````
[
  { "url": "...",
    "title" : "...",
    "description" : "..."
  }
]

````

## Build and run
See comments in Dockerfile
