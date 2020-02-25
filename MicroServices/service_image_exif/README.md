# EXIF Microservice
This API provides a simple wrapper around Drew Noakes' metadata-extractor for EXIF information
* https://github.com/drewnoakes/metadata-extractor
* https://drewnoakes.com/code/exif/

To build the image, follow the buildnotes in the dockerfile.

Sample Execution:
```
curl http://serverNameOrIP:9001/exif/v1/extract -F "file=@/home/user/MicroServices/service_image_exiftool/sample.jpg"
```
