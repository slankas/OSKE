/**
 * 
 */

// start of initialization code
LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

$(document).ready(function() {
	
	LASLogger.instrEvent('application.domainHome');
	
	window.page = "home"; // Page name to be stored in window object so that LasHeader.js could access it for logging

	$('#lnkadjudicator').hide();
	if($('#roleAdjudicator').val() === "true") {
		$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobs/adjudication/count", function(data) {
			if(data != 0){
				var text = "You have 1 job to adjudicate"
				if (data >1 ) {
					text = "You have "+data+" jobs to adjudicate"
				}
				$('#lnkadjudicator').show();
				$('#lnkadjudicator').text(text);
			}
		});
	}
	
	/*
	$('#lnkadjudicator').on('click', function() {
		window.location = "adjudication";
	});
	*/
		
	var table = $('#tblRecentJobs').DataTable({
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"columns" : [ {
			"data" : "jobName",
			"width" : "50%",
		}, {
			"data" : "status",
			"width" : "0",
		}, {
			"className" : "nowrap",
			"data" : "startTime",
			"width" : "0",
		}, {
			"data" : "processingTime",
			"width" : "0",
		}, {
			"data" : "numPageVisited",
			"width" : "0",
		}, {
			"data" : "action",
			"width" : "0",
		}, ],
		"order" : [ [ 2, "desc" ] ]
	});

	var table = $('#tblVisitedPages').DataTable({
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"columns" : [ {
			"data" : "url",
			"width" : "60%",
		}, {
			"data" : "status",
			"width" : "0",
		}, {
			"className" : "nowrap",
			"data" : "visitedTimeStamp",
			"width" : "0",
		}, {
			"data" : "action",
			"width" : "0",
		}, ],
		"order" : [ [ 2, "desc" ] ]
	});

	if ($("#paperHistogram").length) { // signifies that we are on the wolfHunt literature discovery page
		populatDateHistogram();
		populatePageCountHistogram();
		
		populateTopAuthorsTable();
		populateTopUniversitiesTable();
		populateTopVendorsTable();
	}
	else {
	    getNumberOfJobs();
		getRunningNumberOfJobs();
		getElasticSearchStatistics();
		getRecentJobsTable();
		getVisitedPageTable();
		getPageHyperlinks();
	}

	if ($(".newstape-content").length) { // only load the breaking news feed if it exists
		loadBreakingNews();
	}
	
    refreshAlertTables();

    $("#btAcknowledgeAll").click(acknowledgeAllAlerts);
    
    $("#pauseButton").on('click', function(){
    	$().newstape.pause();
    	$('#playButton').show();
    	$('#pauseButton').hide();
    });

    $("#playButton").on('click', function(){
    	$().newstape.play();
    	$('#playButton').hide();
    	$('#pauseButton').show();
    });

    
});




function loadBreakingNews() {
	 $.ajax({
	      type: "GET",
	      url: openke.global.Common.getRestURLPrefix()+"/rssFeed",
	      success: function(data) {
	    	  var newsEntries = data.feed;
	    	  $(".newstape-content").empty();
	    	  for (var i=0;i < newsEntries.length; i++) {
	    		  $(".newstape-content").append('<div class="news-block"><strong><a target=_blank href="'+newsEntries[i].url+'">'+newsEntries[i].title+'</a></strong><br><small>'+newsEntries[i].publishedDateTime+'</small><br>'+newsEntries[i].description+'<p><hr></div>');
	    	  }
	    	  
	    	  $('.newstape').newstape();
	    	  $("#pauseButton").click();
	    	  
	      },
	  	  error: function (xhr, ajaxOptions, thrownError) {
			  		  /*
			        if(xhr.status==403) {
			            $("#visitedPagesArea").hide()
			        }
			        */
	             }
	      });	
}


function refreshAlertTables() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/searchAlert/notifications", refreshAlertTable);
}

function refreshAlertTable(data) {
	$("#searchAlertList").empty();
    for (var i = 0; i < data.notifications.length; i++) {
		var acknowledgeAction = "<span onclick='acknowledgeAlert(this,\""+data.notifications[i].alertID+"\",\""+data.notifications[i].resultURL+"\")' class='fas fa-times'></span>";
        
		var rec = new openke.component.ResultObject("", data.notifications[i].resultTitle, data.notifications[i].resultURL, data.notifications[i].resultDescription, {}, true, true);	
		
		rec.getRecordDOM().find(".relevancyCell").append(acknowledgeAction);
		rec.getRecordDOM().find(".message").append(data.notifications[i].resultTimestamp);
		rec.getRecordDOM().find("div.title").attr("alertID",data.notifications[i].alertID);
		
		$('#searchAlertList').append(rec.getRecordDOM());
		rec.displayRecord();
    }
}

function acknowledgeAlert(obj, alertID, resultURL){
	$(obj).parents('tr').remove();
    
	var url = openke.global.Common.getRestURLPrefix()+"/searchAlert/" + alertID ;
	var data = { "action" : "acknowledge", 
			     "url": resultURL};
    $.ajax({
		type :    "POST",
		dataType: "json",
		data: JSON.stringify(data),
		contentType: "application/json; charset=utf-8",
		url : url,
		success : function(data) {
			if (data.status === "failed") {
				bootbox.alert({
					title: "Acknowledgement Error",
					message: data.message
				})
			}
		},
		error : function(data) {
			bootbox.alert({
				title: "Acknowledgement Error",
				message: "Unable to acknowledge error"
			})

		}
	});  
}

function acknowledgeAllAlerts(){
	$("div#searchAlertList div.title").each(function() {
		
		var url = $(this).find("a").attr('href');
		var alertID = $(this).attr("alertID");
		acknowledgeAlert(this, alertID,url);
	});
	
}

function getVisitedPageTable() {
	 $.ajax({
	      type: "GET",
	      url: openke.global.Common.getRestURLPrefix()+"/visitedPages/recent",
	      success: refreshVisitedPageTable,
	  	  error: function (xhr, ajaxOptions, thrownError) {
	        if(xhr.status==403) {
	            $("#visitedPagesArea").hide()
	        }
	      }});
}

function getPageHyperlinks() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/link/home", function(data) {
		for (var i = 0; i < data.length; i++) {
			var hyperlink = data[i]
		    var ul = document.getElementById("navApplicationHyperlinks");
		    var li = document.createElement("li");
		    var children = ul.children.length + 1
		    li.setAttribute("id", "element"+children)
		    li.setAttribute("class", "active")
		    li.innerHTML = "<a target='_blank' href='" + hyperlink.link + "'>" + hyperlink.displayText + "<span style='font-size: 16px;'></span></a>";
		    ul.appendChild(li)
		}
    });
}

function getRecentJobsTable() {
	 $.ajax({
	      type: "GET",
	      url:  openke.global.Common.getRestURLPrefix()+"/jobsHistory/recent",
	      success: refreshRecentJobsTable,
	  	  error: function (xhr, ajaxOptions, thrownError) {
	        if(xhr.status==403) {
	            $("#recentJobsArea").hide()
	        }
	      }});
}

function stopJob(jobId) {
	LASLogger.instrEvent('application.home.stop_job', {
		jobID : jobId
	});

	if (jobId == null) {
		return false;
	}
	var url = openke.global.Common.getRestURLPrefix()+"/jobs/" + jobId + "/stop"
    $.ajax({
		type : "POST",
		url : url,
		success : function(data) {
			var status = data;
			if (status != null && status != "") {
				
				bootbox.alert({
					title: "Status Changed",
					message: "Requested to stop job"
				})
				
				getRecentJobsTable();
				return false;
			}
		},
		error : function(data) {
			document.getElementById("err_label").style.display = "inline";
			document.getElementById("err_label").innerHTML = data.responseJSON.reason;
		},
		dataType : "text",
	});
	
    return false;
}	
	

function refreshRecentJobsTable(data) {
	var table = $('#tblRecentJobs').DataTable();
	table.clear();
	for (var i = 0; i < data.jobHistory.length; i++) {
		newRow = data.jobHistory[i];

		newRow.action = "<a href= '"+openke.global.Common.getPageURLPrefix()+"/visitedPages?jobHistoryID="	+ newRow.jobHistoryID + "'>Visited Pages</a>";

		if (newRow.status === "processing" && newRow.jobName !== "DirectoryWatcher") {
			newRow.action += " <a href='javascript:stopJob(\"" + newRow.jobID + "\");'>Stop</a>";
		}

		table.row.add(newRow);

	}
	table.draw();
}

function refreshVisitedPageTable(data) {
	var table = $('#tblVisitedPages').DataTable();
	table.clear();
	for (var i = 0; i < data.visitedPages.length; i++) {
		newRow = data.visitedPages[i];
		

		if (newRow.status != 'irrelevant') {
			var id = '' + newRow.id + '';
			var a = "<a target='_blank' href='"+openke.global.Common.getRestURLPrefix()+"/visitedPages/" + id + "/content' >Stored</a>";
			newRow.action = a;
		} else {
			newRow.action = '';
		}
		
 		if (newRow.url.substring(0,5) != "file:" && newRow.url.substring(0,5) != "mail:") {
 			newRow.action += " " + "<a target='_blank' href= '" + newRow.url + "'>" + "Original" + "</a>";
  		}		  		
 		
		newRow.url = newRow.url.match(new RegExp('.{1,60}', 'g')).join("\n"); //split up long URLs for display.  must come after defining the action
 			
		
		table.row.add(newRow);
	}
	table.draw();
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/visitedPages/count", function(data) {
		$('#numberOfPagesVisited').html(data);
	});
}

function getNumberOfJobs() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobs/count", function(data) {
		$('#numberOfJobs').html(data);
	});
}

function getRunningNumberOfJobs() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobs/count?running=true", function(data) {
		$('#numberOfRunningJobs').html(data);
	});
}

function getElasticSearchStatistics() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/statistics", function(data) {
		//LASLogger.logObject(LASLogger.LEVEL_FATAL,data);
		var size = data["_all"]["primaries"]["store"]["size_in_bytes"] / 1000000000.00
		var count = data["_all"]["primaries"]["docs"]["count"] 

		$('#numberOfPagesStored').html(count);
		$('#sizeOfPagesStored').html(size.toFixed(3) + "&nbsp;GB");
	});

}

function navigateTo(location) {
	var completeLocation =openke.global.Common.getPageURLPrefix() +"/" +location
	LASLogger.instrEvent('domain.home.link', {
		link : completeLocation
	});
	
     window.location = completeLocation;
    
}

function executeElasticQuery(query, callback) {
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/citations/data",
		type : "POST",
		contentType: "application/json; charset=utf-8",
		data : JSON.stringify(query),
		dataType : "JSON",
		success:callback	
	});
}

function populatDateHistogram() {
	var query = {
			  "size": 0,
			  "query": {
			    "bool": {
			      "filter": [   { "range": {  "DateCreated": {  "gte": 1254355200000, "lte": 1506816000000}   }    }    ]
			    }
			  },
			  "aggs": {
			    "dates": {
			      "date_histogram": {
			        "field": "DateCreated",
			        "interval": "1M",
			        "time_zone": "Zulu",
			        "min_doc_count": 0
			      }
			    }
			  }
			}
	executeElasticQuery(query,createDateHistogram)
}

function populatePageCountHistogram() {
	var query = {
			  "size": 0,
			  "query": {"bool": {
			      "filter": [
			        {"range": {"DateCreated": {"gte": 1254355200000, "lte": 1506816000000} }  },
			        {"range": {"pdfPageCount": {"lte": 50 }}}
			      ]
			    }},
			  "aggs": {
			    "pageCounts": {
			      "histogram": {
			        "field": "pdfPageCount",
			        "interval": 1,
			        "min_doc_count": 0
			      }
			    }
			  }
			}
	executeElasticQuery(query,createPageCountHistogram)
}

function createDateHistogram(data) {
	
	var x = [];
	var y = [];
	
	var bucketArray = data.aggregations.dates.buckets;
	for (var i=0; i < bucketArray.length; i++) {
		var count = bucketArray[i].doc_count;
		var label = bucketArray[i].key_as_string.substring(0,7);
		y.push(count)
		x.push(label)
	}
	
	
	var layout = {
			title: "Published Papers by Date",
			xaxis: { title: "Date"  },
			yaxis: { title: "Count"  },
			margin: {
			    l: 50,
			    r: 50,
			    b: 50,
			    t: 50,
			    pad: 4
			  }
		};

		var data = [ {
			    x: x, 
			    y: y,
			    type: 'bar'
	    }];

		Plotly.newPlot('paperHistogram', data,layout);
		
		document.getElementById('paperHistogram').on('plotly_click', function(data){
			var dateString = data.points[0].x
			var dateMoment = moment(dateString);
			var timeStartMS = dateMoment.valueOf();
			var timeEndMS   = dateMoment.add(1,'months').valueOf();
			
			var query = {
					  "size": 20, "from" : 0,
					  "query": {"bool": {
					      "filter": [
					        {"range": {"DateCreated": {"gte": timeStartMS, "lte": timeEndMS} }  },
					        {"range": {"pdfPageCount": {"lte": 50 }}}
					      ]
					    }}
					}
			
			
			LASLogger.instrEvent('application.literatureDiscovery.home.dateHistogramSearch', {
				criteria : query
			});
			
			$('#searchQuery').val(JSON.stringify(query));
			$('#searchForm').submit();
		});
}


function createPageCountHistogram(data) {
	
	var x = [];
	var y = [];
	
	var bucketArray = data.aggregations.pageCounts.buckets;
	for (var i=0; i < bucketArray.length; i++) {
		var count = bucketArray[i].doc_count;
		var label = bucketArray[i].key;
		y.push(count)
		x.push(label)
	}
	
	
	var layout = {
			title: "PDF Page Count Distribution",
			xaxis: { title: "Number of Pages"  },
			yaxis: { title: "Number of Documents"  },
			margin: {
			    l: 50,
			    r: 50,
			    b: 50,
			    t: 50,
			    pad: 4
			  }
		};

	var data = [ {
	    x: x,
	    y: y,
	    type: 'bar'
	}];

	Plotly.newPlot('paperPDFPageCount', data,layout);
	document.getElementById('paperPDFPageCount').on('plotly_click', function(data){
		var base64fieldName = btoa("pdfPageCount");
		var base64fieldValue = btoa(data.points[0].x);
		executeAuthorSearch(base64fieldName,base64fieldValue );
	});
}

function executeAuthorSearch(fieldName, fieldValue) {
	var actualName = atob(fieldName)
	var actualValue = atob(fieldValue)
	
	var filter = { "terms" : { } }
	filter.terms[actualName] = [actualValue]
	
	var query={ "query": {  "bool": { "filter": [filter]  }  },
        "size":20, "from":0 }
	
	LASLogger.instrEvent('application.literatureDiscovery.home.search', {
		criteria : query
	});

	$('#searchQuery').val(JSON.stringify(query));
	$('#searchForm').submit();
}

function executeUniversitySearch(fieldValue) {
	var actualValue = atob(fieldValue)
	
	var query = {
		    "query": {"bool": {"filter": [{"nested": {
		        "path": "concepts",
		        "query": {"bool": {"filter": [
		            {"terms": {"concepts.type.keyword": ["University"]}},
		            {"terms": {"concepts.value.keyword": [actualValue]}}
		        ]}}
		    }}]}},
		    "size": 20, "from": 0
		}
	
	LASLogger.instrEvent('application.literatureDiscovery.home.search', {
		criteria : query
	});

	$('#searchQuery').val(JSON.stringify(query));
	$('#searchForm').submit();
}

function executeVendorSearch(fieldValue) {
	var actualValue = atob(fieldValue)
	
	var query = 	{
	    "query": {"bool": {"filter": [{"nested": {
	        "path": "concepts",
	        "query": {"bool": {"filter": [
	            {"terms": {"concepts.type.keyword": ["vendor"]}},
	            {"terms": {"concepts.name.keyword": [actualValue]}}
	        ]}}
	    }}]}},
	    "size": 20, "from": 0
	}
	
	LASLogger.instrEvent('application.literatureDiscovery.home.search', {
		criteria : query
	});
	
	$('#searchQuery').val(JSON.stringify(query));
	$('#searchForm').submit();
}

function populateTopAuthorsTable() {
	var table = $('#topAuthorsTable').DataTable({
		"dom": 'tip',
		"pageLength": 5,
		"columns" : [ {
			"data" : "key",
			"width" : "75%"
		}, {
			"data" : "doc_count",
			"width" : "25%",
		} ],
		"order" : [ [ 1, "desc" ] ]
	});	
	
	var query = {"size":0,"aggs":{"item":{"terms":{"field":"authorFullName.keyword","order":{"_count":"desc"},"size":100}}}}
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/citations/data",
		type : "POST",
		contentType: "application/json; charset=utf-8",
		data : JSON.stringify(query),
		dataType : "JSON",
		success: function(data) {
			var bucketArray = data.aggregations.item.buckets;
			var base64fieldName = btoa("authorFullName.keyword");
			table.clear();
			for (var i = 0; i < bucketArray.length; i++) {
				newRow =bucketArray[i];

				var base64fieldValue = btoa(newRow.key);
				newRow.key= "<a href='javascript:executeAuthorSearch(\""+base64fieldName+"\",\""+base64fieldValue+"\");'>"+newRow.key+"</a>"
				table.row.add(newRow);
			}
			table.draw();			
		}	
	});
	
}


function populateTopUniversitiesTable() {
	var table = $('#topUniversitiesTable').DataTable({
		"dom": 'tip',
		"pageLength": 5,
		"columns" : [ {
			"data" : "key",
			"width" : "75%"
		}, {
			"data" : "doc_count",
			"width" : "25%",
		} ],
		"order" : [ [ 1, "desc" ] ]
	});	
	
	var query = {"size":0,"aggs":{"concepts_root":{"nested":{"path":"concepts"},"aggs":{"universities":{"filter":{"term":{"concepts.type.keyword":"University"}},"aggs":{"attrs":{"terms":{"field":"concepts.value.keyword","order":{"_count":"desc"},"size":100}}}}}}}}
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/citations/data",
		type : "POST",
		contentType: "application/json; charset=utf-8",
		data : JSON.stringify(query),
		dataType : "JSON",
		success: function(data) {
			var bucketArray = data.aggregations.concepts_root.universities.attrs.buckets;
			var base64fieldName = btoa("authorFullName.keyword");
			table.clear();
			for (var i = 0; i < bucketArray.length; i++) {
				newRow =bucketArray[i];

				var base64fieldValue = btoa(newRow.key);
				newRow.key= "<a href='javascript:executeUniversitySearch(\""+base64fieldValue+"\");'>"+newRow.key+"</a>"
				table.row.add(newRow);
			}
			table.draw();			
		}	
	});

}



function populateTopVendorsTable() {
	var table = $('#topVendorsTable').DataTable({
		"dom": 'tip',
		"pageLength": 5,
		"columns" : [ {
			"data" : "key",
			"width" : "75%"
		}, {
			"data" : "doc_count",
			"width" : "25%",
		} ],
		"order" : [ [ 1, "desc" ] ]
	});	
	
	var query = {"size":0,"aggs":{"concepts_root":{"nested":{"path":"concepts"},"aggs":{"vendors":{"filter":{"term":{"concepts.type.keyword":"vendor"}},"aggs":{"attrs":{"terms":{"field":"concepts.name.keyword","size":100}}}}}}}}
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/citations/data",
		type : "POST",
		contentType: "application/json; charset=utf-8",
		data : JSON.stringify(query),
		dataType : "JSON",
		success: function(data) {
			var bucketArray = data.aggregations.concepts_root.vendors.attrs.buckets;
			var base64fieldName = btoa("authorFullName.keyword");
			table.clear();
			for (var i = 0; i < bucketArray.length; i++) {
				newRow =bucketArray[i];

				var base64fieldValue = btoa(newRow.key);
				newRow.key= "<a href='javascript:executeVendorSearch(\""+base64fieldValue+"\");'>"+newRow.key+"</a>"
				table.row.add(newRow);
			}
			table.draw();			
		}	
	});
}