<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.mapView"); %>
    <title>Map View - Found Locations</title>
    <style>
      /* make the div the same sie as the window*/
      #map {
        height: 100%;
      }
      /* Makes fill the window. */
      html, body {
        height: 100%;
        margin: 0;
        padding: 0;
      }
    </style>
    <link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/leaflet_1.3.4/leaflet.css" />
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/leaflet_1.3.4/leaflet.js"></script>
    
    <link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/leaflet.markercluster-1.3.0/MarkerCluster.css" />
    <link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/leaflet.markercluster-1.3.0/MarkerCluster.Default.css" />
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/leaflet.markercluster-1.3.0/leaflet.markercluster-src.js"></script>
  </head>
  <body>
    <div id="map"></div>
    <script>
/**
  Each record in mapRecords (which is an array) has this format:
 
  {
     "textPosition": 710,
     "confidence": 1,
     "textMatched": "Japan",
     "geoData": {
         "geoNameID": 1861060,
         "elevation": -9999999,
         "primaryCountryCode": "Japan",
         "timezone": "Asia/Tokyo",
         "latitude": 35.68536,
         "primaryCountryName": "Japan",
         "preferredName": "Japan",
         "longitude": 139.75309,
         "population": 127288000
     },
     "fuzzy": false,
     "matchedname": "japan",
     count: numOccurancesInDocument
 }
 */

loadMapData();
 
function loadMapData() {
	 opener.RecordLevelAnalytics.getMapData("<%=request.getAttribute("uuid")%>",initMap);
 }
 
function initMap(mapRecords) {
	var map = L.map('map').setView([51.505, -0.09], 5);
	L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
	    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
	}).addTo(map);

    var markerGroup = new L.markerClusterGroup([]);
    for (var i = 0; i < mapRecords.length; i++) {
    	var record = mapRecords[i];
      	var myLatLng = {lat: record.geoData.latitude, lng: record.geoData.longitude};

        var contentString = '<div id="content">'+
        '<h3>'+record.geoData.preferredName+'</h3>'+
        '<div id="bodyContent">'+
        '<a target=_blank href="https://www.google.com/search?q='+record.geoData.preferredName+'">'+
        'https://www.google.com/search?q='+record.geoData.preferredName+'</a><br>'+
        '<a target=_blank href="https://en.wikipedia.org/wiki/'+record.geoData.preferredName+'">'+
        'https://en.wikipedia.org/wiki/'+record.geoData.preferredName+'</a><br>'+        
        '</div>'+
        '</div>';      
        
        var marker = L.marker([record.geoData.latitude, record.geoData.longitude]).bindPopup(contentString)
        markerGroup.addLayer(marker)
    }
    map.addLayer(markerGroup)
    map.fitBounds(markerGroup.getBounds());
 }

    </script>

  </body>
</html>
