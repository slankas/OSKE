# Temporal Microservice
This API provides a simple wrapper the HeidelTime Temporal Tagger
* https://github.com/HeidelTime/heideltime
* http://dbs.ifi.uni-heidelberg.de/index.php?id=129

To build the image, follow the buildnotes in the dockerfile.

The HeidelTime component produces out of memory errors on some documents (most likely due to size / parsing issues).  When these errors occur, the service is set to automatically restart by using a jvm flag to exit immediately with a docker restart policy of always.

Sample Execution:
```
http://serverNameOrIP:9003/temporalTagger/application.wadl
http://serverNameOrIP:9003/temporalTagger/v1/statistics
POST http://serverNameOrIP:9003/temporalTagger/v1/extract
{
  "published_date" : { "date" :"2017-04-04T15:22:30Z" },
	"text": "replace this string with text to tag temporal (time) references"
}
```
