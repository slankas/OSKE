/*$("#lnkExport").click(function(event) {
	event.preventDefault();
});
*/

/**
 * Allows the application to export tables and JSON data to CSV files
 * 
 */
var LASExport = (function () {
	"use strict";

	/**
	 * Export an HTML table to a csv file.
	 * 
	 * @param tablename CSS selector (e.g., "#tableid") to a particular table
	 * @returns
	 */
	function exportTableToCSV(tablename) {
		var table = $(tablename);
		filename = $("#pageName").val() + ".csv";
		var $rows = table.find('tr:has(td)'),
		// Temporary delimiter characters unlikely to be typed by keyboard
		// This is to avoid accidentally splitting the actual contents
		tmpColDelim = String.fromCharCode(11), // vertical tab character
		tmpRowDelim = String.fromCharCode(0), // null character
	
		// actual delimiter characters for CSV format
		colDelim = '","', rowDelim = '"\r\n"',
	
		$labels = table.find('thead tr');
		csv = '"'
				+ $labels.map(function(i, label) {
					var $label = $(label), $cols = $label.find('th');
	
					return $cols.map(function(j, col) {
						var $col = $(col), text = $col.text();
	
						return text.replace(/"/g, '""'); // escape double quotes
	
					}).get().join(tmpColDelim);
	
				}).get().join(tmpRowDelim).split(tmpRowDelim).join(rowDelim).split(
						tmpColDelim).join(colDelim) + rowDelim,
	
		// Grab text from table into CSV formatted string
		csv += $rows.map(function(i, row) {
			var $row = $(row), $cols = $row.find('td');
	
			return $cols.map(function(j, col) {
				var $col = $(col), text = $col.text();
	
				return text.replace(/"/g, '""'); // escape double quotes
	
			}).get().join(tmpColDelim);
	
		}).get().join(tmpRowDelim).split(tmpRowDelim).join(rowDelim).split(
				tmpColDelim).join(colDelim)
				+ '"',
	
		// Data URI
		csvData = 'data:application/csv;charset=utf-8,' + encodeURIComponent(csv);
	
		// alert("filename: " + filename);
		// alert(csvData);
		if (msieversion()) {
			var IEwindow = window.open();
			IEwindow.document.write('sep=,\r\n' + csvData);
			IEwindow.document.close();
			IEwindow.document.execCommand('SaveAs', true, filename);
			IEwindow.close();
		} else {
			var link = document.createElement("a");
			link.href = csvData;
	
			// set the visibility hidden so it will not effect on your web-layout
			link.style = "visibility:hidden";
			link.download = filename;
	
			// this part will append the anchor tag and remove it after automatic
			// click
			document.body.appendChild(link);
			link.click();
			document.body.removeChild(link);
		}
	}

	function msieversion() {
		var ua = window.navigator.userAgent;
		var msie = ua.indexOf("MSIE ");
		// alert("user agent:"+ua);
		if (msie > 0 || !!navigator.userAgent.match(/Trident.*rv\:11\./)) // If
																			// Internet
																			// Explorer,
																			// return
																			// true
		{
			return true;
		} else { // If another browser,
			return false;
		}
		return false;
	}
	
	function escapeDoubleQuote(value) {
		if (typeof value === "string") {
			return value.replace(/"/g,"\"\"");
		}
		else {
			return value;
		}
	}

	/**
	 * Exports a json array to a CSV file
	 * 
	 * Adapted from http://jsfiddle.net/hybrid13i/JXrwM/  by Ashish Panwar
	 * 
	 * @param jsonData  should be a json array. Will attempt to convert a String to JSON.
	 * @param reportName  title for the array
	 * @param columns   array of field names to use as the columns.  If not present, derive from the first record
	 * @returns
	 */
	function exportJSONToCSV(jsonData, reportName, columns) {
	    
	    var arrData = typeof jsonData != 'object' ? JSON.parse(jsonData) : jsonData;   //If jsonData is not an object then JSON.parse will parse the JSON string in an Object
	    	    
	    var CSV = '';    
	    
	    if (typeof columns != 'object') {
	        columns = [];
	        for (var index in arrData[0]) {
	            columns.push(index);
	        }
	    }
	   
        CSV += columns.join() + '\r\n';                     //append header row with line break
	    
	    for (var i = 0; i < arrData.length; i++) {
	        var row = "";
	        
	        for (var index in columns) {
	            row += '"' + escapeDoubleQuote(arrData[i][columns[index]]) + '",';
	        }

	        row.slice(0, row.length - 1);  //takes out the last ","
	        
	        CSV += row + '\r\n';
	    }

	    if (CSV == '') {        
	        alert("Invalid data");
	        return;
	    }   
	    
	    var fileName = reportName.replace(/ /g,"_") + ".csv"; // replaces spaces with underscores in name 
	    

	    var uri = 'data:text/csv;charset=utf-8,' + escape(CSV);
	    
	    var link = document.createElement("a");    
	    link.href = uri;
	    link.class += "hideVisibility";
	    link.download = fileName;
	    
	    document.body.appendChild(link);
	    link.click();
	    document.body.removeChild(link);
	    
	}	
	
	// given an html string(fragment), download it, using the optional filename
	function exportHTMLBlockToWordDoc(htmlString, title ='', filename=''){
	    var preHtml = "<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'><head><meta charset='utf-8'><title>"+title+"</title></head><body>";
	    var postHtml = "</body></html>";
	    var html = preHtml+htmlString+postHtml;

	    var blob = new Blob(['\ufeff', html], {
	    	type: 'application/msword'
	    });
	    
	    // Specify link url
	    var url = 'data:application/vnd.ms-word;charset=utf-8,' + encodeURIComponent(html);
	    
	    // Specify file name
	    if (filename !=null && filename != '' && !filename.endsWith(".doc")) {
	    	filename = filename?filename+'.doc':'document.doc';
	    }
	    
	    // Create download link element
	    var downloadLink = document.createElement("a");

	    document.body.appendChild(downloadLink);
	    
	    if(navigator.msSaveOrOpenBlob ){
	        navigator.msSaveOrOpenBlob(blob, filename);
	    }else{
	        // Create a link to the file
	        downloadLink.href = url;
	        
	        // Setting the file name
	        downloadLink.download = filename;
	        
	        //triggering the function
	        downloadLink.click();
	    }
	    
	    document.body.removeChild(downloadLink);		
	}
	
	
	var privateMembers = {
			msieversion : msieversion
		};
	
	
	return {
		exportTableToCSV: exportTableToCSV,
		exportJSONToCSV: exportJSONToCSV,
		exportHTMLBlockToWordDoc : exportHTMLBlockToWordDoc,
		privateMembers : privateMembers
	};
}());