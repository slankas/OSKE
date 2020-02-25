/**
 * Need to include openke.model.DocumentBucket.js
 * need to call setDefaultInstrumentationPage
 * 
 * Callback methods on actions can also be set when documents are added/removed to/from a bucket.
 * These buckets should take two arguments: documentBucketUUID and documentUUID 
 */

if (typeof openke == 'undefined') { openke = {} }
if (typeof openke.view == 'undefined') {	openke.view = {}  }
openke.view.DocumentBucketSupport = (function () {
	// Used to identify which page the event occurred when sending an instrumentation event
	var instrumentationPage = "application.unknown.";
		
	function setDefaultInstrumentationPage(page) {
		if (instrumentationPage == "application.unknown.") {
			if (page.endsWith(".")==false) {
				page = page + ".";
			}
			
			instrumentationPage = page;
		}
	}
	
	function sendInstrumentationEvent(eventName, eventDataJSON, callback) {
		var eventData = {};
		
		if (typeof eventDataJSON != "undefined") {
			for (var key in eventDataJSON) {
			    if (eventDataJSON.hasOwnProperty(key)) {
			        eventData[key] = eventDataJSON[key];
			    }
			}			
		}	
	    LASLogger.instrEvent(instrumentationPage+eventName, eventData, callback);
	}

	var noopFunction = function(bucketUUID, documentUUID) {return false;};
	
	var addDocumentToBucketCallback      = noopFunction;  // function to be called when a document is added to a bucket
	var removeDocumentFromBucketCallback = noopFunction;  // function to be called when a document is removed a bucket
	
	function setCallBackForAddDocumentToBucket(f) {
		addDocumentToBucketCallback = f;
	}

	function setCallBackForRemoveDocumentFromBucket(f) {
		removeDocumentFromBucketCallback = f;
	}

	
	
function createDocumentBucketSelectFunction(resultObject) {
    return function() { 
    	addToCollection(this,resultObject);
    };
}

// builds a drop select list of the document buckets that a document can be assigned to
function createCollectionSelect(id,resultObject) {
	var userDocumentBuckets = openke.model.DocumentBucket.getDocumentBuckets();
    var collection = "<select class='collectionslist' id='drpdn" + id + "' name='drpdn" + id +
	                          "' data-placeholder='Add to collection...' style='width:250px;' > " +
	                   "<option value='' selected='selected'>Add to document bucket...</option> ";
    for (var j = 0; j < userDocumentBuckets.length; j++) {
    	var localName = userDocumentBuckets[j].tag.replace("'","&apos;");
    	
    	collection += "<option value='" + userDocumentBuckets[j].id + "|"+localName+ "' >" + userDocumentBuckets[j].tag + "</option> ";
    }
    
    collection +="<option value='cNewCllct' >Create new document bucket...</option> ";
    collection += "</select>";	
    
    var collectionDOM = $(collection);
    collectionDOM.change(createDocumentBucketSelectFunction(resultObject));
    return collectionDOM
}


function setCollectionSelectsToDefault() {
    $('.collectionslist').val('');
}

function addToCollection(select, resultObjectRecord) {
	var id = select.id;
	var documentBucketID = $("#" + id).val();

	if (documentBucketID == null || documentBucketID == "" || documentBucketID == '') {
		return false;
	}

	 var documentID = String(id).substring(5);
	
	if (documentBucketID =='cNewCllct'){
		addToNewCollection(documentID, resultObjectRecord);
    }
    else {
    	addToExistingCollection(documentBucketID, documentID, resultObjectRecord);
    }

    return false;
}

function addToNewCollection(documentID, resultObjectRecord) {
    bootbox.prompt("Enter the tag for the new document bucket:", function(newTag){
        // If user hits cancel
        if (newTag === null || newTag.trim() === "") {
        	setCollectionSelectsToDefault();
            return;
        }

        newTag= newTag.trim();

        var pattern = new RegExp(/^[a-zA-Z0-9,.:; _-]*$/);

        // make sure that name matches pattern
        if (!pattern.test(newTag)){
            bootbox.alert({
                title: "Validation Error",
                message: "Bucket tag should only have alphanumeric characters and/or .,;: symbols"
            })
            setCollectionSelectsToDefault();
            return;
        }

        if (openke.model.DocumentBucket.doesTagExist(newTag)) {
            bootbox.alert({
                title: "Tag already exists",
                message: "Please enter a unique tag for the document bucket."
            })
            setCollectionSelectsToDefault();
            return;
        }


        var requestData = {
            tag: newTag,
            question : "",
            description : "",
            personalNotes: ""
        }

        $.ajax({
            contentType: "application/json; charset=utf-8",
            type : "POST",
            url : openke.global.Common.getRestURLPrefix()+"/documentbucket",
            data : JSON.stringify(requestData),
            dataType: 'json',
            success: function(s){
            	if (s.status == "failed") {
            		var errors = s.message;
            		bootbox.alert({
                        title: "Validation Error",
                        message: errors
                    })
            		setCollectionSelectsToDefault();
            		return;
            	}
            	sendInstrumentationEvent('createDocumentBucket',requestData);
            	
                var documentBucketID=s.collectionId;
                addDocumentToCollection(requestData.tag,documentBucketID,documentID, resultObjectRecord);
                
                $('.collectionslist').each(function(index, element) {
                	insertIntoSelect(element, documentBucketID, requestData.tag);
                })
            },
            error: function(e){
                alert(e.status);
            }
        });

        setCollectionSelectsToDefault();

        return;

    });	
}

	function insertIntoSelect(element, documentBucketID, tag) {
		var toInsert=true;
		$(element).find("option").each(function(index,optElement) {
			if (toInsert && (tag < $(optElement).text() || $(optElement).val() == 'cNewCllct')) {
				$(optElement).before($('<option>', {
                value: documentBucketID +"|"+tag,
                text: tag}));
				toInsert=false;
			}
		}
		);
		if (toInsert) {  //most likely this will not occur because of the check on cNewCllct
			$(element).append($('<option>', {
                value: documentBucketID +"|"+tag,
                text: tag}));
		}
	}


function addToExistingCollection(documentBucketID, documentID, resultObjectRecord) {
    var separatorPos = documentBucketID.indexOf("|");
    if (separatorPos == -1) {
        return false;
    }

    var documentBucketName = documentBucketID.substr(separatorPos + 1);
    documentBucketID = documentBucketID.substr(0, separatorPos);
   

    addDocumentToCollection(documentBucketName, documentBucketID, documentID, resultObjectRecord);
}

function addDocumentToCollection(documentBucketName, documentBucketID, documentUUID, resultObjectRecord){

    $.ajax({
        type : "PUT",
        url : openke.global.Common.getRestURLPrefix()+"/documentbucket/"+documentBucketID+"/document/" + documentUUID + "/",
        success : function(data) {
        	if (data.status === "failed") {
        		bootbox.alert({
                    title: "Unable to add to document bucket",
                    message: "System was unable to add the document to the document bucket"
                })
        		return;
        	}
            resultObjectRecord.appendToDocumentBuckets(documentBucketID,documentBucketName, visitDocumentBucket, removeBucketFromResult)
            sendInstrumentationEvent('addToDocumentBucket', {documentBucketUUID: documentBucketID, documentUUID: documentUUID});
            addDocumentToBucketCallback(documentBucketID, documentUUID);
        },
        error : function(data) {
            //TODO: Show failed message
        },
        dataType : "text"
    });

    return;

}

function populateResultObject(resultObjectRecord,collections) {
	for (var k=0; k< collections.length; k++) {
		var documentBucket = openke.model.DocumentBucket.getDocumentBucket(collections[k].collection_id)
		if (typeof(documentBucket) != 'undefined') {
			resultObjectRecord.appendToDocumentBuckets(collections[k].collection_id,documentBucket.tag, visitDocumentBucket, removeBucketFromResult)
		}
	}
}

function removeBucketFromResult(documentUUID, bucketUUID) {
	sendInstrumentationEvent('removeFromDocumentBucket', {documentBucketUUID: bucketUUID, documentUUID: documentUUID});

	openke.model.DocumentBucket.removeDocumentFromBucket(bucketUUID,documentUUID, function(data) { });
	
	removeDocumentFromBucketCallback(bucketUUID, documentUUID);
}

function visitDocumentBucket(bucketUUID) {
	var tag = openke.model.DocumentBucket.getDocumentBucket(bucketUUID).tag
	sendInstrumentationEvent('visitDocumentBucket', {documentBucketUUID: bucketUUID}, function() {window.location=openke.global.Common.getPageURLPrefix()+"/manageDocumentBuckets?uuid="+ bucketUUID+"&tag="+tag;});
}

return {
	setDefaultInstrumentationPage: setDefaultInstrumentationPage,
	setCallBackForAddDocumentToBucket: setCallBackForAddDocumentToBucket,
	setCallBackForRemoveDocumentFromBucket : setCallBackForRemoveDocumentFromBucket,
	
	createCollectionSelect: createCollectionSelect,
	populateResultObject : populateResultObject
	
};
}());
