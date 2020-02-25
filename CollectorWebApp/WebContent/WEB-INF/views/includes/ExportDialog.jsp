<%@ include file="/WEB-INF/views/includes/taglibs.jsp"%>
<c:url var="projName" value="/" />
<div class="modal fade" tabindex="-1" role="dialog" id="exportModal">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h4 class="modal-title" id="myModalExportTitle">Export</h4>
				<button type="button" class="close" data-dismiss="modal" aria-label="Close" onclick="LASExportDialog.cancelExportDialog()">
					<span aria-hidden="true">&times;</span>
				</button>
			</div>
			<div class="modal-body" id="myModalExportDialogText">
			
				<label class="export" for="drpdnDestination"> Destination</label> 
				<select id="drpdnDestination" name="drpdnDestination" class="export" >
					<option value="download" selected>Download</option>
					<option value="directory">Export for Transfer</option>
					<option value="voyant">Voyant</option>
				</select>
				
				<br /> 
				
				<label class="export" for="drpdnFormat">Format</label> 
				<select id="drpdnFormat" name="drpdnFormat" class="export" >
					<option value="jsonObj">JSON Records (single file, line separation)</option>
					<option value="csvFile">Limited CSV File (single file)</option>
					<option value="indTextOnly">Individual Text Only Files (one per file, ZIP)</option>
					<option value="indJSON" selected>Individual JSON Records (one per file, ZIP)</option>
				</select>
				
<!-- 				<br />
				
				 <label class="export" for="drpdnGrouping" style="font-weight:bold">Grouping</label>
	            <select id="drpdnGrouping" name="drpdnGrouping" class="export" onchange="if (this.selectedIndex >=0) LASExportDialog.checkExportValues(event);">
		             <option value="byDate" selected="selected">By Date</option>
	                 <option value="noGroup" selected>No Grouping</option>
	         	</select><br/>  
	         
				<label class="export" for="drpdnFileName">File name</label> 
				<select id="drpdnFileName" name="drpdnFileName" class="export" onchange="if (this.selectedIndex >=0) LASExportDialog.checkExportValues(event);">
					<option value="url" >URL</option>
					<option value="uuid" selected="selected">UUID</option>
					<option value="na" selected>Not Applicable</option>
				</select>
				
				<br /> 
				
				<label class="export" for="inExportName">Export name</label>
				<input class="export" id="inExportName" type="text" name="inExportName" value="" />
				
				<br />
				 
				<label class="export" for="drpdnStemWords">Stem Words</label> 
				<select id="drpdnStemWords" name="drpdnStemWords" class="export">
					<option value="true" selected="selected">Yes</option>
					<option value="false" selected>No</option>
					<option value="na" selected>Not Applicable</option>
				</select> -->
				
				<br />
				
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-outline-primary" id="btExport" onClick="LASExportDialog.submitExport()">Export</button>
				<button type="button" class="btn btn-outline-danger" data-dismiss="modal" onclick="LASExportDialog.cancelExportDialog()">Cancel</button>
			</div>
		</div>
		<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->
<script type="text/javascript" charset="utf8" src="${projName}resources/js/LASExportDialog.js"></script>