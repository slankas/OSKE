var orginalText="";
$( document ).ready(function() {
	$( "#slider" ).slider({
		  value: 100,
		  slide: function( event, ui ) { $("#sliderVal").text(ui.value); },
		  stop: function (event, ui)   { processSummary()  }	
	});
	
	$('#annotate').click(function() {
		processSummary();
	});
	
	
	orginalText = $('#originalText').html();
	processSummary();
});

//TODO: replace this with a call to summarizeText(fullText, summarizationLevel, documentUUID, callback) {
function processSummary() {
	LASLogger.instrEvent('application.textsummary', { "ratio" : $('#slider').slider("option", "value"), "method": "textRank" });
	
	$('#originalText').html(orginalText)
	var postData = {
			"ratio" : $('#slider').slider("option", "value"),
			"text" : $('#originalText').text()
	};
	$.ajax({
		type : "POST",
		url : openke.global.Common.getRestURLPrefix()+"/summary/textRankSummary/",
		data : JSON.stringify(postData),
		success : displaySummary,
		error : function(data) {
			document.getElementById("err_label").style.display = "inline";
			document.getElementById("err_label").innerHTML = data.responseText;
		},
		dataType : "json",
		contentType : "application/json"
	});		
}

function escapeRegExp(str) {
	  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"); // $& means the whole matched string
	}


function displaySummary(data) {
	
	var summary = "";
	var text = $('#originalText').text();
	
	for (var j=0;j<data.summary.length;j++) {
		summary = summary + data.summary[j] +"\n\n"
		
		text = text.replace(new RegExp(escapeRegExp(data.summary[j]), 'g'), "<span style='background:lightgray;'>"+data.summary[j]+"</span>");
	}
	summary = summary.trim().replace(/\n/g, "<br>");
	text = text.replace(/\n/g, "<br>");
	
	
	$('#originalText').html(text);
	$('#summaryText').html(summary)
	
	if ($('#annotate').prop('checked')) {
		var postData = { text: summary, confidence: 0.40 }
		$.ajax({
			type : "POST",
			url : openke.global.Common.getRestURLPrefix()+"/analytics/textAnalytics/annotateResources",
			data : JSON.stringify(postData),
			success: function(data) {
				
				var html = summary;
				
				var resourceArray = data.Resources;
				
				for (var i=resourceArray.length-1; i >=0; i--) {
					var rec = resourceArray[i];
					var offset = rec["@offset"]
					var surfaceForm = rec["@surfaceForm"]
					var newText="<a target=_blank href='"+rec["@URI"]+"'>"+surfaceForm+"</a>";
					
					var foundPosition = html.indexOf(surfaceForm,offset);
					if (foundPosition > 0) {
						html = html.substring(0,foundPosition) + newText + html.substring(foundPosition+surfaceForm.length)
					}
				}
				$('#summaryText').html(html);
				
			},
			error : function(data) {
				document.getElementById("err_label").style.display = "inline";
				document.getElementById("err_label").innerHTML = data.responseText;
			},
			dataType : "json",
			contentType : "application/json"
		});		
	}
}


