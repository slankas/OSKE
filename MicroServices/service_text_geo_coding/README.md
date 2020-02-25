# GeoCoding Microservice
This API provides a gateway to external processes that provide an OpenStreetMap Nominatum API.
* http://wiki.openstreetmap.org/wiki/Nominatim

To build the image, follow the buildnotes in the dockerfile.
To run the image, it is necessary to establish and pass the json parameters provided in the dockerbuild file.  
If necessary, you should update the user agent and keys.

Sample Executions:
```
http://serverNameOrIP:9002/geo/application.wadl
http://serverNameOrIP:9002/geo/v1/geoCode?location=Paris,Texas
http://serverNameOrIP:9002/geo/v1/statistics
```
