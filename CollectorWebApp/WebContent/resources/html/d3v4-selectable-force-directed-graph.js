/**
 * Adapted from http://emptypipes.org/2017/04/29/d3v4-selectable-zoomable-force-directed-graph/
 * 
 * Toggle Functionality from http://www.coppelia.io/2014/07/an-a-to-z-of-extra-features-for-the-d3-force-layout/
 */

function createV4SelectableForceDirectedGraph(svg, graph) {
    // if both d3v3 and d3v4 are loaded, we'll assume
    // that d3v4 is called d3v4, otherwise we'll assume
    // that d3v4 is the default (d3)
    if (typeof d3v4 == 'undefined')
        d3v4 = d3;

    var width = +svg.attr("width"),
        height = +svg.attr("height");

    let parentWidth = d3v4.select('svg').node().parentNode.clientWidth;
    let parentHeight = d3v4.select('svg').node().parentNode.clientHeight;

    var svg = d3v4.select('svg')
    .attr('width', parentWidth)
    .attr('height', parentHeight)

svg.append('defs').append('marker')
.attr('id','arrowhead')
.attr('viewBox','-0 -5 10 10')
.attr('refX',13)
.attr('refY',0)
.attr('orient','auto')
.attr('markerWidth',5)
.attr('markerHeight',5)
.attr('xoverflow','visible')
.append('svg:path')
.attr('d', 'M 0,-5 L 10 ,0 L 0,5')
.attr('fill', '#555')
.style('stroke','none');
    
    // remove any previous graphs
    svg.selectAll('.g-main').remove();

    var gMain = svg.append('g')
    .classed('g-main', true);

    var rect = gMain.append('rect')
    .attr('width', parentWidth)
    .attr('height', parentHeight)
    .style('fill', 'white')

    var gDraw = gMain.append('g');

    var zoom = d3v4.zoom()
    .on('zoom', zoomed)

    gMain.call(zoom);


    function zoomed() {
        gDraw.attr('transform', d3v4.event.transform);
    }

    var color = d3v4.scaleOrdinal(d3v4.schemeCategory20);

    if (! ("links" in graph)) {
        console.log("Graph is missing links");
        return;
    }

    var nodes = {};
    var i;
    for (i = 0; i < graph.nodes.length; i++) {
        nodes[graph.nodes[i].id] = graph.nodes[i];
        graph.nodes[i].weight = 1.01;
    }

    // the brush needs to go before the nodes so that it doesn't
    // get called when the mouse is over a node
    var gBrushHolder = gDraw.append('g');
    var gBrush = null;

    var link = gDraw.append("g")
        .attr("class", "link")
        .selectAll("line")
        .data(graph.links)
        .enter().append("line")
        .attr("stroke-width", function(d) { return 1.5; /*Math.sqrt(d.value);*/ })
        .attr('marker-end','url(#arrowhead)');
    
    var node = gDraw.append("g")
        .attr("class", "node")
        .selectAll("circle")
        .data(graph.nodes)
        .enter().append("circle")
        .attr("r", 5)
        .attr("fill", function(d) { 
            if ('color' in d)
                return d.color;
            else
                return color(d.group); 
        })
        .call(d3v4.drag()
        .on("start", dragstarted)
        .on("drag", dragged)
        .on("end", dragended))
        .on('click', connectedNodes);

    // Label's nodes based upon the ID
    var label = gDraw.append("g").selectAll(".mytext")
	    .data(graph.nodes)
	    .enter()
	    .append("text")
	    .text(function (d) { return d.id; })
	    .style("text-anchor", "middle")
	    .style("fill", "#333")
	    .style("font-family", "Arial")
	    .style("font-size", 10);
    
    var edgepaths = gDraw.append("g").selectAll(".edgepath")
		.data(graph.links)
		.enter()
		.append('path')
		//.attr('d', function(d) {return 'M '+d.source.x+' '+d.source.y+' L '+ d.target.x +' '+d.target.y})
		.attr('class','edgepath')
		.attr('fill-opacity',0)
		.attr('stroke-opacity',0)
		.attr('fill','blue')
		.attr('stroke','red')
		.attr('id',function(d,i) {return 'edgepath'+i})
	    .style("pointer-events", "none");    
      
    var edgelabels = gDraw.append("g").selectAll(".edgelabel")
	    .data(graph.links)
	    .enter()
	    .append('text')
	    .style("pointer-events", "none")
	    .attr('class','edgelabel')
	    .attr('id',function(d,i){return 'edgelabel'+i})
	    //.attr('dx',80)
	    //.attr('dy',0)
	    .attr('fill','#333')
	    .style("font-family", "Arial")
	    .style("font-size", 8);;

  edgelabels.append('textPath')
	  .attr('xlink:href', function (d, i) {return '#edgepath' + i})
	  .style("text-anchor", "middle")
	  .style("pointer-events", "none")
	  .attr("startOffset", "50%")
	  .text(function (d) {return d.label });    
    
    
    // add titles for mouseover blurbs
    node.append("title")
        .text(function(d) { 
            if ('name' in d)
                return d.name;
            else
                return d.id; 
        });

    var simulation = d3v4.forceSimulation()
        .force("link", d3v4.forceLink()
                .id(function(d) { return d.id; })
                .distance(function(d) { 
                    return 100;
                    //var dist = 20 / d.value;
                    //console.log('dist:', dist);

                    return dist; 
                })
              )
        .force("charge", d3v4.forceManyBody().distanceMax(150))
        .force("center", d3v4.forceCenter(parentWidth / 2, parentHeight / 2))
      //  .force("x", d3v4.forceX(parentWidth * 2))
      //  .force("y", d3v4.forceY(parentHeight *2 ));

    simulation
        .nodes(graph.nodes)
        .on("tick", ticked);

    simulation.force("link")
        .links(graph.links);

    function ticked() {
        // update node and line positions at every step of 
        // the force simulation
        link.attr("x1", function(d) { return d.source.x; })
            .attr("y1", function(d) { return d.source.y; })
            .attr("x2", function(d) { return d.target.x; })
            .attr("y2", function(d) { return d.target.y; });

        node.attr("cx", function(d) { return d.x; })
            .attr("cy", function(d) { return d.y; });
        
    	label.attr("x", function(d){ return d.x; })
	         .attr("y", function (d) {return d.y - 10; }); 
    	
        edgepaths.attr('d', function (d) {
            return 'M ' + d.source.x + ' ' + d.source.y + ' L ' + d.target.x + ' ' + d.target.y;
        });
        
        edgelabels.attr('transform', function (d) {
            if (d.target.x < d.source.x) {
                var bbox = this.getBBox();

                rx = bbox.x + bbox.width / 2;
                ry = bbox.y + bbox.height / 2;
                return 'rotate(180 ' + rx + ' ' + ry + ')';
            }
            else {
                return 'rotate(0)';
            }
        });
    }

    var brushMode = false;
    var brushing = false;

    var brush = d3v4.brush()
        .on("start", brushstarted)
        .on("brush", brushed)
        .on("end", brushended);

    function brushstarted() {
        // keep track of whether we're actively brushing so that we
        // don't remove the brush on keyup in the middle of a selection
        brushing = true;

        node.each(function(d) { 
            d.previouslySelected = shiftKey && d.selected; 
        });
    }

    rect.on('click', () => {
        node.each(function(d) {
            d.selected = false;
            d.previouslySelected = false;
        });
        node.classed("selected", false);
        connectedNodes();
    });

    function brushed() {
        if (!d3v4.event.sourceEvent) return;
        if (!d3v4.event.selection) return;

        var extent = d3v4.event.selection;

        node.classed("selected", function(d) {
            return d.selected = d.previouslySelected ^
            (extent[0][0] <= d.x && d.x < extent[1][0]
             && extent[0][1] <= d.y && d.y < extent[1][1]);
        });
        connectedNodes();
    }

    function brushended() {
        if (!d3v4.event.sourceEvent) return;
        if (!d3v4.event.selection) return;
        if (!gBrush) return;

        gBrush.call(brush.move, null);

        if (!brushMode) {
            // the shift key has been release before we ended our brushing
            gBrush.remove();
            gBrush = null;
        }

        brushing = false;
    }

    d3v4.select('body').on('keydown', keydown);
    d3v4.select('body').on('keyup', keyup);

    var shiftKey;

    function keydown() {
        shiftKey = d3v4.event.shiftKey;

        if (shiftKey) {
            // if we already have a brush, don't do anything
            if (gBrush)
                return;

            brushMode = true;

            if (!gBrush) {
                gBrush = gBrushHolder.append('g');
                gBrush.call(brush);
            }
        }
    }

    function keyup() {
        shiftKey = false;
        brushMode = false;

        if (!gBrush)
            return;

        if (!brushing) {
            // only remove the brush if we're not actively brushing
            // otherwise it'll be removed when the brushing ends
            gBrush.remove();
            gBrush = null;
        }
    }

    function dragstarted(d) {
      if (!d3v4.event.active) simulation.alphaTarget(0.9).restart();

        if (!d.selected && !shiftKey) {
            // if this node isn't selected, then we have to unselect every other node
            node.classed("selected", function(p) { return p.selected =  p.previouslySelected = false; });
        }

        d3v4.select(this).classed("selected", function(p) { d.previouslySelected = d.selected; return d.selected = true; });
        connectedNodes();
        
        node.filter(function(d) { return d.selected; })
        .each(function(d) { //d.fixed |= 2; 
          d.fx = d.x;
          d.fy = d.y;
        })

    }

    function dragged(d) {
      //d.fx = d3v4.event.x;
      //d.fy = d3v4.event.y;
            node.filter(function(d) { return d.selected; })
            .each(function(d) { 
                d.fx += d3v4.event.dx;
                d.fy += d3v4.event.dy;
            })
    }

    function dragended(d) {
      if (!d3v4.event.active) simulation.alphaTarget(0);
      d.fx = null;
      d.fy = null;
        node.filter(function(d) { return d.selected; })
        .each(function(d) { //d.fixed &= ~6; 
            d.fx = null;
            d.fy = null;
        })
    }

    /*
    var texts = ['Use the scroll wheel to zoom',
                 'Hold the shift key to select nodes']

    svg.selectAll('text')
        .data(texts)
        .enter()
        .append('text')
        .attr('x', 900)
        .attr('y', function(d,i) { return 470 + i * 18; })
        .text(function(d) { return d; });
    */
    
  //Toggle stores whether the highlighting is on
    var toggle = 0;
    //Create an array logging what is connected to what
    var linkedByIndex = {};
    for (i = 0; i < graph.nodes.length; i++) {
        linkedByIndex[i + "," + i] = 1;
    };
    graph.links.forEach(function (d) {
        linkedByIndex[d.source.index + "," + d.target.index] = 1;
    });
    
    //This function looks up whether a pair are neighbours
    function neighboring(a, b) {
        return linkedByIndex[a.index + "," + b.index];
    }
    function connectedNodes() {
            //Reduce the opacity of all but the neighbouring nodes
        	var selectedNodes = [];
        	 node.each(function(d) {
        		 if (d.selected) {
        			 selectedNodes.push(d)
        		 }
        	 });
        	
        	 if (selectedNodes.length ==0) {
                 node.style("opacity", 1);
                 link.style("opacity", 1);
                 label.style("opacity", 1);
                 edgelabels.style("opacity", 1);
                 return;
        	 }
        	 
            //d = d3.select(this).node().__data__;
            node.style("opacity", function (o) {
            	for (var i=0;i<selectedNodes.length;i++) {
            		var nodeRecord = selectedNodes[i];
            		if (neighboring(nodeRecord, o) || neighboring(o, nodeRecord)) { return 1;}
            	}
            	return 0.1;
            	//return neighboring(d, o) | neighboring(o, d) ? 1 : 0.1;
            });
            
            label.style("opacity", function (o) {
            	for (var i=0;i<selectedNodes.length;i++) {
            		var nodeRecord = selectedNodes[i];
            		if (neighboring(nodeRecord, o) || neighboring(o, nodeRecord)) { return 1;}
            	}
            	return 0.1;
                //return neighboring(d, o) | neighboring(o, d) ? 1 : 0.1;
            });
            
            link.style("opacity", function (o) {
            	for (var i=0;i<selectedNodes.length;i++) {
            		var nodeRecord = selectedNodes[i];
            		if  (nodeRecord.index == o.source.index || nodeRecord.index==o.target.index ) { return 1 }
            	}
            	return 0.1;
            	
                //return d.index==o.source.index | d.index==o.target.index ? 1 : 0.1;
            });
            
            edgelabels.style("opacity", function (o) {
            	for (var i=0;i<selectedNodes.length;i++) {
            		var nodeRecord = selectedNodes[i];
            		if  (nodeRecord.index == o.source.index || nodeRecord.index==o.target.index ) { return 1 }
            	}
            	return 0.1;
                //return d.index==o.source.index | d.index==o.target.index ? 1 : 0.1;
            });

    }    
    
    

    return {
    	node : node,
    	link : link,
		graphData  : graph,
		highlightNodes : connectedNodes    
    };
};