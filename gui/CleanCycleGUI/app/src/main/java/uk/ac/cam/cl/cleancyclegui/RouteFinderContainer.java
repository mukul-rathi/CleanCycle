package uk.ac.cam.cl.cleancyclegui;

import uk.ac.cam.cl.cleancyclerouting.RouteFinder;

class RouteFinderContainer {
    private RouteFinder routeFinder;

    RouteFinderContainer(RouteFinder routeFinder) {
        this.routeFinder = routeFinder;
    }

    public synchronized RouteFinder getRouteFinder() {
        return routeFinder;
    }

    public synchronized void setRouteFinder(RouteFinder routeFinder) {
        this.routeFinder = routeFinder;
    }
}
