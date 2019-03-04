package uk.ac.cam.cl.cleancyclegui;

import android.os.AsyncTask;

import java.io.IOException;

import uk.ac.cam.cl.cleancyclerouting.RouteFinder;

/**
 * An asynchronous task used to cache the current graph data to a file (or whatever system is
 * specified by the route finder).
 *
 * This allows for the task to take place without affecting the UI.
 */
public class CacheGraphAsync extends AsyncTask<RouteFinderContainer, Void, Void> {

    @Override
    protected Void doInBackground(RouteFinderContainer... routeFinderContainers) {
        for (RouteFinderContainer routeFinderContainer : routeFinderContainers) {
            RouteFinder routeFinder = routeFinderContainer.getRouteFinder();
            try {
                System.out.println("CACHING GRAPH... ");
                routeFinder.cacheNodes();
                routeFinder.cacheEdges();
                System.out.println("GRAPH CACHED");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
