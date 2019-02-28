package uk.ac.cam.cl.cleancyclegui;

import java.util.List;

import uk.ac.cam.cl.cleancyclegraph.EdgeComplete;

public interface RouteHandler {
    void handleRoute(List<EdgeComplete> route);
}
