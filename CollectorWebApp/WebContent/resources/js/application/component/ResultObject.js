/**
 * 
 * Create date - 20170306
 * Description: ResultObject represents a particular document/search result.
 * 
 * Notes:
 * - didn't use "Document" as a name as this refers to the HTML page usually within a web context.  
 * 
 * HTML classes in Object:
 * tr.resultRow - overall container, alternate coloring established through CSS
 * td.relevancyCell - provides space for a toggle as to whether or not this result was relevant or not
 * td.sortingCell - show the score/field used to sort results
 * td.resultCell  - contains the actual result records
 *   div.title
 *   div.url
 *   div.documentBuckets
 *   div.text - container for the summary and full text display
 *     div.summary - summarized text
 *     div.fullTextArea
 *          div.slider - jqueury UI slider to adjust what percentage of the text is shown
 *          div.fullText - full text
 *   div.menu - menu / analytics for the particular result
 *   div.message - message to be displayed on an error.
 */

if (typeof openke == 'undefined') { openke = {} }
if (typeof openke.component == 'undefined') {	openke.component = {}  }

/**
 * documentUUID - identifier for the document
 * documentTitle - 
 * documentSummary - summary of the document.  Typically produced by a search result.  Calling establishFullTextToggle will create a summary if one doesn't already exist
 * fullDocument - complete json record that is produced by openKE
 * useRelevancy - show the column that applies the relevancy toggle.
 * useSorting - show column that was used for the sort value
 * documentSource - where did we get the search result from?
 * showSource - should the documentSource be displayed as part of the summary?  
 * useExpansionArrows - should expansion arrows be shown on the text boxes?
 */


openke.component.ResultObject = function (documentUUID, documentTitle, documentURL, documentSummary, fullDocument, useRelavancy, useSorting, documentSource="", showSource=false, useExpansionArrows = true) {	
	if (documentSummary == null) { documentSummary ="";}

    this.uuid       = documentUUID;
    this.sourceUUID = documentUUID;
    this.title      = documentTitle;
    this.url        = documentURL;
    this.summary    = escapeHtml(documentSummary).trim().replace(/\n/g, "<p />").split("&lt;em&gt;").join("<em>").split("&lt;/em&gt;").join("</em>");  // em is put into the highlight text summary for elastic search results.
    this.jsonDoc    = fullDocument; 
    this.source     = documentSource;
    this.showSource = showSource;
    this.useExpansionArrows = useExpansionArrows;
    
    //TODO this logic really doesn't belon here.  Should be constants in the source handlers
    if (this.source == "google")                 { this.source = "Google"}
    else if (this.source == "duckduckgo")        { this.source = "Duck Duck Go"}
    else if (this.source == "microsoftacademic") { this.source = "MS Academic"}
    else if (this.source == "googlescholar")     { this.source = "Google Scholar"}
    
    var domContainerHTML   = "<tr class='resultRow'><td class='relevancyCell'></td><td class='sortingCell'></td><td class='resultCell'></td></tr>";
    var recordHTML = "<div class='title'></div><div class='url'></div><div class='menu'></div><div class='documentBuckets'></div><div class='text'><div class='summary'></div>"+
                     "<div class='fullTextArea'><div style='float:left;width:600px;clear: left' class='slider'><div id='custom-handle' class='ui-slider-handle'></div></div><div style='clear:left;' class='fullText'></div></div>" +
                     "</div><div class='message'></div>";
	this.resultDOM = $(domContainerHTML);
	var recordDOM  = $(recordHTML);

	this.resultDOM.find(".resultCell").append(recordDOM);
		
}


openke.component.ResultObject.prototype.getRecordDOM = function() {
	return this.resultDOM;
}

openke.component.ResultObject.prototype.displayRecord = function() {
	var a = document.createElement("a");
	a.href = this.url;
	
	var dataObject = {
		url: this.url,
		title: this.title,
		documentUUID: this.uuid 
	}
	
	var darkWebString = "onclick='sendResultObjectLinkToInstrumentation(\""+ Base64.encode(JSON.stringify(dataObject))+"\"); return true;' ";
	if (a.href.includes("onion.link") || a.href.includes("onion.cab")) {
		darkWebString = " onclick=\"if confirm('Links to a dark web site. Are you sure you want to continue?') { sendResultObjectLinkToInstrumentation('"+  Base64.encode(JSON.stringify(dataObject))+"'); return true; } else { return false; }\" ";
	}
	
	
	
	// use this for source here...
	var sourceTag = "";
	if (this.showSource) {
		sourceTag = "(" +this.source+")&nbsp;";
	}
	if (this.url != "") {
		this.resultDOM.find(".title").html("<a target='_blank' name='anchor_"+this.uuid+"'  class='title' href='" + this.url + "' " +darkWebString+" >" +  this.title + "</a>");
		var urlDOM = "<a target='_blank'  class='url' href='" + this.url + "' " +darkWebString+">" +  this.url+ "</a>";
		this.resultDOM.find(".url").html(urlDOM);
	}
	else {
		this.resultDOM.find(".title").html("<div name='anchor_"+this.uuid+"'  class='title'>" +  this.title + "</div>");
	}
	
	var arrowHTML = "";
	if (this.useExpansionArrows) {
		arrowHTML = "<span class='docArrow fa fa-plus-circle'></span>";
	}
	
	this.resultDOM.find(".summary").html(arrowHTML+sourceTag+this.summary);
}


openke.component.ResultObject.prototype.displayMenu = function (domMenu, sourceUUID) {
	if (sourceUUID) {
		this.sourceUUID = sourceUUID;
		var newID = "anchor_"+sourceUUID
		this.resultDOM.find(".title > a").attr("id",newID);
	}
	this.resultDOM.find(".menu").append(domMenu);
}

openke.component.ResultObject.prototype.appendToMenu = function (domMenu) {
	this.resultDOM.find(".menu").append(domMenu);
}

openke.component.ResultObject.prototype.appendToDocumentBuckets = function (bucketUUID,bucketName,bucketCallback, deleteCallback) {
	var escBucketName = bucketName.replace(/'/g,"\\'")
	var dbLabel = "<div class='okeSplitButton'><button class='okeSplitButtonLeft btn btn-primary btn-sm' title='Delete document bucket: "+escBucketName+"'><span style='line-height: 0.5;' class='fa fa-times fa-sm'></button><button title='View document bucket: "+escBucketName+"' class='okeSplitButtonRight btn btn-primary btn-sm'>"+bucketName+"</button></div>"
	var dbDOM  = $(dbLabel);
	var that = this;
	dbDOM.find(".okeSplitButtonLeft").click(function(event) {
		$(event.target).parents('.okeSplitButton').remove();
		deleteCallback(that.uuid,bucketUUID);
	});
	dbDOM.find(".okeSplitButtonRight").click(function() {bucketCallback(bucketUUID);});
	this.resultDOM.find(".documentBuckets").append(dbDOM);
}

openke.component.ResultObject.prototype.showMessage = function (message, errorFlag=false) {
	//TODO: display slightly different with errorFlag
	this.resultDOM.find(".message").html(message);
	if (errorFlag == true) {
		this.resultDOM.find(".message").addClass("error");
	}
	else {
		this.resultDOM.find(".message").removeClass("error");
	}
}

openke.component.ResultObject.prototype.hideMessage = function () {
	this.resultDOM.find(".message").hide()
}

openke.component.ResultObject.prototype.showSortPosition = function (sortPosition, neutrality="neutral") {
	if (neutrality !== "neutral" && neutrality !== "positive" && neutrality !== "negative") {
		throw ("Invalid flag for neutrality: must be  neutral, positive, or negative.")
	}
	this.resultDOM.find(".sortingCell").html(sortPosition);
	if (neutrality === "positive") {
		this.resultDOM.find(".sortingCell").removeClass("negative");
		this.resultDOM.find(".sortingCell").addClass("positive");
	}
	else if (neutrality === "negative") {
		this.resultDOM.find(".sortingCell").removeClass("positive");
		this.resultDOM.find(".sortingCell").addClass("negative");
	} 
	else { 
		this.resultDOM.find(".sortingCell").removeClass("positive");
		this.resultDOM.find(".sortingCell").removeClass("negative");
		this.resultDOM.find(".sortingCell").addClass("neutral");
	}
}

openke.component.ResultObject.prototype.displayRelevancyToggle = function (initialRelevancy, callback) {

    var relevancyButtonHTML;

	if (initialRelevancy === undefined) {
		 relevancyButtonHTML = '<span id="rt_'+this.uuid+'" class="unknownrelevant far fa-hand-point-right"></span>';
	}
    else if (initialRelevancy){
        relevancyButtonHTML = '<span id="rt_'+this.uuid+'" class="relevant far fa-thumbs-up"></span>';
    }
    else {
        relevancyButtonHTML = '<span id="rt_'+this.uuid+'" class="notrelevant far fa-thumbs-down"></span>';
    }

    var relevancyDOM = $(relevancyButtonHTML);

    this.resultDOM.find(".relevancyCell").append(relevancyDOM);

    $(relevancyDOM).on('click', function () {
        var spanRef = $(this)
        var id = spanRef.attr('id').substring(3)
        
        if (spanRef.hasClass('relevant')) {
            spanRef.removeClass('relevant');
            spanRef.removeClass('fa-thumbs-up');
            spanRef.addClass('notrelevant');
            spanRef.addClass('fa-thumbs-down');
            callback(id,false)
        }
        else if (spanRef.hasClass('notrelevant')) {
            spanRef.removeClass('notrelevant');
            spanRef.removeClass('fa-thumbs-down');
            spanRef.addClass('unknownrelevant');
            spanRef.addClass('fa-hand-right');
            callback(id,"unkown")
        }
        else if (spanRef.hasClass('unknownrelevant')) {
            spanRef.removeClass('unknownrelevant');
            spanRef.removeClass('fa-hand-right');
            spanRef.addClass('relevant');
            spanRef.addClass('fa-thumbs-up');
            callback(id,true)
        } 
    });
    
}

/* 
 * Used to toggle between summary text provided by the underlying search handler and full-text (which is summarized at 10%) by default.
 * This function will also check if the summary text (ie, this.summary) has any data.  If not, it will populate that data by performing a 1% summarization of the full-text.
 * When full-text summaries are produced, the resulting text will also be annotated where associated records are found in DBPedia.
 * 
 * This action also adds an event handler when selecting text.
 */
openke.component.ResultObject.prototype.establishFullTextToggle = function (fullRecordURL, useFullTextOnly = false, encodeFullText = true) { 
	var that = this;
	
	var switchIcon = "";
	if (!useFullTextOnly && this.useExpansionArrows) {
		switchIcon = "<span class='docArrow fa fa-minus-circle'></span>"
	}
	
	var annotateTextFunction = function(summary) {
		RecordLevelAnalytics.annotateDBPediaText(summary, that.uuid, function(html) {
			that.resultDOM.find(".fullText").html(switchIcon+html);
		});		
	}
	
	/* called when the user updates the sliders or by when switching to full-text view */
	var summarizeFunction = function(showSnackbar=true) {
		var textToSummary = that.jsonDoc.text.trim();
		if (encodeFullText) { textToSummary = escapeHtml(textToSummary) }
		
		var ratio = that.resultDOM.find(".slider" ).slider("option", "value"); 
		
		if (ratio == 100) {
			that.resultDOM.find(".fullText").html(switchIcon+that.jsonDoc.text.replace(/\n/g, "<br>"))
			annotateTextFunction(that.jsonDoc.text.replace(/\n/g, "<br>"));
		}
		else {
			RecordLevelAnalytics.summarizeText(textToSummary, ratio, that.uuid, function(stringArray) { 
				var summary = stringArray.join("\n\n")
				summary = summary.trim().replace(/\n/g, "<br>");
				if (summary == "") {
					if (showSnackbar) {Snackbar.show({text: "No summary text available, incrementing %", duration: 3500}) }
					ratio = Math.min(ratio +5,100);
					that.resultDOM.find(".slider" ).slider("value",ratio);
					that.resultDOM.find("#custom-handle").text( ratio +"%" );
					summarizeFunction(false);
					return;
					//summary = "<br>No text available at that summary level."
				}
				that.resultDOM.find(".fullText").html(switchIcon+summary);
				annotateTextFunction(summary)
			});
		}
	}
	

	
	var defualtLoadFullTextAction = function(data) {
		var text = data.text.trim();
		if (encodeFullText) { text = escapeHtml(text) }
		text = text.replace(/\n/g, "<br>");
		that.resultDOM.find(".fullText").html(switchIcon+text);
		summarizeFunction();
		
		if (text == "") {
			that.resultDOM.find(".fullTextArea").hide();
			that.resultDOM.find(".summary").show();
		}		
	}
	
	
	var loadFullTextRecord = function(url,callback) {
		$.ajax({
			url : url,
			contentType: "application/json; charset=utf-8",
			dataType : "JSON",
			success: function(data) {
				that.jsonDoc = data
				callback(data) 
			}
		});		
	}

	/* called when the user wants to expand to the fulltext view.  summaryFunction / fullTextFunction are basically alternatively called by the user by clicking on text */
	var summaryFunction = function(event) { 
		if (!sliding && !event.isPropagationStopped()) {
	
			that.resultDOM.find(".fullTextArea").show();
			that.resultDOM.find(".summary").hide();
			if (that.jsonDoc == null || JSON.stringify(that.jsonDoc) == "{}" ) {
				loadFullTextRecord(fullRecordURL, defualtLoadFullTextAction)
			} else {
				summarizeFunction();
			}
		}
	}
	
	/* called when the full-text is visible and the user wants to view the original summary/description of the search result */
	var fullTextFunction = function(event) { 
		if (!sliding &&  !event.isPropagationStopped()) {
			that.resultDOM.find(".fullTextArea").hide();
			that.resultDOM.find(".summary").show();
		}
	}
	
	/* creates the default summary when none was originally provided */
	var populateSummaryText = function(data, summarizationLevel = 5) {
		if (typeof data.text != 'undefined') {
			var fullText = data.text.trim();
			if (encodeFullText) {fullText = escapeHtml(fullText) }
			fullText = fullText.replace(/\n/g, "<br>");
			that.resultDOM.find(".fullText").html(switchIcon+fullText);	
			
			RecordLevelAnalytics.summarizeText(data.text, summarizationLevel, that.uuid, function(stringArray) {
		    	var summaryText = stringArray.join("&nbsp;")
		    	if (summaryText.trim() == "") {
		    		if (summarizationLevel > 90) {summaryText = "No summary available"}
		    		else { 
		    			populateSummaryText(data, summarizationLevel +10); 
		    			return; 
		    		}
		    	}
		    	that.summary = escapeHtml(summaryText)
		    	
		    	var arrowHTML = "";
		    	if (that.useExpansionArrows) {
		    		arrowHTML = "<span class='docArrow fa fa-plus-circle'></span>";
		    	}
				that.resultDOM.find(".summary").html(arrowHTML+that.summary);
			} )
		}
	}
	
	// must be called before the other event handlers are setup
	var showSelectedText = function (e) {
	        var text = '';
	        if (window.getSelection) {
	            text = window.getSelection();
	        } else if (document.getSelection) {
	            text = document.getSelection();
	        } else if (document.selection) {
	            text = document.selection.createRange().text;
	        }
			if (typeof text != 'string' && text.toString().trim() != '') {
				//found a selection event
	        	e.stopPropagation();
				if (LASHeader.getCurrrentScratchpadUUID() != null) {  // testing to see if text is still as string object
		        	var selectedText = text.toString()
		        	
		        	bootbox.confirm({
		        	    title: "Create Note in " + LASHeader.getCurrrentScratchpadName(),
		        	    message: "Selected Text:<p><textarea id='ppSelectText' style='width:100%;'>"+selectedText+"</textarea><p>"+
		        	             "Your Note:<p><textarea id='ppNoteText' style='width:100%;'></textarea>",
		        	    buttons: {
		        	        cancel: {
		        	            label: '<i class="fa fa-times"></i> Cancel'
		        	        },
		        	        confirm: {
		        	            label: '<i class="fa fa-check"></i> Confirm'
		        	        }
		        	    },
		        	    callback: function (result) {
		        	    	if (result) {
		        	    		var sourceCrawledDate = that.jsonDoc.crawled_dt;
		        	    		openke.model.ProjectDocument.appendSourceTextAndNote($('#ppSelectText').val(), $('#ppNoteText').val(), that.uuid,  that.title, that.url, sourceCrawledDate, function (){
		        	    				Snackbar.show({text: "Note added to "+LASHeader.getCurrrentScratchpadName(), duration: 3500})
		        	    			}, 
		        	    			function () {
		        	    				bootbox.alert("Unable to append note - check if the current scratchpad exists")
		        	    			});        	        	
		        	        	return;
		        	    		
		        	    	}
		        	    }
		        	});
				}
				else { // user has selected text, but there is not a default document available, need to let the user know
					Snackbar.show({text: "Select a current scratchpad to create a note.", duration: 0})
				}
	        }

	        
	}

	that.resultDOM.find(".summary").mouseup(showSelectedText)
	that.resultDOM.find(".fullText").mouseup(showSelectedText)

	that.resultDOM.find(".fullTextArea").hide();
	
	if (!useFullTextOnly) {
		that.resultDOM.find(".summary").mouseup(summaryFunction)
		that.resultDOM.find(".fullText").mouseup(fullTextFunction)
	}
	else {
		that.resultDOM.find(".fullTextArea").show();
		that.resultDOM.find(".summary").hide();	
	}
	
	var sliding = false; // tracks whether or not the user is currently sliding.  If sliding, don't respond to mouseup events (could possibly eliminate by defining the slider before registering the mouseup events
	var handle = that.resultDOM.find("#custom-handle");
	that.resultDOM.find(".slider" ).slider({
		  value: 10,
		  min:1,
		  max:100,
		  slide: function( event, ui ) {  handle.text( ui.value +"%" );   },
		  start: function (event, ui)   { sliding = true;  },
		  stop: function (event, ui)   { sliding = false; event.stopPropagation(); summarizeFunction()  }	,
		  create: function() {  handle.text( $( this ).slider( "value" ) +"%" );   }
	});
	
	if (!useFullTextOnly && that.resultDOM.find(".summary").text() == "") {
		loadFullTextRecord(fullRecordURL, populateSummaryText)
	}
	
	
	if (useFullTextOnly) {
		summarizeFunction()
	}
	
	return false;
}	

function sendResultObjectLinkToInstrumentation(encodedMessage) {
	var message=  Base64.decode(encodedMessage);
	var obj = JSON.parse(message);
	LASLogger.instrEvent("resultObject.visitLink",obj)
}

