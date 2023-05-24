# HTML Microformat Extractor/parser
Provides a REST-based interface to extract microformat data from web pages: http://microformats.org/

## Creating local environment
```sh
python3 -m venv service_venv
source serverice_venv/bin/activate
pip install --upgrade pip setuptools wheel
pip install -r requirements.txt

flask run --port 8000 --reload --debugger
```



## Client Usage
The service a put request containing the html text of a page to extract data from.

````
curl -v -X POST localhost:8000/microformat/v1/extract -H "Content-Type: text/html" --data-ascii @sample.html 
````

Sample response
````
{
    "items": [
        {
            "type": [
                "h-geo"
            ],
            "properties": {
                "latitude": [
                    "42.3633690"
                ],
                "longitude": [
                    "-71.091796"
                ]
            }
        },
        {
            "type": [
                "h-event"
            ],
            "properties": {
                "url": [
                    "http://www2008.org/"
                ],
                "start": [
                    "20080421"
                ],
                "end": [
                    "20080426"
                ],
                "name": [
                    "WWW 2008 (17th International World Wide Web Conference)"
                ],
                "location": [
                    "Beijing International Convention Center,"
                ],
                "description": [
                    "\"The World Wide Web Conference is a global event bringing together key researchers, innovators, decision-makers, technologists, businesses, and standards bodies working to shape the Web. Since its inception in 1994, the WWW conference has become the annual venue for international discussions and debate on the future evolution of the Web.\""
                ]
            }
        },
        {
            "type": [
                "h-card"
            ],
            "properties": {
                "name": [
                    "Sir Tim Berners-Lee"
                ]
            }
        },
        {
            "type": [
                "h-card"
            ],
            "properties": {
                "name": [
                    "Tim Berners-Lee"
                ],
                "url": [
                    "http://www.w3.org/People/Berners-Lee/"
                ],
                "org": [
                    "World Wide Web Consortium"
                ],
                "adr": [
                    {
                        "type": [
                            "h-adr"
                        ],
                        "properties": {
                            "name": [
                                "77 Massachusetts Ave. (MIT Room 32-G524) Cambridge, MA, 02139 USA"
                            ]
                        },
                        "value": "77 Massachusetts Ave. (MIT Room 32-G524) Cambridge, MA, 02139 USA"
                    }
                ],
                "tel": [
                    "+1 (617) 253 5702"
                ]
            }
        },
        {
            "type": [
                "h-card"
            ],
            "properties": {
                "name": [
                    "Raymond Yee"
                ],
                "given-name": [
                    "Raymond"
                ],
                "family-name": [
                    "Yee"
                ]
            }
        },
        {
            "type": [
                "h-card"
            ],
            "properties": {
                "org": [
                    "UC Berkeley"
                ],
                "job-title": [
                    "Alumnus"
                ]
            }
        }
    ],
    "rels": {
        "friend": [
            "http://laurashefler.net/blog"
        ],
        "met": [
            "http://laurashefler.net/blog"
        ],
        "colleague": [
            "http://laurashefler.net/blog"
        ],
        "coresident": [
            "http://laurashefler.net/blog"
        ],
        "spouse": [
            "http://laurashefler.net/blog"
        ],
        "muse": [
            "http://laurashefler.net/blog"
        ],
        "sweetheart": [
            "http://laurashefler.net/blog"
        ],
        "contact": [
            "http://www.w3.org/People/Berners-Lee/"
        ],
        "license": [
            "http://creativecommons.org/licenses/by/3.0/"
        ],
        "tag": [
            "http://blog.mashupguide.net/category/google-maps/",
            "http://technorati.com/tag/WWW"
        ]
    },
    "rel-urls": {
        "http://laurashefler.net/blog": {
            "text": "Laura Shefler",
            "rels": [
                "colleague",
                "coresident",
                "friend",
                "met",
                "muse",
                "spouse",
                "sweetheart"
            ]
        },
        "http://www.w3.org/People/Berners-Lee/": {
            "text": "Tim Berners-Lee",
            "rels": [
                "contact"
            ]
        },
        "http://creativecommons.org/licenses/by/3.0/": {
            "text": "cc by 3.0",
            "rels": [
                "license"
            ]
        },
        "http://blog.mashupguide.net/category/google-maps/": {
            "text": "Google Maps",
            "rels": [
                "tag"
            ],
            "title": "View all posts in Google Maps"
        },
        "http://technorati.com/tag/WWW": {
            "text": "",
            "rels": [
                "tag"
            ]
        }
    },
    "debug": {
        "description": "mf2py - microformats2 parser for python",
        "source": "https://github.com/microformats/mf2py",
        "version": "1.1.2",
        "markup parser": "html5lib"
    }
}
````

## Build and run
See comments in Dockerfile
