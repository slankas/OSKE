# GeoTagging Microservice
This API provides a simple wrapper the CLAVIN GeoTagger using the Stanford NER parser
* https://clavin.bericotechnologies.com/
* https://github.com/Berico-Technologies/CLAVIN
* https://github.com/Berico-Technologies/CLAVIN-NERD

To build the image, follow the buildnotes in the dockerfile.

Sample Execution:
```
http://serverNameOrIP:9004/geoTagger/application.wadl
http://serverNameOrIP:9004/geoTagger/v1/statistics
POST http://serverNameOrIP:9004/geoTagger/v1/process
{
  "text": "replace this string with text to geo-tag"
}
```
