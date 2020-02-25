# NLP Microservice
This API provides a simple wrapper the Stanford CoreNLP Text processor to extract parse data and triple relations
* https://stanfordnlp.github.io/CoreNLP/
* Stanford Demo: http://corenlp.run/

To build the image, follow the buildnotes in the dockerfile.

Sample Execution:
```
http://serverNameOrIP:9006/nlp/application.wadl
http://serverNameOrIP:9006/nlp/v1/statistics
POST http://serverNameOrIP:9006/nlp/v1/process[?filter=normal,min,max]
{
	"text": "replace this string with text to parse"
}
```
