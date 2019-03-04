package uk.ac.cam.cl.cleancyclegui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import CleanCycle.Analytics.EdgeComplete;

/**
 * A class that allows the list of routes to be passed around and modified.
 *
 * This is necessary so that an asynchronous task can modify the routes accessible by the
 * main activity
 */
public class SavedRoutes implements Serializable {
    private Map<String,List<EdgeComplete>> routes;

    public SavedRoutes() {

    }

    public SavedRoutes(Map<String,List<EdgeComplete>> routes) {
        this.routes = routes;
    }

    /**
     * Check that there are actually routes to be loaded.
     *
     * @throws RoutesNotLoadedException Thrown if the value of routes has not been initialised.
     */
    private synchronized void checkRoutesExist() throws RoutesNotLoadedException {
        if (routes == null) throw new RoutesNotLoadedException();
    }

    /**
     * Get a copy of the stored routes.
     *
     * @return A map containing the same information as the routes map.
     */
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

    /**
     * Set the routes map to that supplied.
     * @param routes
     */
    public synchronized void setRoutes(Map<String,List<EdgeComplete>> routes) {
        this.routes = routes;
    }

    /**
     * Return the number of routes currently stored.
     *
     * @return The number of routes currently stored.
     */
    public synchronized int numberOfRoutes() {
        if (routes == null) return 0;
        return routes.size();
    }

    /**
     * Get the list of names of routes sorted in lexicographic order.
     *
     * @return The list of route names.
     * @throws RoutesNotLoadedException Thrown if the routes map has not been initialised.
     */
    public List<String> getNames() throws RoutesNotLoadedException{
        checkRoutesExist();
        List<String> names = new ArrayList<>(routes.keySet());
        Collections.sort(names);
        return names;
    }

    /**
     * Get the route with the given name.
     *
     * @param name The name of the route.
     * @return The route with the given name.
     * @throws RoutesNotLoadedException Thrown routes has not been initialised.
     */
    public synchronized List<EdgeComplete> getRoute(String name) throws RoutesNotLoadedException {
        checkRoutesExist();
        return new ArrayList<>(routes.get(name));
    }

    /**
     * Add a route with a specified name.
     *
     * @param name The name of the route.
     * @param route The route.
     */
    public synchronized void addRoute(String name, List<EdgeComplete> route) {
        if (routes == null) routes = new HashMap<>();
        routes.put(name, route);
    }

    /**
     * Remove the route with the given name.
     *
     * @param name The name of the route to be removed.
     */
    public synchronized void removeRoute(String name) {
        if (routes == null) return;
        routes.remove(name);
    }

    /**
     * Check if a route exists with a given name.
     *
     * @param name The name.
     * @return A boolean with value true if a route with that name already exists and false otherwise.
     */
    public boolean hasRoute(String name) {
        return routes != null && routes.containsKey(name);
    }
}
