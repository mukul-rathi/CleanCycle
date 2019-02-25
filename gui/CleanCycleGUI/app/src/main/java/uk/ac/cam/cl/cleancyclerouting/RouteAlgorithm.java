package uk.ac.cam.cl.cleancyclerouting;

import java.util.*;

import uk.ac.cam.cl.cleancyclegraph.Edge;
import uk.ac.cam.cl.cleancyclegraph.Node;

public interface RouteAlgorithm {
    public List<Long> getBestPath(Algorithm type, Map<Long, Node> nodes, Map<Long, Edge> edges, Long from, Long to) throws GraphNotConnectedException;
}