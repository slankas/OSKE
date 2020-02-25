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
		LASLogger.instrEvent('application.analystHome.createNewDomainDiscoverySession', {});
		window.location=openke.global.Common.getPageURLPrefix()+'/domainDiscoverySession'		
	})
	
	$("#sessions").DataTable({
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
			}],
			"order" : [ [ 2, "desc" ] ]			
	});
		


	getDomainEstablishedDate();
    getNumberOfJobs();
	getRunningNumberOfJobs();
	getElasticSearchStatistics();
	getPageHyperlinks();
	retrieveDataForDomainDiscoverySessionTable();
	getVisitedPageCount();

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

    $("#btnSearch").on('click', performSearch);
	$('#txtSearchQuery').bind("keyup", function(event) {
		if (event.keyCode == 13) {
			performSearch();
			return false;
		}
	});
	
	
	initalizeConceptPanel();
});



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
	
	for (var i = 0; i < data.length; i++) {
		var newRow = data[i];
				
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

