/**
 * Module: ElasticSupport
 * 
 * Create date - 20171101
 * Description: 
 * Provides methods for creating search queries on the client side.
 * 
 * Note: From a security perspective, we *really* need to be doing this on the server
 *              
 * Usage: creates
 *  
 * 
 */

var ElasticSupport = (function () {
	"use strict";

	var queryFilters = null;
	

	function createFilterField(fieldName, fieldValue) {
		var result = null;

		if (fieldValue.length > 0) {
			var filter = { "terms" : { } }
			filter.terms[fieldName] = fieldValue
			return filter;
		}
		else {
			return null;
		}
	}

	function createFilterEntity(name, values) {
		var filter = {"nested": {
	            "path": "spacy.entities",
	            "query": {"bool": {"filter": [
	            ]}}
	        }}
		
		if (name && name ) {
			filter.nested.query.bool.filter.push({"term": {"spacy.entities.type.keyword": name }});
		}
		
		if (values && values && Array.isArray(values) && values.length >0) {
			filter.nested.query.bool.filter.push({"terms": {"spacy.entities.text.keyword": values }});
		}
		return filter;
	}
	
	function createFilterConcept(name, values) {
		var filter = {"nested": {
	            "path": "concepts",
	            "query": {"bool": {"filter": [
	            ]}}
	        }}
		
		if (name && name ) {
			filter.nested.query.bool.filter.push({"term": {"concepts.fullName.keyword": name }});
		}
		
		if (values && values && Array.isArray(values) && values.length >0) {
			filter.nested.query.bool.filter.push({"terms": {"concepts.value.keyword": values }});
		}
		return filter;
	}
	
	function createMustEntity(name, value) {
		var filter = {"nested": {
	            "path": "spacy.entities",
	            "query": {"bool": {"must": [
	            ]}}
	        }}
		
		if (name && name ) {
			filter.nested.query.bool.must.push({"term": {"spacy.entities.type.keyword": name }});
		}
		
		if (value && value) {
			filter.nested.query.bool.must.push({"term": {"spacy.entities.text.keyword": value }});
		}
		return filter;
	}
	
	function createMustConcept(name, value) {
		var filter = {"nested": {
	            "path": "concepts",
	            "query": {"bool": {"must": [
	            ]}}
	        }}
		
		if (name && name ) {
			filter.nested.query.bool.must.push({"term": {"concepts.fullName.keyword": name }});
		}
		
		if (value && value) {
			filter.nested.query.bool.must.push({"term": {"concepts.value.keyword": value }});
		}
		return filter;
	}
	
	function createMustGeotag(name) {
		var filter = {"nested": {
	            "path": "geotag",
	            "query": {"bool": {"must": [
	            ]}}
	        }}
		
		if (name && name ) {
			filter.nested.query.bool.must.push({"match": {"geotag.geoData.primaryCountryName.keyword": name }});
		}

		return filter;
	}
	
	function createFilterSession(sessionID) {
		var filter = {"nested": {
	            "path": "domainDiscovery.retrievals",
	            "query": {"bool": {"should": [
	            ]}}
	        }}
		
		if (Array.isArray(sessionID)) {
			for(var i=0; i < sessionID.length; i++){
				filter.nested.query.bool.should.push({"match": {"domainDiscovery.retrievals.sessionID.raw": sessionID[i] }});
			}
		}else{
			filter.nested.query.bool.should.push({"match": {"domainDiscovery.retrievals.sessionID.raw": sessionID }});
		}
		return filter;
	}
	
	function createFilterDateCrawlRange(start, end) {
		//handle if they provide one or all
		var gte, lte, filter;
		if ( start != null || start != '' ){ gte = start; }
		if ( end != null || end != '' ){ lte = end; }
		
		if (gte && lte  ) {
			filter = {"range" : {
	            "crawled_dt" : {
	                "gte" : start,
	                "lte" : end
	            }
			  }
	        }
		} else if (gte ) {
			filter = {"range" : {
	            "crawled_dt" : {
	                "gte" : start
	            }
			  }
	        }
		} else if (lte ) {
			filter = {"range" : {
	            "crawled_dt" : {
	                "lte" : end
	            }
			  }
	        }
		}
		
		return filter;
	}
	
	function createFilterCategory(name, values) {
		var filter = {"nested": {
            "path": "concepts",
            "query": {"bool": {"filter": [
            ]}}
        }}
	
		if (name && name ) {
			filter.nested.query.bool.filter.push({"term": {"concepts.category.keyword": name }});
		}
		
		if (values && values && Array.isArray(values) && values.length >0) {
			filter.nested.query.bool.filter.push({"terms": {"concepts.value.keyword": values }});
		}
		return filter;
	}
	

	function createQueryFilter(filters, size, from) {
		var query={ "query": {  "bool": { "filter": filters  }  },
		        "size":size, "from":from }
		
		return query;
	}

	function createQueryAggregationField(fieldName, size) {
		return {"size":0,"aggs":{"item":{"terms":{"field":fieldName,"order":{"_count":"desc"},"size":size}}}}
	}

	/**
	 * 
	 * @param parameterFieldValues array of JSON objects that contain a fieldName and a fieldValue.
	 */
	function createQueryAggregationConceptsField(parentFieldValues, aggregationField, size, pathFlag) {
		var path = "concepts";
		if (pathFlag){ path = pathFlag; }
		var root = "concepts_root";
		
        var query = 		
        	{
			  "size": 0,
			  "aggs": {
			    [root]: {
			      "nested": { "path": path }
			    }
			  }
        	}
        var aggQuery;
        if(root==="concepts_root"){
        	aggQuery = query.aggs.concepts_root;
        }else{
        	aggQuery = query.aggs.spacy_root;
        }
        
        for (var i=0; i < parentFieldValues.length; i++) {
        	var pfObject = parentFieldValues[0];

        	var termObject = {}
        	termObject[pfObject.fieldName] = pfObject.fieldValue;
        	
        	var subAggsQuery = {"items": { "filter": { "term": termObject  } }}
        	
        	aggQuery.aggs = subAggsQuery
        	aggQuery = subAggsQuery.items

        }

        aggQuery.aggs = {
                "item": {
	                  "terms": {
	                    "field": aggregationField,
	                    "size": size
	                  }
	                }
        }

        return query;
		
	}
	
	
	function createQueryChoroplethMap(yAxisField, yAxisSize=35) {
		
		var yPath;
		var yTerm;
		var yField;
		var yActualFieldName = yAxisField.substring(yAxisField.indexOf(":")+1);
		
		if (yAxisField.startsWith('concept:') ){
			yPath = "concepts";
			yTerm = {"concepts.fullName.keyword": yActualFieldName };
			yField = "concepts.value.keyword";
		}
		else if (yAxisField.startsWith("entity:")){
			yPath = "spacy.entities";
			yTerm = {"spacy.entities.type.keyword": yActualFieldName };
			yField = "spacy.entities.text.keyword";
		}
		else {
			LASLogger.log(LASLogger.LEVEL_FATAL, "ElasticSupport.createQueryChoroplethMap: field does not start with an appropriate type: " + yAxisField);
			return null;
		}
		
		var aggsClause =  {
			    "item": {
			      "nested": {  "path": "spacy.entities"     },
			      "aggs": {
			        "item": {
			          "filter": { "term": {"spacy.entities.type.keyword": "GPE" } },
			          "aggs": {
			            "item": {
			              "terms": {  
			            	  "field": "spacy.entities.text.keyword", 
			            	  "size": xAxisSize   },
			              "aggs": {
			                "item": {
			                  "reverse_nested": {},
			                  "aggs": {
			                    "item": {
			                      "nested": { "path": yPath   },
			                      "aggs": {
			                        "item": {
			                          "filter": { "term": yTerm   },
			                          "aggs": {
			                            "item": {
			                              "terms": {
			                                "field": yField,
			                                "size": yAxisSize
			                              },
			                              "aggs" : {
			                                "root_document" : { "reverse_nested": {}}
			                              }
			                            }
			                          }
			                        }
			                      }
			                    }
			                  }
			                }
			              }
			            }
			          }
			        }
			      }
			    }
			  }
			
    return aggsClause;
	}
	
	function createQueryHeatMap(xAxisField,yAxisField,xAxisSize=35, yAxisSize=35) {
		var xPath;
		var xTerm;
		var xField;
		var yPath;
		var yTerm;
		var yField;
	
		var xActualFieldName = xAxisField.substring(xAxisField.indexOf(":")+1);
		var yActualFieldName = yAxisField.substring(yAxisField.indexOf(":")+1);
		
		if (xAxisField.startsWith('concept:') ){
			xPath = "concepts";
			xTerm = {"concepts.fullName.keyword": xActualFieldName };
			xField = "concepts.value.keyword";
		}
		else if (xAxisField.startsWith("entity:")){
			xPath = "spacy.entities";
			xTerm = {"spacy.entities.type.keyword": xActualFieldName };
			xField = "spacy.entities.text.keyword";
		}
		else {
			LASLogger.log(LASLogger.LEVEL_FATAL, "ElasticSupport.createQueryHeatMap: field does not start with an appropriate type: "+ xAxisField);
			return null;
		}
		
		if (yAxisField.startsWith('concept:') ){
			yPath = "concepts";
			yTerm = {"concepts.fullName.keyword": yActualFieldName };
			yField = "concepts.value.keyword";
		}
		else if (yAxisField.startsWith("entity:")){
			yPath = "spacy.entities";
			yTerm = {"spacy.entities.type.keyword": yActualFieldName };
			yField = "spacy.entities.text.keyword";
		}
		else {
			LASLogger.log(LASLogger.LEVEL_FATAL, "ElasticSupport.createQueryHeatMap: field does not start with an appropriate type: " + yAxisField);
			return null;
		}		
		
		var aggsClause =  {
				    "item": {
				      "nested": {  "path": xPath     },
				      "aggs": {
				        "item": {
				          "filter": { "term": xTerm       },
				          "aggs": {
				            "item": {
				              "terms": {  
				            	  "field": xField, 
				            	  "size": xAxisSize   },
				              "aggs": {
				                "item": {
				                  "reverse_nested": {},
				                  "aggs": {
				                    "item": {
				                      "nested": { "path": yPath   },
				                      "aggs": {
				                        "item": {
				                          "filter": { "term": yTerm   },
				                          "aggs": {
				                            "item": {
				                              "terms": {
				                                "field": yField,
				                                "size": yAxisSize
				                              },
				                              "aggs" : {
				                                "root_document" : { "reverse_nested": {}}
				                              }
				                            }
				                          }
				                        }
				                      }
				                    }
				                  }
				                }
				              }
				            }
				          }
				        }
				      }
				    }
				  }
				
        return aggsClause;
		
	}
	
	
	function createQueryHeatMapTimeline(interval,yAxisField,yAxisSize=35) {

		var yPath;
		var yTerm;
		var yField;

		var yActualFieldName = yAxisField.substring(yAxisField.indexOf(":")+1);

		
		if (yAxisField.startsWith('concept:') ){
			yPath = "concepts";
			yTerm = {"concepts.fullName.keyword": yActualFieldName };
			yField = "concepts.value.keyword";
		}
		else if (yAxisField.startsWith("entity:")){
			yPath = "spacy.entities";
			yTerm = {"spacy.entities.type.keyword": yActualFieldName };
			yField = "spacy.entities.text.keyword";
		}
		else {
			LASLogger.log(LASLogger.LEVEL_FATAL, "ElasticSupport.createQueryHeatMap: field does not start with an appropriate type: " + yAxisField);
			return null;
		}		
		
		var aggsClause =  {
	            "item": {
	              "date_histogram" :{
	            	  "field": "published_date.date",
	            	  "interval" : interval,
	            	  "min_doc_count": 0, 
	            	  "format": "M-dd-yyyy"
	              },
                  "aggs": {
                    "concepts_root": {
                      "nested": { "path": yPath   },
                      "aggs": {
                        "item": {
                          "filter": { "term": yTerm   },
                          "aggs": {
                            "item": {
                              "terms": {
                                "field": yField,
                                "size": yAxisSize
                              },
                              "aggs" : {
	                            "root_document" : { "reverse_nested": {}}
	                          }
                            }
                          }
                        }
				      }
				    }
				  }
				}
			  }

        return aggsClause;
		
	}
	
	function createAggregationFrequencyClause(itemField, size) {
		var path;
		var term;
		var field;

		var actualItemField = itemField.substring(itemField.indexOf(":")+1);
		
		if (itemField.startsWith('concept:') ){
			path = "concepts";
			term = {"concepts.fullName.keyword": actualItemField };
			field = "concepts.value.keyword";
		}
		else if (itemField.startsWith("entity:")){
			path = "spacy.entities";
			term = {"spacy.entities.type.keyword": actualItemField };
			field = "spacy.entities.text.keyword";
		}
		
		var aggClause = {
		    "items": {
		        "nested": { "path": path },
		        "aggs": {
		          "items": {
		            "filter": { "term":  term },
		            "aggs": {"items": { "terms": { "field": field,"size": size } }
		            }
		          }
		        }
		      }
		    }
		
		return aggClause;
	}
	
	function createAggregationDateFrequencyClause(itemField) {
		var path;
		var term;

		var actualItemField = itemField.substring(itemField.indexOf(":")+1);
		
		if (itemField.startsWith('concept:') ){
			path = "concepts";
			term = {"concepts.fullName.keyword": actualItemField };
		}
		else if (itemField.startsWith("entity:")){
			path = "spacy.entities";
			term = {"spacy.entities.type.keyword": actualItemField };
		}
		
		var aggClause = {
			    "items_per_day": {
			        "date_histogram": { "field": "published_date.date",  "interval": "1d"  },
			        "aggs": {
			          "items": {
			            "nested": {"path": path },
			            "aggs": {
			              "items": {
			            	  "filter": { "term": term }
			              }
			            }
			          }
			        }
			      }
			    }
		
		return aggClause;
	}
		
	return {
		createFilterField : createFilterField,
		createFilterEntity : createFilterEntity,
		createFilterConcept : createFilterConcept,
		createMustEntity : createMustEntity,
		createMustConcept : createMustConcept,
		createMustGeotag : createMustGeotag,
		createFilterCategory : createFilterCategory,
		createFilterConcept : createFilterConcept,
		createQueryFilter : createQueryFilter,
		createQueryAggregationField: createQueryAggregationField,
		createQueryAggregationConceptsField: createQueryAggregationConceptsField,
		createQueryHeatMap : createQueryHeatMap,
		createQueryHeatMapTimeline : createQueryHeatMapTimeline,
		createAggregationFrequencyClause : createAggregationFrequencyClause,
		createAggregationDateFrequencyClause : createAggregationDateFrequencyClause,
		createFilterSession : createFilterSession,
		createFilterDateCrawlRange : createFilterDateCrawlRange
	};
}());

