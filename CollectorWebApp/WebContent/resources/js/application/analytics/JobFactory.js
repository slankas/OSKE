/**
 * 
 * Create date - 20170424
 * Description: Used to create job and rss-based jobs in "result"-type listing content areas.
 * 
 * Dependencies:
 *   jQuery
 *   LASLogger
 * 
 * Usage:
 *    include script
 *    
 *    On the page initialization code, call (these are examples only - change the page name and default data appropriately)
 *        RecordLevelAnalytics.setDefaultInstrumentationPage("application.pageName.");
 *        setDefaultInstrumentationData( { sessionID : sessionId,  executionNumber: enumber} );
 */

var JobFactory = (function () {
	"use strict";
 
	// For instrumentation events, this will the be basic information sent to the service
	var defaultInstrumentationData = {};
	
	// Used to identify which page the event occurred when sending an instrumentation event
	var instrumentationPage = "application.unknown.";
	
	var defaultCronTime = "0 0 9 ? * 2" //  changed to weekly on mondays at 9:00am "0 0 9 * * ?""0 0 9 * * ?"
	
	function setDefaultInstrumentationData(newData) {
		defaultInstrumentationData = newData;
	}

	function setDefaultInstrumentationPage(page) {
		if (page.endsWith(".")==false) {
			page = page + ".";
		}
		
		instrumentationPage = page;
	}

	function sendInstrumentationEvent(eventName, eventDataJSON) {
		var eventData = JSON.parse(JSON.stringify(defaultInstrumentationData));
		
		if (typeof eventDataJSON != "undefined") {
			for (var key in eventDataJSON) {
			    if (eventDataJSON.hasOwnProperty(key)) {
			        eventData[key] = eventDataJSON[key];
			    }
			}			
		}	
	    LASLogger.instrEvent(instrumentationPage+eventName, eventData);
	}
	
	function sendResultLogEvent(dataURL, dataSourceUUID,name) {
		var dataJSON = { 
				documentUUID:  dataSourceUUID,
	            documentURL:   dataURL};
		sendInstrumentationEvent("result."+name,dataJSON);	
		
	}
		
	function startNewJobForRecord(uuid, fullURL, jsonObject, sourceURL, additionalData) {
		if (sourceURL.startsWith("file")) {
			bootbox.alert("Jobs may not be created from uploaded files.")
			return;
		}
		bootbox.prompt({
		    title: "Enter the justification for this search job:",
		    inputType: 'textarea',
		    callback: function (result) {
		    	if (result === null) {
		    		return; // user clicked cancel
		    	}
		    	submitNewJobForRecord(additionalData.title,sourceURL,result);
		    }
		});		
	}    

	function submitNewJobForRecord(title,url,justificationText) {
		var configurationData = {
				"webCrawler": {
					"maxPagesToFetch": 5000,
					"politenessDelay": 200,
					"includeBinaryContentInCrawling": true,
					"maxDepthOfCrawling": 2,
					"respectRobotsTxt": true
				},
				"limitToHost": true,
				"ignoreRelevancyForImages": true,
			};
		var jobData = {
				"name": title,
				"status": "draft",
				"statusTimestamp": "",
				"primaryFieldValue": url,
				"schedule": defaultCronTime,
				"priority": "100",
				"configuration": JSON.stringify(configurationData),
				"justification": justificationText,
				"sourceHandler": "web",
				"adjudicationAnswers": []
			};
		
		sendInstrumentationEvent("createjob.",jobData);
		submitNewJobRecordToServer(jobData);	

	}
	    
	function startNewOutlinksJobForRecord(uuid, fullURL, jsonObject, sourceURL, additionalData) {
		if (sourceURL.startsWith("file")) {
			bootbox.alert("Jobs may not be created from uploaded files.")
			return;
		}
		if (jsonObject != null) {
			startNewOutlinksJobForRecord(jsonObject,additionalData.title);
		}
		else {
			$.ajax({
				url : fullURL,
				contentType: "application/json; charset=utf-8",
				dataType : "JSON",
				success: function(retrievedObject) {
					validateNewOutlinksForJobRecord(retrievedObject,additionalData.title);
				}
			});
		}		
	}
	
	function validateNewOutlinksForJobRecord(jsonObject,title) {
		//Test that it has outlinks and outlinks length> 0
		if (jsonObject.hasOwnProperty('html_outlinks') == false || jsonObject.html_outlinks.length == 0) {
			bootbox.alert("The corresponding item has no outlinks available.");
			return;
		}
				
		bootbox.prompt({
		    title: "Enter the justification for this outlinks job:",
		    inputType: 'textarea',
		    callback: function (justification) {
		    	if (justification === null) {
		    		return; // user clicked cancel
		    	}
		    	submitNewOutlinksJobForRecord(title,jsonObject.html_outlinks,justification);
		    }
		});		
	}    
	
	function submitNewOutlinksJobForRecord(title,outlinks,justificationText) {
		var configurationData = {
				"webCrawler": {
					"maxPagesToFetch": 5000,
					"politenessDelay": 200,
					"includeBinaryContentInCrawling": true,
					"maxDepthOfCrawling": 2,
					"respectRobotsTxt": true
				},
				"limitToHost": true,
				"ignoreRelevancyForImages": true,
				 "weblist": { "seedURLs": outlinks  }
			};
		var jobData = {
				"name": title,
				"status": "draft",
				"statusTimestamp": "",
				"primaryFieldValue": "",
				"schedule": defaultCronTime,
				"priority": "100",
				"configuration": JSON.stringify(configurationData),
				"justification": justificationText,
				"sourceHandler": "weblist",
				"adjudicationAnswers": []
			};
		
		sendInstrumentationEvent("createOutlinksJob.",jobData);
		submitNewJobRecordToServer(jobData);	
	}
	
	
	function submitNewJobRecordToServer(jobData) {
	    $.ajax({
			type : "POST",
			url : openke.global.Common.getRestURLPrefix()+"/jobs",
			data: JSON.stringify(jobData),
		    contentType: "application/json; charset=utf-8",
			success : function(data) {
				data = JSON.parse(data);
				var id = data.id;
				var url = "addEditJob?jobId="+data.id;
				
				bootbox.confirm({
				    title: "Job Created",
				    message: "The job has been created and placed into a draft status.  Do you want to view the job now? ",
				    buttons: {
				        cancel: {
				            label: '<i class="fa fa-times"></i> No'
				        },
				        confirm: {
				            label: '<i class="fa fa-check"></i> Yes'
				        }
				    },
				    callback: function (result) {
				        if (result) {
				        	window.location = url;
				        }
				    }
				});
			},
			error : function(data) {
				bootbox.alert("Unable to create job: "+JSON.stringify(data));
			},	
	    });    	
	}

	
	
	
	function startNewRSSJobForRecord(uuid, fullURL, jsonObject, sourceURL, additionalData) {
		if (sourceURL.startsWith("file")) {
			bootbox.alert("Jobs may not be created from uploaded files.")
			return;
		}
		var dialog = bootbox.dialog({
		    message: 'Checking '+sourceURL+' for RSS feeds',
		});

		var dataJSON = { documentUUID:  uuid,
	                     url: sourceURL,
	                     title: additionalData.title};
		
		sendInstrumentationEvent("result.checkForRSSFeeds",dataJSON);	
		
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/analytics/sandbox/_doc/"+uuid+"/rss",
			type: "POST",
			data: JSON.stringify(dataJSON),
			contentType: "application/json; charset=utf-8",
			dataType : "JSON",
			success: function(data) {
				dialog.modal('hide');
				if (data.feeds.length == 0) {
					bootbox.alert("No RSS feeds found for "+sourceURL);
				}
				else {
					createRSSTable(data, uuid, additionalData.title, sourceURL);
				}
			},
			error: function (data) {
				bootbox.alert(data.responseJSON.reason);
			}
		});	
	}

	function createRSSTable(data, uuid, title, url) {
		var htmlTable = "<table id='rssTable' style='width:100%'><tr><th  style='width:40%'>Title</th><th style='width:40%'>URL</th><th style='width:20%'></th></tr>";
		
		var feeds = data.feeds;   
		for (var index in feeds) {
			var id = "rss_"+index;
			// not working - illegal return statement var dismissAction = "<a href='javascript:return false;'>Dismiss</a>"
			
			//TODO need to escape title
			
			var createAction = "<a href=\"#\" onclick='JobFactory.createRSSJob(\""+encodeSingleQuote(feeds[index].title)+"\",\""+feeds[index].url+"\",\""+id+"\");return false;' title='Create RSS Job'>Create</a>"
			var dismissAction = "<a href=\"#\" onclick='$(\"#"+id+"\").remove();JobFactory.checkForNoRssRows();return false;' title='Dismiss as possible RSS Job'>Dismiss</a>"
			
			htmlTable += "<tr id='"+id+"'><td>"+feeds[index].title+"</td><td><a target=_blank href='"+feeds[index].url+"'>"+feeds[index].url+"</a></td><td>"+createAction+"&nbsp;&nbsp;"+dismissAction+"</td></tr>"
		}
		
		htmlTable += "</table>"
		var dialog = bootbox.dialog({
			size: "large",
			title: "Available Feeds",
			message: htmlTable
		});		
	}

	function checkForNoRssRows() {
		if ( $("#rssTable tr").length === 1 ) {  // we have a header row
			bootbox.hideAll();
		}
	}

	function createRSSJob(title,url,rssTableRowID) {	
		bootbox.prompt({
		    title: "Enter the justification for this RSS job:",
		    inputType: 'textarea',
		    callback: function (result) {
		    	if (result === null) {
		    		return; // user clicked cancel
		    	}
		    	submitNewRSSJob(title,url,result,rssTableRowID);
		    }
		});		
	}

	function submitNewRSSJob(title,url,justificationText,rssTableRowID) {
		var configurationData = {
					"webCrawler": {
						"includeBinaryContentInCrawling": true,
						"maxDepthOfCrawling": 0,
						"respectRobotsTxt": true
					},
					"ignoreRelevancyForImages": true,
				};
		var jobData = {
					"name": title,
					"status": "draft",
					"statusTimestamp": "",
					"primaryFieldValue": url,
					"schedule": defaultCronTime,
					"priority": "100",
					"configuration": JSON.stringify(configurationData),
					"justification": justificationText,
					"sourceHandler": "feed",
					"adjudicationAnswers": []
				};
			
		sendInstrumentationEvent("createRSSjob.",jobData);
		submitNewJobRecordToServer(jobData);
			
		$("#"+rssTableRowID).remove();
		checkForNoRssRows();

	}
	
	function getRandomizedStartTime() {
		return "0 0 "+ (Math.floor(Math.random() * 8) + 8) + " ? * "+  (Math.floor(Math.random() * 5) + 2)		
	}
	
	
	return {
		setDefaultInstrumentationData : setDefaultInstrumentationData, 
		setDefaultInstrumentationPage : setDefaultInstrumentationPage,
		
		startNewJobForRecord       : startNewJobForRecord,
		startNewOutlinksJobForRecord: startNewOutlinksJobForRecord,
		startNewRSSJobForRecord    : startNewRSSJobForRecord,
		submitNewJobRecordToServer : submitNewJobRecordToServer,
		checkForNoRssRows : checkForNoRssRows,
		createRSSJob : createRSSJob,                              // need reference for bootbox dialog
		defaultCronTime : defaultCronTime,
		getRandomizedStartTime : getRandomizedStartTime
	};
}());