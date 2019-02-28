package uk.ac.cam.cl.cleancyclegui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import CleanCycle.Analytics.EdgeComplete;

public class SavedRoutes implements Serializable {
    private Map<String,List<EdgeComplete>> routes;

    public SavedRoutes() {

    }

    public SavedRoutes(Map<String,List<EdgeComplete>> routes) {
        this.routes = routes;
    }

    private synchronized void checkRoutesExist() throws RoutesNotLoadedException {
        if (routes == null) throw new RoutesNotLoadedException();
    }

    private synchronized Map<String,List<EdgeComplete>> copyOfRoutes() {
        Map<String,List<EdgeComplete>> rts = new HashMap<>();
        for (Map.Entry<String,List<EdgeComplete>> entry : routes.entrySet()) {
            rts.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return rts;
    }

    public synchronized  Map<String,List<EdgeComplete>> getRoutes() {
        return copyOfRoutes();
    }

    public synchronized void setRoutes(Map<String,List<EdgeComplete>> routes) {
        this.routes = routes;
    }

    public synchronized int numberOfRoutes() {
        //checkRoutesExist();
        if (routes == null) return 0;
        return routes.size();
    }

    public List<String> getNames() throws RoutesNotLoadedException{
        checkRoutesExist();
        List<String> names = new ArrayList<>(routes.keySet());
        Collections.sort(names);
        return names;
    }

    public synchronized List<EdgeComplete> getRoute(String s) throws RoutesNotLoadedException {
        checkRoutesExist();
        return new ArrayList<>(routes.get(s));
    }

    public synchronized void addRoute(String name, List<EdgeComplete> route) {
        if (routes == null) routes = new HashMap<>();
        routes.put(name, route);
    }

    public synchronized void removeRoute(String name) {
        if (routes == null) return;
        routes.remove(name);
    }

    public boolean hasRoute(String name) {
        return routes != null && routes.containsKey(name);
    }
}
