package uk.ac.cam.cl.cleancyclegui;

import java.util.List;

import uk.ac.cam.cl.cleancyclegraph.EdgeComplete;

class RouteContainer {
    private List<EdgeComplete> route;

    public RouteContainer(List<EdgeComplete> route) {
        this.route = route;
    }

    public synchronized List<EdgeComplete> getRoute() {
        return route;
    }

    public synchronized void setRoute(List<EdgeComplete> route) {
        this.route = route;
    }
}
