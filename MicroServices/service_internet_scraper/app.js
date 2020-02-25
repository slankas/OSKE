const puppeteer = require('puppeteer');
const express = require('express');
const bodyParser = require('body-parser');
const puppetTools = require('./puppetTools');
const LASLogger  = require('./LASLogger');

var page;

(async () => {
  const browser = await puppeteer.launch();
  page = await browser.newPage();

  const userAgent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36';

  puppetTools.configurePageObject(page,userAgent);

})();

var app = express();
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));


app.post("/crawl", (req, res, next) => {
  if (typeof req.body.url === "undefined") {
    res.status(400).send('Invalid call: missing "url" in JSON object')
    return
  }
  if (typeof req.body.waitFor === "undefined") {
    res.status(400).send('Invalid call: missing "waitFor" in JSON object')
    return
  }
  LASLogger.log(LASLogger.LEVEL_INFO, "crawl request: " + JSON.stringify(req.body));

  //var response = puppetTools.downloadPage(page,req.body.url, req.body.waitFor);
  (async () => {
    try {
      await page.goto(req.body.url);
      await page.waitFor(req.body.waitFor)
      var response =  await page.content();

      res.send(response);
    }
    catch (err) {
        LASLogger.log(LASLogger.LEVEL_ERROR, "Failed crawl request: " + JSON.stringify(req.body));
        LASLogger.log(LASLogger.LEVEL_ERROR, err);
        res.status(500).send("Unable to retrieve url");
    }
  })();
});

app.use(function (err, req, res, next) {
  console.error(err.stack)
  res.status(500).send('Invalid call')
})

LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

app.listen(3000, () => { console.log("PuppeteerScraper running on port 3000"); });
