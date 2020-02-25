<%@ include file="/WEB-INF/views/includes/taglibs.jsp"%>
<c:url var="projName"  value="/" />
	<div class="modal fade" tabindex="-1" role="dialog" id="myModal">
	  <div class="modal-dialog">
	    <div class="modal-content">
	      <div class="modal-header">
	        <h4 class="modal-title" id ="myModalTitle"></h4>
	        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
	      </div>
	      <div class="modal-body" id ="myModalDialogText">
	      </div>
	      <div class="modal-footer">
	        <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
	      </div>
	    </div><!-- /.modal-content -->
	  </div><!-- /.modal-dialog -->
	</div><!-- /.modal -->
	<script type="text/javascript" charset="utf8" src="${projName}resources/js/LASMessageDialog.js"></script>