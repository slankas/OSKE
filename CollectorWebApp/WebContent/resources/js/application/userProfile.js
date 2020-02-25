/**
 * 
 */

LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);


$(document).ready(function() {
	 LASLogger.instrEvent('application.userProfile.startPage');

     $('#btnHome').on('click', function() {
    	 LASLogger.instrEvent('application.userProfile.gotoDomainHome');
    	 window.location = openke.global.Common.getContextRoot();
    });

     $("#usrAgrHis").on('click', function() {
    		LASLogger.instrEvent('application.userProfile.gotoUserAgreementHistory');
    	    window.location = openke.global.Common.getContextRoot() +"system/uaHistory";
    	    return false;
    });

    var tblUserAccess = $('#tblUserAccess').DataTable({
         "pageLength" : 10,
         "lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
         "dom": 'p',
         "columns" : [ {
             "data" : "domain",
             "width" : "20%"
         }, {
             "data" : "role",
             "width" : "20%"
         }
         ],
         "order" : [ [ 0, "asc" ] ]
     });

     refreshTable();     
     
});

function refreshTable() {
    $.getJSON(openke.global.Common.getContextRoot()+"rest/system/user/"+$("#author").val(), refreshUserTable);
}

function refreshUserTable(data) {
    var table = $('#tblUserAccess').DataTable();
    table.clear();
    
    var userAccess = data.access;

    for (var i = 0; i < userAccess.length; i++) {
        var record = userAccess[i];
        
        var newRecord = {};
        newRecord.domain = record.domain;
        newRecord.role   = record.role;
       
        table.row.add(newRecord);
    }
    table.draw();

}


