package org.insight_centre.aceis.io.streams.yasper;

import org.apache.commons.rdf.api.Graph;

public class Putter implements Runnable{
    YASPERSensorStream yss;
    Graph graph;

    public Putter(YASPERSensorStream yss, Graph graph) {
        this.yss = yss;
        this.graph = graph;
    }

    @Override
    public void run() {
        yss.put(graph, System.currentTimeMillis());
    }
}
