/**
 * 
 * Description: Script to view book back index, concept index and document listings
 * 
 * Dependencies:
 *   jQuery
 *   jqTree 1.3.8
 *   LASLogger
 * 
 * Usage:
 * 
 * Notes:
 * Index Data format 
 * Data format
			{
			allDocumentData: [{}]
			  id: UUID of the data.  Can directly access this record using /indexName/_doc/UUID
			  title:
			  url:  what was the source of the data
			  
			bookBack [{}]    Keyphrase data.  need to eliminate keyphrases unded a certain length.  or that have non character data besides a |
			                 levels identified by a |
			  keyphrase
			  documents : [{}]
			    id: UUID
			
			conceptIndex [{}]
			  concept:  This is the conceptFullName field.  levels identified by a .	
			  documents: [{}]
			    id: UUID
			    values: []
				
			Metadata : {
			dateCreated: "2017-05-08T10:14:13.033154+00:00"
			totalConcepts: 25
			totalDocuments: 109
			totalKeywords: 701
			title: Indextitle
			}
			} 
 */

Set.prototype.intersection = function(setB) {
    var intersection = new Set();
    for (var elem of setB) {
        if (this.has(elem)) {
            intersection.add(elem);
        }
    }
    return intersection;
}

Set.prototype.difference = function(setB) {
    var difference = new Set(this);
    for (var elem of setB) {
        difference.delete(elem);
    }
    return difference;
}

if (typeof openke == 'undefined') { openke = {} }
if (typeof openke.view == 'undefined') {	openke.view = {}  }
openke.view.DocumentIndexView = (function () {
	"use strict";
 
	// For instrumentation events, this will the be basic information sent to the service
	var defaultInstrumentationData = {};

	var originalData;          // This is the document stored when creating the index
	var originalConceptTree;   // this is a copy of the hierarchy from the original data load without any filters applied.
	var originalKeyPhraseTree; // this is the keyphrase hierarchy as defined from the original data.
	
	var documentMap;    // hash table indexed by documentId of the documents in the index
	/*
	var conceptMap;     // hash table of concepts, indexed by the conceptFullName
	*/
	var fullKeyphraseMap;   // hash table of the keyphrase data, indexed by keyphrase
	
	var treeKeyphrase;  // jqTree structure for the keyphrase 
	var treeConcept;    // jqTree structure for the concept
	var treeDocument;   // jqTree structure for the documents
	
	var filters     = [];   // include filters
	var antiFilters = [];   // specifies which documents should be excluded..
	
	var documentArea;        // what area does the current index cover?
	var documentIndexID;     // what is the ID for the current index?	
		
	var popupMenuOffsetY = 0; // used to correct positioning off popup menu
	
	var initialized = false;
	
	/**
	 * Initializes the component
	 * Sets up the generic document analytic menus
	 * Initializes any HTML components (e.g., sliders) not dependent on data
	 * Defines the event handlers for the various HTML components 
	 * Begins the process to load data
	 * 
	 */
	function initialize(documentAreaParam,documentIndexIDParam) {	
		documentArea = documentAreaParam
		documentIndexID = documentIndexIDParam
	
		if (!initialized) {
			OKAnalyticsManager.defineStandardDocumentMenu("application.viewIndex.", false, true); 
			openke.model.DocumentBucket.setDefaultInstrumentationPage('application.viewIndex');
			openke.view.DocumentBucketSupport.setDefaultInstrumentationPage('application.viewIndex');
			openke.model.DocumentBucket.loadAll();
			
			// Setup the 3 separate slides used for keyphases, concepts, and documents.
			var splitConfig = {   sizes: [20, 80],     minSize: 150  }
			Split(['#keyIndexNavigation', '#textArea'], splitConfig);
			Split(['#conceptIndexNavigation', '#conceptTextArea'], splitConfig);
			Split(['#documentIndexNavigation', '#documentTextArea'], splitConfig);
			
			showKeyIndex();
			
			$('#allFilters').hide();
			$(document).bind("click", hidePopupMenu);
		    $('#documentMenu').click(function(event) {
		    	event.stopPropagation();
		    });	
		    $(document).bind("mousedown", function (e) { 
		        // If the clicked element is not the menu
		        if (!$(e.target).parents(".custom-menu").length > 0) {
		            
		            // Hide it
		            $(".custom-menu").hide(100);
		        }
		    });
		    
			$('#btDocuments').click(showDocumentIndex);
			$('#btKeyphrases').click(showKeyIndex);
			$('#btConcepts').click(showConceptIndex);
			$(window).resize(resizeIndexBlocks);
	
			resizeIndexBlocks();
			initialized = true;
		}
		
		loadIndex(documentArea, documentIndexID);
	}
	
	function setPopupMenuYOffset(offsetValue) {
		popupMenuOffsetY = offsetValue;
	}
		
	/*initial loading of index, initializing all trees*/
	function loadIndex(documentArea, documentIndexID) {
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/documentIndex/"+documentArea+"/"+documentIndexID,
			type:"GET",
			contentType: "application/json; charset=utf-8",
			dataType : "text JSON",
			success: function(data) {
				if (data.found == false) {
					bootbox.alert("Indexing data not found.");
					return;
				}
				
				originalData = data;
				
				$('#indexTitle').text("Index: "+data.metadata.title);
				$('#indexViews').css('visibility', 'visible');
				$('#indexBlock').css('visibility', 'visible');
				

				// Establish map objects to quickly index data.  For concepts and keyphrases, create sets to easily test if a document belongs
				documentMap = {}
				data.allDocumentData.forEach(function (dataObject) { documentMap[dataObject.id]=dataObject; });
				
				// don't necessarily need the map, but I do need the document set.
				//conceptMap = {}
				data.conceptIndex.forEach(function (conceptObject) { 
					conceptObject.documentSet = new Set();
					conceptObject.documents.forEach(function (dataObject) { conceptObject.documentSet.add (dataObject.id)});
					//conceptMap[conceptObject.concept] = conceptObject; 
				});

				//keyphraseMap = {}
				data.bookback.forEach(function (keyPhraseObject) {   // ? do I need to deal with the split here?
					keyPhraseObject.documentSet = new Set();
					keyPhraseObject.documents.forEach(function (dataObject) { keyPhraseObject.documentSet.add (dataObject.id)});
					//keyphraseMap[keyPhraseObject.keyphrase] = keyPhraseObject; 
				});

				// create the "original" concept & keyPhrase hierarchies
				originalConceptTree   = computeConceptData(originalData);
				originalKeyPhraseTree = computeKeyphraseData(originalData,new Set(),true); 
				
				
				displayData()
			}
		})
	}

	function displayData() {
		var data = originalData;
		var includedFiles = applyFilters(data, originalConceptTree, originalKeyPhraseTree);                    // included files is an array of document IDs
		includedFiles     = applyAntiFilters(data, originalConceptTree, originalKeyPhraseTree, includedFiles);

		//remove any documents currently being displayed
		$('.contentBlock').html('<i>Select an index term from the left to start.</i>')
		$('#documentTextBlock').html('<i>Select a document from the left to start.</i>')
		// compute metaData during the generationProcess
		var numKeyphrases = generateKeyphraseTree(data,includedFiles);
		var numConcepts   = generateConceptTree(data,includedFiles);
		var numDocuments  = generateDocumentTree(data,includedFiles);  // can get the count from included files

		var metaData = {
			dateCreated: originalData.metadata.dateCreated,
			totalKeywords: numKeyphrases,
			totalConcepts: numConcepts,
			totalDocuments: numDocuments
		}
			
		displayMetaData(metaData);
	}
	
	function displayMetaData(data) {
		$('#metaData').html('Keyphrases:&nbsp'+data.totalKeywords+'&nbsp;&nbspConcepts:&nbsp'+data.totalConcepts+'&nbsp;&nbsp;Documents:&nbsp'+data.totalDocuments+
				            '<br>'+'Creation Timestamp:&nbsp'+data.dateCreated.substring(0,19))
	}

	function resizeIndexBlocks() {
		var newHeight = $(window).height() - 180 - $("#allFilters").height();
			
		if ($("#allFilters").height() > 40) {
			newHeight = newHeight - 50;
		}

	    $('.indexBlock').height(newHeight);
	}	

	function hidePopupMenu() {
		document.getElementById("documentMenu").className = "menuHide";
	}	

	function mouseX(evt) {
	    if (evt.pageX) {
	        return evt.pageX;
	    } else if (evt.clientX) {
	       return evt.clientX + (document.documentElement.scrollLeft ?
	           document.documentElement.scrollLeft :
	           document.body.scrollLeft);
	    } else {
	        return null;
	    }
	}

	function mouseY(evt) {
	    if (evt.clientY) {
	       return evt.clientY + (document.documentElement.scrollTop ?
	       document.documentElement.scrollTop :
	       document.body.scrollTop);
	    } else if (evt.pageY) {
	    	return evt.pageY;
	    }
	    else {
	        return null;
	    }
	}
	
	
	function showKeyIndex() {
		$('.viewIndexMenu').removeClass('active')
		$('#documentIndexBlock').hide()
		$('#conceptIndexBlock').hide()
		$('#indexBlock').show()
		$('#btKeyphrases').addClass('active')
		resizeIndexBlocks();
	}

	function showConceptIndex() {
		$('.viewIndexMenu').removeClass('active')
		$('#documentIndexBlock').hide()
		$('#indexBlock').hide()
		$('#conceptIndexBlock').show()
		$('#btConcepts').addClass('active')
		resizeIndexBlocks();
	}
	
	function showDocumentIndex(){
		$('.viewIndexMenu').removeClass('active')
		$('#indexBlock').hide()
		$('#conceptIndexBlock').hide()
		$('#documentIndexBlock').show()
		$('#btDocuments').addClass('active')
		resizeIndexBlocks();
	}
	
	
	/**
	 * From a given concept full name string (e.g, pestle.social.education), find the corresponding
	 * node in the concept tree.
	 * 
	 * @param node     this is the current node to examine
	 * @param conceptFullName  this is the concept full name. levels are separated by a period
	 * @returns
	 */
	function getNodeForConcept(node, conceptFullName) {
		if (node.fullName != undefined) {
			if (node.fullName == conceptFullName) {
				return node;
			}
		}
		for (var i=0; i < node.children.length; i++) {
			var childNode = node.children[i];
			if (conceptFullName.startsWith(childNode.fullName)) {
				var answer = getNodeForConcept(childNode,conceptFullName);
				if (answer != null) {
					return answer;
				}
			}

		}
		
		return null; // was not found
	}
	
	/**
	 * Based on the include filters, returns a set of documents that are included in the current view
	 * If there are no include filters, all documents are returned
	 * 
	 */
	function applyFilters(data, conceptTree, keyphraseTree) {
		var includedFiles = new Set();
		
		if (filters.length == 0) {
			data.allDocumentData.forEach(function (dataObject) { includedFiles.add (dataObject.id)});
			return includedFiles;
		}
		
		var conceptRootNode = { children: conceptTree};
		
		data.allDocumentData.forEach(function (dataObject) {
			var foundFilter = false;
			
			for (var i=0; i< filters.length; i++) {
				var currentFilter = filters[i];
				if (currentFilter.filterType === "document") {
					if (dataObject.id === currentFilter.documentID) {
						foundFilter = true;
					}
					else {
						foundFilter = false;
						break;
					}
				}
				else if (currentFilter.filterType === "keyphrase") {
					var node = fullKeyphraseMap[currentFilter.keyphrase]
					if (node.documentSet.has(dataObject.id)) {
						foundFilter = true;
					}
					else {
						foundFilter = false; 
						break;
					}
				}
				else if (currentFilter.filterType === "concept") {
					var treeNode = getNodeForConcept(conceptRootNode, currentFilter.conceptFullName);
					
					if (treeNode.allDocuments.has(dataObject.id)) {
						foundFilter = true;
					}
					else {
						foundFilter = false;
						break;
					}
				}
			}
			
			if (foundFilter) {
				includedFiles.add(dataObject.id);
			}
			
		});
		return includedFiles;
		
	}
	
	/**
	 * Based upon the exclude filters, removes any documents which meet that criteria.
	 * If there are no exclude filters, then no action needs to take place and the includedFiles is returned
	 * unaltered
	 */
	function applyAntiFilters(data, conceptTree, keyphraseTree, includedFiles){
		if (antiFilters.length == 0) {
			return includedFiles;
		}
		
		var conceptRootNode = { children: conceptTree};
		
		for (var i=0; i< antiFilters.length; i++) {
			var currentFilter = antiFilters[i];
			if (currentFilter.filterType === "document") {
				includedFiles.delete(currentFilter.documentID);
			}
			else if (currentFilter.filterType === "keyphrase") {
				var node = fullKeyphraseMap[currentFilter.keyphrase];
				node.documentSet.forEach(function (documentID) { includedFiles.delete(documentID) })
			}
			else if (currentFilter.filterType === "concept") {
				var treeNode = getNodeForConcept(conceptRootNode, currentFilter.conceptFullName);
				treeNode.allDocuments.forEach(function(documentID) { includedFiles.delete(documentID) });
			}
		}
		
		return includedFiles;
	}
	
	function dedupeArray(a) {
	    var temp = {};
	    for (var i = 0; i < a.length; i++)
	        temp[a[i]] = true;
	    return Object.keys(temp);
	}
	
	/** 
	 * This is called at the start to produce a hierarchy of all of the data for concepts
	 *  
	 * @param data
	 * @returns
	 */
	function computeKeyphraseData(data, includeOnlyFileSet = new Set(), computeFullMapFlag=false) {
		var keyphraseData = [];
		var keyphraseMap  = {};    //temporary structure to hold a pointer to a parent node to make lookups easier
		var id = 0;
		
		if (computeFullMapFlag) {
			fullKeyphraseMap = {};
		}

		data.bookback.forEach(function (bookbackObject, bbIndex) {
			var keyphrase = bookbackObject.keyphrase;
			var parts = keyphrase.split("|");
			
			// validate that this keyphrase is valid.  If the top part of the keyphrase is invalid, toss it all out  /^[a-zA-Z0-9]+$/.test(parts[0]) == false
			if (parts[0].length < 3 || parts[0].match(/^[0-9a-z ]+$/i) == null) {
				return;
			}
			// if there was a child, make sure that's valid.  If not just delete and continue processing
			if (parts.length ==2 && (parts[1].length <3 || parts[1].match(/^[0-9a-z ]+$/i) == null)) {
				parts.pop();
			}
			
			var matchedDocuments = bookbackObject.documentSet;
			if (includeOnlyFileSet.size > 0) {
				matchedDocuments = bookbackObject.documentSet.intersection(includeOnlyFileSet);
				if (matchedDocuments.size == 0) {
					return; //ie, break out of this keyphrase, they are not any documents to display
				}
			}
						
			var parentNode;
			if  (keyphraseMap.hasOwnProperty(parts[0])) {
				parentNode = keyphraseMap[parts[0]];
				matchedDocuments.forEach(function(id) {parentNode.documentSet.add(id); });
			}
			else {
				parentNode = { name: parts[0], children: [], documents:[], documentSet: new Set(matchedDocuments), id: bbIndex.toString()}   
				keyphraseMap[parts[0]] = parentNode;
				keyphraseData.push(parentNode);
			}
						
			if (parts.length == 2) {
				var childNode = { name: parts[1], children: [], documents:[...matchedDocuments], documentSet: new Set(matchedDocuments), id: parentNode.id+"."+bbIndex.toString()} 
				parentNode.children.push(childNode);
			}
		})
		
		keyphraseData.forEach(function(node) {

			// Let's try removing child data from the parent
			node.children.forEach(function(childNode) {
				node.documentSet = node.documentSet.difference(childNode.documentSet);
			})
			
			node.documents = [...node.documentSet];
			
			if (computeFullMapFlag) {
				fullKeyphraseMap[node.name] = node;
				node.children.forEach(function(childNode) {
					fullKeyphraseMap[childNode.name] = childNode;
				})
			}
			relabelTreeNodes(node);                       // puts the number of documents onto the node
		});
		// this shouldn't be necessary because of how we construct the tree, keyphraseData.forEach(populateTreeNodesWithDocuments);    // for each node, mark which documents fall into node and all of its children

		return keyphraseData;
	}

	
	function generateKeyphraseTree(data,includedFiles) {
		var savedTreeState = null;  // tree state doesn't appear to work. may be because not all nodes have indices
		if (treeKeyphrase != undefined) {
			savedTreeState = treeKeyphrase.tree('getState')
			treeKeyphrase.tree('destroy');
			$('#keyTree').unbind();
			$("#keyphraseTreeMenu li").unbind();
		}
		
		var foundConcepts = 0;
		
		var keyphraseData = computeKeyphraseData(data,includedFiles);

		// create the jqTree
		var autoOpen = (savedTreeState == null)
		treeKeyphrase = $('#keyTree')
		treeKeyphrase.tree({
				data: keyphraseData,  
				useContextMenu: true,
				autoOpen: autoOpen
			})
		
		var keyphrase = ""
		$('#keyTree').bind('tree.contextmenu',  function(event) {
			var node = event.node;
			keyphrase = node.name;
	        //event.stopPropagation();
	        $("#keyphraseTreeMenu").finish().toggle(100).css({ top: mouseY(event.click_event) + 'px', left:mouseX(event.click_event) + 'px'  });
		});
		
		$("#keyphraseTreeMenu li").click(function(e) {
		    var str = keyphrase.substring(0, keyphrase.lastIndexOf('(')).trim();
		 	var newFilter = {
	 	        		"filterType":"keyphrase",
	 	        		"keyphrase":str,
	 	        		"name":str
	 	    }
			
	 	    // This is the triggered action name
	 	    switch($(this).attr("data-action")) {
	 	        case "addFilter": addFilter(newFilter);
	 	                          break;
	 	        case "addNotFilter": addAntiFilter(newFilter);
	 	        	                 break;
	 	    }
	 	  
	 	    // Hide it AFTER the action was triggered
	 	    $("#keyphraseTreeMenu").hide(100);
	 	    
	 	  });
		
		
		$('#keyTree').bind( 'tree.click', function(event) {
			//event.stopPropagation();
			$("#keyTree .jqtree-selected").removeClass("jqtree-selected");
			var node = event.node
			$(node.element).addClass("jqtree-selected")
			var clickEvent = event.click_event;

			if (node.documents == undefined || node.documents.length==0) {
				return
			}
		
			if (node.documents.length==1) {
				displayText(node.documents[0],"#textBlock",null,node.name)
				return
			}
		
			$("#documents").empty();
			for(var d = 0;d< node.documents.length;d++) {
				var dom = $('<a href="">'+documentMap[node.documents[d]].url+'</a>');
				var documentID = node.documents[d]
				dom.click( createKeyphraseViewFunction (documentID,node.name));
				$("#documents").append("<li></li>" );
				$("#documents li:last-child").append(dom )
				//$("#documents").append('<li><a href=""  onclick="displayConceptText(\''+node.documents[d]+'\',\''+node.name+'\'); hidePopupMenu(); return false;">'+documentMap[node.documents[d]].url+'</li>');
			}
			sortUnorderedList("#documents");
		
	        $("#documentMenu").css({left: mouseX(clickEvent)});
	        $("#documentMenu").css({top: mouseY(clickEvent) - popupMenuOffsetY});
     	    document.getElementById("documentMenu").className = "menuShow";

	        window.event.returnValue = false;
	        clickEvent.stopPropagation();
        
	        return;

		})		
		
		if (savedTreeState != null) {
			treeKeyphrase.tree('setState',savedTreeState);
		}
		
		return countNodes(treeKeyphrase.tree('getTree'))

	}
	
	function relabelTreeNodes(node) {
		if (node.documents.length > 0) {
			node.name = node.name + " ("+node.documents.length+")";
		}
		node.children.forEach(relabelTreeNodes);
	}
	
	function populateTreeNodesWithDocuments(conceptDataNode) {
		var allDocuments = new Set();
		
		conceptDataNode.documents.forEach( function(documentUUID) {allDocuments.add(documentUUID);});
		
		conceptDataNode.children.forEach( function (child){
			var childDocuments = populateTreeNodesWithDocuments(child);
			childDocuments.forEach (function (doc) { allDocuments.add(doc)});
		});
		conceptDataNode.allDocuments = allDocuments;
		return allDocuments;
	}

	/** 
	 * This is called at the start to produce a hierarchy of all of the data for concepts
	 *  
	 * @param data
	 * @returns
	 */
	function computeConceptData(data, includeOnlyFileSet = new Set()) {
		var conceptData = [];
		var id = 0;
		
		data.conceptIndex.forEach(function (conceptObject, conceptIndex){
			var matchedDocuments = conceptObject.documentSet;
			if (includeOnlyFileSet.size > 0) {
				matchedDocuments = conceptObject.documentSet.intersection(includeOnlyFileSet);
				if (matchedDocuments.size == 0) {
					return; //ie, break out of this concept
				}
			}
			
			var conceptLevels = conceptObject.concept.split('.');
			var currentNode = conceptData;
			
			var lastNode;
			var fullName = "";
			for (var i=0;i<conceptLevels.length;i++) {
				var value = conceptLevels[i];
				if (i>0) { fullName = fullName +"."}
				fullName = fullName + value;
				
				var found = false;
				for (var j=0;j<currentNode.length;j++) {
					if (value == currentNode[j].name) {
						found = true;
						currentNode=currentNode[j].children;
						lastNode = currentNode[j];
						break;
					}
				}
				if (found == false) {
					id = id -1;
					var newNode = { name: value, children: [], documents:[], fullName: fullName, id: id}   
					currentNode.push(newNode)
					currentNode = newNode.children
					lastNode = newNode;
				}
				
			}
			lastNode.id = conceptIndex; //use the array index position as the node ID
			matchedDocuments.forEach(function(id) {lastNode.documents.push(id)});
			
	
		})

		conceptData.forEach(relabelTreeNodes);                  // puts the number of documents onto the node
		conceptData.forEach(populateTreeNodesWithDocuments);    // for each node, mark which documents fall into node and all of its children

		return conceptData;
	}
	
	
	function generateConceptTree(data,includedFiles){
		
		var savedTreeState = null;  // tree state doesn't appear to work. may be because not all nodes have indices
		if (treeConcept != undefined) {
			savedTreeState = treeConcept.tree('getState')
			treeConcept.tree('destroy');
			$('#conceptTree').unbind();
			$("#conceptTreeMenu li").unbind();
		}
		
		var foundConcepts = 0;
		
		var conceptData = computeConceptData(data,includedFiles);

		// create the jqTree
		var autoOpen = (savedTreeState == null)
		treeConcept = $('#conceptTree')
		treeConcept.tree({
				data: conceptData,  
				useContextMenu: true,
				autoOpen: autoOpen
			})
		
		var conceptFullName =""
		$('#conceptTree').bind('tree.contextmenu',  function(event) {
			var node = event.node;
		    conceptFullName = node.fullName;
	        //event.stopPropagation();
	        $("#conceptTreeMenu").finish().toggle(100).css({
		            top: mouseY(event.click_event) + 'px',
		            left:mouseX(event.click_event) + 'px'
		        });
		        
		});
		
		$("#conceptTreeMenu li").click(function(e) {
	     	var newFilter = {
		        		"filterType" : "concept",
		        		"conceptFullName":conceptFullName,
		        		"name":conceptFullName
		    };
			
	 	    // This is the triggered action name
	 	    switch($(this).attr("data-action")) {
	 	        case "addFilter": addFilter(newFilter);
	 	                          break;
	 	        case "addNotFilter": addAntiFilter(newFilter);
	 	        	                 break;
	 	    }
	 	  
	 	    // Hide it AFTER the action was triggered
	 	    $("#conceptTreeMenu").hide(100);
	 	    
	 	  });
		
		
		$('#conceptTree').bind( 'tree.click', function(event) {
			//event.stopPropagation();
			$("#conceptTree .jqtree-selected").removeClass("jqtree-selected");
			var node = event.node
			$(node.element).addClass("jqtree-selected")
			var clickEvent = event.click_event;

			if (node.documents == undefined || node.documents.length==0) {
				return
			}
		
			if (node.documents.length==1) {
				displayText(node.documents[0],"#conceptTextBlock",node.fullName)
				return
			}
		
			$("#documents").empty();
			for(var d = 0;d< node.documents.length;d++) {
				var dom = $('<a href="">'+documentMap[node.documents[d]].url+'</a>');
				var documentID = node.documents[d]
				dom.click( createConceptViewFunction (documentID,node.fullName));
				$("#documents").append("<li></li>" );
				$("#documents li:last-child").append(dom )
				//$("#documents").append('<li><a href=""  onclick="displayConceptText(\''+node.documents[d]+'\',\''+node.name+'\'); hidePopupMenu(); return false;">'+documentMap[node.documents[d]].url+'</li>');
			}
			sortUnorderedList("#documents");
		
	        $("#documentMenu").css({left: mouseX(clickEvent)});
	        $("#documentMenu").css({top: mouseY(clickEvent) - popupMenuOffsetY});
     	    document.getElementById("documentMenu").className = "menuShow";

	        window.event.returnValue = false;
	        clickEvent.stopPropagation();
        
	        return;

		})		
		
		if (savedTreeState != null) {
			treeConcept.tree('setState',savedTreeState);
		}
		
		return countNodes(treeConcept.tree('getTree'))
	}
	
	/**
	 * How many nodes (include the initial node if it is a defined node) fall under the given node?
	 * @param node
	 * @returns
	 */
	function countNodes(node) {
		var result = 0;
		if (node.name !== "" || node.fullName != undefined) {
			result = 1;
		}
		for (var i=0; i < node.children.length; i++) {
			result += countNodes(node.children[i]);
		}
		
		return result;
	}
	
	function createConceptViewFunction(documentID, fullName)  {
		return function() {
			displayText(documentID, "#conceptTextBlock",fullName);
			hidePopupMenu();
			return false;
		};
	}
	
	function createKeyphraseViewFunction(documentID, keyphrase)  {
		return function() {
			displayText(documentID, "#textBlock",null, keyphrase);
			hidePopupMenu();
			return false;
		};
	}	
	
	/**
	 * Sorts an unordered list alphabetically.  Copied from http://stackoverflow.com/questions/37889443/sort-an-unordered-list-alphabetically
	 * 
	 * @param selector
	 * @returns
	 */
	function sortUnorderedList(selector) {
		var $ul = $(selector);
		$ul.find('li').sort(function(a, b) {
		    var upA = $(a).text().toUpperCase();
		    var upB = $(b).text().toUpperCase();
		    return (upA < upB) ? -1 : (upA > upB) ? 1 : 0;
		  }).appendTo(selector);
	};
	
	
	/**
	 * 
	 * 
	 * includedFiles is a set of documents IDs to include
	 */ 
	function  generateDocumentTree(data,includedFiles) {
		//$('#documentIndexBlock').css('visibility', 'visible');
		//$('#documentIndexBlock').height($(window).height() - 150);
		
		// re-claim resources from the prior tree if it exists
		if (treeDocument != undefined) {
			//save state (doesn't matter for document, does for the others)
			treeDocument.tree('destroy');
			$("#documentTreeMenu li").unbind();
			$('#documentTree').unbind();
		}
		
		// compute the ordered list of nodes to be created
		var documentTreeData = [];
		
		includedFiles.forEach(function (id){
			var doc = documentMap[id];
			var newNode = { "name":  doc.title === "" ? doc.url : doc.title, "id": doc.id, "url": doc.url }
			documentTreeData.push(newNode);
		})
		documentTreeData.sort(function (a,b) { 
			if ( a.name.startsWith("http") && b.name.startsWith("http") == false ) {
					return 1;
			}
			if ( b.name.startsWith("http") && a.name.startsWith("http") == false ) {
					return -1;
			} 
			
			return a.name.toUpperCase().localeCompare(b.name.toUpperCase());
		});
	
		// create the jqTree
		treeDocument = $('#documentTree')
		treeDocument.tree({
				data: documentTreeData,  
				useContextMenu: true,
			})
		
		// when the node is clicked, display it.
		$('#documentTree').bind( 'tree.click', function(event) {
			var node = event.node
			var clickEvent = event.click_event;
			displayText(node.id,"#documentTextBlock")
			window.event.returnValue = false;
	        clickEvent.stopPropagation();
	        
	        return;
		})
		
		//define two closure variables with the document name and ID (we really just need the ID)!
		var docID = ""
		var docName = ""
		$('#documentTree').bind('tree.contextmenu',
				function(event) {
					var node = event.node;
					docID = node.id
				    docName = node.name
				       
				    $("#documentTreeMenu").finish().toggle(100).css({ top: mouseY(event.click_event) + 'px', left:mouseX(event.click_event) + 'px'});        
			});
			 
		//When the user selects filter / not filter, create the object
		$("#documentTreeMenu li").click(function(e){
		     	e.stopPropagation()
		 	    var newFilter =	{
		        		"filterType":"document",
		        		"documentID":docID,
		        		"name":docName
		        }	 	   
		 	    switch($(this).attr("data-action")) {
		 	    	case "addFilter":    addFilter(newFilter);
		 	    	                     break;
		 	        case "addNotFilter": addAntiFilter(newFilter);
		 	                             break;
		 	    } 
		     	$("#documentTreeMenu").hide(100);
		     })
		     
		
		return documentTreeData.length;
	}

	function addFilter(newFilter) {
		if (newFilter.name === "") {return;}
		filters.push(newFilter);
		displayData();
		displayFilters(newFilter);
	}
	
	function addAntiFilter(newFilter) {
		antiFilters.push(newFilter);
		displayData();
		displayAntiFilters(newFilter);
	}
	
	
	function displayText(uuid,displayBlock, conceptFullName = null, keyPhrase = null) {    //TODO: lots of duplication in this method compared to others

		$.ajax({
			url: openke.global.Common.getRestURLPrefix()+"/document/"+documentArea+"/_doc/"+uuid,
			type:"GET",
			contentType: "application/json; charset=utf-8",
			dataType : "text JSON",
			success: function(data) {
				if (data.found == false) {
					$('#documentTextBlock').text("Document not found")
				}
				else {
					var docRecord = data;
					var url  = docRecord.url;
					var uuid = docRecord.source_uuid;
					var title = docRecord.url;
					if (docRecord.hasOwnProperty("html_title")) { title = docRecord.html_title; }				
					
					var text = escapeHtml(docRecord.text).replace(/\n/g, "<p />");
					
					//TODO: for the others, put a callback in place here...
					if (conceptFullName != null) { // if a concept was select, highlight where it occurs in the document
						var uniqueConceptValues = new Set();  // create a set of the concept values this document has for that concept....
						docRecord.concepts.forEach(function (conceptRecord) {
							if (conceptRecord.fullName == conceptFullName ) { uniqueConceptValues.add(conceptRecord.value); }
						});
						
						uniqueConceptValues.forEach(function(value) { //for each of the unique values, highlight that text
							var termwords = value //values.join("|");
							termwords = termwords.replace("$","\\$");
							//"(?:"+termwords+"[s]?)*"
							var regex = new RegExp("((?:"+termwords+")s?)([\s.,])?","gi");
							var repl = '<span style = "background-color:yellow;" tabindex="0">$1</span>$2';
							text = text.replace(regex,repl)
						})
					}
					if (keyPhrase != null) {
						keyPhrase = keyPhrase.substring(0, keyPhrase.lastIndexOf('(')).trim();
						var regex = new RegExp("((?:"+keyPhrase+")s?)([\s.,])?","gi");
						var repl = '<span style = "background-color:yellow;" tabindex="0">' + keyPhrase + '</span>';
						text = text.replace(regex,repl)
					}
				
					// text
					var newDocRecord = JSON.parse(JSON.stringify(docRecord));
					newDocRecord.text = text;
										
					var rec = new openke.component.ResultObject(uuid, title, url, "", newDocRecord, true, true);	
					$(displayBlock).html("<table style='width:100%'></table>");
					$(displayBlock + ' table').append(rec.getRecordDOM());
					rec.displayRecord();

					var additionalData = {
							domain : openke.global.Common.getDomain(),
							storageArea : documentArea,
							type : "_doc",
							title: title
					}
					var domMenu = OKAnalyticsManager.produceObjectAnalyticsMenu(uuid, "", docRecord ,url, additionalData,rec);  //note, not all of these need to be defined.  The called analytic will check
					rec.displayMenu(domMenu);	
					
					var collectionDOM = openke.view.DocumentBucketSupport.createCollectionSelect(docRecord.source_uuid,rec)
					rec.appendToMenu(collectionDOM);
					if (typeof(docRecord.user_collection) != 'undefined') {
						openke.view.DocumentBucketSupport.populateResultObject(rec,docRecord.user_collection)
					}
					
					rec.establishFullTextToggle("",true,false);
					
				}

			},

			error: function(data) {
				alert("error")
			}
		})
	}	
	
	function createFunction(callBack,filterObject) {
		return function(event) { 
			event.preventDefault(); 
			callBack(event, filterObject); 
			return false;
			};
	}
	
	function displayFilters(filterObject) {
		
		var value = filterObject.name.replace(/["]+/g, '');
		var filterLabel = $('<button class="btn btn-primary  btn-sm filterButton" data-placement="bottom" title="'+value+'"><span class="fas fa-times"></span> '+value+'</button>');
		//filterLabel.tooltip();
		
		var callback = createFunction(deleteFilter,filterObject)
		filterLabel.click(callback);
		$('#allFilters').show()
		$('#allFiltersContent').append(filterLabel);
		resizeIndexBlocks();
	}
	
	function displayAntiFilters(filterObject) {
		
		var value = filterObject.name.replace(/["]+/g, '').toString();
		
		var antiFilterLabel = $('<button class="btn btn-primary  btn-sm filterButton  filters"  data-placement="bottom" title="'+value+'"><span class="fas fa-times"></span> <span class="strikethrough">'+value+'</span></button>');
		//antiFilterLabel.tooltip();
		
		var callback = createFunction(deleteAntiFilter,filterObject);
		antiFilterLabel.click(callback);
		$('#allFilters').show()
		$('#allFiltersContent').append(antiFilterLabel)    
		resizeIndexBlocks();
	}
	
	function getPropertyFieldName(type) {
		var property; // what field contains the ID for given filter type
		if(type == "keyphrase") {
			  property = "keyphrase"
		  }
		  else if(type == "document") {
			  property = "documentID"
		  }
		  else{
			 property= "conceptFullName"
		  }
		return property;
	}
	
	/*utility function to delete from filters/antifilters*/
	function findAndRemove(array, filterObject) {
		var type  = filterObject.filterType
		var property = getPropertyFieldName(type)
		var value = filterObject[property]
		  
		array.forEach(function(result, index) {
		    if(result["filterType"] === type) {
		      if(result[property] === value)
		    	array.splice(index, 1);
		    }    
		  });
	}
	
	function deleteFilter(ele, filterObject) {
		$(ele.target).closest('.filterButton').remove();
		findAndRemove(filters,filterObject)
		displayData();
		
	    if(filters.length == 0 && antiFilters.length == 0) {
			$('#allFilters').hide()
		}
		//$('.tooltip').hide();
		resizeIndexBlocks();
	}

	function deleteAntiFilter(ele,filterObject) {
		$(ele.target).closest('.filterButton').remove();
		findAndRemove(antiFilters,filterObject)
		displayData()
		
		if(filters.length == 0 && antiFilters.length == 0) {
			$('#allFilters').hide()
		}
		//$('.tooltip').hide();
		resizeIndexBlocks();
	}	
	
	
	return {
		initialize : initialize,
		setPopupMenuYOffset: setPopupMenuYOffset
	};
}());

