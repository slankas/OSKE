# structuralExtraction Microservice
This API provides a service to extract content from HTML utilizing the contentExtractor capabilities within OpenKE.

To build the image, follow the buildnotes in the dockerfile.

The domain should map to a domain code within OpenKE or have been configured with "createCache".

Sample Execution:
```
http://serverNameOrIP:9010/structuralExtraction/application.wadl
http://serverNameOrIP:9010/structuralExtraction/v1/statistics
http://serverNameOrIP:9010/structuralExtraction/v1/invalidateCache/{domain}
http://serverNameOrIP:9010/structuralExtraction/v1/process/{domain}/{url }
POST http://serverNameOrIP:9010/structuralExtraction/v1/process
{
	 "html"   : HTML content to extract
	 "url"    : URL to utilize as the base for the html document
	 "domain" : domain that should be used to get the content extraction records.  must be a valid openKE domain (or a domain created with "ceateCache").
}
```
The following allows you to create temporary configuration for a domain,
which will be valid for up to 60 minutes.  If the configuration is no longer present, then an "status" of "error"
will be returned with the following "message": "No content extraction records defined for the domain"

POST http://serverNameOrIP:9010/structuralExtraction/v1/createCache/{domain}
[
CER Records - can export from OpenKE
]

## Example
This examples configures the microservice to extract the ip addresses and ports from a table on this page: https://www.us-proxy.org/
Once this configuration is in place, the user can then make a GET call to http://serverNameOrIP:9010/structuralExtraction/v1/process/proxy/https://www.us-proxy.org/
to retrieve a table of information.

```
POST http://serverNameOrIP:9010/structuralExtraction/v1/createCache/proxy
[
  {
    "recordSelector": "table#proxylisttable tr",
    "hostname": "www.us-proxy.org",
    "pathRegex": "",
    "domainInstanceName": "verification",
    "userEmailID": "testuser@ncsu.edu",
    "lastDatabaseChange": "2017-09-22T00:48:58Z",
    "recordName": "proxy",
    "recordExtractBy": "text",
    "recordExtractRegex": "",
    "id": "0000015e-a70b-fcc6-c0a8-380100000007",
    "parentRecordName": "",
    "recordParentID": ""
  },
  {
    "recordSelector": "td:eq(0)",
    "recordParentID": "0000015e-a70b-fcc6-c0a8-380100000007",
    "hostname": "www.us-proxy.org",
    "pathRegex": "",
    "domainInstanceName": "verification",
    "userEmailID": "testuser@ncsu.edu",
    "lastDatabaseChange": "2017-09-22T00:49:46Z",
    "recordName": "ipAddress",
    "recordExtractBy": "text",
    "recordExtractRegex": "",
    "id": "0000015e-a70d-2a0f-c0a8-380100000008",
    "parentRecordName": "www.us-proxy.org: proxy"
  },
  {
    "recordSelector": "td:eq(1)",
    "recordParentID": "0000015e-a70b-fcc6-c0a8-380100000007",
    "hostname": "www.us-proxy.org",
    "pathRegex": "",
    "domainInstanceName": "verification",
    "userEmailID": "testuser@ncsu.edu",
    "lastDatabaseChange": "2017-09-22T00:49:57Z",
    "recordName": "port",
    "recordExtractBy": "text",
    "recordExtractRegex": "",
    "id": "0000015e-a710-05b5-c0a8-380100000009",
    "parentRecordName": "www.us-proxy.org: proxy"
  }
]
```
