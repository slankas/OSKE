//File also needs to include LASLogger
var LASHeader = (function () {
	"use strict";
	
	var openkeLocalStore  = window.localStorage; // convenience variable such that we can switch between setting  
	
	function getValueFromStore(fieldName) {
		var fullName = openke.global.Common.getDomain()+'_'+fieldName
		if (openkeLocalStore[fullName] != null && openkeLocalStore[fullName] != "null") {	return  openkeLocalStore[fullName]; 	}
		else {	return null;}				
	}

	function setValueToStore(fieldName, newValue) {
		var fullName = openke.global.Common.getDomain()+'_'+fieldName;
		openkeLocalStore[fullName] = newValue
	}
	
	function removeValueFromStore(fieldName) {
		var fullName = openke.global.Common.getDomain()+'_'+fieldName;
		openkeLocalStore.removeItem(fullName);
	}
	
	function getCurrentProjectUUID()  {	return getValueFromStore("projectUUID"); }
	function getCurrrentProjectName() {	return getValueFromStore("projectName"); }
	function getCurrrentScratchpadUUID() {	return getValueFromStore("scratchpadUUID");	}
	function getCurrrentScratchpadName() {	return getValueFromStore("scratchpadName");	}

	function setCurrentProject(newID,newName)    { setValueToStore("projectUUID",newID);	setValueToStore("projectName",newName); displayCurrentHeader(); }
	function setCurrrentScratchpad(newID, newName)   { setValueToStore("scratchpadUUID",newID);  setValueToStore("scratchpadName",newName); displayCurrentHeader();	}
		
	function displayCurrentHeader() {
		var projectName = getCurrrentProjectName()
		if (projectName == null) {
			$("#headerCurrentProject").html("<i><a href='#' onclick='LASHeader.navigateTo(\"plan\");'>unassigned</a></i>")			
		}
		else {
			$("#headerCurrentProject").html("<i><a href='#' onclick='LASHeader.navigateTo(\"plan?projectUUID="+getCurrentProjectUUID()+"\"); return false;'>"+projectName+"</a></i>")	
		}
		
		var documentName = getCurrrentScratchpadName();
		if (documentName == null) {
			$("#headerCurrentScratchPad").html("<i><a href='#' onclick='LASHeader.navigateTo(\"document\");'>unassigned</a></i>")	
		}
		else {
			$("#headerCurrentScratchPad").html("<i><a href='#' onclick='LASHeader.navigateTo(\"document?documentUUID="+getCurrrentScratchpadUUID()+"\");'>"+documentName+"</a></i>")
		}		
	}
	
	
	// Change to the current page and send an instrumentation event
    function navigateTo(location) {
		var completeLocation =openke.global.Common.getPageURLPrefix() +"/" +location
		//alert("*** navigate to "+completeLocation);
		LASLogger.instrEvent('topNavigation.link', {
				link : completeLocation
		}, function() {window.location = completeLocation;	});
	} 
	

		
	function getDefaultHome() {
		if (window.sessionStorage["home"] == null) {
			return "analyst";
		}
		else {
			return window.sessionStorage["home"];
		}
	}

	function setDefaultHome(homeLocation) {
		window.sessionStorage["home"] = homeLocation;
		$.getJSON(openke.global.Common.getRestURLPrefix()+"/user/session/setHomePage/"+homeLocation, function(data) {
			if (data.status == "success") {
				LASHeader.navigateTo('');
			}
			else {
				bootbox.alert("Error: home page not set appropriately.")
			}
		});
	}

	var privateMembers = {
			getCurrentProjectUUID   : getCurrentProjectUUID,

			getCurrrentScratchpadUUID : getCurrrentScratchpadUUID,
			getCurrrentScratchpadName : getCurrrentScratchpadName,
	};

	return {
		navigateTo : navigateTo,
		
		setCurrentProject     : setCurrentProject,
		setCurrrentScratchpad    : setCurrrentScratchpad,
		
		getCurrrentProjectName  : getCurrrentProjectName,
		getCurrentProjectUUID   : getCurrentProjectUUID,
		getCurrrentScratchpadName : getCurrrentScratchpadName,
		getCurrrentScratchpadUUID : getCurrrentScratchpadUUID,
		
		displayCurrentHeader : displayCurrentHeader,
		
		getDefaultHome: getDefaultHome,
		setDefaultHome : setDefaultHome,
				
		privateMembers : privateMembers
	};
}());


/*
Code was from when we had a search box at the top of the page.  doesn't currently exist....
$('#txtSearch').bind("keyup", function(event) {  	
    if (event.keyCode == 13) {
    	LASLogger.instrEvent('application.' + window.page + '.search', {
    		searchtext : $('#txtSearch').val()
    	});

        window.location =openke.global.Common.getPageURLPrefix() +"/search?query=" + $('#txtSearch').val();
        return false;
    }
});
*/
$(document).ready(function() {
	
	/*
	$("#settings").on('click', function() {
	    window.location = openke.global.Common.getContextRoot() +"system/userProfile";
	    return false;
	});
    */

	 if (currentPage && currentPage.startsWith("domain.")) {
		 var navPart = currentPage.substr(7)
		 if (navPart.indexOf(".") > -1) {
			 navPart = navPart.substr(0,navPart.indexOf("."))
		 }
		 $('#topNav_'+navPart).addClass("active")
		 if (currentPage.startsWith("domain.analyze.")) {
			 var analyticItem = "analytic"+currentPage.substring(15);
			 $('#topNav_'+analyticItem).addClass("active")
		 }
	 }

	 $('.okMenu').on('click', function(e) {
		 var destination = this.id.substr(7)
		 //alert(destination);
		 switch(destination) {
		    case "dashboard":  LASHeader.navigateTo(''); break;
		    case "plan":  LASHeader.navigateTo('plan'); break;
		    case "discover":  LASHeader.navigateTo('domainDiscovery'); break;
		    case "document":  LASHeader.navigateTo('document'); break;
		    case "concepts":  LASHeader.navigateTo('concept'); break;
		    case "documentBuckets":  LASHeader.navigateTo('manageDocumentBuckets'); break;
		    case "docIndexes" :  LASHeader.navigateTo('documentIndex');         break; // TODO: eliminate in redesign
		    case "search"     :  LASHeader.navigateTo('search');                break; // TODO: eliminate in redesign
		    case "searchAlerts":  LASHeader.navigateTo('manageSearchAlerts'); break;
		    case "structuralExtraction":  LASHeader.navigateTo('structuralExtraction'); break;
		    case "jobs":  LASHeader.navigateTo('manageJobs'); break;
		    case "users":  LASHeader.navigateTo('maintainUsers'); break;
		    case "collector":  LASHeader.navigateTo('jobStatus'); break;
		    case "analyze":  LASHeader.navigateTo('analyze'); break;
		    case "analyticfrequencies":  LASHeader.navigateTo('analyticFrequencies'); break;
		    case "analyticvisualize":  LASHeader.navigateTo('analyticVisualize'); break;
		    case "analyticheatMap":  LASHeader.navigateTo('analyticHeatMap'); break;
		    case "analyticheatMapTimeline":  LASHeader.navigateTo('analyticHeatMapTimeline'); break;
		    case "analyticChoroplethMap":  LASHeader.navigateTo('analyticChoroplethMap'); break;
		    
		    default: alert("Unknown location for top level navigation: "+destination);break;
		 }
	 });
	 
	$('#btLogOut').on('click',	logout);	
	LASHeader.displayCurrentHeader();
});


//TODO: add this check in the main controller and set an attribute to initiate the popup.
//$(document).ready(function() {
   // $.getJSON(openke.global.Common.getContextRoot()+"rest/domain/userAgreement/checkApproved", generatePopUp);
//});

function generatePopUp(data){

    var expirationDate = Date.parse(data.userAgreementApproved[0].expirationTimestamp);
    var d1 = new Date(expirationDate);
    data.userAgreementApproved[0].expirationTimestamp = (d1.getMonth()+1) + '/' + d1.getDate() + '/' + d1.getFullYear();

	if(data.status==="success"){
        bootbox.confirm({
            title: "User Agreement Expiration Notice",
            message: "Your user agreement will expire on "+data.userAgreementApproved[0].expirationTimestamp+"."+"<br>"+"<br>"+"Do you wish to complete a new user agreement at this time?",
            buttons: {
                cancel: {
                    label: 'Complete Later',
                    className: 'btn-danger'
                },
                confirm: {
                    label: 'Complete New Agreement',
                    className: 'btn-success'
                }
            },
            callback: function (result) {
                if(result){
                    window.location = openke.global.Common.getContextRoot() +"userAgreement/sign";
                }
                else{
                    return;
                }
            }
        });
	}

}