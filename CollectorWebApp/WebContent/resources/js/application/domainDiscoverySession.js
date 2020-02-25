LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

var sessionId = "";    // what is the current session?  if a session has not yet been created, this will be an empty string.
var enumber   = 0;

var priorExecutionData = { };    // what are the records of the prior searches.  Using a hash table of execution objects, indexed by the number

var searchURL = openke.global.Common.getRestURLPrefix()+"/searchSession/{sessionId}/execution/{executionNumber}/export";
var searchURLAll = openke.global.Common.getRestURLPrefix()+"/searchSession/{sessionId}/export";
	
var ldaStatusTimer; 
var crawlStatusTimer;      // when set, has a point to an interval timer that checks the status of a crawl
var secondaryProcessingStatusTimer;      // when set, has a point to an interval timer that checks the status of the secondary processing.  Once this is done, we kick off creating the DiscoveryIndex.  Starts when the crawl status is complete.
var executionIndexStatusTimer;  // checks whether or not a discovery index has been created for a specific execution.

var processedUUIDs = {};   // tracks which UUIDs have been processed / updated on the UI so that they are not constantly updated.

var openedWindows = [];    // tracks any windows that have been opened.

var showIndexViewSnackbar = true; // should a message be displayed when an index view is available.  for prior sessions, don't display the message

var exportAllExecutions = false;  //toggle; choose searchURL if false or searchURLAll if true; set when clicking exportAll button

var translateTargetLanguage = "none"; //if user changes language dropdown #targetLanguage set to target language

$(document).ready(function() {
	$('#documentIndexView').hide()
	
	window.setQueryText = function(text) {
		$("#searchTerms").val(text);
	}
	
	// Override window.open so we can automatically keep track of all instances.
	window._open = window.open; // saving original function
	window.open = function(url,name,params){
		var newWin = window._open(url,name,params);
	    openedWindows.push(newWin);
	    return newWin;
	}
	$(window).on('unload', function() {
		for (var i=0; i< openedWindows.length; i++) {
			var popup = openedWindows[i];
			if (popup && !popup.closed) {
				popup.close();
			}
	    }
		
	});
	
	$('#searchAPI option[data-default="true"]').prop('selected', true);
	
	OKAnalyticsManager.defineStandardDocumentMenu("application.domainDiscovery.",true); 
	OKAnalyticsManager.registerObjectAnalytic("a16",null, "Search OutboundLinks",searchOutboundLinks);
	openke.model.DocumentBucket.setDefaultInstrumentationPage('application.domainDiscovery');
	openke.view.DocumentBucketSupport.setDefaultInstrumentationPage('application.domainDiscovery');
	
	openke.view.DocumentBucketSupport.setCallBackForAddDocumentToBucket(documentAddedToBucket);
	openke.view.DocumentBucketSupport.setCallBackForRemoveDocumentFromBucket(documentRemovedFromBucket);
		
	openke.model.DocumentBucket.loadAll()
	
	LASLogger.log(LASLogger.LEVEL_INFO,"Registered object level analytics / menu");	
	
	$('#analyzeButton').click(gotoAnalyze);
	$('#exportAllButton').click(function () { openExportDialog("all")});
	
	$('#btHome').click(function(){
		LASLogger.instrEvent('application.domainDiscoverySession.home', {}, function() {window.location=openke.global.Common.getPageURLPrefix();});
	});
	
	$('.sessionList').click(function(){
		LASLogger.instrEvent('application.domainDiscoverySession.discoveryHome', {}, function() {window.location=openke.global.Common.getPageURLPrefix()+"/domainDiscovery";});
	});
		
	$('#searchButton').click(initiateSearch);
	$('#urlSearchButton').click(initiateURLSearch);
	//$('#suggestButton').click(initiateSuggest);
	$('#relatedGoogle').click(initiateRelatedGoogleSearches);
	
	$('#relatedDictionaryExpansion').click(function() { getDictionaryExpansion()})
	$(document).on("click",'table.dictionaryTable td',  function() {
		$(this).toggleClass("text-muted");
		$(this).toggleClass("highlight-selected");
		
		/*
		if ( $(this).hasClass("wn-key")){
			appendToWordNetHolderKey( $(this) );
		}else{
			appendToWordNetHolder( $(this) );
		}*/
		
	});
	$(document).on("click",'.def-dropdown',  function(evt) {
		evt.stopPropagation();
		$(this).find(".def-dropdown-content").toggle()
		
	});	
	
	$(document).click(function(e){
		if ($(e.target).closest('.def-dropdown-content').length != 0) { return false; } 
		$('.def-dropdown-content').hide();
	});
	
	$('#relatedWordsGeneral').click(function(){
		getWordNetExpansions("general");
	});
	$('#relatedWordsSpecific').click(function(){
		getWordNetExpansions("specific");
	});
	$(document).on("click",'table.wordnetTable td',  function() {
		$(this).toggleClass("text-muted");
		$(this).toggleClass("highlight-selected");
		if ( $(this).hasClass("wn-key")){
			appendToWordNetHolderKey( $(this) );
		}else{
			appendToWordNetHolder( $(this) );
		}
		
	});
	

	
	
	$('#nlexSemantic').click(function(){
		getNLExpansion("semantic");
	});
	$('#nlexSyntactic').click(function(){
		getNLExpansion("syntactic");
	});
	$(document).on("click", 'table.nlexTable td', function() {
		$(this).toggleClass("text-muted");
		$(this).toggleClass("highlight-selected");
		if ( $(this).hasClass("nlex-key")){
			appendToNLEXHolderKey( $(this) );
		}else{
			appendToNLEXHolder( $(this) );
		}
		
	});
	
	
	$('#stopButton').hide();
	$('#stopButton').click(stopSearch);
	$('#urlStopButton').hide();
	$('#urlStopButton').click(stopSearch);
	
	$('#translateTermsButton').hide();
	$('#translateTermsButton').click(translateSearchTerms);
	
	$('#createJobButton').click(startNewJob);
	$('#urlCreateJobButton').click(startNewJobFromWebList)
	$('#createSearchAlert').click(startNewSearchAlert); 
	$('#searchResultsExport').click(function () { openExportDialog("execution")});
    $('#searchResultStatistics').click(showResultsStatistics);

	$('#switchToListView').click(switchToListView);
    $('#switchToIndexView').click(switchToIndexView);
    
    $('#btPriorExecutions').click (showPriorExecutions);
    $('#btPriorExecutionsCSV').click (showPriorExecutionsCSV);
    
    $('#btPriorExecutionsStatistics').click(function() { alert("not implemented");});
    $('#btPriorExecutionsStatisticsCSV').click(showSessionSummaryReport);
    
    if ($("#translateSupport").val()=="false") {
    	$("#enableForeignLanguageSearch").hide();
    }
    if ($("#nlExpansionSupport").val()=="false") {
    	$(".nlExpansionMenuItem").hide();
    }
	//	<input type="hidden" name="nlExpansionSupport" id="nlExpansionSupport" value="false" />  
	
    
    // event handler for translate checkbox (enable language select if checked, disable if not)
	$("#translateCheck").change(switchTranslateSelect);
	
    $("#targetLanguage").change(translateSearchTerms);
    
    $('.sessionExists').hide();
    hideSearchActions();
	$('#relevenceLegend').hide();
	//$('#topicModelling').hide();
	
	$('#searchResultsExport').hide();
	
	$('#btCreateIndex').click(function() {
		var documentArea = "sandbox";
		var documentIndexID = sessionId;
		var title = $("#sessionName").val();
		var maxNumResults = -1;
		var query = { "nested": {
			                        "path": "domainDiscovery.retrievals",
			                        "score_mode": "avg",
			                        "query": {"bool": {"must": [{"match": {"domainDiscovery.retrievals.sessionID.raw": sessionId}}]}}
	    }};
		
		DocumentIndex.createIndex(documentArea, documentIndexID, query, title, maxNumResults)

	});
	
	$('#btShowIndex').hide()
	$('#btShowIndex').click(function() {
		DocumentIndex.showIndexView("sandbox",sessionId)
	});
	
	$('.fileUse').hide();
	$('.urlListUse').hide();
	$('#searchAPI').change(searchAPIChanged);
	
	
    $('#fileupload').fileupload({
    	url : openke.global.Common.getRestURLPrefix()+"/searchSession/fileUpload",
    	formData: function (form) { return [{name: 'sessionID', value: sessionId},
    		                                {name: 'sessionName', value: $('#sessionName').val()}];},
        dataType: 'json',
        done: function (e, data) {
        	//TODO LASLogger.instrEvent('application.domainDiscoverySession.topicModel - start', dataJSON);
        	$('#topics').html("");
        	$('#titleSessionName').text(": "+$('#sessionName').val().trim());
        	//$('#sessionName').attr('disabled','disabled');      // changed 20180830, Can't edit the session name once it "begins"
        	$('#searchTerms').val(data.result.searchResults[0].title);
        	
        	sessionId = data.result.sessionID;
        	enumber   = data.result.executionnumber;
        	$('.sessionExists').show();
        	var sessionJsonData = {
        			searchTerms : "",
        			sessionName : $('#sessionName').val(),
        			numberOfSearchResults : 1,
        			searchAPI: $("#searchAPI").val(),
        			advConfig: {},
        			fileUpload: data.result.searchResults[0].title
        	}
        	successfulSessionInitiated(data.result, sessionJsonData,data.result.sessionID);
        }, 
        fail : function (e,data) {
        	//TODO LASLogger.instrEvent('application.domainDiscoverySession.topicModel - start', dataJSON);
        	bootbox.alert({title :"Error", message: "Unable to upload file in session: " + JSON.stringify(data.result,null,4) });
        }
    });
	
	
	$('#btTopics').click(initiateTopicModelling);
	
	sendInstrumentationEvent('.onPage', {sessionID: $("#sessionUUID").val()});
	
	// user clicked on an existing session from the discovery listing page ...
	if ($("#sessionUUID").val() != "") {
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+$("#sessionUUID").val(),
			type: "GET",
			contentType: "application/json; charset=utf-8",
			dataType : "JSON",
			error: function(data) {
				bootbox.alert({title :"Error", message: "The requested session does not exist.  Starting a new session." });
			},
			success: function(data) {
				 $('.sessionExists').show();
				 
				//LASLogger.logObject(LASLogger.LEVEL_FATAL,data)
				sessionId = data.sessionID;
				$('#sessionName').val(data.sessionName);
				$('#titleSessionName').text(": "+data.sessionName);
				//$('#sessionName').attr('disabled','disabled');      // editable again, 20180830 Can't edit the session name once it "begins"
				var eUrl = openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution";
				//now, get execution information
				$.ajax({
					url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution",
					type: "GET",
					contentType: "application/json; charset=utf-8",
					dataType : "JSON",
					success: function(data) {
						if (data.length > 0) {
							var index = data.length;
							while (index > 0) {
								index = index-1
								record = data[index];
								if (record.searchAPI == "custom") {continue;}  // need to present the search terms from the most recent search query (skip outbound links)
								$('#searchTerms').val(record.searchTerms);
								$('#numberOfsearchResults').val(record.numSearchResults);
								$("#searchAPI").val(record.searchAPI);
								$("#advConfig").val(JSON.stringify(record.advancedConfiguration));
								$("#targetLanguage").val(record.targetLanguage).prop('selected', true);
								$("#translateCheck").prop('checked', record.translateCheck);
								$('#searchTermsTranslate').val(record.searchTermsTranslated);
								break;
							}
							if ( $("#translateCheck").is(":checked")) {
								$("#targetLanguage").prop('disabled', false);
								$("#transRow").removeClass('d-none');
								$('#translateTermsButton').show();
							}
						}
						
						
						$("#executionList").empty();
						for (var index in data) {
							appendToSearchHistoryList(data[index]);
						}
						
					},
					error: function(data) {
						bootbox.alert({title :"Error", message: "Unable to load prior execution information." });
					},
				});
				
				DocumentIndex.checkIndexExists("sandbox",sessionId);

			}
		});
	}
	else if ($("#passedSearchTerms").val() != "") {
		var searchTerms = atob($("#passedSearchTerms").val());
		var searchSource = atob($("#passedSearchSource").val());
		var sessionName = searchTerms.replace(/[^a-zA-Z_ \-,0-9\.\(\)]/g, "");
		$('#sessionName').val(sessionName);
		$('#searchTerms').val(searchTerms);
		$('#searchAPI').val(searchSource);
		initiateSearch();
	}
	else { // user is creating a new session
		var projectName = LASHeader.getCurrrentProjectName();
		if (projectName !=null ) {
			$('#sessionName').val(projectName);
		}
	}
	
});



function switchTranslateSelect(){
	if ( $("#translateCheck").is(":checked")) {
		$("#targetLanguage").prop('disabled', false);
		$("#transRow").removeClass('d-none');
		$('#translateTermsButton').show();
	}else{
		$('#targetLanguage option').prop('selected', false);
		$("#targetLanguage").prop('disabled', true);
		$("#transRow").addClass('d-none');
		$("#searchTermsTranslate").val("");
		$('#translateTermsButton').hide();
	}
}


function translateSearchTerms() {
	
	//call endpoint to get translation
	var text = $('#searchTerms').val();
	var srcLang = "auto";
	var destLang = $('#targetLanguage :selected').val();
	
	var url = openke.global.Common.getRestURLPrefix()+"/searchSession/translate/searchTerms/"+srcLang+"/"+destLang+"/"+text;
	
	$.getJSON( url, function(data) {
		$.each( data, function( key, value ) {
		    $('#searchTermsTranslate').val(value);
		  });
	});
}

function getTargetLanguage() {
	if ( document.getElementById('translateCheck').checked && $('#targetLanguage').val().trim() != "" && $('#targetLanguage').val().trim() != "none")  {
		return $('#targetLanguage').val().trim()
	}
	else {
		return "en";
	}
}

function replaceDictionaryExpansion(wordPosition, dictionaryPostion) {
	let term = dictionaryExpansionData[wordPosition].term
	let def  = dictionaryExpansionData[wordPosition].definitions[dictionaryPostion]
    let targetLanguage = getTargetLanguage()
	let url = openke.global.Common.getRestURLPrefix()+"/searchSession/wordnetDefinition/"+targetLanguage+"/"+term+"/definition/" + def;
	
	
	$(".overlay").show();
    $.ajax({
		type : "GET",
		url : url,
		success : function ( data ) {
			//console.log(JSON.stringify(data,null,4))
			
			$("#dictionaryTable td[data-row='"+wordPosition+"']").each(function( index ) {
				if (index > 0) {
					this.remove();
				}
		    });
			var trRef = $("#dictionaryTable td[data-row='"+wordPosition+"']").parent();
			var position = 0;
			for(var i=0; i < data.length; i++){
		    	let expWord = data[i].word;
		    	if ( expWord.toLowerCase() != term.toLowerCase() ) {
		    		position = position +1;
		    		let newCell = "<td class='wn-val text-muted' style='cursor: pointer;' data-row='"+wordPosition+"' data-item='"+position+"' title='"+data[i].definition+"'><span class='highlight'>"+expWord+"</span></td>";
		    		trRef.append(newCell);
		    	}
		    }
			
			$(".overlay").hide();
			
		},
		error : function(data) {
			$(".overlay").hide();
		},
		dataType : "json",
		contentType : "application/json"
	});
	
	
	return false;
}

function createDictionaryExpansionTermCell(rowNum, termObject) {
	let cell="<td class='wn-key highlight-selected' style='cursor: pointer;' data-row='"+rowNum+"' data-item='0'><span class='highlight'>"+termObject.term+"</span>";
	
	if (termObject.definitions.length >0 ) {
		cell = cell + "<div class='def-dropdown'><span class='fas fa-arrows-alt-v' style='float: right'><div class='def-dropdown-content'>";
		
		for (var i=0;i < termObject.definitions.length; i++ ) {
			cell = cell + "<a href='javascript: replaceDictionaryExpansion("+rowNum+","+i+");'>" + termObject.definitions[i]+ "</a>"
		}
		  
		cell = cell +"</div></div>"
	}
	cell = cell + "</td>"
	return cell;
}

function  getModifiedDictionarySearchTerms() {
	var list = [];
	$("#dictionaryTable .highlight-selected").each(function( index ) {
  		list.push( this .innerText );
    });
    return list.join(" ");
}

var dictionaryExpansionData = {}

function getDictionaryExpansion() {
	let searchTerms = $('#searchTerms').val().replace(/\s+/g,' ').trim();
	
	if (searchTerms === "") {
		Snackbar.show({text: "You must enter some search terms to get related searches.", duration: 5000})
		return;
	}
	let targetLanguage = getTargetLanguage()
	let url = openke.global.Common.getRestURLPrefix()+"/searchSession/wordnetDefinition/"+targetLanguage+"/"+searchTerms
	
	
	$(".overlay").show();
    $.ajax({
		type : "GET",
		url : url,
		success : function ( data ) {
			dictionaryExpansionData = data;
			//console.log(JSON.stringify(data,null,4))
			
			$(".overlay").hide();
			
			let message = "";
			let tableBody = "<tbody>";
			let noresults = [];
			
			for (var k=0; k < data.length; k++) {
				let termObject = data[k];
				
				if ( termObject.expansions.length > 0 ) {		    
				    tableBody = tableBody + "<tr>" + createDictionaryExpansionTermCell(k, termObject);
				    for(var i=0; i < termObject.expansions.length; i++){
				    	let expWord = termObject.expansions[i].word;
				    	if ( expWord.toLowerCase() != termObject.term.toLowerCase() ) {
				    		tableBody = tableBody + "<td class='wn-val text-muted' style='cursor: pointer;' data-row='"+k+"' data-item='"+i+"' title='"+termObject.expansions[i].definition+"' ><span class='highlight'>"+expWord+"</span></td>";
				    	}
				    }
				    tableBody = tableBody + "</tr>";
				} else {
					noresults.push(termObject.term);
					tableBody = tableBody + "<tr>" + createDictionaryExpansionTermCell(k, termObject)+"</tr>";
				}
			    
			}
			tableBody = tableBody + "</tbody>";
			message = "<div class='table-responsive'><table class='table dictionaryTable' id='dictionaryTable'>"+tableBody+"</table></div>";
			if(noresults.length > 0){
				message += "<br><footer class='blockquote-footer'>No WordNet related words found for:&nbsp;";
				for(var j=0; j < noresults.length; j++){
					message += noresults[j]+", ";
				}
				message = message.slice(0, -2);
				message += "</footer>";
			}
			
			//create the dialog

			wordnetDialog = bootbox.dialog({
				title: "Related Word Suggestions",
				message: message,
				onEscape: function() {},
				buttons: {
					ok: {
			            label: '<i class="fa fa-check"></i> Apply',
			            className: "btn-outline-primary",
			            callback: function(){
			            	if (getTargetLanguage() == "en") {
			            		$('#searchTerms').val( getModifiedDictionarySearchTerms );
			            	}
			            	else {
			            		$('#searchTermsTranslate').val( getModifiedDictionarySearchTerms );
			            	}
			            }
			        },
			        cancel: {
			            label: '<i class="fa fa-times"></i> Cancel',
			            className: "btn-outline-danger",
			            callback: function(){
			            	///$('#searchTerms').val(text);
			            }
			        }
			    },
			    closeButton: false,
			    className: "bootboxWide"
			});
		},
		error : function(data) {
			$(".overlay").hide();
		},
		dataType : "json",
		contentType : "application/json"
	});
	
}


function getWordNetExpansions(type) {
	//reset the holding list of terms
	$('#wordnetHolder').val('');
	$('#wordnetHolderKey').val('');
	
	var text = $('#searchTerms').val().replace(/\s+/g,' ').trim();
	
	if (text === "") {
		Snackbar.show({text: "You must enter some search terms to get related searches.", duration: 3500})
		return;
	}
	
	var arrText = text.split(' '); //to keep track of order
	var urlwn = openke.global.Common.getRestURLPrefix()+"/searchSession/wordnet/"+type+"/"+text;
	var noresults = [];
	var typeTitle;
	
	$.getJSON(urlwn, function( data ) {
		var message = "";
		var tableBody = "<tbody>";
		
		for (var k=0; k < arrText.length; k++) {
			if (data[arrText[k]]){
				var key = arrText[k];
				var value = data[key];
				$('#wordnetHolderKey').val( $('#wordnetHolderKey').val().replace(/\s+/g,' ').trim()+","+k+"_0_"+key );
			}
			
			if ( JSON.parse(value).length > 0 && JSON.parse(value) != null ) {
				var obj = JSON.parse(value);
			    var words;
			    var arrWords = [];
			    for(i in obj){
			    	words += obj[i].word + ",";
			    	arrWords.push(obj[i].word);
			    }
			    words = words.slice(0, -1);
			    
			    tableBody = tableBody + "<tr><td class='wn-key highlight-selected' style='cursor: pointer;' data-row='"+k+"' data-item='0'><span class='highlight'>"+key+"</span></td>";
			    for(var i=0; i < arrWords.length; i++){
			    	if ( arrWords[i].toLowerCase() != key.toLowerCase() ) {
			    		tableBody = tableBody + "<td class='wn-val text-muted' style='cursor: pointer;' data-row='"+k+"' data-item='"+i+"'><span class='highlight'>"+arrWords[i]+"</span></td>";
			    	}
			    }
			    tableBody = tableBody + "</tr>";
			}else{
				noresults.push(key);
				tableBody = tableBody + "<tr><td class='wn-key highlight-selected' style='cursor: pointer;' data-row='"+k+"' data-item='0'><span class='highlight'>"+key+"</span></td></tr>";
			}
		    
		  }
		
		tableBody = tableBody + "</tbody>";
		message = "<div class='table-responsive'><table class='table wordnetTable' id='wordnetTable'>"+tableBody+"</table></div>";
		if(noresults.length > 0){
			message += "<br><footer class='blockquote-footer'>No related words found for:&nbsp;";
			for(var j=0; j < noresults.length; j++){
				message += noresults[j]+", ";
			}
			message = message.slice(0, -2);
			message += "</footer>";
		}
		
		//create the dialog
		typeTitle = (type === "general" ? 'Generalized' : 'Specialized');
		wordnetDialog = bootbox.dialog({
			title: typeTitle+" Related Word Suggestions",
			message: message,
			onEscape: function() {},
			buttons: {
				ok: {
		            label: '<i class="fa fa-check"></i> Apply',
		            className: "btn-outline-primary",
		            callback: function(){
		            	$('#searchTerms').val( getModifiedSearchTerms );
		            }
		        },
		        cancel: {
		            label: '<i class="fa fa-times"></i> Cancel',
		            className: "btn-outline-danger",
		            callback: function(){
		            	$('#searchTerms').val(text);
		            }
		        }
		    },
		    closeButton: false,
		    className: "bootboxWide"
		});	
		
	});
}


function appendToWordNetHolder($thisobject) {
	var rowWord = $thisobject.data("row")+"_"+$thisobject.data("item")+"_"+$thisobject.text();
	var currentTerms = $('#wordnetHolder').val().replace(/\s+/g,' ').trim();
	
	if ( currentTerms.includes(rowWord) ){
		currentTerms = currentTerms.replace(rowWord, '');
		$('#wordnetHolder').val(currentTerms);
	}else{
		$('#wordnetHolder').val(currentTerms + "," + rowWord);
	}
}

function appendToWordNetHolderKey($thisobject) {
	var rowWord = $thisobject.data("row")+"_"+$thisobject.data("item")+"_"+$thisobject.text();
	var currentTerms = $('#wordnetHolderKey').val().replace(/\s+/g,' ').trim();
	
	if ( currentTerms.includes(rowWord) ){
		currentTerms = currentTerms.replace(rowWord, '');
		$('#wordnetHolderKey').val(currentTerms);
	}else{
		$('#wordnetHolderKey').val(currentTerms + "," + rowWord);
	}
}

function getModifiedSearchTerms() {
	var currentTerms = $('#wordnetHolder').val().replace(/\s+/g,' ').trim().substring(1);
	var currentTermsKey = $('#wordnetHolderKey').val().replace(/\s+/g,' ').trim().substring(1);
	var rawfinalTerms = currentTermsKey + "," + currentTerms;
	var finalTerms = rawfinalTerms.replace(/^,/, '').replace(/,$/, '');
	var ftArray = finalTerms.split(",");

	ftArray = ftArray.sort(compare);
	
	var ft = ftArray.join(' ').replace(/\d_\d_/g, '');
	return ft.trim();
}


function getNLEXModifiedSearchTerms() {
	var currentTerms = $('#nlexHolder').val().replace(/\s+/g,' ').trim().substring(1);
	var currentTermsKey = $('#nlexHolderKey').val().replace(/\s+/g,' ').trim().substring(1);
	var rawfinalTerms = currentTermsKey + "," + currentTerms;
	var finalTerms = rawfinalTerms.replace(/^,/, '').replace(/,$/, '');
	var ftArray = finalTerms.split(",");

	ftArray = ftArray.sort(compare);
	
	var ft = ftArray.join(' ').replace(/\d_\d_/g, '');
	return ft.trim();
}




function compare(a,b){
	return a.localeCompare(b, undefined, { numeric:true });
}



function getNLExpansion(type) {
	//reset the holding list of terms
	$('#nlexHolder').val('');
	$('#nlexHolderKey').val('');
	
	var text = $('#searchTerms').val().replace(/\s+/g,' ').trim();
	
	if (text === "") {
		Snackbar.show({text: "You must enter some search terms to get related searches.", duration: 3500})
		return;
	}
	
	var arrText = text.split(' '); //to keep track of order
	var urlwn = openke.global.Common.getRestURLPrefix()+"/searchSession/nlex/"+type+"/"+text;
	var noresults = [];
	var typeTitle;
	
	$.getJSON(urlwn, function( data ) {
		var message = "";
		var tableBody = "<tbody>";
		
		for (var k=0; k < arrText.length; k++) {
			if (data[arrText[k]]){
				var key = arrText[k];
				var value = data[key];
				$('#nlexHolderKey').val( $('#nlexHolderKey').val().replace(/\s+/g,' ').trim()+","+k+"_0_"+key );
			}
			
			if ( JSON.parse(value).length > 0 && JSON.parse(value) != null ) {
				var obj = JSON.parse(value);
			    var words;
			    var arrWords = [];
			    for(i in obj){
			    	words += obj[i].word + ",";
			    	arrWords.push(obj[i].word);
			    }
			    words = words.slice(0, -1);
			    
			    tableBody = tableBody + "<tr><td class='nlex-key highlight-selected' style='cursor: pointer;' data-row='"+k+"' data-item='0'><span class='highlight'>"+key+"</span></td>";
			    for(var i=0; i < arrWords.length; i++){
			    	if ( arrWords[i].toLowerCase() != key.toLowerCase() ) {
			    		tableBody = tableBody + "<td class='nlex-val text-muted' style='cursor: pointer;' data-row='"+k+"' data-item='"+i+"'><span class='highlight'>"+arrWords[i]+"</span></td>";
			    	}
			    }
			    tableBody = tableBody + "</tr>";
			}else{
				noresults.push(key);
				tableBody = tableBody + "<tr><td class='nlex-key highlight-selected' style='cursor: pointer;' data-row='"+k+"' data-item='0'><span class='highlight'>"+key+"</span></td></tr>";
			}
		    
		  }
		
		tableBody = tableBody + "</tbody>";
		message = "<div class='table-responsive'><table class='table nlexTable' id='nlexTable'>"+tableBody+"</table></div>";
		if(noresults.length > 0){
			message += "<br><footer class='blockquote-footer'>No NL Expansion related words found for:&nbsp;";
			for(var j=0; j < noresults.length; j++){
				message += noresults[j]+", ";
			}
			message = message.slice(0, -2);
			message += "</footer>";
		}
		
		//create the dialog
		typeTitle = (type === "semantic" ? 'Semantic' : 'Syntactic');
		nlexDialog = bootbox.dialog({
			title: typeTitle+" Related Word Suggestions",
			message: message,
			onEscape: function() {},
			buttons: {
				ok: {
		            label: '<i class="fa fa-check"></i> Apply',
		            className: "btn-outline-primary",
		            callback: function(){
		            	$('#searchTerms').val( getNLEXModifiedSearchTerms );
		            }
		        },
		        cancel: {
		            label: '<i class="fa fa-times"></i> Cancel',
		            className: "btn-outline-danger",
		            callback: function(){
		            	$('#searchTerms').val(text);
		            }
		        }
		    },
		    closeButton: false,
		    className: "bootboxWide"
		});	
		
	});
}


function appendToNLEXHolder($thisobject) {
	var rowWord = $thisobject.data("row")+"_"+$thisobject.data("item")+"_"+$thisobject.text();
	var currentTerms = $('#nlexHolder').val().replace(/\s+/g,' ').trim();
	
	if ( currentTerms.includes(rowWord) ){
		currentTerms = currentTerms.replace(rowWord, '');
		$('#nlexHolder').val(currentTerms);
	}else{
		$('#nlexHolder').val(currentTerms + "," + rowWord);
	}
}

function appendToNLEXHolderKey($thisobject) {
	var rowWord = $thisobject.data("row")+"_"+$thisobject.data("item")+"_"+$thisobject.text();
	var currentTerms = $('#nlexHolderKey').val().replace(/\s+/g,' ').trim();
	
	if ( currentTerms.includes(rowWord) ){
		currentTerms = currentTerms.replace(rowWord, '');
		$('#nlexHolderKey').val(currentTerms);
	}else{
		$('#nlexHolderKey').val(currentTerms + "," + rowWord);
	}
}


function openExportDialog(typeFlag) {
	var domainDiscoveryObj = { "sessionID": sessionId }
	
	if (typeFlag === "execution") {
		domainDiscoveryObj["executionNumber"] = enumber;
	}
	
	LASExportDialog.openExportDialog("domainDiscoverySession",{},domainDiscoveryObj);
}



function gotoAnalyze() {
	var sessionIDs = [$("#sessionUUID").val()];
	var sessionNames = [$("#sessionName").val()];
	var cards = [];
	var excludeSessions = [false];
	var keywords = [];
	var crawltime = [];
	var filters = [];
	
	
	var currentFilters = {
			cards: cards,
			sessionIDs: sessionIDs,
			sessionNames: sessionNames,
			excludeSessions: excludeSessions,
			keywords: keywords,
			crawltime: crawltime
	}
	
	var sessFilter = [];
	sessFilter = ElasticSupport.createFilterSession(sessionIDs);
	if ( sessFilter != '' | sessFilter != null){
		filters.push(sessFilter);
	}
	
	openke.model.Analytics.clearAnalyticFilterData();
	openke.model.Analytics.setAnalyticFilterData(currentFilters, filters);
	
	window.location=openke.global.Common.getPageURLPrefix()+"/analyze"
}


function searchAPIChanged() {
	var api= $("#searchAPI").val();
	if (api === "file") {
		$('.noFileUse').hide();
		$('.fileUse').show();
	} else if (api === "weblist") {
		$('.noFileUse').hide();
		$('.fileUse').hide();
		$('.urlListUse').show();
	}
	else {
		$('.noFileUse').show();
		$('.fileUse').hide();
		$('.urlListUse').hide();
		var label = $('#searchAPI option:selected').attr("data-primarylabel");
		$('#searchTermsLabel').text(label+":");
	}
}

/**
 * 
 * @param event  WARNING - this can be null if we call this method when the initial load is compete.
 * @param useDefaultOptions
 * @param numResults
 * @returns
 */
function initiateTopicModelling(event, useDefaultOptions = false, numResults = 20) {
	$('#topicControls').hide();
	//$('#ldaButton').prop("disabled", true);
	
	var dataJSON = { numTopics: $('#numTopics').val(), 
			         stemWords: (document.getElementById('stemWordFlag').checked),
			         relevantFlag: (document.getElementById('relevantFlag').checked),
			         unkrelevantFlag: (document.getElementById('unkrelevantFlag').checked),
			         notrelevantFlag: (document.getElementById('notrelevantFlag').checked),
			         sessionID : sessionId,
			         executionNumber: enumber,
			         defaultOptionsUsed: false
			       };
	
	if (useDefaultOptions) {
		dataJSON.defaultOptionsUsed = true;
		dataJSON.stemWords = false
		dataJSON.numTopics=  4; // numResults / 6; //default to four to simplify
		$("#stemWordFlag").prop('checked', false);
		$('#numTopics').val(dataJSON.numTopics);
	}
	LASLogger.log(LASLogger.LEVEL_INFO, "initiateTopicModelling - default option flag: "+useDefaultOptions);
	
	LASLogger.instrEvent('application.domainDiscoverySession.topicModel - start', dataJSON);
	
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution/"+enumber+"/LDA",
		type: "PUT",
		data:  JSON.stringify( dataJSON),
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			if (data.status === "success") {
				$('#topics').html("<p>Generating topics ....</p>");
				ldaStatusTimer = setInterval(function(){ checkLdaStatus(ldaStatusTimer,data.sessionUUID) }, 5000);
			}
			else {
				bootbox.alert({title :"Error", message: "Unable to initiate topic session: " + JSON.stringify(data,null,4) });
			}
		},
		error: function(data) {
			bootbox.alert({title :"Error", message: "Unable to initiate topic session: " + JSON.stringify(data,null,4) });
	    }
	});
	return false;
	
}

function hideSearchActions() {
	$('.crawlComplete').hide();
}

function showInitialSearchActions() {
	$('.crawlComplete').show();
	$('#switchToListView').hide();
	$('#switchToIndexView').hide();
}

function showViewOnLoad() {
	$("#searchResults").show();
	$("#documentIndexView").hide();
}

function showSearchSwitchAction(actionToShow) {
	if (actionToShow == 'list') {
		$('#switchToListView').show();
		$('#switchToIndexView').hide();
	}
	else if (actionToShow == 'index') {
		$('#switchToListView').hide();
		$('#switchToIndexView').show();
	} 
}

function switchToListView() {
	$("#searchResults").show();
	$("#documentIndexView").hide();
	showSearchSwitchAction("index");
	return false;
}

function switchToIndexView() {
	$("#searchResults").hide();
	$("#documentIndexView").show();
	showSearchSwitchAction("list");
	return false;
}


var displayedRecords = {};

function displayResults(searchResults, searchAPI=""){
	$('#results > tbody').html("");
	displayedRecords = {};
	$.each( searchResults, function( i, value ){
		var resultRow = searchResults[i];
		var uuid = resultRow.uuid;
		
		var title = resultRow.title;
		var url   = resultRow.url;
		var description = resultRow.description;
		
		var sortingLabel = "";
		var neutrality   = "neutral";
		if (resultRow.positionRelativeToPriorExecution < -2000000000) {
			sortingLabel = "";
		}
		else if (resultRow.positionRelativeToPriorExecution > 2000000000) {
			sortingLabel = "New";
			neutrality   = "positive";
		}
		else if (resultRow.positionRelativeToPriorExecution >= 0) {
			sortingLabel = resultRow.positionRelativeToPriorExecution;
			neutrality   = "positive";
		}
		else  if (resultRow.positionRelativeToPriorExecution < 0) {
			sortingLabel = resultRow.positionRelativeToPriorExecution;
			neutrality   = "negative";
		}
		
		var showSource = searchAPI.includes("federated"); // show the source of a result if we are using federated search API
		
		var rec = new openke.component.ResultObject(uuid, title, url, description, {}, true, true, resultRow.source,showSource);

		$('#results > tbody:last-child').append(rec.getRecordDOM());
		rec.displayRecord();
		rec.showSortPosition(sortingLabel,neutrality);
		displayedRecords[uuid] = rec;
	});
}

function displayDroppedResults(searchResults){
	if (searchResults.length ==0) {return;}
	$('#results > tbody:last-child').append("<tr><td colspan=3><hr></td></tr>");
	$.each( searchResults, function( i, value ){
		var resultRow = searchResults[i];
		var uuid = resultRow.uuid;
		
		var title = resultRow.title;
		var url   = resultRow.url;
		var description = resultRow.description;
		
		var rec = new openke.component.ResultObject(uuid, title, url, description, {}, true, true, resultRow.source,false);		
		$('#results > tbody:last-child').append(rec.getRecordDOM());
		rec.displayRecord();
		rec.showSortPosition("---","negative");
		displayedRecords[uuid] = rec;		
	});
}

/* Generate results after clicking search*/
function initiateURLSearch() {
	initiateSearch(null, true)
}
function initiateSearch(event, webListSearch=false) {  //warning - event may be null
	if ($('#sessionName').val().trim() === "") {
		bootbox.alert("You must enter a session name.")
		return;
	}
	if (!webListSearch && $('#searchTerms').val().trim() === "" ) {
		bootbox.alert("You must enter search terms.")
		return;
	}
	if (webListSearch) {
		var urlErrors = validateURLList();
		if (urlErrors != "") {
			bootbox.alert(urlErrors);
			return;
		}
	}
	
	//set global var if target language is not english
	if ( $("#translateCheck").is(":checked")) {
		translateTargetLanguage = $('#targetLanguage :selected').val();
	}
	
	//TODO: Add client-side validation on number of search results: number >0, <= 300
	
	var numResults = $('#numberOfsearchResults').val();
	if (webListSearch) {
		numResults = 1;
	}
	else {
		if (isPositiveInteger(numResults) == false) {
			bootbox.alert("Number of search results must be an integer greater than zero.")
			return;
		}
		numResults = Number.parseInt(numResults)
	}
	var searchAPI        = $("#searchAPI").val();
	var maxSearchResults = $('#searchAPI option:selected').attr("data-maxresults");
	if (searchAPI === null) {
		bootbox.alert("Selected search API is invalid.");
		return;
	}	
	maxSearchResults = Number.parseInt(maxSearchResults)
	
	if (isJSON($("#advConfig").val()) == false) {
		bootbox.alert("Advanced configuration must be a valide JSON object.");
		return;
	}
	
	if (maxSearchResults > 0) {
		if (numResults > maxSearchResults) {
			Snackbar.show({text: searchAPI + " returns a maximum of "+ maxSearchResults +" search results.  Value changed to "+ maxSearchResults +".", duration: 3500})
			$('#numberOfsearchResults').val(maxSearchResults);
			return;
		}
	}
	
	$('#searchButton').hide();
	$('#urlSearchButton').hide();
	hideSearchActions();
	showViewOnLoad();

	clearAllIntervalTimers()
	// clearing all timers instead of specific ones
	//clearInterval(crawlStatusTimer);
	//clearInterval(ldaStatusTimer);
	//clearInterval(secondaryProcessingStatusTimer);
	//clearInterval(executionIndexStatusTimer);
	
	processedUUIDs = {};
	
	$('#topics').html("");
	//$('#topicModelling').css('visibility', 'hidden');
	$('#titleSessionName').text(": "+$('#sessionName').val().trim());
	//$('#sessionName').attr('disabled','disabled');      // 20180830 - allow session names to be changed....  Can't edit the session name once it "begins"
	
	//getting all the user entered data
	var sessionJsonData = {
			searchTerms : $('#searchTerms').val(),
			searchTermsTranslate : $('#searchTermsTranslate').val(),
			translateCheck : $('#translateCheck').is(':checked'),
			targetLanguage : $('#targetLanguage :selected').val(),
			sessionName : $('#sessionName').val(),
			numberOfSearchResults : $('#numberOfsearchResults').val(),
			searchAPI: $("#searchAPI").val(),
			advConfig: JSON.parse($("#advConfig").val())
	}
	
	if (webListSearch) {
		sessionJsonData.urls = createURLListFromTextArea();
	}
		
	if (sessionId === "") {
		//Now all data for session is acquired so update the Domain_Discovery_system table sending via AJAX-PUT 
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/searchSession/",
			type: "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(sessionJsonData),
			dataType : "JSON",
			success: function(data) {
				if (data.status == 'error') { //TODO: need to clean up. Display error mesage on screen. Fix issues with UI disabled etc...
					alert(data.message);
					$('#searchButton').show();
					$('#urlSearchButton').show();
					return;
				}
				$('.sessionExists').show();
				
				sessionId = data.sessionID;

				initiateSessionExecution(sessionId, sessionJsonData, webListSearch);
			}
		});
	}
	else{
		initiateSessionExecution(sessionId, sessionJsonData, webListSearch);
	}
}



function successfulSessionInitiated(data,sessionData, sessionID) {
	enumber = data.executionnumber;
	
	sessionData['sessionID'] = sessionID;
	sessionData['excecutionNumber'] = enumber;	
	
	RecordLevelAnalytics.setDefaultInstrumentationData( {sessionID : sessionId, executionNumber: enumber});			
	LASLogger.instrEvent("application.domainDiscoverySession.startSearch",sessionData);

    dataglobal=data.searchResults;

    displayResults(data.searchResults,data.searchAPI);
	displayDroppedResults(data.droppedResults);
	
	//manipulate our timers, at this point, just check on crawling.  secondary queue and execution discovery index should be stopped
	clearInterval(secondaryProcessingStatusTimer);
	clearInterval(executionIndexStatusTimer);
	crawlStatusTimer = setInterval(function(){ checkCrawlStatus(enumber) }, 5000);
	checkCrawlStatus(enumber);
	
}

function initiateSessionExecution(sessionID, sessionData, webListSearch) {
	var searchResults;
	var documentStatus;
	var executionNumber
	
	$('#stopButton').show();
	$('#stopButton').prop("disabled", false);
	$('#urlStopButton').show();
	$('#urlStopButton').prop("disabled", false);
	enumber = enumber + 1; // temporary hack to prevent undefined number

	var url = openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionID+"/execution/"+translateTargetLanguage
	if (webListSearch) {
		url = openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionID+"/executionByURL"
	}
	
	
	//send the search terms to generate search results
	
	$.ajax({
		url : url,
		type: "POST",
		data: JSON.stringify(sessionData),
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			successfulSessionInitiated(data,sessionData, sessionID);
		},
		error: function (data) {
			bootbox.alert(data.responseJSON.reason);
			$('#stopButton').hide();
			$('#stopButton').prop("disabled", true);
			$('#urlStopButton').hide();
			$('#urlStopButton').prop("disabled", true);
			$('#searchButton').show();
			$('#urlSearchButton').show();
		}
		
	});
	

}

function displayDocumentStatus(searchResults,executionNumber){
	$.each( searchResults, function( i, value ){
		var resultRow = searchResults[i];
		var uuid = resultRow.uuid;
		
		if (uuid in processedUUIDs) {  
			return;
		}
		
		var displayResultObject = displayedRecords[uuid];
		
		if (resultRow.status === "error") {
			processedUUIDs[uuid] = true;
			
			displayResultObject.showMessage("Errored: "+resultRow.errorMessage, true);
		}
		else if (resultRow.status === "crawled") {
			processedUUIDs[uuid] = true;
			
			var recordURL = openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution/"+executionNumber+"/document/"+uuid;
			var additionalData = {
				domain : openke.global.Common.getDomain(), 
				searchSession: sessionId,
				execution: enumber,
				title: resultRow.title,
				storageArea : "sandbox",
				type : "_doc"
			}
			
			var domMenu = OKAnalyticsManager.produceObjectAnalyticsMenu(uuid, recordURL, null,resultRow.url, additionalData,displayResultObject);  //note, not all of these need to be defined.  The called analytic will check
			var collectionDOM = openke.view.DocumentBucketSupport.createCollectionSelect(uuid,displayResultObject)
			displayResultObject.displayMenu(domMenu, resultRow.originalSourceUUID);
			displayResultObject.appendToMenu(collectionDOM);
			displayResultObject.displayRelevancyToggle(resultRow.relevant,updateRelevancyFlag);
			displayResultObject.establishFullTextToggle(recordURL);
			displayResultObject.hideMessage();

			if (typeof(resultRow.source) != 'undefined' &&  typeof(resultRow.source.user_collection) != 'undefined') {
				openke.view.DocumentBucketSupport.populateResultObject(displayResultObject,resultRow.source.user_collection)
			}
			
		}
		else {
			var line = resultRow.status;
			displayResultObject.showMessage(line, false);	
		}
		
	});
}

function documentAddedToBucket(bucketUUID, documentUUID) {
	// minor delay needed to ensure action is handled on the server side
	setTimeout(function() { $.get(openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution/"+enumber+"/document/"+documentUUID+"/updateCache"); }, 1000);
}

function documentRemovedFromBucket(bucketUUID, documentUUID) {
	// minor delay needed to ensure action is handled on the server side
	setTimeout(function() { $.get(openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution/"+enumber+"/document/"+documentUUID+"/updateCache"); }, 1000);
}


var numOfResults;

function checkCrawlStatus(executionNumber) {

	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution/"+executionNumber+"/status",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			if (data.status == "complete") {
				$('#overallStatus').html("Status: complete,  Number of Results: "+data.total+", Pages Retrieved: "+data.successful+", Errors Thrown: "+data.errored+ " Execution Time: " +data.processTime+"ms");
				$('#relevenceLegend').show();
				//$('#ldaButton').prop('disabled', false);
				//$('#topicModelling').css('visibility', 'visible');
				//$('#ldaButton').prop("disabled", false);

                numOfResults = data.total;

				clearInterval(crawlStatusTimer);
				
				$('#stopButton').hide();
				$('#urlStopButton').hide();
				$('#searchButton').show();
				$('#urlSearchButton').show();
				
				showInitialSearchActions();
				//$('#searchResultsExport').show();
				
				if (isRecordInSearchHistoryList(data.executionParameters)) {
					LASLogger.log(LASLogger.LEVEL_WARN, "Execution entry already in list. not initiating topic modelling or secondary processing check.")
					// cancel all interval timers and restoring topic modeling to initial state
					clearAllIntervalTimers();
					//reset LDA/topicmodelling box
					$('#topics > p').html("");
					$('#topControls').show();
					Snackbar.show({text: "Topic modelling and index view creation may not be available. Re-execute search if needed.", duration: 3500})
					
					return;
				}
				appendToSearchHistoryList(data.executionParameters);
				sendInstrumentationEvent("searchComplete");
				
				initiateTopicModelling(null,true, numOfResults);
				
				//TODO.  start checking the secondary processing.
				secondaryProcessingStatusTimer = setInterval(function(){ checkSecondaryProcessingStatus(executionNumber, data.documentIndexID, data.total) }, 5000);
				checkSecondaryProcessingStatus(executionNumber, data.documentIndexID, data.total);
			}
			else if (data.status == "cancelling") {
				$('#overallStatus').html("Status: cancel request received,  Number of Results: "+data.total+", Pages Retrieved: "+data.successful+", Errors Thrown: "+data.errored+ ", Processing: "+ (data.total-data.crawled)+ " Execution Time: " +data.processTime+"ms");				
			}
			else if (data.status == "cancelled") {
				$('#overallStatus').html("Status: cancelled,  Number of Results: "+data.total+", Pages Retrieved: "+data.successful+", Errors Thrown: "+data.errored+ " Execution Time: " +data.processTime+"ms");			
				//$('#ldaButton').prop('disabled', false);
				//$('#topicModelling').css('visibility', 'visible');
				showInitialSearchActions();
				$('#ldaButton').show();
				clearInterval(crawlStatusTimer);
				
				$('#stopButton').hide();
				$('#urlStopButton').hide();
				$('#searchButton').show();	
				$('#urlSearchButton').show(); 
				appendToSearchHistoryList(data.executionParameters);
				sendInstrumentationEvent("searchCancelled - complete");
			}
			else {
				$('#overallStatus').html("Status: crawling,  Number of Results: "+data.total+", Pages Retrieved: "+data.successful+", Errors Thrown: "+data.errored+ ", Processing: "+ (data.total-data.crawled)+ " Execution Time: " +data.processTime+"ms");
				$('#relevenceLegend').show();
			}
			displayDocumentStatus(data.searchResults, data.executionNumber);
		}
	});
}

function checkSecondaryProcessingStatus(executionNumber, documentIndexID, numSearchResults) {

	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution/"+executionNumber+"/secondaryStatus",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			if (data.documents.length > 0) {
				LASLogger.logObject(LASLogger.LEVEL_INFO,data);
			}
			else {
				LASLogger.log(LASLogger.LEVEL_INFO, "Secondary queue finished - created discovery index")
				clearInterval(secondaryProcessingStatusTimer);
				// create discovery index
				showIndexViewSnackbar = true;
				initiateExecutionDiscoveryIndexProcess(documentIndexID,executionNumber,numSearchResults)	
			}
		},
		error: function() {
			LASLogger.log(LASLogger.LEVEL_INFO,"unable to find search execution")
		}
	});
}

function initiateExecutionDiscoveryIndexProcess(documentIndexID,executionNumber,numSearchResults) {
	var executionDiscoveryIndex = new openke.model.DocumentIndex( documentIndexID,"sandbox", "application.domainDiscoverySession.")
	executionDiscoveryIndex.exists(function(result) {
		if (result) { // index alread exists, display it
			executionIndexCompleted(documentIndexID,'sandbox')
		} else {
			var query = {"nested": {
			    "path": "domainDiscovery.retrievals",
			    "query": {"bool": {"must": [
			        {"match": {"domainDiscovery.retrievals.sessionID.raw": sessionId}},
			        {"match": {"domainDiscovery.retrievals.executionNumber": executionNumber}}
			    ]}}
			}}
				
			executionDiscoveryIndex.createIndex( query,$('#sessionName').val(), numSearchResults, executionIndexCompleted)
		}
	});	
}

function executionIndexCompleted(documentIndexID, documentArea) {
	LASLogger.log(LASLogger.LEVEL_INFO, "Ready to display: "+ documentIndexID);

	var x = window.scrollX;
	var y = window.scrollY;
	$("#searchResults").hide();
	$("#documentIndexView").show();  //needs to be visible to layout properly
	openke.view.DocumentIndexView.initialize(documentArea,documentIndexID);
	openke.view.DocumentIndexView.setPopupMenuYOffset(480);
	$("#searchResults").show();
	$("#documentIndexView").hide();
	showSearchSwitchAction('index')
	window.scroll(x,y)
	if (showIndexViewSnackbar) {
		Snackbar.show({text: "Document index view now available", duration: 3500})
	}
	
	return false;
}

function stopSearch(){
	$('#stopButton').prop("disabled", true);
	$('#urlStopButton').prop("disabled", true);
	
	sendInstrumentationEvent("stopSearch");

	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution/"+enumber+"/cancel",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			bootbox.alert("Cancel request sent!")
		},
		error : function(data) {
			$('#stopButton').hide();
			$('#urlStopButton').hide();
			$('#searchButton').show();
			$('#urlSearchButton').show();
			bootbox.alert("Unable to stop: "+JSON.stringify(data));
		},	
	});
	
	
}

function addWordToSearchQuery(word) {
	var text= $('#searchTerms').val() + " " + word;
	$('#searchTerms').val(text);
}

function displayLdaTopics(topics) {
	var topicData;
 	
	var html = "";
	
	var stemmedSearchTerms = {};
	var terms = $('#searchTerms').val().match(/\S+/g) || [];
	for (var i = 0; i < terms.length; i++) {
		stemmedSearchTerms[terms[i].toLowerCase()] = true;
		stemmedSearchTerms[stemmer(terms[i]).toLowerCase()] = true;
	}
		
	$.each( topics, function( i, topicRecord ){
		 var keywords = "";
		 var recordHTML = '<div class="card card-default"><div class="card-header">';
		 for (var j=0;j<topicRecord.keywords.length;j++) {
			 var word = topicRecord.keywords[j];
			 
			 if (word.toLowerCase() in stemmedSearchTerms) {
				 word = "<i>"+word+"</i>"
			 }
			 else {
				 var wordEncoded = encodeSingleQuote(word);
				 word = "<span onclick='addWordToSearchQuery(\""+wordEncoded+"\");'>"+word+"</span>"
			 }
			 
			 keywords += word + "&emsp;";
		 }
		 recordHTML+= keywords + "</div><table class='table'>"
		 var previousUUID = "";
		 for (var j=0;j<topicRecord.documents.length;j++) {
			 var docRecord = topicRecord.documents[j];
			 
			 if (previousUUID == docRecord.documentUUID) { continue;}
			 previousUUID = docRecord.documentUUID;
			 
			 var titleText = docRecord.documentURL
			 if ($("#anchor_"+docRecord.documentUUID).length > 0) {titleText = $("#anchor_"+docRecord.documentUUID).text() }
			
			 var dataJSON = { sessionID : sessionId,
					          executionNumber: enumber,
					          documentUUID:  docRecord.documentUUID,
					          documentURL:   encodeURIComponent(docRecord.documentURL),
					          documentTitle: encodeURIComponent(titleText)};
			 var dataString =  JSON.stringify(dataJSON);
			 
			 var onclickPage = "onclick='LASLogger.instrEvent(\"application.domainDiscoverySession.topicModel.onPage\","+dataString+");' ";
			 var onclickURL = "onclick='LASLogger.instrEvent(\"application.domainDiscoverySession.topicModel.visitURL\","+dataString+");' ";
			 
			 recordHTML += "<tr><td>" + Number(docRecord.score).toFixed(2) + "</td><td>" + titleText +
			                        "<br><a "+onclickPage+" href='#anchor_"+docRecord.documentUUID+"'>On Page</a>&emsp;"+
			                        "<a "+onclickURL+" target=_blank href='"+docRecord.documentURL+"'>Visit</a>"+
			                        "</td></tr>"
		 }		 
		 recordHTML += "</table></div>"
		 html += recordHTML;
	});

	$('#topics').html(html);
}

function checkLdaStatus(ldaStatusTimer, ldaSessionUUID) {
	var ldaStatus = false;
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution/"+enumber+"/LDA/"+ldaSessionUUID,
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			if (data.status === "failure") {
				clearInterval(ldaStatusTimer);
				$('#topics > p').html("Processing failure: "+ JSON.stringify(data.errors));
				$('#topControls').show();
				$('#topicControls').show();
			}
			else if (data.message.endsWith("complete")) {			
				clearInterval(ldaStatusTimer);
				$('#topics > p').html("");
				$('#topControls').show();
				
				sendInstrumentationEvent('topicModel - finished');
				displayLdaTopics(data.topics);
				$('#topicControls').show();
			}
			
		}
	});
}

function updateRelevancyFlag(uuid, relevancyFlag) {
    var relData = { relevant: relevancyFlag };

    var eventDataJSON = { sessionID : sessionId,
        executionNumber: enumber,
        documentUUID:  uuid,
        relevant: relevancyFlag};

    sendInstrumentationEvent("relevancyToggle",eventDataJSON);

    $.ajax({
        url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution/"+enumber+"/document/"+uuid,
        type: "PUT",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(relData),
        dataType : "JSON",
        success: function (data) {
            //TODO, if status = error, need to display a message
        }
    });

    return false;
}

function startNewJob() {
	if ($('#searchTerms').val().trim() === "" || $('#sessionName').val().trim() === "") {
		bootbox.alert("You must enter values for the session name and search terms.");
		return;
	}	
	
	if ($('#searchAPI').val() === null ) {
		bootbox.alert("Selected search API is invalid.");
		return;
	}	
	
	var supportsJob = $('#searchAPI option:selected').attr("data-supportsjob")
	if (supportsJob === "false") {
		bootbox.alert("You cannot create a job from that handler.");
		return;
	}
	bootbox.prompt({
	    title: "Enter the justification for this search job:",
	    inputType: 'textarea',
	    callback: function (result) {
	    	if (result === null) {
	    		return; // user clicked cancel
	    	}
	    	submitNewJob(result);
	    }
	});		
}

function validateURLList() {
	var errors = [];
	var urls  = $('#urlList').val().split("\n")
	var foundURL = false;
	for (var i = 0; i < urls.length; i++) {
		var u = urls[i].trim();
		if (u === "") { continue;}
		try {
			new URL(u);
			foundURL = true;
	    } catch (_) {
	    	errors.push(u);
		}			
	}
	if (errors.length > 0) {
		var errorMessage = "The following URLs are invalid:<ul>";
		for (var i=0; i < errors.length; i++) {
			errorMessage = errorMessage + "<li>" + errors[i];
		}
		errorMessage = errorMessage +"</ul>"
		return errorMessage;
	}
	else if (!foundURL) {
		return "No valid URLs entered";
	}

	else {
		return "";
	}
}

function createURLListFromTextArea() {
	var result = []
	var urls  = $('#urlList').val().split("\n")
	for (var i = 0; i < urls.length; i++) {
		var u = urls[i].trim();
		if (u === "") { continue;}
		try {
			new URL(u);
			result.push(u);
	    } catch (_) {
	    	LASLogger.log(LASLogger.LEVEL_WARN,"Ignoring invalid url: "+u)  
		}			
	}
	return result;
}

function startNewJobFromWebList() {
	if ($('#urlList').val().trim() === "" || $('#sessionName').val().trim() === "") {
		bootbox.alert("You must enter values for the session name and one or more URLS to include in the job.");
		return;
	}	
	
	if ($('#searchAPI').val() !== "weblist" ) {
		bootbox.alert("Selected search API is invalid.");
		return;
	}	
	
	var urlErrors = validateURLList();
	if (urlErrors != "") {
		bootbox.alert(urlErrors);
		return;
	}

	
	bootbox.prompt({
	    title: "Enter the justification for this search job:",
	    inputType: 'textarea',
	    callback: function (result) {
	    	if (result === null) {
	    		return; // user clicked cancel
	    	}
	    	submitNewJob(result);
	    }
	});			
}

function submitNewJob(justificationText) {
	var sourceHandlerName = $("#searchAPI").val();

	$.getJSON(openke.global.Common.getRestURLPrefix()+"/handler/source/" + sourceHandlerName + "/defaultConfig",
		function(result) {
			submitNewJobWithConfigData(result,justificationText)
		});

	return false;
}

function submitNewJobWithConfigData(configurationData, justificationText) {
	var handler = $("#searchAPI").val();
	if (handler === null) {
		bootbox.alert("Selected search API is invalid.");
		return;
	}	

	var maxSearchResults = $('#searchAPI option:selected').attr("data-maxresults");

	var numResults = $('#numberOfsearchResults').val();
	if (isPositiveInteger(numResults) == false) {
		bootbox.alert("Number of search results must be an integer greater than zero.")
		return;
	}
	numResults = Number.parseInt(numResults)
	maxSearchResults = Number.parseInt(maxSearchResults)
	
	maxSearchResults = Math.min(numResults,maxSearchResults);
	
	if (maxSearchResults > 0) {
		configurationData[handler] = {};
		configurationData[handler].length= maxSearchResults;
	}
	var advConfigObj = JSON.parse($("#advConfig").val()) 
	for (var key in advConfigObj) {
		configurationData[handler][key] = advConfigObj[key]
	}
	
	if (handler === "weblist") {
		configurationData.weblist = {}
		configurationData.weblist.seedURLs = createURLListFromTextArea();
	}
	
	var jobData = {
			"name": $('#sessionName').val().trim(),
			"status": "draft",
			"statusTimestamp": "",
			"primaryFieldValue": $('#searchTerms').val().trim(),
			"schedule": JobFactory.getRandomizedStartTime(),
			"priority": "100",
			"configuration": JSON.stringify(configurationData),
			"justification": justificationText,
			"sourceHandler": handler,
			"adjudicationAnswers": []
		};
	
	sendInstrumentationEvent("createjob.",jobData);
	JobFactory.submitNewJobRecordToServer(jobData);
}
    

    
function sendInstrumentationEvent(eventName, eventDataJSON) {
	if (typeof eventDataJSON == "undefined") {
		eventDataJSON = { sessionID : sessionId,  executionNumber: enumber};
	}	
    LASLogger.instrEvent('application.domainDiscoverySession.'+eventName, eventDataJSON);
}

function showPriorExecutions() {
	sendInstrumentationEvent("showPriorExecutions");
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			var dialog = bootbox.dialog({
			    message: '<table id="example" class="display" width="100%"></table>',
			    size: 'large'
			});
			
			var columns   = [ "executionNumber", "searchAPI","numSearchResults","searchTerms", "startTime","endTime"];
			var colLabels = [ "#", "API", "Number of Results", "terms", "Start","End"];
			LASTable.displayJSONInTable(data,colLabels,columns,"example");
		},
		error: function (data) {
			bootbox.alert(data.responseJSON.reason);
		}
	});	
    return false;  	
}

function showPriorExecutionsCSV() {
	sendInstrumentationEvent("showPriorExecutionsAsCSV");
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			var columns   = [ "executionNumber", "searchAPI","numSearchResults","searchTerms", "startTime","endTime"];
			LASExport.exportJSONToCSV(data,"executions",columns);		
		},
		error: function (data) {
			bootbox.alert(data.responseJSON.reason);
		}
	});	
    return false;  	
}

// Adds a button/link to the list of prior executions in the search history table.  Updates the priorExecution json object
function appendToSearchHistoryList(record) {
	priorExecutionData[record.executionNumber] = record;
	
	var title = record.searchAPI+"\n"+ record.searchTerms.replace(/\"/g,"\\\"")+"\nNumber of search results: "+ record.numSearchResults;
	var bt = $('<button class="btn btn-link" data-toggle="tooltop" title="'+title+'" onclick="return loadPriorSearchResult('+record.executionNumber+')">'+record.executionNumber+'</button>')
	$("#executionList").append(bt);
}

// checks to see whether or not the record is present, already.  if so some type of loop has occurred and we need to recover
function isRecordInSearchHistoryList(record) {
	return (typeof priorExecutionData[record.executionNumber] !== 'undefined');
}

function clearAllIntervalTimers() {
    for (var i = setTimeout(function() {}, 0); i > 0; i--) {
	    window.clearInterval(i);
	    window.clearTimeout(i);
    }	
}


function loadPriorSearchResult(execNumber) {
	
	var executionNumber = priorExecutionData[execNumber].executionNumber;
	enumber = executionNumber;
	RecordLevelAnalytics.setDefaultInstrumentationData( {sessionID : sessionId, executionNumber: enumber});

	sendInstrumentationEvent("showPriorSearchExecutionsResults");
	
	$('#searchTerms').val(priorExecutionData[execNumber].searchTerms);
	$('#numberOfsearchResults').val( priorExecutionData[execNumber].numSearchResults);
	$("#searchAPI").val(priorExecutionData[execNumber].searchAPI)
	if (priorExecutionData[execNumber].searchAPI == "weblist") {
		var urlArray = priorExecutionData[execNumber].advancedConfiguration.urls
		$("#urlList").val(urlArray.join("\n"));
		$("#advConfig").val("{}");
	}
	else {
		$("#advConfig").val(JSON.stringify(priorExecutionData[execNumber].advancedConfiguration));
	}
	
	$("#targetLanguage").val(priorExecutionData[execNumber].targetLanguage);
	$("#translateCheck").prop('checked', priorExecutionData[execNumber].translateCheck);
	$('#searchTermsTranslate').val(priorExecutionData[execNumber].searchTermsTranslated);
	
	searchAPIChanged();
	
	var priorDialog = bootbox.dialog({
	    message: 'Loading prior results ...',
	});
	    
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution/"+executionNumber+"/document",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {	
			if (data.documents.length == 0) {
				//priorDialog.modal('hide');
				setTimeout(function() { priorDialog.modal('hide'); }, 500);  //dialog wasn't being hidden without this delay
				bootbox.alert ("No records exist for that search execution.");
				return false;
			}
			
			processedUUIDs = {};  // this is necessary so that the links under the display results will be processed
			
			var results = [];
			var okToSort = true;  // It's possible that records in cache don't have the latest execution #'s.  If this is the case, trust the order returned from the server
			for (var index in data.documents) {
				var record = data.documents[index].domainDiscovery;
				if (typeof(record) === "undefined") { record = {
					title : data.documents[index].html_title,
					url : data.documents[index].url}
				}
				record.uuid = data.documents[index].source_uuid;
				record.originalSourceUUID = data.documents[index].source_uuid;
				record.documentUUID = data.documents[index].source_uuid;
				record.source = data.documents[index]
				
				record.status = "crawled";
				
				if (okToSort) {
					var recordPos = Number.MAX_SAFE_INTEGER;
					for (var index in record.retrievals) {
						if (record.retrievals[index].executionNumber == executionNumber) { recordPos = record.retrievals[index].searchPosition; break;}
					}
					if (recordPos == Number.MAX_SAFE_INTEGER) {
						okToSort=false;
					}
				}
				
				results.push(record);
			}
			
			if (okToSort) {
				results.sort(function(a,b) {
					var aPos = Number.MAX_SAFE_INTEGER;
					var bPos = Number.MAX_SAFE_INTEGER;
					for (var index in a.retrievals) {
						if (a.retrievals[index].executionNumber == executionNumber) { aPos = a.retrievals[index].searchPosition; break;}
					}
					for (var index in b.retrievals) {
						if (b.retrievals[index].executionNumber == executionNumber) { bPos = b.retrievals[index].searchPosition; break;}
					}
					
					return (aPos > bPos) ? 1 : ((bPos > aPos) ? -1 : 0);
					
				} );
			}
			
			displayResults(results);
			displayDocumentStatus (results,executionNumber);
			
			$('#overallStatus').html("Status: Loaded from prior search result (execution  #"+executionNumber+")");			
			$('#relevenceLegend').show();
			$('#searchResultStatistics').show();
			
			$('#topics').html("");
			showInitialSearchActions();
			showViewOnLoad();
			switchTranslateSelect();
			
			showIndexViewSnackbar = false;
			initiateExecutionDiscoveryIndexProcess(data.execution.documentIndexID,executionNumber,data.execution.numSearchResults)
			
			setTimeout(function() { priorDialog.modal('hide'); }, 500); // closure wasn't occuring under Bootstrap 4 without the delay
		},
		error: function (data) {
			priorDialog.modal('hide');
			bootbox.alert(data.responseJSON.reason);
		}
	});	
	
	return false;
}


function showResultsStatistics() {
	sendInstrumentationEvent("searchResultStatistics");
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution/"+enumber+"/summary",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			var columns = [ "title","url","description","publishDate","resultPosition","textLength","textMinimizedLength","totalOutgoingLinks","totalOutgoingLinksDifferentDomain","numConcepts"];
			
			LASExport.exportJSONToCSV(data.documentSummary,openke.global.Common.getDomain()+" " +$('#sessionName').val() +" "+ enumber +" result statistic" ,columns);
		},
		error: function (data) {
			bootbox.alert(data.responseJSON.reason);
		}
	});	
    return false;    
}

function searchOutboundLinks(uuid, fullURL, jsonObject, sourceURL) {
	var dataJSON = { sessionID : sessionId,
            executionNumber: enumber,
            documentUUID:  uuid};
	sendInstrumentationEvent("result.searchOutboundLinks",dataJSON);	
	
	var searchResults;
	var documentStatus;
	var executionNumber
	
	$('#stopButton').show();
	$('#stopButton').prop("disabled", false);
	$('#urlStopButton').show();
	$('#urlStopButton').prop("disabled", false);
	$('#searchButton').hide();
	$('#urlSearchButton').hide();
	hideSearchActions();
		

	// sets the URL and API for the query area
	$('#searchTerms').val(sourceURL);
	$("#searchAPI").val("custom");
	
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution/"+enumber+"/document/"+uuid+"/outbound",
		type: "POST",
		data: JSON.stringify(dataJSON),
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			enumber = data.executionnumber;
			RecordLevelAnalytics.setDefaultInstrumentationData( {sessionID : sessionId, executionNumber: enumber});

			
			dataJSON['excecutionNumber'] = enumber;			
			LASLogger.instrEvent("application.domainDiscoverySession.startURLCrawl",dataJSON);

			displayResults(data.searchResults);
			crawlStatusTimer = setInterval(function(){ checkCrawlStatus(enumber) }, 5000);
			checkCrawlStatus(enumber);
		},
		error: function (data) {
			bootbox.alert(data.responseJSON.reason);
			$('#stopButton').hide();
			$('#stopButton').prop("disabled", true);
			$('#urlStopButton').hide();
			$('#urlStopButton').prop("disabled", true);			
			$('#searchButton').show();
			$('#urlSearchButton').show();
		}
	});
}

/** 
 This function will call the Azure auto complete API to get a list of additional search terms a user may want to apply
 */
function initiateSuggest() {
	alert("Bing/Azure autocomplete disabled");
	/*
	if ($("#searchTerms").val().trim() === "") {
		bootbox.alert("You must enter a search term to see suggestions.")
		return 
	}
	
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/suggest?query="+$("#searchTerms").val(),
		type: "GET",
		success: function(data) {
			    var message = "";
				var groups = data.suggestionGroups;
				for (var i=0;i < groups.length; i++) {
					var suggestArray = data.suggestionGroups[i].searchSuggestions;
					for (var j=0; j < suggestArray.length; j++) {
						var text = suggestArray[j].displayText;
						message = message + "<a href='javascript:$(\"#searchTerms\").val(\""+text+"\");suggestDialog.hide()'>"+text + "</a><br>";
					}
				}
				suggestDialog = bootbox.dialog({
					//size: "large",
					title: "Search Suggestions",
					message: message,
					onEscape: function() {},
				});	
			},
		error: function (data) {
			bootbox.alert(data.responseJSON.reason);
		}
	});	
	*/
}

/**
 Uses a the GoogleHandler to get possible Google Searches 
 */
function initiateRelatedGoogleSearches() {
	if ($("#searchTerms").val().trim() === "") {
		Snackbar.show({text: "You must enter some search terms to get related searches.", duration: 3500})
		return 
	}
	
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/googleRelated?query="+$("#searchTerms").val(),
		type: "GET",
		success: function(data) {
				var message = "";
				var searchItems = data.relatedSearches;
				for (var i=0;i < searchItems.length; i++) {
					var text = searchItems[i];
					message = message + "<a href='javascript:$(\"#searchTerms\").val(\""+text+"\");suggestDialog.modal(\"hide\")'>"+text + "</a><br>";
				}
				if (message == "") {
					message = "Error: Unable to retrieve related searches"
				}
				suggestDialog = bootbox.dialog({
					//size: "large",
					title: "Google Related Search Suggestions",
					message: message,
					onEscape: function() {},
				});	
			},
		error: function (data) {
			bootbox.alert(data.responseJSON.reason);
		}
	});
}


var cronField;
function createCronWidget(value) {
	cronField = $('#searchAlertSchedule').cron({
		    initial: value,
		    customValues: {
		        "2 hours" : "0 0 */2 * * ?",
		        "4 hours" : "0 0 */4 * * ?",
		        "8 hours" : "0 0 */8 * * ?",
		        "12 hours" : "0 0 */12 * * ?",
		        "2 days at 0700" : "0 0 7 */2 * ?"
		    },
		    useGentleSelect: true,
		    effectOpts: {
		        openSpeed: 200,
		        closeSpeed: 200
		    }
		});			
}

function startNewSearchAlert() {
	var formHTML = "<div><div class='myLine'>Name: <input type='text' size=60 maxlength=256 id='searchAlertName'></div><div class='myLine'> Schedule: <div id='searchAlertSchedule'></div></div><div class='myLine'>Acknowledge initial results: <input id='preacknowledge' checked type='checkbox' value='true'></div></div><script>createCronWidget('0 0 */8 * * ?'); $('#searchAlertName').val($('#sessionName').val()+':'+ $('#searchAPI').val());</script>"
	var recordDOM  = $(formHTML);
	
	bootbox.confirm({
	    title: "Create Search Alert",
	    message: recordDOM,
	    buttons: {
	        cancel: {
	            label: '<i class="fa fa-times"></i> Cancel'
	        },
	        confirm: {
	            label: '<i class="fa fa-check"></i> Create'
	        }
	    },
	    callback: function (result) {
	    	if (result) {
	    		if ($("#searchAlertName").val().trim() === "") {
	    			bootbox.alert("You must enter a search alert name.")
	    			return 
	    		}	 
	    		if ($("#searchAlertName").val().trim().length > 255) {
	    			bootbox.alert("The search alert name must be less than 256 characters.")
	    			return 
	    		}	 
	    		
	    	    var alertPost = {
	    	            alertName : document.getElementById("searchAlertName").value,
	    	            searchTerm : document.getElementById("searchTerms").value,
	    	            cronSchedule : cronField.cron("value"),
	    	            numberOfSearchResults : $('#numberOfsearchResults').val(),
	    	    		sourceHandler : $("#searchAPI").val(),
	    	    		preacknowledge: $("#preacknowledge").is(":checked")
	    	        };

	    	    $.ajax({
	    	            contentType: "application/json; charset=utf-8",
	    	            type : "POST",
	    	            url : openke.global.Common.getRestURLPrefix()+"/searchAlert",
	    	            data : JSON.stringify(alertPost),
	    	            dataType: 'json',
	    	            success: function(s){
	    	                if (s.status==="success") {
	    	                	alertPost.alertID = s.alertID;
	    	                	LASLogger.instrEvent('application.alertNotification.searchAlertCcreated', alertPost);
	    	                	bootbox.alert("Search alert created")
	    	                }
	    	                else {
	    	                	bootbox.alert("Unable to create search alert")
	    	                }
	    	                return;
	    	            },
	    	            error: function(e){
	    	            	console.log(e.status);
	    	                bootbox.alert("Unable to create search alert")
	    	    			return 
	    	            }
	    	        });	    		
	    		
	    	}
	    }
	});
}


