<div id="documentIndexView">
<div id="metaData"></div>
<br>
<div class="row">
	<div class="col-md-12">
		<ul class="nav nav-tabs">
		   <li class="nav-item"><span class="nav-link viewIndexMenu active"  id="btKeyphrases">Keyphrases</span> </li> 
		   <li class="nav-item"><span class="nav-link viewIndexMenu" id="btConcepts">Concepts</span> </li> 
		   <li class="nav-item"><span class="nav-link viewIndexMenu" id="btDocuments">Documents</span> </li> 		   
		</ul>
	</div>
</div>
	
<div class="card card-primary filtercard" id="allFilters">
  <div class="card-header filtercard-header"><strong>Filters</strong></div> 
  <div class="card-body filtercard-body"  id="allFiltersContent"></div>
</div>

<ul class='custom-menu' id='conceptTreeMenu'>
  <li data-action = "addFilter">Filter</li>
  <li data-action = "addNotFilter">Not Filter</li>
</ul>

<ul class='custom-menu' id='keyphraseTreeMenu'>
  <li data-action = "addFilter">Filter</li>
  <li data-action = "addNotFilter">Not Filter</li>
</ul>

<ul class='custom-menu' id='documentTreeMenu'>
  <li data-action = "addFilter">Filter</li>
  <li data-action = "addNotFilter">Not Filter</li>
</ul>

<div class="indexBlock" id="indexBlock">
	<div id="keyIndexNavigation" class="split split-horizontal indexNavigation">
		<div id="keyTree"></div>
	</div>
	<div id="textArea" class="split split-horizontal">
		<div id="textBlock" class="contentBlock">
			<i>Select an index term from the left to start.</i>
		</div>
	</div>
</div>

<div class="indexBlock" id="documentIndexBlock">
	<div id="documentIndexNavigation" class="split split-horizontal indexNavigation">
		<div id="documentTree"></div>
	</div>
	<div id="documentTextArea" class="split split-horizontal">
		<div id="documentTextBlock" class="contentBlock">
			<i>Select an index term from the left to start.</i>
		</div>
	</div>
</div>

<div class="indexBlock" id="conceptIndexBlock">
	<div id="conceptIndexNavigation" class="split split-horizontal indexNavigation">
		<div id="conceptTree"></div>
	</div>
	<div id="conceptTextArea" class="split split-horizontal">
		<div id="conceptTextBlock" class="contentBlock">
			<i>Select an index term from the left to start.</i>
		</div>
	</div>
</div>




<!-- used to select a document when more than one is present for a menu item-->
<div class="menuHide" id="documentMenu" >
	<ul id="documents">
		<li><a href="#"></a></li>
		<li><a href="#"></a></li>
		<li><a href="#">C</a></li>
	</ul>
</div>
</div>