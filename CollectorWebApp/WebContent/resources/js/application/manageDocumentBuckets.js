LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

var searchURL = openke.global.Common.getRestURLPrefix()+"/search"; // Search url on server.

var searchResultJSON; // Raw search json -> records from searching/retrieving the documents in a collection
var searchResult = new Object(); // Individual search results keyed on id.
var newSearch = false; // Do not populate paginator if new page is false.
var resultSize = 20;              // how many results should be shown on each page?
var MAX_RESULTS_TO_ALLOW = 10000; // how many results can the user possibly see?
var savedQuery = {};              // this is the current query for the search results.  needs to be saved as users can alter criteria.
var shownCollectionID;            // if a collection's documents are currently being shown, what is the collection ID?


var uuidRegEx = /^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}_/;

//options is the parameter used for the paginator
var options = {
	size : 'normal',
	alignment : 'center',
	currentPage : 1,
	totalPages : 10,
	numberOfPages : 10,
	onPageClicked : function(e, originalEvent, type, page) {
		LASLogger.instrEvent('application.buckets.paging', {
			criteria : savedQuery,
			page_number : page
		});

		savedQuery.from = (page - 1) * resultSize;
		getElasticSearchData(savedQuery);
	}
}

$(document).ready(function() {
    LASLogger.instrEvent('application.documentBuckets');
    openke.model.DocumentBucket.setDefaultInstrumentationPage('application.documentBuckets');
	openke.view.DocumentBucketSupport.setDefaultInstrumentationPage('application.documentBuckets');
	openke.model.DocumentBucket.loadAll();
	
	$('#btnDomainHome').click(function(){
		LASLogger.instrEvent('application.documentBuckets.home', {}, function() {window.location=openke.global.Common.getPageURLPrefix();});
	});
	
    $('#collectDocumentcard').hide();
    
    OKAnalyticsManager.defineStandardDocumentMenu("application.documentBuckets.",true);   
    
	var table = $('#tblBuckets').DataTable({
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"columns" : [ {
			"data" : "tag",
			"width" : "0",
		}, {
			"data" : "question",
			"width" : "0",
		},{
			"data" : "owner",
			"width" : "0",
		}, {
			"data" : "description",
			"width" : "0",
		}, {
			"data" : "dateCreated",
			"width" : "0",
		}, {
			"data" : "action",
			"width" : "0",
			"orderable": false
		}, ],
		"order" : [ [ 2, "desc" ] ]
	});

	refreshBucketTable(true);
});

function refreshBucketTable(initialLoad=false) {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/documentbucket/", function(data) {populateCollection(data,initialLoad)});	
}
function populateCollection(data, initialLoad) {
	var table = $('#tblBuckets').DataTable();
	table.clear();
	for (var i = 0; i < data.length; i++) {
		newRow = data[i];

		var localName = data[i].tag;
		localName = localName.replace("'","&apos;");
		
		newRow.action =  "<a href='javascript:showCollection(\"" + data[i].id + "\",\""+localName+"\");'>Show Documents</a>";
		newRow.action += "<br><a href='javascript:editCollection(\"" + data[i].id + "\");'>Edit Bucket</a>";
		newRow.action += "<br><a href='javascript:deleteCollection(\"" + data[i].id + "\",\""+localName+"\");'>Delete Bucket</a>";

		table.row.add(newRow);

	}
	table.draw();
	if (initialLoad) {
		var documentBucketID = $('#documentBucketID').val()
	    if (documentBucketID != null && documentBucketID != "null" && documentBucketID != "") {
	    	showCollection(documentBucketID,$('#documentBucketTag').val());
	    }
	}
}

function editCollection(collectionId) {
	window.location =openke.global.Common.getPageURLPrefix()+"/addEditDocumentBucket?CollectionId=" + collectionId;
}

function generateQuery(collectionId) {
	var sQuery = {
		"query": {
			"nested": {
				"path": "user_collection",
				"query": {	"match": {	"user_collection.collection_id": collectionId	}	}
		}}}
	
	sQuery["highlight"] = { "fields": { "text": { "fragment_size": 400, "number_of_fragments": 1   } } };
	sQuery["from"] = 0;
	sQuery["size"] = resultSize;

	return sQuery;
}

function showCollection(collectionId,name) {
	LASLogger.instrEvent('application.documentBuckets.show', {
		collectionId : collectionId,
		tag : name
	});
	//alert(name);

	var collectionName = name;
	
	$('#searchResultsTitle').text("Documents for "+name);
	$('#collectDocumentcard').show();
	
	var sQuery = generateQuery(collectionId);

	savedQuery = sQuery;
	newSearch = true;

	$('#pager').hide();

	LASLogger.instrEvent('application.documentBuckets.getdocuments', {
		collectionId : collectionId,
	});

	getElasticSearchData(savedQuery, collectionId, collectionName);	
	$('html, body').animate({ 'scrollTop': $('#searchResultsTitle').offset().top},1000);
}	

function getElasticSearchData(queryJSON, collectionId, collectionName) {
	var url = openke.global.Common.getRestURLPrefix()+"/documentbucket/documents";
    $.ajax({
		type : "POST",
		url : url,
		data : JSON.stringify(queryJSON),
		success : function(data) {
			populateSearchResults(data, collectionId, collectionName)
		},
		error : function(data) {
			//TODO
		},
		dataType : "json",
		contentType : "application/json"
	});
}

function populateSearchResults(jsonData, collectionID, collectionName) {
	searchResultJSON = jsonData;

	searchResult['took'] = jsonData.took;
	searchResult['timed_out'] = jsonData.timed_out;
	searchResult['shards'] = jsonData.shards;
	searchResult['total_hits'] = jsonData.hits.total.value;
	searchResult['max_score'] = jsonData.hits.max_score;
	
	shownCollectionID = collectionID;
	$('#collectDocumentPanel').show();
	
	$('#tblSearchResults').empty();

	if (jsonData.hits.total.value == 0) {
		$('#tblSearchResults').append('<tr><td><div><label>No documents belong to this bucket</label></div></td></tr>')
		$('#searchResultsHeading').text("Empty Collection");
		$('#searchResultsExport').hide();
	}
	else {
		var startNum = savedQuery.from + 1;
		var endNum   = savedQuery.from + resultSize;
		if (endNum > jsonData.hits.total.value) { endNum = jsonData.hits.total.value }
		$('#searchResultsHeading').text("Search Results: Showing "+ startNum+" to "+ endNum+" of "+jsonData.hits.total.value);
		$('#searchResultsExport').show();
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
				type : "doc",
				title: title
		}
		var recordURL = openke.global.Common.getRestURLPrefix()+"/document/normal/_doc/"+uuid; // needed to get fulltext
		var domMenu = OKAnalyticsManager.produceObjectAnalyticsMenu(uuid, "", jsonData.hits.hits[i]._source ,jsonData.hits.hits[i]._source.url, additionalData,rec);  //note, not all of these need to be defined.  The called analytic will check
		var collectionDOM = openke.view.DocumentBucketSupport.createCollectionSelect(uuid,rec)
		rec.displayMenu(domMenu);
		rec.appendToMenu(collectionDOM);
		rec.establishFullTextToggle(recordURL);

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
		 $('#pager').show();
		 		 
		 newSearch = false;
	 } 
	 
	 
	 // Link in indexing functionality:
		$('#btCreateIndex').off('click').click(function() {
			var documentArea = "normal";
			var documentIndexID = shownCollectionID;
			var title = collectionName;
			var maxNumResults = -1;
			var query = { "nested": { "path": "user_collection",
				                      "query": {
				                    	  "match": {
				                               "user_collection.collection_id": documentIndexID
					}	 }   }};
			
			DocumentIndex.createIndex(documentArea, documentIndexID, query, title, maxNumResults)

		});
		
		$('#btShowIndex').hide()
		$('#btShowIndex').off('click').click(function() {
			DocumentIndex.showIndexView("normal",shownCollectionID)
		});	 
		DocumentIndex.checkIndexExists("normal",shownCollectionID);
}

function deleteCollection(collectionId, collectionName) {
	LASLogger.instrEvent('application.documentBuckets.delete.called', {
		collectionId : collectionId
	});


	bootbox.confirm({
	    title: "Delete Bucket: "+collectionName,
	    message: "Are you sure you want to delete this bucket?",
	    buttons: {
	        cancel: {
	            label: '<i class="fa fa-times"></i> Cancel'
	        },
	        confirm: {
	            label: '<i class="fa fa-trash-alt"></i> Delete'
	        }
	    },
	    callback: function (result) {
	    	if (result) {	
	    		LASLogger.instrEvent('application.documentBuckets.delete.confirmed', {
	    			collectionId : collectionId
	    		});	    
	    		var url = openke.global.Common.getRestURLPrefix()+"/documentbucket/"+collectionId;
	    	    $.ajax({
	    			type : "DELETE",
	    			url : url,
	    			success : function(data) {
	    				if (shownCollectionID === collectionId) {
	    					$('#collectDocumentPanel').hide();
	    				}
	    				refreshBucketTable();
	    			},
	    			error : function(data) {
	    				//TODO
	    				// alert("error");
	    			},
	    			dataType : "text",
	    		});	
	    	}
	    	else {
	    		LASLogger.instrEvent('application.documentBuckets.delete.cancelled', {
	    			collectionId : collectionId
	    		});	    	
	    	}
	    }
	});

	

}	


function createDocumentBucket() {
	LASLogger.instrEvent('application.documentBuckets.createBucket', {}, function() {window.location= openke.global.Common.getPageURLPrefix()+"/addEditDocumentBucket";});
}


