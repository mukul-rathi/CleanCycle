package uk.ac.cam.cl.cleancyclegui;

import uk.ac.cam.cl.cleancyclerouting.RouteFinder;
import uk.ac.cam.cl.cleancyclerouting.RouteFinderUtil;

class RouteFinderContainer {
    private RouteFinder routeFinder;
    private RouteFinderUtil routeFinderUtil;

    RouteFinderContainer(RouteFinder routeFinder, RouteFinderUtil routeFinderUtil) {
        this.routeFinder = routeFinder;
        this.routeFinderUtil = routeFinderUtil;
    }

    public synchronized RouteFinder getRouteFinder() {
        return routeFinder;
    }

    public synchronized void setRouteFinder(RouteFinder routeFinder) {
        this.routeFinder = routeFinder;
    }

    public RouteFinderUtil getRouteFinderUtil() {
        return routeFinderUtil;
    }
}
