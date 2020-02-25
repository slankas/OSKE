
/**
 * Allows the application to export tables and JSON data to CSV files
 * 
 * Requires jQuery and dataTables javascript files to have been loaded.
 * 
 */
var LASTable = (function () {
	"use strict";


    /**
     * Display a JSON array of objects in a DataTable.
     * 
     * @param jsonData array of json data that has the elements in columnFields present.
     * @param columnLabels what are the labels to use at the top of the table
     * @param columnFields what are the actual field names to use when retrieving data for a column
     * @param tableID   ID of the table element to convert on the HTML page
     * @returns nothing
     */
	function displayJSONInTable(jsonData, columnLabels, columnFields, tableID, sort) {
		
		if ( $.fn.DataTable.isDataTable('#'+tableID) ) {
			var table = $('#'+tableID).DataTable();
		    for (var i=0; i< jsonData.length; i++) {
		    	table.row.add(jsonData[i]);
		    }
			if (jsonData.length > 0) {
				table.draw();
			}
		} else {
			sort = sort || [[ 0, "asc" ]];
			
			var columns = [];
			
			for (var index in columnLabels) {
				var col = { title: columnLabels[index],
						    data:  columnFields[index]
				          };
				columns.push(col);
			}
			
			$('#'+tableID).DataTable( {
		        data: jsonData,
		        columns: columns, 
		        order: sort,
		        "lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]]
		    } );
		}		
		

	}
	
	var privateMembers = {
		};
	
	
	return {
		displayJSONInTable: displayJSONInTable,
		privateMembers : privateMembers
	};
}());