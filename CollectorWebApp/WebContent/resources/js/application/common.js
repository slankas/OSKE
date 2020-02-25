if (typeof openke == 'undefined') { openke = {} }
if (typeof openke.global == 'undefined') {	openke.global = {}  }
openke.global.Common = (function () {
	"use strict";
	
	// used throughout all of the application to get the current domain for the application - which is critical for almost all
	// of the URLs back to the server.  The hidden input field is set in header.jsp.  User authorization ensures security if the client changes the value.
	function getDomain() {
		var domain = $('#domain').val();
		
		if( !(domain && domain !== "null" )) {
			domain="system";
		}
		return domain;
	}
	
	// returns the "context"/"application" root of the application on the server...
	// if not set properly, returns "/openke/"
	function getContextRoot() {
		var contextRoot = $('#applicationContextRoot').val();
		
		if( !(contextRoot && contextRoot !== "null" )) {
			contextRoot="/openke/";
		}
		return contextRoot;
	}
	
	// convenience method that returns the contextRoot + "rest/"+domain
	function getRestURLPrefix() {
		return getContextRoot()+"rest/"+openke.global.Common.getDomain();
	}
	
	function getPageURLPrefix() {
		return getContextRoot()+openke.global.Common.getDomain();
	}
	
	// throttles calls to a function to a specified time period
	function debounce(func, wait, immediate) {
	    var timeout;
	    return function() {
	        var context = this,
	            args = arguments;
	        var later = function() {
	            timeout = null;
	            if (!immediate) func.apply(context, args);
	        };
	        var callNow = immediate && !timeout;
	        clearTimeout(timeout);
	        timeout = setTimeout(later, wait);
	        if (callNow) func.apply(context, args);
	    };
	};
	
	var privateMembers = {
			
	}
	return {
		getDomain : getDomain,
		getContextRoot : getContextRoot,
		getRestURLPrefix : getRestURLPrefix,
		getPageURLPrefix : getPageURLPrefix,
		
		debounce : debounce,
		
		privateMembers : privateMembers
	};
}());
 
function escapeHtml(text) {
	var map = {
		'&' : '&amp;',
		'<' : '&lt;',
		'>' : '&gt;',
		'"' : '&quot;',
		"'" : '&#039;'
	};

	return text.replace(/[&<>"']/g, function(m) {
		return map[m];
	});
}

function encodeSingleQuote(str) {
	str = str.replace("&apos;","'");
	str = str.replace("&#x27;","'");
	str = str.replace("'","\\'");
	
	return str;
}

function escapeHtmlNewLine(text) {
	var map = {
		'\n' : '<br></br>',
		'\t' : '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;',
	};

	return text.replace(/[\n\t]/g, function(m) {
		return map[m];
	});
}

function escapeHtmlNewLine2(text) {
	var map = {
		'\n' : '<br>',
		'\t' : '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;',
	};

	return text.replace(/[\n\t]/g, function(m) {
		return map[m];
	});
}

function isNormalInteger(str) {
    var n = Math.floor(Number(str));
    return String(n) === str && n >= 0;
}

// from: http://stackoverflow.com/questions/3710204/how-to-check-if-a-string-is-a-valid-json-string-in-javascript-without-using-try
function isValidJSON (jsonString){
    try {
        var o = JSON.parse(jsonString);

        // Handle non-exception-throwing cases:
        // Neither JSON.parse(false) or JSON.parse(1234) throw errors, hence the type-checking,
        // but... JSON.parse(null) returns null, and typeof null === "object", 
        // so we must check for that, too. Thankfully, null is falsey, so this suffices:
        if (o && typeof o === "object") {
            return true;
        }
    }
    catch (e) { }

    return false;
};


function isPositiveInteger(str) {
    var n = ~~Number(str);
    return String(n) === str && n > 0;
}

function isJSON(str) {
    try {
        JSON.parse(str);
    } catch (e) {
        return false;
    }
    return true;
}

// Checks if a value is an array (true), false otherwise.  From https://webbjocke.com/javascript-check-data-types/
function isArray (value) {
	return value && typeof value === 'object' && value.constructor === Array;
}

// checks if a value is an object.  https://webbjocke.com/javascript-check-data-types/
function isObject (value) {
	return value && typeof value === 'object' && value.constructor === Object;
}

function openNewWindow(data, title) {
	var screen_width = screen.width * .5;
    var screen_height = screen.height* .5;
    var top_loc = screen.height *.15;
    var left_loc = screen.width *.1;
    
    var css = '<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/bootstrap/css/bootstrap.css" />' +
	  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/demonstrator.css" />' +
	  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/elasticSearch/light_style.css" />' +
	  		'<link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.10.10/css/dataTables.jqueryui.min.css">' ;

    var js =  
    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/external/jquery-3.1.1.js"></script>' +
    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/bootstrap/js/bootstrap.js"></script>' +
    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/external/bootbox.js"></script>' +
    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/LASLogger.js"></script>' +
    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/application/common.js"></script>' +
    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/elasticSearch/jsontree.min.js"></script>';

			
    var myWindow = window.open("",'_blank','toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);

    myWindow.document.write(css);
    if (typeof title !== 'undefined') {
    	myWindow.document.write('<h2>'+title+'</h2>');
    }
    
    myWindow.document.write('<div class="form-group col-md-12">' + data + '</div>' + js);
    myWindow.focus();
    
    return false;
}

//from: http://stackoverflow.com/questions/19491336/get-url-parameter-jquery-or-how-to-get-query-string-values-in-js
var getUrlParameter = function getUrlParameter(sParam) {
    var sPageURL = decodeURIComponent(window.location.search.substring(1)),
        sURLVariables = sPageURL.split('&'),
        sParameterName,
        i;

    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');

        if (sParameterName[0] === sParam) {
            return sParameterName[1] === undefined ? true : sParameterName[1];
        }
    }
};


/**
*  Base64 encode / decode
*  http://www.webtoolkit.info
*  Include because atob/btoa can't handle utf-8
**/
var Base64 = {

    // private property
    _keyStr: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="

    // public method for encoding
    , encode: function (input)
    {
        var output = "";
        var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
        var i = 0;

        input = Base64._utf8_encode(input);

        while (i < input.length)
        {
            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);

            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;

            if (isNaN(chr2))
            {
                enc3 = enc4 = 64;
            }
            else if (isNaN(chr3))
            {
                enc4 = 64;
            }

            output = output +
                this._keyStr.charAt(enc1) + this._keyStr.charAt(enc2) +
                this._keyStr.charAt(enc3) + this._keyStr.charAt(enc4);
        } // Whend 

        return output;
    } // End Function encode 


    // public method for decoding
    ,decode: function (input)
    {
        var output = "";
        var chr1, chr2, chr3;
        var enc1, enc2, enc3, enc4;
        var i = 0;

        input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");
        while (i < input.length)
        {
            enc1 = this._keyStr.indexOf(input.charAt(i++));
            enc2 = this._keyStr.indexOf(input.charAt(i++));
            enc3 = this._keyStr.indexOf(input.charAt(i++));
            enc4 = this._keyStr.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output = output + String.fromCharCode(chr1);

            if (enc3 != 64)
            {
                output = output + String.fromCharCode(chr2);
            }

            if (enc4 != 64)
            {
                output = output + String.fromCharCode(chr3);
            }

        } // Whend 

        output = Base64._utf8_decode(output);

        return output;
    } // End Function decode 


    // private method for UTF-8 encoding
    ,_utf8_encode: function (string)
    {
        var utftext = "";
        string = string.replace(/\r\n/g, "\n");

        for (var n = 0; n < string.length; n++)
        {
            var c = string.charCodeAt(n);

            if (c < 128)
            {
                utftext += String.fromCharCode(c);
            }
            else if ((c > 127) && (c < 2048))
            {
                utftext += String.fromCharCode((c >> 6) | 192);
                utftext += String.fromCharCode((c & 63) | 128);
            }
            else
            {
                utftext += String.fromCharCode((c >> 12) | 224);
                utftext += String.fromCharCode(((c >> 6) & 63) | 128);
                utftext += String.fromCharCode((c & 63) | 128);
            }

        } // Next n 

        return utftext;
    } // End Function _utf8_encode 

    // private method for UTF-8 decoding
    ,_utf8_decode: function (utftext)
    {
        var string = "";
        var i = 0;
        var c, c1, c2, c3;
        c = c1 = c2 = 0;

        while (i < utftext.length)
        {
            c = utftext.charCodeAt(i);

            if (c < 128)
            {
                string += String.fromCharCode(c);
                i++;
            }
            else if ((c > 191) && (c < 224))
            {
                c2 = utftext.charCodeAt(i + 1);
                string += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
                i += 2;
            }
            else
            {
                c2 = utftext.charCodeAt(i + 1);
                c3 = utftext.charCodeAt(i + 2);
                string += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
                i += 3;
            }

        } // Whend 

        return string;
    } // End Function _utf8_decode 

}
