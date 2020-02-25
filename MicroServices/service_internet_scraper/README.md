# PuppetScraper
Relatively simple node.js application that wraps [Puppeteer](https://pptr.dev/) to perform scrapping of dynamic websites.

Presently, only one rest endpoint exists:
```
POST serverAddress:3000/crawl
{
  "url": "urlToCrawl",
  "waitFor": "CSS selector or time in MS to wait for"
}
```
