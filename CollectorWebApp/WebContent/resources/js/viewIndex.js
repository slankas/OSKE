/**
 *
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


$(document).ready(function() {
	documentArea  = $('#documentArea').val();;
	documentIndexID = $('#documentIndexID').val();;	
	openke.view.DocumentIndexView.initialize(documentArea,documentIndexID);
})



