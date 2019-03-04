package uk.ac.cam.cl.cleancyclegui;

import java.util.List;

import CleanCycle.Analytics.EdgeComplete;

public interface RouteHandler {
    void handleRoute(List<EdgeComplete> route);
}
