/**
 * 
 * Create date - 20181120
 * Description: 
 *              
 * Usage:
 *  
 * 
 */

var AnalyticFilter = (function () {
	"use strict";

	var queryFilters = null;
	var saveOptions = {};
	
	var spacyMapping = { "PERSON" : "People",
			             "GPE"    : "Places",
			             "NORP"   : "Groups",
			             "ORG"    : "Organizations",
			             "EVENT"  : "Events",
 			             "FAC"    : "Facilities",
			             "LOC"    : "Physical Locations",
			             "PRODUCT"     : "Products",
			             "WORK_OF_ART" : "Works of Art",
			             "LAW"         : "Laws",
			             "LANGUAGE"    : "Languages",
			             "MONEY"       : "Money"};
	
	var startDate; // Filter: crawl start and end date
	var endDate;
	
	Array.prototype.unique = function() {
		return this.filter(function (value, index, self) { 
			return self.indexOf(value) === index;
		});
	}
	
	function getSelectedValues(selectFieldID) {
		var selectedValues = [];  
		$("#"+selectFieldID + " :selected").each(function(){
		        selectedValues.push($(this).val()); 
		    });
		return selectedValues;
	}
	
	function sortSelectListNumerically(selectFieldID) {
		var sel = $('#'+selectFieldID);
		var selected = sel.val(); // cache selected value, before reordering
		var opts_list = sel.find('option');
		opts_list.sort(function(a, b) { 
			var strA = $(a).text();
			var strB = $(b).text();
			var valueA = Number.parseInt(strA.substring(1,strA.indexOf(")")))
			var valueB = Number.parseInt(strB.substring(1,strB.indexOf(")")))
			
			return valueA > valueB ? -1 : 1; 
		});
		sel.html('').append(opts_list);
		sel.val(selected); // set cached selected value		
	}
	
	function sortSelectListAlphabetically(selectFieldID) {
		var sel = $('#'+selectFieldID);
		var selected = sel.val(); // cache selected value, before reordering
		var opts_list = sel.find('option');
		opts_list.sort(function(a, b) { return $(a).val() > $(b).val() ? 1 : -1; });
		sel.html('').append(opts_list);
		sel.val(selected); // set cached selected value		
	}	
	
	function sortNumerically() {
		$('select[id$="KeywordList"]').each(function () {
		    sortSelectListNumerically(this.id);
		});
	}
	
	function sortAlphabetically() {
		$('select[id$="KeywordList"]').each(function () {
		    sortSelectListAlphabetically(this.id);
		});	
	}
	
	function compareByKey(a,b) {
		return a.key.localeCompare(b.key);
	}
	
	
	function createFilter(selectField, termField,filters) {
		var keywords = getSelectedValues(selectField);
		if (keywords.length > 0) {
			var filter = ElasticSupport.createFilterField(termField,keywords);
			filters.push(filter)
		}
	}
	
	function produceFilters() {
		var filters = [];
		var typeFilters = [];
		 
		$('select[id$="KeywordList"]').each(function () {
		    var selectField = this.id;
		    var selectName = $(this).data('origname');
		    var selectedValues = getSelectedValues(selectField);
		    var conceptType = $(this).data('type');
		    
		    if(selectedValues.length > 0){
		    	switch (conceptType ) {
				case "Entity":
					typeFilters = ElasticSupport.createFilterEntity(selectName, selectedValues);
					filters.push(typeFilters);
					typeFilters = [];
					break;
				case "Concept":
					typeFilters = ElasticSupport.createFilterConcept(selectName, selectedValues);
					filters.push(typeFilters);
					typeFilters = [];
					break;
				case "Category":
					typeFilters = ElasticSupport.createFilterCategory(selectName, selectedValues);
					filters.push(typeFilters);
					typeFilters = [];
					break;
				}
		    }
		    
		});	
		
		if ($('#filterInputKeywords').val() != '' && $('#filterInputKeywords').val() != null){
			var arrKeywords = [];
			var val = $('#filterInputKeywords').val().toLowerCase();
			var keywordsFilter = [];
			
			if(val.indexOf(',') > -1){
				arrKeywords = val.split(',');
			}else{
				arrKeywords = [val];
			}

			for( var i=0; i < arrKeywords.length; i++){
				filters.push({"term": {"text": arrKeywords[i].trim() }});
			}
		}
		
		if ($('#sessionCheck').is(':checked') === false) {
			var noSessFilter = {"bool":{"must_not":{"exists":{"field":"domainDiscovery"}}}}

			filters.push(noSessFilter);
		}
		
		if ($('#filterSelectSession').is(':disabled') === false){
			var sessFilter = [];
			sessFilter = ElasticSupport.createFilterSession($('#filterSelectSession').val());
			if ( sessFilter != '' | sessFilter != null){
				filters.push(sessFilter);
			}
		}
		
		if ( $('#startTimePick').val() != '' || $('#endTimePick').val() != '' ) {
			var start, end = '';
			if ( $('#startTimePick').val() != '' ) {
				start = moment.utc( $('#startTimePick').val(), "YYYY-MM-DD HH:mm:ss");
			}
			if ( $('#endTimePick').val() != '' ) {
				end = moment.utc( $('#endTimePick').val(), "YYYY-MM-DD HH:mm:ss");
			}
			
			var datecrawlFilter = [];
			
			datecrawlFilter = ElasticSupport.createFilterDateCrawlRange(start, end);
			if ( datecrawlFilter != null || datecrawlFilter != '' ) {
				filters.push(datecrawlFilter);
			}
		}
		
		return filters;
	}
	
	function produceFiltersFromSavedState(savedState) {
		var filters = [];
		var typeFilters = [];
		var cards = savedState.cards;
		var keywords = savedState.keywords;
		var excludeSessions = savedState.excludeSessions;
		var sessions = savedState.sessionIDs;
		var crawltime = savedState.crawltime;
		
		 
		for (var i=0; i < cards.length; i++) {
			var stateItem = cards[i];
			
		    var selectName = stateItem.originalName;
		    var selectedValues = stateItem.selectedValues
		    var type = stateItem.type
		    
		    if(selectedValues.length > 0){
		    	switch (type ) {
				case "Entity":
					typeFilters = ElasticSupport.createFilterEntity(selectName, selectedValues);
					filters.push(typeFilters);
					typeFilters = [];
					break;
				case "Concept":
					typeFilters = ElasticSupport.createFilterConcept(selectName, selectedValues);
					filters.push(typeFilters);
					typeFilters = [];
					break;
				case "Category":
					typeFilters = ElasticSupport.createFilterCategory(selectName, selectedValues);
					filters.push(typeFilters);
					typeFilters = [];
					break;
				}
		    }
		}
		
		if (keywords && keywords.length > 0){
			for( var i=0; i < keywords.length; i++){
				filters.push({"term": {"text": keywords[i].trim() }});
			}
		}
		
		if (excludeSessions && excludeSessions === true) {
			var noSessFilter = {"bool":{"must_not":{"exists":{"field":"domainDiscovery"}}}}
			filters.push(noSessFilter);
		}
		
		if (sessions){
			var sessFilter = ElasticSupport.createFilterSession(sessions);
			if ( sessFilter != '' | sessFilter != null){
				filters.push(sessFilter);
			}
		}
		
		if ( crawltime ) {
			var start, end = '';
			if (crawltime.startime){
				start = crawltime.startime;
			}
			if (crawltime.endtime){
				end = crawltime.endtime;
			}
			
			var datecrawlFilter = ElasticSupport.createFilterDateCrawlRange(start, end);
			if ( datecrawlFilter != null ) {
				filters.push(datecrawlFilter);
			}
		}
		
		return filters;
	}
	
	
	function produceFilterState() {
		var selectCards = [];

		$('.selectcard').each(function () {
			var selectCardState = {
				id : this.id,
				name : this.name,
			    originalName : $(this).data('origname'),
			    title: $(this).data('title'),
			    type  : $(this).data('type'),
			    selectedValues : getSelectedValues(this.id)
			}
			selectCards.push(selectCardState);
		});
		
		var currentFilters = {
				cards: selectCards,
				sessionIDs: saveOptions['filterSelectSession'],
				sessionNames: saveOptions['filterSelectSessionNames'],
				excludeSessions: saveOptions['excludeSessions'],
				keywords: saveOptions['filterInputKeywords'],
				crawltime: saveOptions['crawled_dt']
		}
		return currentFilters;
	}
	
	
	function applyFilters() {
		var filters = produceFilters();
		
		openke.model.Analytics.setAnalyticFilterData(produceFilterState(), filters);
		
		opener.filterData(filters)
		
		LASLogger.instrEvent('application.analyticFilter.filters.applyFilters', {
			filters : filters
		});
	}
	
	
	function clearFilters() {
		$('select[id$="KeywordList"]').each(function () {
		    $(this).val("");
		});
		
		$('#filterInputKeywords').val("")
		$('#filterSelectSession option').prop('selected', false).trigger('chosen:updated');
		$('#startTimePick').val("")
		$('#endTimePick').val("")
		
		queryFilters = null;
		saveOptions = {};
		
		loadFilters({});
		
		LASLogger.instrEvent('application.analyticFilter.filters.clearFilters')
	}
	
	
	function loadFilters(savedOptions) {
		$(".selectcard").each(function () {
		    loadCard($(this).data("type"), this.name, $(this).data("origname"), this.id, savedOptions);
		});	
	}
	
	
	function updateFilters() {
		// warning this assumes that only filter selects are on the page.  if other selects are added, need to add a specific class to identify ours
		saveOptions = {}
		var arrTypes = [];
		$("select[id$='KeywordList'] option:selected").each(function(){  
			        var input = $(this);
			        var selectID = $(this).closest("select").attr("id");
			        var type = $(this).closest("select").data('type');
			        arrTypes.push(type);
					if(saveOptions.hasOwnProperty(selectID)){
			        	saveOptions[selectID].push(input.val());
			        }else{
			        	saveOptions[selectID] = [input.val()];
			        }
			    });
		
		if ($('#filterInputKeywords').val() != '' && $('#filterInputKeywords').val() != null){
			var arrKeywords = [];
			var val = $('#filterInputKeywords').val().toLowerCase();
			if(val.indexOf(',') > -1){
				arrKeywords = val.split(',');
			}else{
				arrKeywords = [val];
			}

			saveOptions['filterInputKeywords'] = arrKeywords;
		}
		
		//if 'include discovery sessions' is not checked, we need to exclude that data
		if ($('#sessionCheck').is(':checked') === false) {
			saveOptions['excludeSessions'] = true;
		}else{
			saveOptions['excludeSessions'] = false;
		}
		
		if ($('#filterSelectSession').is(':disabled') === false){
			var val = $('#filterSelectSession').val();
			if ( val != '' | val != null){
				saveOptions['filterSelectSession'] = val;
				var arrSessNames = [];
				$('#filterSelectSession option:selected').each(function(){
					arrSessNames.push($(this).text());
				});
				saveOptions['filterSelectSessionNames'] = arrSessNames;
			}
		}
			
		if ( $('#startTimePick').val() != '' || $('#endTimePick').val() != '' ){
			var start, end = '';
			if ( $('#startTimePick').val() != '' ) {
				start = moment.utc( $('#startTimePick').val(), "YYYY-MM-DD HH:mm:ss");
			}
			if ( $('#endTimePick').val() != '' ) {
				end = moment.utc( $('#endTimePick').val(), "YYYY-MM-DD HH:mm:ss");
			}
			
			var crawlTimes; 
			
			if ( start != '' && end != '' ) { 
				crawlTimes = {
					startime: start,
					endtime: end
				} 
			}else if ( start != '' ){
				crawlTimes = {
					startime: start
				}
			}else{
				crawlTimes = {
					endtime: end
				}
			}

			saveOptions['crawled_dt'] = crawlTimes;
		}
		
		var filters = produceFilters();
		queryFilters = {"bool": {"filter": filters }}
		
		loadFilters(saveOptions);
		
		LASLogger.instrEvent('application.analyticFilter.filters.updateFilters', {
			criteria : queryFilters,
			savedOption: saveOptions
		});
	}
	
	
	function searchByFilters() {
		var myFilters = produceFilters();
		
		var query= ElasticSupport.createQueryFilter(myFilters, 20, 0);

		LASLogger.instrEvent('application.analyticFilter.filters.searchByFilters', {
			criteria : query
		});
		
		$('#searchQuery').val(JSON.stringify(query));
		$('#analyzeSearchForm').submit();
		
	}
	
	
	function selectAllSessions() {
		if ( $('#filterSelectSession').is(':enabled') ) {
			$('#filterSelectSession option').prop('selected', true).trigger('chosen:updated');
		}
	}
	
	
	function deselectAllSessions() {
		if ( $('#filterSelectSession').is(':enabled') ) {
			$('#filterSelectSession option').prop('selected', false).trigger('chosen:updated');
		}
	}
	
	
	function initialize() {
		LASLogger.instrEvent('application.analyticFilter.filters');

		// Establish event handlers for Buttons
		$("#btnApplyfilter").click(applyFilters);
		$("#btnUpdatefilters").click(updateFilters);
		$("#btnClearfilters").click(clearFilters);
		$("#btnSortAlpha").click(sortAlphabetically);
		$("#btnSortFreq").click(sortNumerically);
		$("#btnSearchByFilters").click(searchByFilters);
		
		// event handler for domain checkbox (enable domain select if checked, disable if not)
		$("#sessionCheck").change(switchDomainSelect);
		
		// Establish event handlers when an entity/concept/category is selected 
		$("#filterSelectEntity").bind("change", function(){
			createCard("filterSelectEntity", "Entity");
		});
		$("#filterSelectConcept").bind("change", function(){
			createCard("filterSelectConcept", "Concept");
		});
		$("#filterSelectCategory").bind("change", function(){
			createCard("filterSelectCategory", "Category");
		});
		
		//Restore prior state if necessary
		var savedFilterState = openke.model.Analytics.getAnalyticFilterState();
		var selectItemsToRemove = { "Entity" : [], "Concept": [], "Category" : [] }
		var filters = {}
		if (savedFilterState != null) {
			
			var cards = savedFilterState.cards;

			recreateCards(cards)
			for (var i=0; i < cards.length; i++) {
				var state = cards[i];
				filters[state.id] = state.selectedValues;
				selectItemsToRemove[state.type].push(state.originalName)
			}
			
			queryFilters = {"bool": {"filter":  produceFiltersFromSavedState(savedFilterState) }}
		}
		else {
			//entity card buttons since they are loaded
			$('button[id^="cardBtnEntity"]').each(function () {
				$(this).bind("click", function(){
					$("#" + this.id).closest(".col-md-2").remove();
					var display = spacyMapping.hasOwnProperty(this.name) ? spacyMapping[this.name] : this.name; 
					var value = this.name; 
					$("#filterSelectEntity").append("<option value='"+value+"'>"+display+"</option>");
					sortSelectListAlphabetically($("#filterSelectEntity").attr("id"));
				});
			});

		}
		
		loadSelectDropDown("spacy_root", "spacy.entities", "spacy.entities.type.keyword", "filterSelectEntity", "Entity",  selectItemsToRemove.Entity);
		loadSelectDropDown("concepts_root", "concepts", "concepts.fullName.keyword", "filterSelectConcept", "Concept", selectItemsToRemove.Concept);
		//remove categories post symposium: loadSelectDropDown("concepts_root", "concepts", "concepts.category.keyword", "filterSelectCategory", "Category", selectItemsToRemove.Category);
		
		loadFilters(filters);
		
		//date fields
		$("#startTimePick").datetimepicker({
			format : 'Y-m-d H:i:s',
			onShow:function( ct ){
				this.setOptions({
					maxDate:$("#endTimePick").val()?$("#endTimePick").val():false
				})
			}
		});
		
		$("#endTimePick").datetimepicker({
			format : 'Y-m-d H:i:s',
			onShow:function( ct ){
				this.setOptions({
					minDate:$("#startTimePick").val()?$("#startTimePick").val():false
				})
			}
		});
		
		//You can show datepicker on click on the calendar icon
		$(".fa-calendar-alt").on("click", function(){
		        $(this).siblings("input").datetimepicker("show");    
		});
		
		loadSessionChosen();
		
		
		//
		//re-select any saved state
		//
		if (savedFilterState != null) {
			
			if (savedFilterState.keywords && savedFilterState.keywords.length > 0){
				$("#filterInputKeywords").val(savedFilterState.keywords.join(","));
			}
			
			if (savedFilterState.excludeSessions){
				if ( savedFilterState.excludeSessions == 'false' ) {
					$('#sessionCheck').prop('checked', true);
				}else {
					$('#sessionCheck').prop('checked', false);
				}
			}
			
			//chosen select is restored if necessary in loadSessionChosen() 

			
			if (savedFilterState.crawltime) {
				var startdatetime, enddatetime = '';
				if (savedFilterState.crawltime.startime){
					startdatetime = savedFilterState.crawltime.startime;
					$("#startTimePick").val( moment.utc(startdatetime).format("YYYY-MM-DD HH:mm:ss") );
				}
				
				if (savedFilterState.crawltime.endtime){
					enddatetime = savedFilterState.crawltime.endtime;
					$("#endTimePick").val( moment.utc(enddatetime).format("YYYY-MM-DD HH:mm:ss") );
				}
			}
		}
	}
	
	
	function recreateCards(filterState) {
		$("#cardContainer").empty();
		for (var i= 0; i < filterState.length; i++) {
			var cardState = filterState[i];
			createCardVersion2(cardState.originalName,cardState.type);
			//remove the selected option
			var dropDownSelectID = "#filterSelect"+cardState.type;
			$(dropDownSelectID+" option[value='" + cardState.originalName + "']").remove();
		}
	}
	
	
	function compare(a,b) {
		  if (a.doc_count < b.doc_count)
		    return 1;
		  if (a.doc_count > b.doc_count)
		    return -1;
		  return 0;
		}
	
	
		
	function loadConcepts(query, selectField, type, savedOptions) {
		if (queryFilters) {	query.query = queryFilters; 	}  // used for applying filters on ourselves
		
		//const charRegExp = /^\w+(?!\:\(\")(?:\s|\w|\.)+$/;
		
		
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/citations/data",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(query),
			dataType : "JSON",
			success: function(data) {
				var buckets = data.aggregations.concepts_root; 
				while (buckets.hasOwnProperty("items")) { buckets = buckets.items;}
				buckets = buckets.item.buckets;
				
				buckets.sort(compare);
				$('#'+selectField).find('option').remove();  // if we have previously loaded data, clear that out
				for (var i=0; i< buckets.length; i++) {
					if(openke.model.Analytics.isValidLabel(buckets[i].key)){
						if(type==="Category"){
							//truncate text to make more readable
							var pestleText = buckets[i].key.substring(buckets[i].key.lastIndexOf('.') + 1);
							$('#'+selectField).append($('<option>', {
							    value: buckets[i].key,
							    text: "("+ buckets[i].doc_count +") "+pestleText 
							}));
						}else{
							$('#'+selectField).append($('<option>', {
							    value: buckets[i].key,
							    text: "("+ buckets[i].doc_count +") "+buckets[i].key 
							}));
						}
					}
				}
				if (savedOptions.hasOwnProperty(selectField)) {
					$('#'+selectField).val(savedOptions[selectField]); // set cached selected value
				}
			}	
		});		
	}
	
	
	function loadCard(type, card, nameOrig, cardId, savedOptions) {
		var query;
		switch (type) {
		case "Entity":
			query = ElasticSupport.createQueryAggregationConceptsField( [ {fieldName: "spacy.entities.type.keyword", fieldValue: card} ], "spacy.entities.text.keyword", 2000, "spacy.entities");
			break;
		case "Concept":
			//have to switch back to dots from underscores we did for jquery id
			card = nameOrig;
			query = ElasticSupport.createQueryAggregationConceptsField( [ {fieldName: "concepts.fullName.keyword", fieldValue: card} ], "concepts.value.keyword", 2000, "concepts");
			break;
		case "Category":
			//have to switch back to dots from underscores we did for jquery id
			card = nameOrig;
			query = ElasticSupport.createQueryAggregationConceptsField( [ {fieldName: "concepts.category.keyword", fieldValue: card} ], "concepts.value.keyword", 2000, "concepts");
			break;
		}
		loadConcepts(query, cardId, type, savedOptions)
	}
	
	function createCardVersion2(name, type) {
		var originalName = name;
		
		if (type==="Category"||type==="Concept"){
			name = name.replace(/\./g, "_");
		}
		
		var title = spacyMapping.hasOwnProperty(name) ?  spacyMapping[name] : name;
		var title = title.toLowerCase();
		var orginalTitle = title;
		
		if (type==="Concept" && originalName.includes(".")) {
			title = originalName.substring(originalName.lastIndexOf(".")+1);
			orginalTitle = originalName;
		}
		title = title.charAt(0).toUpperCase() + title.slice(1);
		
		var cardID = "card_"+name;
		var closeButtonID = "cardBtn" + type + name;
		var selectID      = name+type+"KeywordList";
		var cardHTML = '<div class="col-md-2" id="'+cardID+'">' +
				       '  <div class="card card-default padBottom">' +
				       '    <div class="card-header"> ' +
				       '      <strong class="float-left" title="'+orginalTitle+'">'+title+'</strong>' +
                       '      <a><button class="btn btn-danger close float-right" id="'+closeButtonID+'" name="'+originalName+'" aria-label="Close">&times;</button></a>' +
                       '    </div>' +
                       '    <div class="card-body">'+
                       '      <select class="selectcard" name="'+name+'" id="'+selectID+'" data-type="'+type+'" data-origname="'+originalName+'" data-title="'+title+'" form="filterForm" multiple size=10 style="width: 100%; max-width: 100%;"> '+
				       '      </select>' +
				       '	</div>' +
				       '  </div>'+
				       '</div>'
		$('#cardContainer').append(cardHTML);
		//attach the close button
		$("#"+closeButtonID).click( function() {
			$("#"+cardID).remove();
			var dropDownSelectID = "filterSelect"+type
			var displayValue = originalName
			if (type === "Entity" ) { displayValue = spacyMapping.hasOwnProperty(originalName) ? spacyMapping[originalName] : originalName;}
			$('#'+dropDownSelectID).append("<option value='"+originalName+"'>"+displayValue+"</option>");
			sortSelectListAlphabetically(dropDownSelectID);
		});
		
	}
	
	function createCard(dropDownId, type){
		var name = $('#'+dropDownId).find(":selected").val();
		var nameOrig = name;
		var value = $('#'+dropDownId).find(":selected").val();
		var id = name + type + "KeywordList";
		var i = $('select[id$="KeywordList"]').length + 1;
		
		//special case for category or concept, since jquery can't handle id's with dots like pestles.political.country.asia.eastern_asia.China
		if(type==="Category"||type==="Concept"){
			id = id.replace(/\./g, "_");
			name = name.replace(/\./g, "_");
		}

		if(!$("#"+id).length) {
			createCardVersion2(nameOrig, type)
			
			//remove the selected option
			$('#'+dropDownId+" option[value='" + value + "']").remove();
					
			//load the card
			loadCard(type, name, nameOrig, id, {});
		}else{
			bootbox.alert(name + " already selected");
		}
	}

	
	function loadSelectDropDown(aggs, path, field, selectId, type, exclusions){
		var query =
		{
		  "size": 0,
		  "aggs": {
		    [aggs]: {
		      "nested": {
		        "path": path
		      },
		      "aggs": {
		        "item": {
		          "terms": {
		            "field": field,
		            "size": 2000
		          }
		        }
		      }
		    }
		  }
		}
		
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/citations/data",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(query),
			dataType : "JSON",
			success: function(data) {
				
				var buckets; // = data.aggregations.spacy_root;
				if(aggs==="concepts_root"){
					buckets = data.aggregations.concepts_root;
		        }else{
		        	buckets = data.aggregations.spacy_root;
		        }
				while (buckets.hasOwnProperty("items")) { buckets = buckets.items;}
				buckets = buckets.item.buckets;
				buckets.sort(compareByKey);
				buildDropdown(buckets, $("#"+selectId), 'Select an option', type,exclusions);
			}
		});
	}


    function buildDropdown (result, dropdown, emptyMessage, type, exclusions){
    	var idCounter = 0;
        // Remove current options
        dropdown.html('');

        // Add the empty option with the empty message
        dropdown.append('<option value="">' + emptyMessage + '</option>');

        // Check result isn't empty
        if(result != ''){
            // Loop through each of the results and append the option to the dropdown
            $.each(result, function(k, v) {
            	var selectId = "#" + v.key + type + "KeywordList";
            	if(!$(selectId).length){
            		if(v.doc_count > 0){
            			if (exclusions.includes(v.key)) { return; }
            			var value = v.key
            			var display = v.key
            			if (type === "Entity" && spacyMapping.hasOwnProperty(value)) { display = spacyMapping[value]}
	            		dropdown.append('<option value="' + value + '">' + display + '</option>');
	            		idCounter++;
            		}
            	}
            });
        }
    }
    
	
    /*function getSessions() {
    	$.getJSON(openke.global.Common.getRestURLPrefix()+"/searchSession", loadSessionDropDown);
    }

    function loadSessionDropDown(data) {
    	var sessionDropdown = $('#filterSelectSession');
    	sessionDropdown.html('');
    	
    	if(data != ''){
	    	for (var i = 0; i < data.length; i++) {
	    		var newRow = data[i];
	    		sessionDropdown.append('<option value="' + newRow.sessionID + '">' + newRow.sessionName + '</option>');
	    	}	
    	}
    }*/
    
    function loadSessionChosen(){
    	var sessionDropdown = $('#filterSelectSession');
    	var savedFilterState = openke.model.Analytics.getAnalyticFilterState();
    	
    	$.getJSON(openke.global.Common.getRestURLPrefix()+"/searchSession", function(json){
    		sessionDropdown.empty();

    		if (savedFilterState != null && savedFilterState.sessionIDs && savedFilterState.sessionIDs.length > 0) {
    			var arrSessions = savedFilterState.sessionIDs;
    			var arrNames = savedFilterState.sessionNames;
    			$.each(json, function(index, item){
    				if ( jQuery.inArray(item.sessionID, arrSessions) !== -1 ){
    					sessionDropdown.append('<option value="' + item.sessionID + '" selected>' + item.sessionName + '</option>');
    				}else{
    					sessionDropdown.append('<option value="' + item.sessionID + '">' + item.sessionName + '</option>');
    				}
        		});
    			
    		}else{
    			$.each(json, function(index, item){
        			sessionDropdown.append('<option value="' + item.sessionID + '">' + item.sessionName + '</option>');
        		});
    		}
    		
    		sessionDropdown.chosen();
    	});
    }
    
    function switchDomainSelect(){
    	if ( $("#sessionCheck").is(":checked")) {
    		$("#filterSelectSession").prop('disabled', false).trigger("chosen:updated");
    	}else{
    		$('#filterSelectSession option').prop('selected', false).trigger('chosen:updated');
    		$("#filterSelectSession").prop('disabled', true).trigger("chosen:updated");
    	}
    }

	
	return {
		initialize : initialize,
	};
}());

var callback = function (){
	   $('.chosen-container.chosen-container-multi').innerWidth("100%"); 
	};

$(document).ready(function() {
	AnalyticFilter.initialize();
	$('.chosen-container.chosen-container-multi').innerWidth("100%");
})

$(window).resize(callback);

