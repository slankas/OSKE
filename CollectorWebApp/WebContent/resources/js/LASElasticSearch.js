LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

var searchURL = openke.global.Common.getRestURLPrefix()+"/search"; // Search url on server.

var searchResult = new Object(); // Individual search results keyed on id.

var searchResultJSON; // Raw search json.

var startDate; // Filter: crawl start and end date
var endDate;

var newSearch = false; // Do not populate paginator if new page is false.

var resultSize = 20;              // how many results should be shown on each page?
var MAX_RESULTS_TO_ALLOW = 10000; // how many results can the user possibly see?
var savedQuery = {};              // this is the current query for the search results.  needs to be saved as users can alter criteria.

// options is the parameter used for the paginator
var options = {
	size : 'normal',
	alignment : 'center',
	currentPage : 1,
	totalPages : 10,
	numberOfPages : 10,
	onPageClicked : function(e, originalEvent, type, page) {
		LASLogger.instrEvent('application.search.paging', {
			searchtext : $('#txtSearchQuery').val(),
			criteria : savedQuery,
			page_number : page
		});

		savedQuery.from = (page - 1) * resultSize;
		getElasticSearchData(savedQuery);
	}
}

var uuidRegEx = /^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}_/;

function clearSearchFilters() {
	$('#txtDomain').val("");
	$('#startTime').val("");
	$('#endTime').val("");
	
	clearFieldsTable();
}

$(document).ready(function() {
	window.page = "search"; // Page name to be stored in window object so that LasHeader.js could access it for logging

	window.setQueryText = function(text) {
		$("#txtSearchQuery").val(text);
	}

	$('#ecFilter').slideToggle();
	 
	$("#ehFilter").click(function () {
	    $('#ecFilter').slideToggle('500');
	    $(this).find('i').toggleClass('fa-angle-right fa-angle-down');
	});

	$('#txtSearchQuery, #txtDomain, #startTime, #endTime').bind("keyup", function(event) {
		if (event.keyCode == 13) {
			elasticSearch();
			return false;
		}
	});
		
	LASLogger.instrEvent('application.search');
	OKAnalyticsManager.defineStandardDocumentMenu("application.search.",true);
	openke.model.DocumentBucket.setDefaultInstrumentationPage('application.search');
	openke.view.DocumentBucketSupport.setDefaultInstrumentationPage('application.search');
	$('#btAddConcept').click(showConceptPopup)
	
	
	document.getElementById("err_label").style.display = "none";
	$('#searchResultsExport').hide();
	$('#btCreateIndex').hide();
	$('#summarizeDocuments').hide();
    $('#pager').hide();
	$("#startTime").datetimepicker({
		 format : 'Y-m-d H:i:s',
		 onChangeDateTime:function(ct,$i){
			 if (ct == null) {
				 startDate = null;
			 } else {
				 startDate = ct.getTime() - new Date().getTimezoneOffset() * 60000;
			 }
		}
	});
	$("#endTime").datetimepicker({
		 format : 'Y-m-d H:i:s',
		 onChangeDateTime:function(ct,$i){
			 if (ct == null) {
				 endDate = null; 
			 } else {
				 endDate = ct.getTime() - new Date().getTimezoneOffset() * 60000;
			 }
		}
	});
	
	$("#btCreateIndex").on('click', createDocumentIndex);
	$("#searchResultsExport").on('click', function (){ LASExportDialog.openExportDialog("search",savedQuery, {}); });
	
	$.getJSON(searchURL + "/mapping", function(data) { // nest the remain functionality to make search the filter drop-down is populated with the mappings
		refreshMapping(data);
	
		openke.model.DocumentBucket.loadAll();
		
		$(".chosen-select").chosen();
		$(".chosen-container").width("250px"); // While cloning a chosen-select, a new chosen-container is created but its width was not set.

		// code to auto-resize filter input fileds
		$('.textbox-autosize').on('keyup', autoResizeTextInput);
		
		
		
		if ($('#txtSearchQuery').val() != null && $('#txtSearchQuery').val() != "") {
			elasticSearch();
		}
		else if ($('#queryObject').val() != "" ) {
			var passedQuery = JSON.parse(atob($('#queryObject').val()));
			
			if (passedQuery.hasOwnProperty("filterField")) {
				
		    	$('#txtSearchQuery').val(passedQuery.query)
		    	$('input[name=radScope][value='+passedQuery.scope+']').attr('checked', true); 
		    	$("#sortField").val(passedQuery.sortField);
		    	$('input[name=sortOrder][value='+passedQuery.sortOrder+']').attr('checked',true);   
		    	$("#drpdnSearchType").val(passedQuery.searchType);	
		    	
		    	if (passedQuery.searchType == 'keyword') {
		    		$("#drpdnSearchType").val("prefix")
		    		$('#useKeyword').attr('checked',true);  
		    	}
		    	
		    	if (passedQuery.filterField != "") {
		    		$("#drpdnSearchField").chosen().val( passedQuery.filterField).trigger("chosen:updated");
		    		$("#txtSearchField").val(passedQuery.filterValue);
		    	}
		    	
		    	if (passedQuery.hasOwnProperty("startDate") && passedQuery.startDate != null) {
		    		var startDate = new Date(passedQuery.startDate + new Date().getTimezoneOffset() * 60000);	
		    		$("#startTime").datetimepicker({value: startDate});
		    	}
		    	if (passedQuery.hasOwnProperty("endDate") && passedQuery.endDate != null) { 
					var endDate = new Date(passedQuery.endDate + new Date().getTimezoneOffset() * 60000);
		    		$("#endTime").datetimepicker({  value: endDate });
		    	}
		    	
		    	if ( ($('#txtSearchQuery').val() != null && $('#txtSearchQuery').val() != "") || passedQuery.filterField != "") {
		    		elasticSearch();
		    	}
			}
			else {
				var startQueryStr = atob($('#queryObject').val())
				var startQuery = JSON.parse(startQueryStr);
				startQuery.highlight = { "fields": {"text": {"number_of_fragments": 1, "fragment_size": 400 } } }
				startQuery.sort = [    { "_score": { "order": "desc" }}   ]
				console.log(JSON.stringify(startQuery));
				savedQuery = startQuery;

				newSearch = true;
				$('#pager').hide();

				LASLogger.instrEvent('application.search.formsearch.submittedQuery', {
					searchtext : $('#txtSearchQuery').val(),
					criteria : startQuery
				});

				getElasticSearchData(savedQuery);				
			}
			
		}
	});


});




function showConceptPopup() {
	LASLogger.instrEvent('application.search.show_concept_popup');

	//e.preventDefault();
	var screen_width = 700; //screen.width * .8;
    var screen_height = 500; //screen.height* .7;
    var top_loc = screen.height *.15;
    var left_loc = screen.width *.1;

    var newWin = window.open(openke.global.Common.getPageURLPrefix()+"/conceptSelector",'_blank','toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);
    newWin.focus();	
}

function conceptShiftSearchCalled(searchObject) {
	if (searchObject.filterField != "") {
	
		/*
		var searchObject = {
			    "serchText" : "",
			    "scope"     : "text",
			    "sortField" : "relevance",
			    "sortOrder" : "desc",
			    "searchType": "keyword",
			    "filterField" : "concepts.fullName",
			    "filterValue" : prefix
			}
		*/
		var tblSearchFields = document.getElementById('tblSearchFields');
		var foundRowNumber = 1;
		var foundRow = false;
		for (var foundRowNumber = 1; foundRowNumber < tblSearchFields.rows.length; foundRowNumber++) {
			var field           = tblSearchFields.rows[foundRowNumber].cells[0].getElementsByTagName('select')[0].value;
			var fieldSearchType = tblSearchFields.rows[foundRowNumber].cells[2].getElementsByTagName('select')[0].value;
			var fieldVal        = tblSearchFields.rows[foundRowNumber].cells[3].getElementsByTagName('input')[0].value;
	
			if ((field && fieldVal) || ( field && (fieldSearchType == "exists" || fieldSearchType == "not exists")  )) {
				continue;
			}
			foundRow = true;
			break;
		}
		if (foundRow == false) {  // need to add another row for concepts, no need to increment rowNumber as the for loop put us at the end
			insertRow(null);
		}	
		
		// store the search object

		$(tblSearchFields.rows[foundRowNumber].cells[0].getElementsByTagName('select')[0]).chosen().val( searchObject.filterField).trigger("chosen:updated");
		$(tblSearchFields.rows[foundRowNumber].cells[3].getElementsByTagName('input')[0]).val(searchObject.filterValue);
	}
}


function autoResizeTextInput(e) {
	// Add an arbitary buffer of 15 pixels.
    var whitespaceBuffer = 15;
    var je = $(this);
    var minWidth = parseInt(je.attr('data-min-width'));
    var newVal = je.val();
    var sizingSpanClass = 'invisible-autosize-helper';
    var $span = je.siblings('span.' + sizingSpanClass).first();
    // If this element hasn't been created yet, we'll create it now.
    if ($span.length === 0) {
        $span = $('<span/>', {
            'class': sizingSpanClass,
            'style': 'display: none;'
        });
        je.parent().append($span);
    }
    $span = je.siblings('span').first();
    $span.text(newVal) ; // the hidden span takes 
    // the value of the input
    $inputSize = $span.width();
    $inputSize += whitespaceBuffer;
    if($inputSize > minWidth)
        je.css("width", $inputSize) ; // apply width of the span to the input
    else
        je.css("width", minWidth) ; // Ensure we're at the min width
}


var nestedElements = [];  // nested elements must be treated differently when creating the search queries.

function refreshMapping(mapping) {
	nestedElements = [];  // reset the list of "nested" items

	var database = Object.keys(mapping)[0];
	
	var webProperties = mapping[database].mappings.properties;  // this is a JSON object for the properties under "web"  TODO!!!!!S
	
	refreshMappingProperties(webProperties,"",false);
	
	$('.chosen-select').trigger("chosen:updated");
}

function getMappingFieldFullName(prefix, name) {
	return prefix == "" ? name : prefix +"." + name;
}

function refreshMappingProperties(levelProperties, prefix, parentNested) {
	var keys = Object.keys(levelProperties);

	for (var i = 0; i < keys.length; i++) { // loop through all of the fields in "web"
		// check if this type is nested, if so track it in nestedElements
		var nestedElement = parentNested
		if (parentNested === false && levelProperties[keys[i]].type) {
			if (levelProperties[keys[i]].type === "nested") {
				nestedElement = true;
			}
		}
		
		if (levelProperties[keys[i]].properties) { // if the field has subfields, loop through those ...
			refreshMappingProperties(levelProperties[keys[i]].properties, getMappingFieldFullName(prefix,keys[i]),nestedElement  );
		} 
		else {		
			var option = $('<option value="' + getMappingFieldFullName(prefix,keys[i])  + '"></option>').text(getMappingFieldFullName(prefix,keys[i]));
			$('#drpdnSearchField').append(option);
			if (nestedElement) {
				nestedElements.push(getMappingFieldFullName(prefix,keys[i]));
			}
		}
	}	
	
	
	
}




function validateCrawlDates() {
	if (endDate < startDate) {
		document.getElementById("err_label").style.display = "inline";
		document.getElementById("err_label").innerHTML = "End date is before start date";
		
		return false;
	}
	
	return true;
}

function elasticSearch() {	
	
	var queryType = "match";
	
	if($("#drpdnSearchType").val() == 'regex') {
		queryType = "regexp";
	} else if ($("#drpdnSearchType").val() == 'exactPhrase') {
		queryType = "match_phrase";
	}
	
	var queryScope = $('input[name=radScope]:checked').val();

	if (searchURL == null || searchURL == "") {
		return;
	} 
	
	if(!validateCrawlDates()) {
		return;
	}

	var sQuery = {
			"highlight": {
			    "fields": {
			      "text": {
			        "number_of_fragments": 1,
			        "fragment_size": 400
			      }
			    }
			  },
			  "from" : 0,
			  "size" : resultSize,
			  "query": {
				    "bool": {
				      "must": {},
				      "filter": {
				        "bool": {
				          "must"     : [],
				          "should"   : [],
				          "must_not" : []
				        }
				      }
				    }
			  }
	};
	
	if ($('#txtSearchQuery').val().trim() === "*" || $('#txtSearchQuery').val().trim() === "") {
		sQuery.query.bool.must = { "match_all": {}};
	} else {
		sQuery.query.bool.must[queryType] = {};
		sQuery.query.bool.must[queryType][queryScope] = $('#txtSearchQuery').val();
	}
	
	var sortField = $("#sortField").val();
	var sortOrder = $("input[name='sortOrder']:checked").val();
	
	if (sortField === "crawled_dt" || sortField === "text_length" || sortField === "html_title.keyword" || sortField ===  "published_date.date") {
		var sortPart = {};
		sortPart[sortField] = { "order": sortOrder}
		sQuery.sort = [  ]
		sQuery.sort.push(sortPart)
	}
	else if (sortField === "relevance" && sortOrder === "asc") {
		sQuery.sort = [    { "_score": { "order": "asc" }}   ]
	}
	else if (sortField === "relevance") {
		sQuery.sort = [    { "_score": { "order": "desc" }}   ]
	}
	
	if ($("#txtDomain").val() != null && $("#txtDomain").val().trim() != '') {	
		sQuery.query.bool.filter.bool.must.push( { "term" : { "domain.keyword" : $("#txtDomain").val().trim() } } );
	}
	
	var startDateTime = $( "#startTime").val();
	if (startDateTime != null && startDateTime.trim() != '') {
		sQuery.query.bool.filter.bool.must.push({ "range": { "crawled_dt": { "gte": startDate } } });
	}
	
	var endDateTime = $( "#endTime").val();
	if (endDateTime != null && endDateTime.trim() != '') {
		sQuery.query.bool.filter.bool.must.push({ "range": { "crawled_dt": { "lte": endDate } } });
	}	
	
	var tblSearchFields = document.getElementById('tblSearchFields');

	for (var i = 1; i < tblSearchFields.rows.length; i++) {
		var field           = tblSearchFields.rows[i].cells[0].getElementsByTagName('select')[0].value;
		var useKeyfield     = tblSearchFields.rows[i].cells[1].getElementsByTagName('input')[0].checked;
		var fieldSearchType = tblSearchFields.rows[i].cells[2].getElementsByTagName('select')[0].value;
		var fieldVal        = tblSearchFields.rows[i].cells[3].getElementsByTagName('input')[0].value;
		var boolOpr         = tblSearchFields.rows[i].cells[4].getElementsByTagName('select')[0].value;
		

		if ((field && fieldVal) || ( field && (fieldSearchType == "exists" || fieldSearchType == "not exists")  )) {
			var isNested = (nestedElements.indexOf(field) > -1); 

			if (useKeyfield) {
				field += ".keyword"
			}
			
			// Build query predicate
			var queryData = {};
			if (fieldSearchType.startsWith("range_")) {
				queryData['range'] = {};
				queryData['range'][field] = {};
				queryData['range'][field][fieldSearchType.substring(6)] = fieldVal;
			} 
			else if (fieldSearchType == "exists" || fieldSearchType == "not exists") {
				queryData = { "exists" : {"field": field }};
			}
			else {
				queryData[fieldSearchType] = {};
				queryData[fieldSearchType][field] = fieldVal;
			}
			
			// Pack into nested structure as necessary
			var subQuery;
			if (isNested) {
				var searchPart = {}
				searchPart[field] = fieldVal;
				
				var searchType = {}
				searchType[fieldSearchType] = searchPart
		
				subQuery = {
				          "nested": {
				              "path": field.split(".")[0],
				              "score_mode": "max",
				              "query": {
				                "bool": {
				                  "must": [queryData ]
				                }
				              }
				            }
				          }
			}
			else {
				subQuery = queryData;
			}
			
			// Append to filter in the right place
			if (boolOpr == 'andnot' || fieldSearchType == "not exists") {
				sQuery.query.bool.filter.bool.must_not.push(subQuery);			
			} else if (boolOpr == 'and') {
				sQuery.query.bool.filter.bool.must.push(subQuery);
			} else if (boolOpr == 'or') {
				sQuery.query.bool.filter.bool.should.push(subQuery);
			} 
		}	
	}
	
	console.log(JSON.stringify(sQuery));
	savedQuery = sQuery;

	newSearch = true;
	$('#pager').hide();

	LASLogger.instrEvent('application.search.formsearch', {
		searchtext : $('#txtSearchQuery').val(),
		criteria : sQuery
	});


	getElasticSearchData(savedQuery);
}

function getElasticSearchData(queryJSON) {
	$(".overlay").show();
    $.ajax({
		type : "POST",
		url : searchURL,
		data : JSON.stringify(queryJSON),
		success : populateSearchResults,
		error : function(data) {
			$(".overlay").hide();
	 		document.getElementById("err_label").style.display = "inline";
			document.getElementById("err_label").innerHTML = data.responseText;
		},
		dataType : "json",
		contentType : "application/json"
	});

}

function summarizeAllDocuments() {
	LASLogger.instrEvent('application.search.summarizeAllDocuments');

	var text = "";	
	for(var id in searchResult) {
		if (id.length > 10 ) {
			text  += "\n" + searchResult[id]._source.text;
		}
	}

	text = text.replace(/[^\x00-\x7F]/g, "").replace(/[\&]+/g, "")

	$("#summarizeTextField").val(text);
	$("#summarizeForm").submit();
	return false;
	
}


// from a given field, pulls out that value from the record and returns it
function extractSortValue(sortField, record) {
	var result = "";
	if (sortField === "crawled_dt" || sortField === "text_length") {
		result = record._source[sortField]; 
	}
	else if (sortField === "html_title.keyword") {
		result = "";
	}
	else if (sortField ===  "published_date.date") {
		result = record._source.published_date.date;
	}
	else {
		result = new Number(record._score);
		result = result.toFixed(3);
	}
	
	return result;
}

function extractSortKeyFromSavedQuery() {
	var sorted = savedQuery.sort[0];
	var sortkey, sortedkeys = [];
	for (sortkey in sorted) {
	  if (sorted.hasOwnProperty(sortkey))
		  sortedkeys.push(sortkey)
	}
	var actualSortKey = sortedkeys[0]; // will need to change this if we go to multiple sort keys
	return actualSortKey;
}

function populateSearchResults(data) {
	document.getElementById("err_label").style.display = "none";
	$(".overlay").hide();
	
	if (data.hasOwnProperty('_shards') && data._shards.hasOwnProperty('failures') &&
	    data._shards.failures.length > 0 &&
	    data._shards.failures[0].reason.reason.includes('highlighting') ) {
		var queryCopy = Object.assign({}, savedQuery);
		delete queryCopy.highlight
		getElasticSearchData(queryCopy)
		return;
	}
	
	
	var jsonData = data;
	searchResultJSON = data;
	
	if (data.error) {
		$('#tblSearchResults').empty();
		$('#tblSearchResults').append('<tr><td><div><label>Unable to execute query</label></div></td></tr>')
		$('#searchResultsHeading').text("Search Results: None");
		$('#searchResultsExport').hide();
		$('#btCreateIndex').hide();
		$('#summarizeDocuments').hide();	
		
		return;
	}
	
	var actualSortKey = extractSortKeyFromSavedQuery();

	searchResult['took'] = jsonData.took;
	searchResult['timed_out'] = jsonData.timed_out;
	searchResult['shards'] = jsonData.shards;
	searchResult['total_hits'] = jsonData.hits.total.value;
	searchResult['max_score'] = jsonData.hits.max_score;
		
	$('#tblSearchResults').empty();

	if (jsonData.hits.total.value == 0) {
		var noHits= "<label'>" + "Your search - " + "<em>" +$('#txtSearchQuery').val() + "</em>" + " - did not match any documents." + "</label>";
		var suggestion = "<p style='margin-top:1em'>Suggestions:</p>";
		var bulletPonits = "<ul style='margin-left:1.3em;margin-bottom:2em'>" + 
								"<li>Make sure all words are spelled correctly.</li>" + 
								"<li>Try different keywords.</li>" +
								"<li>Try more general keywords.</li>" + 
							"</ul>";
		
		$('#tblSearchResults').append('<tr><td><div>' + noHits + "<br>" + suggestion + bulletPonits +  '</div></td></tr>')
		$('#searchResultsHeading').text("Search Results: None");
		$('#searchResultsExport').hide();
		$('#btCreateIndex').hide();
		$('#summarizeDocuments').hide();
	}
	else {
		var startNum = savedQuery.from + 1;
		var endNum   = savedQuery.from + resultSize;
		if (endNum > jsonData.hits.total.value) { endNum = jsonData.hits.total.value }
		if (jsonData.hits.total.relation === "gte") {
			$('#searchResultsHeading').text("Search Results: Showing "+ startNum+" to "+ endNum+" of "+jsonData.hits.total.value+"+");
		}
		else {
			$('#searchResultsHeading').text("Search Results: Showing "+ startNum+" to "+ endNum+" of "+jsonData.hits.total.value);
		}
		$('#searchResultsExport').show();
		$('#btCreateIndex').show();
		//$('#summarizeDocuments').show();  problems with submitting larger documents...
	}

	for (var i = 0; i < jsonData.hits.hits.length; i++) {	
		searchResult[jsonData.hits.hits[i]._id] = jsonData.hits.hits[i];

		// Not all of the HTML attributes are being removed from the text.  This encodes any HTML special characters
		var tempText = jsonData.hits.hits[i]._source.text.substring(0,400);
		if (jsonData.hits.hits[i].hasOwnProperty('highlight')) {
			tempText = jsonData.hits.hits[i].highlight.text[0];
		}
		tempText = escapeHtml(tempText);
		tempText = tempText.split("&lt;em&gt;").join("<em>");  
		tempText = tempText.split("&lt;/em&gt;").join("</em>"); 
			
		var title = "";
		var url = ""; 
		var sortValue = extractSortValue(actualSortKey,jsonData.hits.hits[i]);
		var uuid = jsonData.hits.hits[i]._id;
		
		if (jsonData.hits.hits[i]._source.url.startsWith("file:")) {
			var u = jsonData.hits.hits[i]._source.url;
			var n = u.lastIndexOf("/");
			if (n > -1) {
				u = u.substring(n+1)
			}
			u = u.replace(uuidRegEx,"");
			url = "";
			if (jsonData.hits.hits[i]._source.html_title != null) {
				title = jsonData.hits.hits[i]._source.html_title ;
			}
			else {
				title = u;
			}
		}
		else {
			url =  jsonData.hits.hits[i]._source.url 
			if (jsonData.hits.hits[i]._source.html_title != null) {
				title = jsonData.hits.hits[i]._source.html_title ;
			} else {
				var a = document.createElement("a");
				a.href = jsonData.hits.hits[i]._source.url;
				title = a.hostname;
			}
		}
		
		
		var rec = new openke.component.ResultObject(uuid, title, url, tempText, jsonData.hits.hits[i]._source, true, true);		
		$('#tblSearchResults').append(rec.getRecordDOM());
		rec.displayRecord();

		var additionalData = {
				domain : openke.global.Common.getDomain(),
				storageArea : "normal",
				type : "_doc",
				title: title
		}
		var recordURL = openke.global.Common.getRestURLPrefix()+"/document/normal/_doc/"+uuid; // needed to get fulltext
		var domMenu = OKAnalyticsManager.produceObjectAnalyticsMenu(uuid, "", jsonData.hits.hits[i]._source ,jsonData.hits.hits[i]._source.url, additionalData,rec);  //note, not all of these need to be defined.  The called analytic will check
		var collectionDOM = openke.view.DocumentBucketSupport.createCollectionSelect(jsonData.hits.hits[i]._id,rec)

		rec.displayMenu(domMenu);
		rec.establishFullTextToggle(recordURL);
		rec.appendToMenu(collectionDOM);
		rec.showSortPosition(sortValue);
			
		if (typeof(jsonData.hits.hits[i]._source.user_collection) != 'undefined') {
			openke.view.DocumentBucketSupport.populateResultObject(rec,jsonData.hits.hits[i]._source.user_collection)
		}
	}
	 
	 if (newSearch && jsonData.hits.total.value > 0) {
		 options.currentPage = 1;
		 
		 if (jsonData.hits.total.value > MAX_RESULTS_TO_ALLOW) {
			 options.totalPages = Math.floor(MAX_RESULTS_TO_ALLOW / resultSize); // from + size must be less than or equal to: [10000]
		 } else {
			 options.totalPages = jsonData.hits.total.value / resultSize;
			 if ( (jsonData.hits.total.value % resultSize) > 0 ){ options.totalPages = options.totalPages + 1 }
		 }

		 if (options.totalPages > 10) {
			 options.numberOfPages = 10;
		 } else {
			 options.numberOfPages = options.totalPages;
		 }

		 $('#pager').bootstrapPaginator(options);	
		 if (options.numberOfPages >= 2) {
			 $('#pager').show();
		 }
		 		 
		 newSearch = false;
	 } 
	 
}


function clearFieldsTable() {
	$("#tblSearchFields tr:gt(1)").remove();
	
	var tblSearchFields = document.getElementById('tblSearchFields');
	var current_row = tblSearchFields.rows[1];
	
	var inp0 = current_row.cells[0].getElementsByTagName('select')[0];
	inp0.value = '';
	$('.chosen-select').trigger("chosen:updated");

	var inp1 = current_row.cells[1].getElementsByTagName('input')[0];
	inp1.value = '';
	
	var inp2 = current_row.cells[2].getElementsByTagName('select')[0];
	inp2.value = 'and';
	
	var inp2 = current_row.cells[3].getElementsByTagName('input')[0];
	inp2.value = 'Add';
	inp2.setAttribute("onclick", "javascript: insertRow(this);");

}

//Deletes a row from search field
//Source: http://jsfiddle.net/7AeDQ/
function deleteRow(row) {
	var rowIndex = row.parentNode.parentNode.rowIndex;
	document.getElementById('tblSearchFields').deleteRow(rowIndex);
}

var filterRowID = 1;

// Adds a row to search field
function insertRow(row) {
	var tblSearchFields = document.getElementById('tblSearchFields');

	var new_row = tblSearchFields.rows[1].cloneNode(true);
	
	filterRowID = filterRowID +1;
	
	var rowID = filterRowID; // length won't work if we add/delete things, was  len = tblSearchFields.rows.length;

	var current_row = tblSearchFields.rows[tblSearchFields.rows.length - 1];
	var add_button = current_row.cells[5].getElementsByTagName('input')[0];
	add_button.value = 'Remove';
	add_button.setAttribute("onclick", "javascript: deleteRow(this);");

	var inp0 = new_row.cells[0].getElementsByTagName('select')[0];
	inp0.id += rowID;
	inp0.selected = '';
	inp0.value = '';
	
	// We need to remove this, because, chosen plugin will recreate this div.
	var inp01 = new_row.cells[0].getElementsByTagName('div')[0];
	inp01.id += rowID;
	
	var inp = new_row.cells[1].getElementsByTagName('input')[0];
	inp.id += rowID;	
	
	var inp2 = new_row.cells[2].getElementsByTagName('select')[0];
	inp2.id += rowID;
	inp2.selected = 'prefix';
	
	var inp1 = new_row.cells[3].getElementsByTagName('input')[0];
	inp1.id += rowID;
	inp1.value = '';
	$(inp1).on('keyup', autoResizeTextInput);
	
	var inp2 = new_row.cells[4].getElementsByTagName('select')[0];
	inp2.id += rowID;
	inp2.selected = 'and';
	
	var inp2 = new_row.cells[5].getElementsByTagName('input')[0];
	inp2.id += rowID;
	inp2.value = 'Add';
	inp2.setAttribute("onclick", "javascript: insertRow(this);");

	$('#tblSearchFields > tbody').append(new_row)
	
	$(".chosen-select").chosen();
	$("#" + inp01.id).remove();
	
	// While cloning a chosen-select, a new chosen-container is createdm but its width was not set.
	$(".chosen-container").width("250px");

}

function createDocumentIndex() {
	bootbox.confirm({
	    title: "Create Document Index from Search Results",
	    message: `<form id="confirmForm" action="" >
		    <div class="form-group">\
		      <label for="indexName">Document Index Name:</label> <input type="text" name="indexName" id="indexName" size=50 />
		      <div style="color: red; display:none" id="indexNameError"></div>
		    </div>
		    <div class="form-group">
		      <label for="numDocs">Number of Documents:</label> <input type="text" name="numDocs" id="numDocs" size=5/></div>
		    <div style="color: red; display:none" id="indexNumberError"></div>
		    </form>
		    `,
	    buttons: {
	    	confirm: {
	            label: '<span class="fas fa-pencil-alt"></span> Create',
	            className: 'btn-default'
	        },
	        cancel: {
	            label: '<span class="fas fa-times"></span> Cancel',
	            className: 'btn-info'
	        }
	        
	    },
	    callback: function (result) {
	    	document.getElementById("indexNameError").style.display = "none";
	    	document.getElementById("indexNumberError").style.display = "none";
	        if (result) {
	        	if ($('#indexName').val() == null || $('#indexName').val().trim() == "") {
	        		document.getElementById("indexNameError").style.display = "inline";
	        		document.getElementById("indexNameError").innerHTML = "Enter a name for the document index.";
	        		$("#indexName").focus();
	        		return false;
	        	}
	        	if ($('#numDocs').val() == null || $('#numDocs').val().trim() == "" || isNaN(parseInt($('#numDocs').val()))  ) {
	        		document.getElementById("indexNumberError").style.display = "inline";
	        		document.getElementById("indexNumberError").innerHTML = "Enter a value between 1 and 1000 for number of documents.";
	        		$("#numDocs").focus();
	        		return false;
	        	}
	        	var numDocs = parseInt($('#numDocs').val());
	        	if (numDocs <1 || numDocs > 1000) {
	        		document.getElementById("indexNumberError").style.display = "inline";
	        		document.getElementById("indexNumberError").innerHTML = "Enter a value between 1 and 1000 for number of documents.";
	        		$("#numDocs").focus();
	        		return false;
	        	}

	        	var documentArea = "normal";
	    		var documentIndexID = RecordLevelAnalytics.generateUUID();
	    		var title = $('#indexName').val().trim();
	    		var query = savedQuery.query;
	    		
	    		DocumentIndex.createIndex(documentArea, documentIndexID, query, title, numDocs,false)

	        	bootbox.alert("Document index now being created.  Visit Document Indexes to view.")
	        }
	    }
	});
	
}

