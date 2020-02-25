

```
http://serverNameOrIP:9005/topicmodel/application.wadl
http://serverNameOrIP:9005/topicmodel/v1/statistics
http://serverNameOrIP:9005/topicmodel/v1/LDA/359f5100-c20c-4d89-8194-bd7425aeab8c

POST http://serverNameOrIP:9005/topicmodel/v1/LDA
{"numKeywords": 10,
"maxIterations": 50,
"documents": [
  {
"text": "some text.",
"uuid": "uuid1",
"url": "http://someURL1.com"
},
    {
"text": "text is here",
"uuid": "uuid2",
"url": "http://someURL2.com"
},
    {
"text": "here is text",
"uuid": "uuid",
"url": "http://someURL3.com"
}
],
"stemWords": false,
"numTopics": 2
}

This creates a sessionUUID in the results.  Use that to get the processing status and results...
http://serverNameOrIP:9005/topicmodel/v1/LDA/{sessionID}
http://serverNameOrIP:9005/topicmodel/v1/LDA/359f5100-c20c-4d89-8194-bd7425aeab8c

```
