/**
 * 
 */

// start of initialization code
"use strict"
LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);


var pageConceptChart = null;

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
	
	$('#btDomainDiscoveryNew').click(function() {
		
		if ($("#discSearchTerms").val().trim() == "") {
			Snackbar.show({text: "You must enter some search terms to create a discovery session", duration: 3500});
			return false;
		}
		
		var discObject = {
			searchTerms: $("#discSearchTerms").val(),
			searchSource: $("#discSearchSource").val()
		}
		
		LASLogger.instrEvent('application.analystHome.createNewDomainDiscoverySession', discObject);		
		$.redirectPost(openke.global.Common.getPageURLPrefix()+'/domainDiscoverySession',discObject);
		
	})
	
	$("#sessions").DataTable({
		"oLanguage": {
			"sSearch": "Filter:"
			},
		"pageLength" : 10,
		"dom": 'ftip',
		"columns" : [ {
				"data" : "sessionName",
				"width" : "75%"
			}, {
				"data" : "userID",
				"width" : "25%"
			}, {
				"data" : "creationDateTime",
				"className" : "nowrap"
			}, {
				"data" : "lastActivityDateTime",
				"className" : "nowrap"
			}],
			"order" : [ [ 3, "desc" ] ]			
	});
	
	// custom css to fix formatting of search label
	$("div.analystDomainDiscovery #sessions_filter > label").css("display","inline-flex");
	$("div.analystDomainDiscovery #sessions_filter  input").css({"position": "relative", "top":"-5px"});

	//These statistics are not used on this version
	//getDomainEstablishedDate();
    //getNumberOfJobs();
	//getRunningNumberOfJobs();
	//getElasticSearchStatistics();
	//getPageHyperlinks();
	//getVisitedPageCount();
	retrieveDataForDomainDiscoverySessionTable();

	if ($(".newstape-content").length) { // only load the breaking news feed if it exists
		loadBreakingNews();
	}
	
    refreshAlertTables();

    $("#btAcknowledgeAll").click(acknowledgeAllAlerts);
    
    $("#newsSettingButton").on('click', openNewsSettings);
    
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

    $("#btnSearch").on('click', performSearch);
	$('#txtSearchQuery').bind("keyup", function(event) {
		if (event.keyCode == 13) {
			performSearch();
			return false;
		}
	});
	
	$("#cbShowAllDiscoverySessions").change(retrieveDataForDomainDiscoverySessionTable);
	
	
	initalizeConceptPanel();
	loadProjectList();
});

function copyStylesInline(destinationNode, sourceNode) {
	   var containerElements = ["svg","g"];
	   for (var cd = 0; cd < destinationNode.childNodes.length; cd++) {
	       var child = destinationNode.childNodes[cd];
	       if (containerElements.indexOf(child.tagName) != -1) {
	            copyStylesInline(child, sourceNode.childNodes[cd]);
	            continue;
	       }
	       var style = sourceNode.childNodes[cd].currentStyle || window.getComputedStyle(sourceNode.childNodes[cd]);
	       if (style == "undefined" || style == null) continue;
	       for (var st = 0; st < style.length; st++){
	            child.style.setProperty(style[st], style.getPropertyValue(style[st]));
	       }
	   }
	}


function initalizeConceptPanel() {
	$("#conceptDateRangePane").css("visibility", "hidden");
	$("#btTopConcepts").on('click', function(){
		pageConceptChart.startAtTop();
    });	
	$("#btResetConcepts").on('click', function(){
		$('#conceptStartTime').val("");
		$('#conceptEndTime').val("");
		setConceptDateField("conceptStartDate",null);
		setConceptDateField("conceptEndDate",null);
		pageConceptChart.resetToTop();
    });
	$("#btConceptSettings").on('click', function() {
		if ($("#conceptDateRangePane").css("visibility") == "hidden") {
			$("#conceptDateRangePane").css("visibility", "visible");
			$("#btConceptSettings").removeClass("btn-default")
		}
		else {
			$("#conceptDateRangePane").css("visibility", "hidden");
			$("#btConceptSettings").addClass("btn-default")
		}
	});
	
	$("#btConceptDownload").on('click', function() {
		  if (LASHeader.getCurrrentScratchpadUUID() == null) {
			  bootbox.alert("You must select a current scratchpad first")
			  return 
		  }
		  var myImage = Plotly.toImage($("#conceptPie")[0],{format:'png',height:400,width:600})
		  myImage.then(function(data) {
			  openke.model.ProjectDocument.appendImage(data, "concept breakdown", function () {
				  Snackbar.show({text: "Chart appended to "+LASHeader.getCurrrentScratchpadName(), duration: 3500})
			  }, function() {
				  bootbox.alert("Unable to append image")
			  });
			  return false;			 
		  });
	});
	
	$("#btConceptHelp").on('click', function() {
		var dialog = bootbox.dialog({
		    title: 'Concept Breakdown: Help',
		    message: '<p>Click on wedge to drill down, Shift-click on wedge to search.<p>Click on a label in the legend to remove/include the value in the pie chart.'+
		             '<p>The settings icon will display a panel to allow you set a date range to filter the results.'
		});
	});
	$("#conceptStartTime").datetimepicker({
		 format : 'Y-m-d H:i:s',
		 onChangeDateTime:function(ct,$i){
			 var conceptStartDate = null;
			 if (ct != null) {
				 conceptStartDate = ct.getTime() - new Date().getTimezoneOffset() * 60000;
			 }
			 setConceptDateField("conceptStartDate",conceptStartDate);
			 pageConceptChart.setStartDate(conceptStartDate);
		}
	});
	$("#conceptEndTime").datetimepicker({
		 format : 'Y-m-d H:i:s',
		 onChangeDateTime:function(ct,$i){
			 var conceptEndDate = null;
			 if (ct != null) {
				 conceptEndDate = ct.getTime() - new Date().getTimezoneOffset() * 60000;
			 }
			 setConceptDateField("conceptEndDate",conceptEndDate);
			 pageConceptChart.setEndDate(conceptEndDate);
		}
	});
	
	var startDateMillis = getConceptDateField("conceptStartDate");
	if (startDateMillis != null && startDateMillis != "null") {
		var startDate = new Date(Number(startDateMillis) + new Date().getTimezoneOffset() * 60000);	
		$("#conceptStartTime").datetimepicker({value: startDate});
	}
	var endDateMillis = getConceptDateField("conceptEndDate");
	if (endDateMillis != null && endDateMillis != "null") {
		var endDate = new Date(Number(endDateMillis) + new Date().getTimezoneOffset() * 60000);	
		$("#conceptEndTime").datetimepicker({value: endDate});
	}
	
	pageConceptChart  = new ConceptChart("conceptPie", "normal", "", getConceptDateField("conceptStartDate"), getConceptDateField("conceptEndDate"), performConceptChartSearch)
	
}

function setConceptDateField(fieldName, milliseconds) {
	var fullName = openke.global.Common.getDomain()+'_'+fieldName;
	sessionStorage[fullName] = milliseconds
}

function getConceptDateField(fieldName) {
	var fullName = openke.global.Common.getDomain()+'_'+fieldName
	if (sessionStorage[fullName] != null && sessionStorage[fullName] != "null") {
		return  Number(sessionStorage[fullName]);
	}
	else {
		return null;
	}
}

function performConceptChartSearch(searchObject) {
		LASLogger.instrEvent('application.analystHome.searchFromConcept', searchObject);
		
		$('#searchQuery').val(JSON.stringify(searchObject));
		$('#searchForm').submit();
}

function performSearch() {
	var queryText = $('#txtSearchQuery').val().trim()
	var queryScope = $('input[name=radScope]:checked').val();
	var sortField = $("#sortField").val();
	var sortOrder = $("input[name='sortOrder']:checked").val();
	var searchType = $("#drpdnSearchType").val();
	
	var searchObject = {
	    "serchText" : queryText,
	    "scope"     : queryScope,
	    "sortField" : sortField,
	    "sortOrder" : sortOrder,
	    "searchType": searchType
	}
	
	LASLogger.instrEvent('application.analystHome.search', searchObject);
	window.location=openke.global.Common.getPageURLPrefix()+'/search?searchQuery='+queryText+'&type='+searchType+'&scope='+queryScope+'&sort='+sortField+'&sortOrder='+sortOrder	
}

function retrieveDataForDomainDiscoverySessionTable() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/searchSession", populateDomainDiscoverySessionTable);
}

function populateDomainDiscoverySessionTable(data) {
	var table = $('#sessions').DataTable();
	table.clear();
	
	var showAllUsers = $("#cbShowAllDiscoverySessions").is(':checked');
	
	for (var i = 0; i < data.length; i++) {
		var newRow = data[i];
		if (showAllUsers == false && newRow.userID != $('#author').val()) {
			continue;
		} 
				
		var name = "<a onclick='LASLogger.instrEvent(\"application.domainDiscovery.sessionSelected\", {sessionID : \""+newRow.sessionID+"\",sessionName : \""+newRow.sessionName+"\" }); return true;' href='"+openke.global.Common.getPageURLPrefix()+"/domainDiscoverySession?sessionUUID="+ newRow.sessionID+"'>"+newRow.sessionName+"</a>";
		var deleteAction = "<span onclick='deleteSession(\""+newRow.sessionID+"\",\""+newRow.userID+"\",this)' class='fas fa-trash-alt'></span>"
		newRow.sessionName = name;
		newRow.action = deleteAction
		table.row.add(newRow);
	}
	table.draw();	
}



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
        
		var rec = new openke.component.ResultObject("", data.notifications[i].resultTitle, data.notifications[i].resultURL, data.notifications[i].resultDescription, {}, true, true, "", false, false);	
		
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


function getPageHyperlinks() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/link/home", function(data) {
		for (var i = 0; i < data.length; i++) {
			var hyperlink = data[i]
		    var ul = document.getElementById("navApplicationHyperlinks");
			/*
		    var li = document.createElement("li");
		    var children = ul.children.length + 1
		    li.setAttribute("id", "element"+children)
		    li.setAttribute("class", "nav-item")
		    li.innerHTML = "<a target='_blank' href='" + hyperlink.link + "'>" + hyperlink.displayText + "<span style='font-size: 16px;'></span></a>";
		    */
			var li = document.createElement("button")
			li.setAttribute("class","btn btn-light")
			li.setAttribute("style","text-align: left;")
			li.setAttribute("onclick","location.href='"+hyperlink.link+"'")
			li.innerHTML = hyperlink.displayText;
		    ul.appendChild(li)
		}
    });
}


function getDomainEstablishedDate() {
	//domainEstablishedDate
	$.getJSON(openke.global.Common.getContextRoot()+"rest/domain/"+openke.global.Common.getDomain()+"/establishedDate", function(data) {
		$('#domainEstablishedDate').html(data.establishedDate.replace(/[-:]/g,"").substring(0,8));
	});
	
	//.replaceAll("-|:","").substring(0,15)
	
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

function getVisitedPageCount() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/visitedPages/count", function(data) {
		$('#numberOfPagesVisited').html(data);
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

function loadProjectList() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/project/", populateProjectList);	
}

function populateProjectList(data) {
	if (data.length == 0) {
		$("#projectList").html("<i>No active projects</i>")
		return;
	}
	$("#projectList").html("<p id='projectLinks'></p>")
	
	var foundInactiveProject = false
	for (var i = 0; i < data.length; i++) {
		var projectRecord = data[i]

		var html = '<a href="#" onClick="visitProject(\''+ projectRecord.id+'\'); return false" onMouseOver="">'+escapeHtml(projectRecord.name)+'</a><br>'
		
		if (projectRecord.status == 'inactive') {
			foundInactiveProject = true;
			if (showInactiveProjects) {
				html = "<i>"+html+"</i>"
			} else {
				continue;
			}
		}
			
		$('#projectLinks').append(html)
	}
	
	if (foundInactiveProject) {
		$('#projectLinks').append("<p>&nbsp;<p>");
		if (showInactiveProjects) {
			$('#projectLinks').append('<div class="text-right"><button class="btn btn-primary btn-sm" id="btOnlyActive" onClick="setInactiveProjectDisplay(false); return false">Active Projects Only</button></div>');
		}
		else {
			$('#projectLinks').append('<div class="text-right"><button class="btn btn-primary btn-sm" id="btOnlyActive" onClick="setInactiveProjectDisplay(true); return false">All Projects</button></div>');
		}
	}
}

function visitProject(projectUUID) {
	LASLogger.instrEvent('application.plan.projectSelected', { projectUUID: projectUUID}, function() {window.location=openke.global.Common.getPageURLPrefix()+"/plan?projectUUID="+projectUUID;});
}

/* from https://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript */
function generateUUID() { // Public Domain/MIT
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

var currentFeeds = [];
var openInProcess = false
function openNewsSettings() {
	if (openInProcess) {return;}
	openInProcess = true;
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/rssFeed/userFeed",
		type : "GET",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			openInProcess = false;
			currentFeeds = [];
			var htmlToAdd = "";
			var keywordText = data.keywords.join(",");
			for (var i=0; i < data.feeds.length; i++) {
				var obj = data.feeds[i];
				if (obj.hasOwnProperty("uuid") == false) {
					obj.uuid = generateUUID();
				}
				htmlToAdd = htmlToAdd + createFeedString(obj.title,obj.url,obj.uuid);
				$("#currentFeedList").append(htmlToAdd);
				currentFeeds.push({title: obj.title, url: obj.url, guid: obj.uuid})
			}
			
			var dialog = bootbox.dialog({
				title: 'News Configuration',
				message: "<h4>Current Feeds</h4><div id='currentFeedList' class=list-group>"+htmlToAdd+"</div><hr><input type='text' id='txtNewFeed' size='50' placeholder='Feed URL'><button class='btn btn-primary' id='btAddFeed' onClick='addFeed(); return false'>Add</button>"+
				         "<br>Required Keywords: <small>(separate by comma - leave blank to show all entries)</small><br><textarea id='txtKeywords' rows='3' style='width:100%;'>"+keywordText+"</textarea><hr>",
				buttons: {
			        confirm: {
			            label: 'Save',
			            className: 'btn-primary',
			            callback: saveFeeds
			        },
			        cancel: {
			            label: 'Cancel',
			            className: 'btn-default'
			        }
			    }
			    });

		},
		error: function(jqXHR, textStatus, errorThrown) {
			openInProcess = false;
			Snackbar.show({text: errorThrown, duration: 3500, pos:'top-center'});
		}
	});	

}

function saveFeeds() {
	var eventObj = { };
	LASLogger.instrEvent("updateNewsFeedList", eventObj)

	var docObj = {
		feeds: currentFeeds,
		keywords: $('#txtKeywords').val().split(',')
	}

	$.ajax({
		type : "PUT",
		url :  openke.global.Common.getRestURLPrefix()+"/rssFeed/userFeed",
		data: JSON.stringify(docObj),
		contentType: "application/json; charset=utf-8",
		success : function(data) {
			if (data.status == "success") {
				Snackbar.show({text: "Feed list updated", duration: 3500});
				loadBreakingNews();
			}
			else {
				Snackbar.show({text: data.error, duration: 3500, pos:'top-center'});
				LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to save feed URLs: " + JSON.stringify(data.message))
				LASLogger.logObject(LASLogger.LEVEL_ERROR, docObj);
			}
		},
		error : function(jqXHR, textStatus, errorThrown) {
			Snackbar.show({text: textStatus, duration: 3500, pos:'top-center'});
			LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to save feed URLs: " + textStatus)
			LASLogger.logObject(LASLogger.LEVEL_ERROR, docObj)				
		}
	});		
}


function addFeed() {
	if ($("#txtNewFeed").val().trim() == "") {
		Snackbar.show({text: "Enter an RSS feed URL", duration: 3500, pos:'top-center'});
		return;
	}
	
	//using base64 encoding (btoa) necessary as the server wouldn't properly recognize the url otherwise.  The "/" also causes problem, so convert to a $
	var urlEncoded = btoa($("#txtNewFeed").val()).replace(/\//g, "$");
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/rssFeed/validateFeed/"+ urlEncoded,
		type : "GET",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			if (data.status === "success") {
				$("#txtNewFeed").val("");
				var guid = generateUUID();
				var htmlToAdd = createFeedString(data.title,data.url,guid);
				$("#currentFeedList").append(htmlToAdd);
				currentFeeds.push({title: data.title, url: data.url, guid: guid})
			}
			else {
				Snackbar.show({text: data.error, duration: 3500, pos:'top-center'});
			}
		},
		error: function(jqXHR, textStatus, errorThrown) {
			Snackbar.show({text: errorThrown, duration: 3500, pos:'top-center'});
		}
	});
}

function removeFeed(guid) {
	$("#feed_"+guid).remove();
	
	for (var i=0; i < currentFeeds.length; i++) {
		var currObj = currentFeeds[i];
		if (currObj.guid == guid) {
			currentFeeds.splice(i,1);
			break;
		}
	}
}

function createFeedString(title, url, guid){
	return "<div id='feed_"+guid+"' class='list-group-item list-group-item-action flex-column align-items-start'><div class='d-flex w-100 justify-content-between'><p class='mb-1'>"+ title +"</p>" +
           "<span class='badge badge-primary badge-pill cursorPointer' style='padding: .35em .5em;' onClick='removeFeed(\""+guid+"\");return false;'>X</span></div>"+
           "<p class='mb-1'><a target='_blank' href='"+url+"'>"+url+"</a></p></div>";
}


// Add the ability to redirect with POSTing data: Source: https://stackoverflow.com/questions/19036684/jquery-redirect-with-post-data   "JimtheDev"
//jquery extend function
$.extend(
{
    redirectPost: function(location, args)
    {
        var form = $('<form></form>');
        form.attr("method", "post");
        form.attr("action", location);

        $.each( args, function( key, value ) {
            var field = $('<input></input>');

            field.attr("type", "hidden");
            field.attr("name", key);
            field.attr("value", value);

            form.append(field);
        });
        $(form).appendTo('body').submit();
    }
})
