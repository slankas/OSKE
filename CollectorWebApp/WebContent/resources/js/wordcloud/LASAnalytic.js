var data;
$(document).ready(function() {
	LASLogger.instrEvent('application.wordCloudPage');
    
	window.page = "word_cloud"; // Page name to be stored in window object so that LasHeader.js could access it for logging
});

$('input:radio[name=frequency]').change( function(){	    	
	renderCloud();
});

function renderWordCloud(words) {
	data = words;
	renderCloud();
}

function renderCloud() {
	$("#divWordCloud").empty();
	var frequency = $('input[name=frequency]:checked').val();

	data.sort(function(a, b) { 
	    return b[frequency] - a[frequency];
	});
	
	var fill = d3.scale.category20();

	var maxFrequency = data[0][frequency];
	var minFrequency = data[data.length - 1][frequency];

	var fontMin = 20;
	var fontMax = 72;

	var layout = d3.layout.cloud().size([ 500, 500 ]).words(
			data.map(function(d) {
				return {
					text : d.word,
					size : d[frequency] == minFrequency ? fontMin
							: (d[frequency] / maxFrequency)
									* (fontMax - fontMin) + fontMin,
					test : "haha"
				};
			})).padding(5).rotate(function() {
		return ~~(Math.random() * 2) * 90;
	}).font("Impact").fontSize(function(d) {
		return d.size;
	}).on("end", draw);

	layout.start();

	function draw(words) {
		d3.select('.divWordCloud').append("svg:svg").attr("width",
				layout.size()[0]).attr("height", layout.size()[1]).append("g")
				.attr(
						"transform",
						"translate(" + layout.size()[0] / 2 + ","
								+ layout.size()[1] / 2 + ")").selectAll("text")
				.data(words).enter().append("text").style("font-size",
						function(d) {
							return d.size + "px";
						}).style("font-family", "Impact").style("fill",
						function(d, i) {
							return fill(i);
						}).attr("text-anchor", "middle").attr(
						"transform",
						function(d) {
							return "translate(" + [ d.x, d.y ] + ")rotate("
									+ d.rotate + ")";
						}).text(function(d) {
					return d.text;
				});
	}

}