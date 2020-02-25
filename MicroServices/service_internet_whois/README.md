# whois microservice
This API provides a simple wrapper the whois service.
The service utilizes the whois client in the Apache Commons network package.
The primary class contains a lists of the primary registrars by top-level domain to contact.
The class will utilize any defined registrar URL to get back to primary registrar.

To build the image, follow the buildnotes in the dockerfile.

Sample Execution:
```
http://serverNameOrIP:9008/whois/application.wadl
http://serverNameOrIP:9008/whois/v1/statistics
http://serverNameOrIP:9008/whois/v1/find/{domain name}
```
