package uk.ac.cam.cl.cleancyclerouting;

import java.util.*;

import CleanCycle.Analytics.Edge;
import CleanCycle.Analytics.Node;

public interface RouteAlgorithm {
    public List<Long> getBestPath(Algorithm type, Map<Long, Node> nodes, Map<Long, Edge> edges, Long from, Long to) throws GraphNotConnectedException;
}