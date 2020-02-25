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
