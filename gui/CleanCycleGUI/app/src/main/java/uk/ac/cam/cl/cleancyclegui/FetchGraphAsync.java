package uk.ac.cam.cl.cleancyclegui;


import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import CleanCycle.Analytics.EdgeComplete;
import CleanCycle.Analytics.MapInfoContainer;
import CleanCycle.Analytics.Node;
import uk.ac.cam.cl.cleancyclerouting.GraphNotConnectedException;
import uk.ac.cam.cl.cleancyclerouting.NotSetUpException;
import uk.ac.cam.cl.cleancyclerouting.RouteFinder;

/**
 * An asynchronous task to fetch the open street maps graph and plot a route etc
 */
class FetchGraphAsync extends AsyncTask<LatLng, Void, List<EdgeComplete>> {
    private RouteFinderContainer routeFinderContainer;
    private MapInfoContainer mapInfoContainer;
    private AlgorithmContainer algorithmContainer;
    private RouteContainer routeContainer;
    private RouteHandler routeHandler;
    private FetchGraphUtil util;
    private String url;
    private int port;

    public FetchGraphAsync(RouteContainer routeContainer,
                           RouteFinderContainer routeFinderContainer,
                           MapInfoContainer mapInfoContainer,
                           AlgorithmContainer algorithmContainer,
                           RouteHandler routeHandler,
                           FetchGraphUtil util,
                           String url,
                           int port
    ) {
        this.routeFinderContainer = routeFinderContainer;
        this.mapInfoContainer = mapInfoContainer;
        this.routeContainer = routeContainer;
        this.algorithmContainer = algorithmContainer;
        this.routeHandler = routeHandler;
        this.util = util;
        this.url = url;
        this.port = port;
    }

    @Override
    protected List<EdgeComplete> doInBackground(LatLng... points) {
        if (points.length != 2) return null;
        try {
            util.updateLabel("I: finding route");
            if (routeFinderContainer.getRouteFinder() == null) {
                InputStream nodes = util.getNodesStream();
                InputStream edges = util.getEdgesStream();
                routeFinderContainer.setRouteFinder(new RouteFinder(url, port, mapInfoContainer, routeFinderContainer.getRouteFinderUtil()));
            }

            List<EdgeComplete> cur = routeFinderContainer.getRouteFinder().findRouteEdges(
                    algorithmContainer.getAlgorithm(),
                    points[0].latitude, points[0].longitude,
                    points[1].latitude, points[1].longitude);



            // perform Chaikin here because this keeps the computation asynchronous and prevents
            // frames from being missed (the UI is the main thread)
            for (int i=0; i<3; ++i) cur = chaikin(cur);

            routeContainer.setRoute(cur);
            return cur;

        } catch (NotSetUpException e) {
            e.printStackTrace();
            util.updateLabel( "E: Graph not set up.");
        } catch (GraphNotConnectedException e) {
            e.printStackTrace();
            util.updateLabel("E: Graph not connected.");
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<EdgeComplete> route) {
        routeHandler.handleRoute(route);
    }


    /**
     * Not particularly difficult (see further graphics).
     * Just a basic function to smooth out the routes. Makes sharp corners less sharp and fades
     * splits changes in edge colour across multiple edges.
     *
     * @param edgesIn The edges to be smoothed.
     * @return The smoothed edges.
     */
    private List<EdgeComplete> chaikin(List<EdgeComplete> edgesIn) {
        if (edgesIn.size() < 2) return edgesIn;
        LinkedList<EdgeComplete> edgesOut = new LinkedList<>();
        List<Node> nodes = new ArrayList<>();
        for (int i=0; i<edgesIn.size()-1; ++i) {
            EdgeComplete edgeOne = edgesIn.get(i);
            EdgeComplete edgeTwo = edgesIn.get(i+1);
            Node one = edgeOne.node1;
            Node two = edgeOne.node2;
            Node p2i  = new Node(0, 0.9375 * one.Latitude + 0.0625 * two.Latitude, 0.9375 * one.Longitude + 0.0625 * two.Longitude);
            Node p2i1 = new Node(0, 0.0625 * one.Latitude + 0.9375 * two.Latitude, 0.0625 * one.Longitude + 0.9375 * two.Longitude);
            nodes.add(p2i);
            nodes.add(p2i1);
        }

        for (int i=0; i<nodes.size()-1; ++i) {
            Node one = nodes.get(i);
            Node two = nodes.get(i+1);

            double pollution = (i & 2) == 2
                    ? edgesIn.get(i/2).Pollution
                    : (edgesIn.get(i/2).Pollution + edgesIn.get(i/2+1).Pollution) / 2;

            EdgeComplete edge = new EdgeComplete(0,0,one,two,0, pollution);
            edgesOut.addLast(edge);
        }
        EdgeComplete first = edgesOut.getFirst();
        EdgeComplete last = edgesOut.getLast();
        edgesOut.addFirst(new EdgeComplete(0,0, edgesIn.get(0).node1, first.node1, 0, edgesIn.get(0).Pollution));
        edgesOut.addLast(new EdgeComplete(0,0, last.node2, edgesIn.get(edgesIn.size()-1).node2, 0, edgesIn.get(edgesIn.size()-1).Pollution));
        return new ArrayList<>(edgesOut);
    }
}
