/**
 * Create date - 20170424
 * Description: 
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

var RecordLevelAnalytics = (function () {
	"use strict";
 
	// For instrumentation events, this will the be basic information sent to the service
	var defaultInstrumentationData = {};
	
	// Used to identify which page the event occurred when sending an instrumentation event
	var instrumentationPage = "application.unknown.";
	
	var popupWindowForRecords;
	
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

	function loadDocumentRecord(url, callback, createPopUp = true) {
		
		if (createPopUp) {
			popupWindowForRecords = getNewWindowReference();
		}
		
		$.ajax({
			url : url,
			contentType: "application/json; charset=utf-8",
			dataType : "JSON",
			success: function(data) {
				callback(data,url)
			}
		});

		return false;
	}	
	
	var documentToExpand = null;
	var additionalDataForExpandedDocument = null;
	function getDocumentToExpand() {
		return documentToExpand;
	}
	function getAdditionalDataForExpandedDocument() {
		return additionalDataForExpandedDocument;
	}
	function showDocumentInNewWindow(uuid, fullURL, jsonObject,sourceURL,additionalData) {
		sendResultLogEvent(uuid,fullURL,"showDocumentInNewWindow");
		additionalDataForExpandedDocument = additionalData;
		
		if (jsonObject != null) {
			showDocumentInNewWindowCallback(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, showDocumentInNewWindowCallback, false)
		}				
	}

	function showDocumentInNewWindowCallback(jsonObject) {
		documentToExpand = jsonObject;
		var screen_width = screen.width * .80;
	    var screen_height = screen.height* .80;
	    var top_loc = screen.height *.15;
	    var left_loc = screen.width *.1;
	
	    var myWindow = window.open(openke.global.Common.getPageURLPrefix()+'/viewDocument','_blank','toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);
	}

	function showFullJSONRecord(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"showCompleteRecord");
		
		if (jsonObject != null) {
			showFullJSONRecordCallBack(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, showFullJSONRecordCallBack)
		}		
	}
	
	function showFullJSONRecordCallBack(jsonObject) {
		var rawData = JSONTree.create(jsonObject);
		openNewWindow(rawData);	
	}	
	
	function showFullJSONRecordEditor(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"showCompleteRecordEditor");
		
		if (jsonObject != null) {
			displayJSONObjectInNewWindow(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, displayJSONObjectInNewWindow)
		}		
	}
	
	function showTranslatedSideBySide(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"showTranslatedSideBySide");
		
		if (jsonObject != null) {
			showTransJSONRecordCallBack(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, showTransJSONRecordCallBack)
		}		
	}
	
	function showTransJSONRecordCallBack(jsonObject) {
		var nativeTitle = jsonObject.domainDiscovery.nativeTitle;
		var nativeText = jsonObject.nativeText;
		var transTitle = jsonObject.domainDiscovery.title;
		var transText = jsonObject.text;
		openNewSideBySideWindow(nativeTitle, nativeText, transTitle, transText);	
	}
	

	function getDomainForURL(data) {
		  var    a      = document.createElement('a');
		         a.href = data;
		  return a.hostname;
	}
	
		
	
	function showDomainWhoIs(uuid, fullURL, jsonObject, sourceURL) {
		sendResultLogEvent(uuid,fullURL,"showDomainWhoIs");
		
		var domainName = getDomainForURL(sourceURL);
		popupWindowForRecords = getNewWindowReference();
		
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/analytics/whois/"+domainName,
			contentType: "application/json; charset=utf-8",
			dataType : "JSON",
			success: function(data) {
				if (data.status=='error') {
					displayJSONObjectInNewWindow(data);
				}
				else {
					displayJSONObjectInNewWindow(data.whois);
				}

				
			}
		});
	
		return false;		
	}	
	
	
	function showText(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"showCompleteRecord");
		
		if (jsonObject != null) {
			showTextCallBack(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, showTextCallBack)
		}		
	}
	function showTextCallBack(jsonObject) {
		var rawData = JSONTree.create(jsonObject.text);
		openNewWindow(jsonObject.text.replace(/\n/g, '<br>'));		
	}	
	
	/**
	 * Create a UUID
	 * 
	 * Code from https://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript/8809472#8809472
	 * 
	 * @returns random UUID, albeit te initial 13 hex positions, use a portion of 
	 *          the current date and time to help guarantee uniqueness.
	 *     
	 */
	function generateUUID() {
	    var d = new Date().getTime();
	    if (typeof performance !== 'undefined' && typeof performance.now === 'function'){
	        d += performance.now(); //use high-precision timer if available
	    }
	    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
	        var r = (d + Math.random() * 16) % 16 | 0;
	        d = Math.floor(d / 16);
	        return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
	    });
	}

	
	function downloadOriginalContent(uuid, fullURL, jsonObject, sourceURL) {
		sendInstrumentationEvent("result.analytic.record.downloadOriginal",{ documentUUID:  uuid, sourceURL:   sourceURL});	
		
		var a = "<a target='_blank' href='"+openke.global.Common.getRestURLPrefix()+"/visitedPages/" + uuid+ "/storage' >Stored</a>";
		
		$(a)[0].click();
	}	
	
	
	function showConcepts(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"showConcepts");
		
		if (jsonObject != null) {
			showConceptsCallBack(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, showConceptsCallBack, false);
		}
	}
	function showConceptsCallBack(jsonObject) {
		//var rawData = JSONTree.create(jsonObject.concepts);
		//openNewWindow(rawData);	
		
		var columnLabels = ["Name", "Value", "Type", "Text Position"];
 		var columnFields = ["fullName","value","type","position"];
 			
 		var dialog = bootbox.dialog({
 			size: "large",
 			title: "Concepts",
 			message: "<table id='conceptTable' style='width:100%'></table>"
 		});	
 		
 		if (jsonObject['concepts'].length > 0 && jsonObject['concepts'][0].hasOwnProperty("sentencePosition")) {
 			var concepts = jsonObject['concepts'];
 			for (var i =0 ;i < concepts.length; i++) {
 				concepts[i]["position"] = "sentence: " + concepts[i]["sentenceIndex"] + ", position: " + concepts[i]["sentencePosition"]
 			}
 			
 		}
 		
 			
 		LASTable.displayJSONInTable(jsonObject['concepts'], columnLabels, columnFields,'conceptTable');
	}	
	
	function getMapData(uuid, callback) {
		var documentURL = openke.global.Common.getRestURLPrefix()+"/document/sandbox/_doc/"+uuid;
		$.ajax({
			url : documentURL,
			contentType: "application/json; charset=utf-8",
			dataType : "JSON",
			success: function(jsonObject) {
				var uniqueMapRecords = [];
		 		var foundRecords = {};
		 		
		 		for (var i=0; i < jsonObject['geotag'].length; i++) {
		 			var record = jsonObject['geotag'][i];
		 			var key = record.geoData.preferredName +";" + record.textMatched
		 			if (foundRecords.hasOwnProperty(key)) {
		 				record = foundRecords[key];
		 				record.count = record.count + 1;
		 			}
		 			else {
		 				record.count = 1;
		 				uniqueMapRecords.push(record);
		 				foundRecords[key] = record;
		 			}
		 		}
		 		callback(uniqueMapRecords)
			
			}
		});
	}
		
	function showGeoTags(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"showGeoTags");
		
		if (jsonObject != null) {
			showGeoTagCallBack(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, showGeoTagCallBack, false);
		}
	}
	
	var queueStatus  = {}
	var queueOrder   = []
	var queueData    = {} // array of the json full records for records put into tha queue and processed.  used for ready callbacks
	var statusWindow = null; // used to display process status
	
	function addToQueueStatus(uuid, title, url, queuePosition) {
		var statusItem = { uuid: uuid, title: title, url:url, queuePosition: queuePosition}
		if (!queueStatus.hasOwnProperty(uuid)) {
			queueOrder.push(uuid)
		}
		queueStatus[uuid]=statusItem;
	}
	
	// by returning false, we signify that the popup should not be shown.
	function markItemInQueueReady(jsonObject) {
		queueData[jsonObject.uuid] = jsonObject
		if (queueStatus.hasOwnProperty(jsonObject.uuid)) {
			var result = false;
			if (queueStatus[jsonObject.uuid].queuePosition == -1) { result = true; }
			else {	queueStatus[jsonObject.uuid].queuePosition = -1;}
			showDocumentStatusWindow(true);
			return result;
		}
		return true;
	}
	
	function showDocumentStatusWindow(onlyUpdateIfOpen=false) {
		if (onlyUpdateIfOpen == true) {
			if (statusWindow == null || statusWindow.closed) {
				return;
			}
		}
		if (statusWindow == null|| statusWindow.closed) {
			statusWindow = openNewWindow("");
		}
		
		var html = "<h3>Processing Status</h3><table style='width:100%;'><tr><th>Title / URL</th><th>Queue Position</th></tr>";
		for (var i=0;i<queueOrder.length;i++) {
			var rowUUID = queueOrder[i].trim();
			var rowRecord = queueStatus[rowUUID];
			html += "<tr><td><i>"+ rowRecord.title +"</i><br>"+rowRecord.url +"</td><td>"
			if (rowRecord.queuePosition == 0) {
				html += "Processing"
			}
			else if (rowRecord.queuePosition > 0) {
				html += rowRecord.queuePosition
			}
			else {
				html += '<button class="btn btn-default btn-xs" onclick="window.opener.RecordLevelAnalytics.showGeoTagCallBackFromQueue(\''+rowUUID+'\');">Geo Tags</button>'
			}
			html += "</td><tr>";
		}
		html += '</table><p>&nbsp;<p><button class="btn btn-default btn-xs" id="btStatusClose" onclick="self.close()">Close</button>';
		
		statusWindow.document.body.innerHTML = html;
	}
	
	function showGeoTagCallBackFromQueue(uuid) {
		showGeoTagCallBack(queueData[uuid]);
	}
	
	function showGeoTagCallBack(jsonObject, fullURL) {
		
		if (jsonObject.hasOwnProperty("geotag") == false) {
			var statusURL = openke.global.Common.getRestURLPrefix()+"/document/" + jsonObject.uuid+ "/secondaryProcessingStatus"
			$.ajax({
				url : statusURL,
				contentType: "application/json; charset=utf-8",
				dataType : "JSON",
				success: function(data) {
					if (data.queuePosition == -1) {
						delete geoQueueStatus[data.documentID]
					}
					addToQueueStatus(jsonObject.uuid, jsonObject.html_title, jsonObject.url, data.queuePosition)
					showDocumentStatusWindow();
										
					setTimeout(function(){ 
						loadDocumentRecord(fullURL, showGeoTagCallBack, false);
					}, 5000);
				}
			});
				
			return;
		} 
		var showResult = markItemInQueueReady(jsonObject);
		if (showResult == false) { return;}
		
		var columnLabels = ["Name", "Text Matched", "Num Occurances", "Latitude", "Longitude"];
 		var columnFields = ["geoData.preferredName","textMatched","count", "geoData.latitude", "geoData.longitude"];
 			
 		var dialog = bootbox.dialog({
 			size: "large",
 			title: "Geo Tags",
 			message: "<table id='geoTable_"+jsonObject.uuid+"' style='width:100%'></table><div><a id='mapAction' href='#'>Map Display</a></div>"
 		});
		$('#mapAction').on('click', function() {
			var screen_width = screen.width * .80;
		    var screen_height = screen.height* .80;
		    var top_loc = screen.height *.15;
		    var left_loc = screen.width *.1;
		
		    var myWindow = window.open(openke.global.Common.getPageURLPrefix()+'/mapView?uuid='+jsonObject.uuid,'_blank','toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);
		   
		});
		
		var uniqueMapRecords = [];
 		var foundRecords = {};
 		
 		for (var i=0; i < jsonObject['geotag'].length; i++) {
 			var record = jsonObject['geotag'][i];
 			var key = record.geoData.preferredName +";" + record.textMatched
 			if (foundRecords.hasOwnProperty(key)) {
 				record = foundRecords[key];
 				record.count = record.count + 1;
 			}
 			else {
 				record.count = 1;
 				uniqueMapRecords.push(record);
 				foundRecords[key] = record;
 			}
 		}
 			
 		LASTable.displayJSONInTable(uniqueMapRecords, columnLabels, columnFields,'geoTable_'+jsonObject.uuid);
	}		

	
	function showHeader(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"showHeader");
		
		if (jsonObject != null) {
			showHeaderCallBack(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, showHeaderCallBack)
		}
	}
	function showHeaderCallBack(jsonObject) {
		var rawData = JSONTree.create(jsonObject.http_headers);
		openNewWindow(rawData);	
	}

	function showMetaData(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"showMetaData");
		
		if (jsonObject != null) {
			showMetaDataCallBack(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, showMetaDataCallBack)
		}
	}
	function showMetaDataCallBack(jsonObject) {
		var rawData = JSONTree.create(jsonObject.html_meta);
		openNewWindow(rawData);	
	}
	
	function showOutlinks(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"showOutlinks");
		
		if (jsonObject != null) {
			showOutlinksCallBack(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, showOutlinksCallBack)
		}
	}
	function showOutlinksCallBack(jsonObject) {
		var rawData = JSONTree.create(jsonObject.html_outlinks);
		openNewWindow(rawData);	
	}
	
	function showProvenance(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"showProvenance");
		
		if (jsonObject != null) {
			showProvenanceCallBack(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, showProvenanceCallBack)
		}
	}
	function showProvenanceCallBack(jsonObject) {
		var rawData = JSONTree.create(jsonObject.provenance);
		openNewWindow(rawData);	
	}
	
	function showStructuredData(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"showStructuredData");
		
		if (jsonObject != null) {
			showStructuredDataCallBack(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, showStructuredDataCallBack)
		}
	}
	function showStructuredDataCallBack(jsonObject) {
		var rawData = JSONTree.create(jsonObject.structured_data);
		openNewWindow(rawData);	
	}
	
	
	function showStructuralExtraction(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"showStructuralExtraction");
		
		if (jsonObject != null) {
			showStructuralExtractionCallBack(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, showStructuralExtractionCallBack)
		}
	}
	function showStructuralExtractionCallBack(jsonObject) {
		var rawData = JSONTree.create(jsonObject.structuralExtraction);
		openNewWindow(rawData);	
	}
	
	function googleInfo(uuid, fullURL, jsonObject, sourceURL) {
		sendInstrumentationEvent("result.analytic.google.info",{ documentUUID:  uuid, sourceURL:   sourceURL});	
		$('<a target=_blank href="https://www.google.com/#q=info:'+encodeURIComponent(sourceURL)+'">Info</a>')[0].click();
	}
	
	function googleLinkTo(uuid, fullURL, jsonObject, sourceURL) {
		sendInstrumentationEvent("result.analytic.google.linkTo",{ documentUUID:  uuid, sourceURL:   sourceURL});	
		$('<a target=_blank href="https://www.google.com/#q=link:'+encodeURIComponent(sourceURL)+'">Link to</a>')[0].click();
	}
	
	function googleRelated(uuid, fullURL, jsonObject, sourceURL) {
		sendInstrumentationEvent("result.analytic.google.linkTo",{ documentUUID:  uuid, sourceURL:   sourceURL});	
		$('<a target=_blank href="https://www.google.com/#q=related:'+encodeURIComponent(sourceURL)+'">Related</a>')[0].click();		
	}
		
	function displayKeywords(uuid, fullURL, jsonObject, sourceURL, additionalData, resultObject) {
		var dataJSON = { documentUUID:  uuid};
		sendInstrumentationEvent("result.analytic.keywords",dataJSON);	
		
		var percent = 0.25
		
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/document/"+additionalData.storageArea+"/"+additionalData.type+"/"+resultObject.sourceUUID+"/keyword?percentage="+percent,
			contentType: "application/json; charset=utf-8",
			dataType : "JSON",
			success: function(data) {
				if (data.found == false) {
					bootbox.alert("Unable to find the document record");
					return;  
				}
				for (var j=0;j<data.keywords.length;j++) {
					data.keywords[j].score = parseFloat(Math.round(data.keywords[j].score * 1000) / 1000).toFixed(3);
				}
				
	 			var columnLabels = ["Keyword", "Score"];
	 			var columnFields = ["word","score"];
	 			
	 			var dialog = bootbox.dialog({
	 				size: "large",
	 				title: "Keywords",
	 				message: "<table id='kwTable' style='width:100%'></table>"
	 			});		
	 			
	 			LASTable.displayJSONInTable(data['keywords'], columnLabels, columnFields,'kwTable',[[ 1, "desc" ]]);
			}
		});
	
		return false;
	}

	function displayKeyphrases(uuid, fullURL, jsonObject, sourceURL, additionalData, resultObject) {		
		var dataJSON = { documentUUID:  uuid};
		sendInstrumentationEvent("result.analytic.keyphrases",dataJSON);	
		
		var percent = 0.25
		
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/document/"+additionalData.storageArea+"/"+additionalData.type+"/"+resultObject.sourceUUID+"/keyphrase?percentage="+percent,
			contentType: "application/json; charset=utf-8",
			dataType : "JSON",
			success: function(data) {
				for (var j=0;j<data.keyphrase.length;j++) {
					data.keyphrase[j].score = parseFloat(Math.round(data.keyphrase[j].score * 1000) / 1000).toFixed(3);
				}
				
	 			var columnLabels = ["Keyphrase", "Score"];
	 			var columnFields = ["phrase","score"];
	 			
	 			var dialog = bootbox.dialog({
	 				size: "large",
	 				title: "Keyphrases",
	 				message: "<table id='kpTable' style='width:100%'></table>"
	 			});		
	 			
	 			LASTable.displayJSONInTable(data['keyphrase'], columnLabels, columnFields,'kpTable',[[ 1, "desc" ]]);
			}
		});
	
		return false;
	}
	
	function annotateResources(uuid, fullURL, jsonObject, sourceURL, additionalData, resultObject) {
		var dataJSON = { documentUUID:  uuid};
		sendInstrumentationEvent("result.analytic.annotateResources",dataJSON);	
		
		var conf = 0.40
		$(".overlay").show();
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/document/"+additionalData.storageArea+"/"+additionalData.type+"/"+resultObject.sourceUUID+"/annotateDBPedia?confidence="+conf,
			contentType: "application/json; charset=utf-8",
			dataType : "JSON",
			success: function(data) {
				
				var html = $('#text_'+uuid).html();
				
				var resourceArray = data.Resources;
				
				for (var i=resourceArray.length-1; i >=0; i--) {
					var rec = resourceArray[i];
					var offset = rec["@offset"]
					var surfaceForm = rec["@surfaceForm"]
					var newText="<a target=_blank href='"+rec["@URI"]+"'>"+surfaceForm+"</a>";
					
					var foundPosition = html.indexOf(surfaceForm,offset);
					if (foundPosition > 0) {
						html = html.substring(0,foundPosition) + newText + html.substring(foundPosition+surfaceForm.length)
					}
				}
				$('#text_'+uuid).html(html);
				$(".overlay").hide();
				
			}
		});
	
		return false;		
	}
	
	
	function displayNamedEntities(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"analytic.displayNamedEntities");
		
		if (jsonObject != null) {
			displayNamedEntitiesCallback(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, displayNamedEntitiesCallback,false)
		}
		return false;
	}
	
	function displayNamedEntitiesCallback(data) {
		
		var entities = {}

		var dialog = bootbox.dialog({
				size: "large",
				title: "Discovered Named Entities",
				message: "<table id='entityTable' style='width:100%'></table>"
			});	
		
		var columnLabels = ["Entity", "Type", "Count"];
 		var columnFields = ["entity","namedType", "count"];	
 		
 		var startSplitPosition = 0;
		var splits = data.text.split("\n");
		var deferreds = [];
		for (var s=0; s< splits.length; s++) {
			
			function requestNamedEntitiesForSentence(overallDocumentPosition, textDocument) { //creating closure to keep documentPosition correction
			    var defer = $.ajax({
					type :    "POST",
					dataType: "json",
					data:      JSON.stringify(tempDocument),
					url : 	   openke.global.Common.getRestURLPrefix()+"/nlp/process",
				    contentType: "application/json; charset=utf-8",
					success : function(data) {
						var newEntities = [];
						for (var i=0; i < data.sentences.length; i++) {
							var sentence = data.sentences[i];
							var lastType = "O";
							
							for (var j=0; j < sentence.tokens.length; j++) {
								var namedType = sentence.tokens[j].namedEntity;
								if (namedType === "O") { lastType="O"; continue;}

								var word      = sentence.tokens[j].word;
								var record
								if (lastType == namedType) {
									var record = newEntities[newEntities.length-1];
									record.entity = record.entity + " " + word;
								}
								else {
									record = { "entity": word, "namedType" : namedType, "count" :1 };
									
									newEntities.push(record)
								}
								
								lastType = namedType;
									
							}
					
						}
						
						// loop though results and add to hash 
						var uniqueAdd = []
						for (var i=0; i < newEntities.length; i++) {
							var record = newEntities[i];
							var key = record.entity+";"+record.namedType;
							
							if (entities.hasOwnProperty(key)) {
								entities[key].count = entities[key].count +1 
							}
							else {
								entities[key] = record
								uniqueAdd.push(record)
							}							

						}
						

			 			LASTable.displayJSONInTable(uniqueAdd, columnLabels, columnFields,'entityTable',[[ 0, "asc" ]]);
						
					},
					error : function(data) { 
						// log error to console

					}
				});			
			    deferreds.push(defer);
			}
			var tempDocument = { "text" : splits[s] };
			requestNamedEntitiesForSentence(startSplitPosition,tempDocument);
			startSplitPosition += splits[s].length +1; // +1 accounts for splitting on \n 
			
		}
		/* logic to make sure all of the ajax calls have been retrieved.
		var myTimeout = setTimeout(function() { alert("timeout")}, 10000)   //Apply a maximum time to waiting for all of them to be complete
		$.when.apply($, deferreds).then(function() {
			window.clearTimeout(myTimeout);
			alert("Complete!");
		});
		*/
		
		return false;		
	}	
	
	
	function displayRelations(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"analytic.displayRelations");
		
		if (jsonObject != null) {
			displayRelationsCallback(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, displayRelationsCallback,false)
		}
		return false;
	}
	
	var uniqueTriples = {};
	function displayRelationsCallback(data) {
		
		uniqueTriples = {};
		
		//var triples = [];
		var dialog = bootbox.dialog({
				size: "large",
				title: "Discovered Relations",
				message: "<table id='relTable' style='width:100%'></table><div><a id='relAction'>View as Graph</a></div>"
			});	
		$('#relAction').on('click', function() {
			var screen_width = screen.width * .80;
		    var screen_height = screen.height* .80;
		    var top_loc = screen.height *.15;
		    var left_loc = screen.width *.1;
		
		    var myWindow = window.open(openke.global.Common.getContextRoot()+'resources/html/relationGraph.html','_blank','toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);
		   
		});


		
		var columnLabels = ["Subject", "Relation", "Object","Confidence"];
 		var columnFields = ["subj","rel","obj","conf"];	
 			
		var splits = data.text.split("\n");
		for (var s=0; s< splits.length; s++) {
			var tempDocument = { "text" : splits[s] };
			
		    $.ajax({
				type :    "POST",
				dataType: "json",
				data:      JSON.stringify(tempDocument),
				url : 	   openke.global.Common.getRestURLPrefix()+"/nlp/process",
			    contentType: "application/json; charset=utf-8",
				success : function(data) {
					var triples = [];
					for (var j=0;j<data.sentences.length;j++) {
					  for (var i=0;i<data.sentences[j].triples.length;i++) {
						var tripleRecord = data.sentences[j].triples[i];
						tripleRecord.count = 1;
						
						var key = tripleRecord.subj+";"+tripleRecord.rel+";"+tripleRecord.obj;
						if (uniqueTriples.hasOwnProperty(key) == false) {
							uniqueTriples[key] = tripleRecord
							triples.push(tripleRecord)
						}
						else {
							var r = uniqueTriples[key];
							r.count = r.count+1;
						}
					  }	
					}
					
	
		 			
		 			LASTable.displayJSONInTable(triples, columnLabels, columnFields,'relTable',[[ 1, "desc" ]]);
					
				},
				error : function(data) { 
					// log error to console

				}
			});			
		}
		
		return false;		
	}	
	
	function getGraphRelationData() {
		var nodes = {};
		var links = [];
	
		for (var key in uniqueTriples) {
		    if (uniqueTriples.hasOwnProperty(key)) {
		    	var record = uniqueTriples[key];
		 
		    	var subject = record.subj;
		    	if (nodes.hasOwnProperty(subject)) {
		    		var subjRecord = nodes[subject];
		    		subjRecord.count = subjRecord.count + 1;
		    	}
		    	else {
		    		nodes[subject] = { "id": subject, "group" : 1, "count" : 1};
		    	}
		    	
		    	var object = record.obj;
		    	if (nodes.hasOwnProperty(object)) {
		    		var objRecord = nodes[object];
		    		objRecord.count = objRecord.count + 1;
		    	}
		    	else {
		    		nodes[object] = { "id": object, "group" : 1, "count" : 1};
		    	}		    	
		    
		    	var linkRecord = {"source" : subject, "target" : object, "relation": record.rel, "label": record.rel, "value" : record.count}
		    	links.push(linkRecord);
		    }
		}		

		var nodeArray = [];
		for (var key in nodes) {
		    if (nodes.hasOwnProperty(key)) {
		    	nodeArray.push(nodes[key])
		    }
		}
		
		
		var graph = { "nodes" : nodeArray, "links" : links }
		return graph;
	}
	
	
	
	function viewRobotsFile(uuid, fullURL, jsonObject, sourceURL, additionalData) {
		sendInstrumentationEvent("result.robots",{ documentUUID:  uuid, sourceURL:   sourceURL});	
		
		var u = new URL(sourceURL);
		var robotsURL = u.protocol + "//" + u.host + "/robots.txt";
		
		$('<a target=_blank href="'+robotsURL+'">robots</a>')[0].click();	
		
	}



	function summarizeRecord(uuid, fullURL, jsonObject) {
		sendResultLogEvent(uuid,fullURL,"analytic.summarizeText");
		
		if (jsonObject != null) {
			summarizeRecordCallback(jsonObject);
		}
		else {
			loadDocumentRecord(fullURL, summarizeRecordCallback,false)
		}
	}
	function summarizeRecordCallback(data) {
		var submitForm = $("<form accept-charset='UTF-8' name='summarize' id='summarizeForm' action='"+openke.global.Common.getPageURLPrefix()+"/summary' method=post target=_blank><input type=hidden name='summaryText' value='' id='summarizeTextField'/></form>");
		
		//var text = data.text.replace(/[^\x00-\x7F]/g, "").replace(/[\&]+/g, "");
		//text = LASTextAnalysis.eliminateNonSentences(text);

		$(document.body).append(submitForm);
		$(submitForm).find('#summarizeTextField').val(data.text)
		submitForm[0].submit();
		$( "#summarizeForm" ).remove();
	}
	
	function viewOverallStatistics(uuid, fullURL, jsonObject, sourceURL, additionalData, resultObject) {
		sendResultLogEvent(uuid,fullURL,"showQuickStats");
		popupWindowForRecords = getNewWindowReference();
		
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/document/"+additionalData.storageArea+"/"+additionalData.type+"/"+resultObject.sourceUUID+"/statistics",
			contentType: "application/json; charset=utf-8",
			dataType : "JSON",
			success: function(data){
				var rawData = JSONTree.create(data);
				openNewWindow(rawData);
			}
		});

		return false;
	}

	function showWordCloud(uuid, fullURL, jsonObject, sourceURL, additionalData) {
		///var id = id.getAttribute('id');
		
		sendResultLogEvent(uuid,fullURL,"showWordCloud");
		
		var newWindow = getNewWindowReference()
	    newWindow.location =openke.global.Common.getPageURLPrefix()+"/wordCloud?type=" + additionalData.type + "&uuid=" + uuid+"&storageArea="+additionalData.storageArea;
		return false;
	}	
	
	
	function getSelectionText() {
	    var text = "";
	    if (window.getSelection) {
	        text = window.getSelection().toString();
	    } else if (document.selection && document.selection.type != "Control") {
	        text = document.selection.createRange().text;
	    }
	    return text;
	}
	
	
	function getNewWindowReference() {
		var screen_width = screen.width * .5;
	    var screen_height = screen.height* .5;
	    var top_loc = screen.height *.15;
	    var left_loc = screen.width *.1;
	
	    var myWindow = window.open("",'_blank','toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);
	    return myWindow;
	}

	function openNewWindow(data, additionalCSS = '', additionalJS = '') {
	
	    var css = '<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/bootstrap-4.1.2/css/bootstrap.css" />' +
		  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/demonstrator.css" />' +
		  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/elasticSearch/light_style.css" />' +
		  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/resultObject.css" />' +
		  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/mytable.css" />' + 
		  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/pagination.css" />' + 
		  		additionalCSS;
	
	    var js =  '<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/external/jquery-3.3.1.min.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/bootstrap-4.1.2/js/bootstrap.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/external/bootbox.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/LASLogger.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/LASTable.js"></script>'
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/application/common.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/jsontree.0.2.1/jsontree.js"></script>' +additionalJS;
	    	/*
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/ace/ace.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/application/component/ResultObject.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/LASTable.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/application/analytics/OKAnalyticsManager.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/application/analytics/RecordLevelAnalytics.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/application/analytics/JobFactory.js"></script>' +additionalJS;
	*/
				
	    //popupWindowForRecords is set in getDocumentRecord().  This is needed to work around an issue
	    //with Safari not displaying popup windows creating in async handling
	    
	    if (popupWindowForRecords == null) {
			popupWindowForRecords = getNewWindowReference();
	    }
	    
	    popupWindowForRecords.document.write("<html><head>"+css +"</head><body'>");
	    popupWindowForRecords.document.write('<div class="form-group col-md-12">' + data + '</div>' );
	    popupWindowForRecords.document.write("</body>"+js+"</html>");

	    popupWindowForRecords.focus();
	    var returnWindowReference = popupWindowForRecords;
	    
	    popupWindowForRecords = null; // forces re-creation if necessary
	    
	    return returnWindowReference;
	}
	
	
	function openNewSideBySideWindow(nativeTitle, nativeText, transTitle, transText) {
		
	    var css = '<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/bootstrap-4.1.2/css/bootstrap.css" />\n' +
		  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/demonstrator.css" />\n' +
		  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/elasticSearch/light_style.css" />\n' +
		  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/resultObject.css" />\n' +
		  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/sideBySide.css" />\n' +
		  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/external/jquery-ui-1.12.1.css" />\n';
	
	    var js =  '<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/external/jquery-3.3.1.min.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/bootstrap-4.1.2/js/bootstrap.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/external/bootbox.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/LASLogger.js"></script>' +
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/LASTable.js"></script>'
	    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/application/common.js"></script>';

				
	    //popupWindowForRecords is set in getDocumentRecord().  This is needed to work around an issue
	    //with Safari not displaying popup windows creating in async handling
	    
	    if (popupWindowForRecords == null) {
			popupWindowForRecords = getNewWindowReference();
	    }
	    
	    popupWindowForRecords.document.write("<!DOCTYPE html>\n<html>\n<head>\n"+css +"</head>\n<body>\n");
	    
	    popupWindowForRecords.document.write("<div class=sideBySideRow>\n");
		    popupWindowForRecords.document.write('<div class=sideBySideColumn style="background-color:#ebf3f9">\n');
			    popupWindowForRecords.document.write("<h3>" + nativeTitle + "</h3>\n");
			    popupWindowForRecords.document.write("<hr/>\n");
			    popupWindowForRecords.document.write("<p>" + nativeText + "</p>\n");
			popupWindowForRecords.document.write("</div>\n");
			
			popupWindowForRecords.document.write('<div class=sideBySideColumn style="background-color:#fff">\n');
		    	popupWindowForRecords.document.write("<h3>" + transTitle + "</h3>\n");
		    	popupWindowForRecords.document.write("<hr/>\n");
		    	popupWindowForRecords.document.write("<p>" + transText + "</p>\n");
		    popupWindowForRecords.document.write("</div>\n");
		popupWindowForRecords.document.write("</div>\n");
	    
	    popupWindowForRecords.document.write("\n</body>\n"+js+"\n</html>");

	    popupWindowForRecords.focus();
	    var returnWindowReference = popupWindowForRecords;
	    
	    popupWindowForRecords = null; // forces re-creation if necessary
	    
	    return returnWindowReference;
	}
	
	
	

	function displayJSONObjectInNewWindow(data) {	    
	    var js = '<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/ace/ace.js"></script>';
				
	    //popupWindowForRecords is set in getDocumentRecord().  This is needed to work around an issue
	    //with Safari not displaying popup windows creating in async handling
	    
	    if (popupWindowForRecords == null) {
			popupWindowForRecords = getNewWindowReference();
	    }
	    
	    popupWindowForRecords.document.writeln('<div style="height: 200px;" id="editor">'+escapeHtml(JSON.stringify(data,null,4))+'</div>');
	    popupWindowForRecords.document.writeln(js);
	    popupWindowForRecords.document.writeln('<script>editor = ace.edit("editor");editor.setOptions({ maxLines: Infinity});editor.setTheme("ace/theme/xcode");editor.session.setMode("ace/mode/json");editor.getSession().setTabSize(4); editor.getSession().setUseSoftTabs(true);')
	    popupWindowForRecords.document.writeln('editor.getSession().setUseWrapMode(true);'); 
	    popupWindowForRecords.document.writeln('</script>');
	    popupWindowForRecords.focus();
	    
	    var returnWindowReference = popupWindowForRecords;
	    popupWindowForRecords = null; // forces re-creation if necessary
	    
	    return returnWindowReference;
	}
	
	/**
	 * For the passed in text, and the summarizationLevel (which should be between 1 and 100),
	 * produce a summary of that text.  The summarization is returned as an array string to the
	 * callback method.
	 * 
	 * documentUUID: what is the source document identifier?
	 */
	function summarizeText(fullText, summarizationLevel, documentUUID, callback) {
		var dataJSON = { 
				documentUUID:  documentUUID,
	            summarizationLevel:   summarizationLevel
	    };
		sendInstrumentationEvent("summarizeText",dataJSON);	
		
		var postData = {
				"ratio" : summarizationLevel,
				"text" : fullText
			};
			$.ajax({
				type : "POST",
				url : openke.global.Common.getRestURLPrefix()+"/summary/textRankSummary/",
				data : JSON.stringify(postData),
				success : function(data) {
					callback(data.summary);
				},
				error : function(data) {
					LASLogger.logObject(LASLogger.LEVEL_ERROR,data);
				},
				dataType : "json",
				contentType : "application/json"
			});	
	}

	/**
	 * Annotates the passed in text where recors can be located from DBPedia.
	 *  
	 * returns an HTML fragment with the text annotated with hyperlinks to DBPedia
	 */
	function annotateDBPediaText(text, documentUUID, callback) {
		var postData = { text: text, confidence: 0.40 }
		var instrumentationJSON = { 
				documentUUID:  documentUUID
	    };
		sendInstrumentationEvent("annotateTextWithDBPedia",instrumentationJSON);	
		$.ajax({
			type : "POST",
			url : openke.global.Common.getRestURLPrefix()+"/analytics/textAnalytics/annotateResources",
			data : JSON.stringify(postData),
			success: function(data) {
				var html = text;
				
				var resourceArray = data.Resources;
				if (typeof resourceArray != 'undefined'){
					for (var i=resourceArray.length-1; i >=0; i--) {
						var rec = resourceArray[i];
						var offset = rec["@offset"]
						var surfaceForm = rec["@surfaceForm"]
						var newText="<a target=_blank href='"+rec["@URI"]+"'>"+surfaceForm+"</a>";
						
						var foundPosition = html.indexOf(surfaceForm,offset);
						if (foundPosition > 0) {
							html = html.substring(0,foundPosition) + newText + html.substring(foundPosition+surfaceForm.length)
						}
					}
				}
				callback(html)
				
			},
			error : function(data) {
				LASLogger.logObject(LASLogger.LEVEL_ERROR,data);
			},
			dataType : "json",
			contentType : "application/json"
		});
	}

	return {
		setDefaultInstrumentationData : setDefaultInstrumentationData, 
		setDefaultInstrumentationPage : setDefaultInstrumentationPage,
		
		showDocumentInNewWindow : showDocumentInNewWindow, 
		showFullJSONRecord : showFullJSONRecord,
		showFullJSONRecordEditor : showFullJSONRecordEditor,
		showTranslatedSideBySide: showTranslatedSideBySide,
		showText     : showText, 
		showConcepts : showConcepts,
		showGeoTags  : showGeoTags,
		showGeoTagCallBackFromQueue : showGeoTagCallBackFromQueue,
		showHeader   : showHeader,
		showMetaData : showMetaData,
		showOutlinks : showOutlinks,
		showProvenance : showProvenance,
		showStructuredData: showStructuredData,
		showStructuralExtraction : showStructuralExtraction,

		showDomainWhoIs   : showDomainWhoIs,
		
		summarizeText: summarizeText,
		annotateDBPediaText : annotateDBPediaText,
		
		annotateResources : annotateResources,
		
		displayKeywords      : displayKeywords, 
		displayKeyphrases    : displayKeyphrases, 
		displayNamedEntities : displayNamedEntities,
		displayRelations     : displayRelations,
		showWordCloud     : showWordCloud,
		summarizeRecord   : summarizeRecord,
		viewOverallStatistics : viewOverallStatistics,
		viewRobotsFile : viewRobotsFile,
		
		googleInfo: googleInfo,
		googleLinkTo : googleLinkTo,
		googleRelated : googleRelated,
		
		generateUUID : generateUUID,
			
		getGraphRelationData : getGraphRelationData,
		getMapData : getMapData,
		getDocumentToExpand : getDocumentToExpand,
		getAdditionalDataForExpandedDocument : getAdditionalDataForExpandedDocument,
		
		downloadOriginalContent :downloadOriginalContent
	};
}());