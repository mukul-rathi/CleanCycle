package uk.ac.cam.cl.cleancyclerouting;

import android.annotation.SuppressLint;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.stream.Collectors;

import uk.ac.cam.cl.cleancyclegraph.Edge;
import uk.ac.cam.cl.cleancyclegraph.EdgeComplete;
import uk.ac.cam.cl.cleancyclegraph.Node;

public class RouteFinder{
    private Map<Long, Node> nodes = null;
    private Map<Long, Edge> edges = null;
    private RouteAlgorithm algorithm;

    private InputStream nodesIs;
    private InputStream edgesIs;

    public RouteFinder(InputStream nodes, InputStream edges){
        nodesIs = nodes;
        edgesIs = edges;
        algorithm = new LeastPollutedRA();
    }

    /*private List<Edge> findPath(Long startNode, Long endNode) {
        List<Edge> ret = new ArrayList<>();
        Map<Long,Edge> pred = new HashMap<>();
        Node n = nodes.get(startNode);
        LinkedList<Node> nodes = new LinkedList<>();
        nodes.addLast(n);
        while (!nodes.isEmpty()) {
            Node node = nodes.pollFirst();
            // TODO: finish depth first search to find path between nodes
        }
    }*/

    @SuppressLint("NewApi")
    public List<EdgeComplete> findRouteEdges(Algorithm algo, Double lat1, Double lon1, Double lat2, Double lon2) throws NotSetUpException, GraphNotConnectedException {
        List<Node> nods = findRoute(algo, lat1, lon1, lat2, lon2);
        List<Edge> edgs = new ArrayList<>();
        for (int i=0; i<nods.size()-1; ++i) {
            Node first = nods.get(i);
            Node second = nods.get(i+1);

            boolean done = false;
            for (Edge edge : edges.values()) {
                if (first.ID == edge.Node1ID && second.ID == edge.Node2ID) {
                    edgs.add(edge);
                    done = true;
                    break;
                }
            }
            if (!done) System.err.println(String.format(Locale.ENGLISH, "Failed to find edge containing nodes: %d and %d", first.ID, second.ID));
        }
        return edgs.stream().map(x -> {
            EdgeComplete ec = new EdgeComplete(x.ID, x.WayID, nodes.get(x.Node1ID), nodes.get(x.Node2ID), x.Distance);
            ec.Pollution = x.Pollution;
            return ec;
        }).collect(Collectors.toList());
    }

    public List<Node> findRoute(Algorithm algo, Double lat1, Double lon1, Double lat2, Double lon2) throws NotSetUpException, GraphNotConnectedException {
        List<Long> nodeIds = findBestPath(algo, lat1, lon1, lat2, lon2);
        List<Node> nodeList = new ArrayList<>();
        for (Long n : nodeIds) {
            nodeList.add(nodes.get(n));
        }
        return nodeList;
    }

    private List<Long> findBestPath(Algorithm algo, Double lat1, Double long1, Double lat2, Double long2) throws NotSetUpException, GraphNotConnectedException{
        if(nodes == null){
            if(!fetchNodes()) throw new NotSetUpException(1);
        }
        if(edges == null){
            if(!fetchEdges()) throw new NotSetUpException(2);
        }
        Long from = findClosestNode(lat1, long1);
        Long to = findClosestNode(lat2, long2);
        return algorithm.getBestPath(algo, nodes, edges, from, to);
    }

    @SuppressWarnings("unchecked")
    private boolean fetchNodes(){
        //this is a placeholder
        try {
            nodes = (Map<Long, Node>)deserialize(nodesIs);
            return true;
        } catch(Exception exc){
            exc.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean fetchEdges(){
        //this is a placeholder
        try {
            edges = (Map<Long, Edge>)deserialize(edgesIs);
            normalize();
            return true;
        } catch(Exception exc){
            exc.printStackTrace();
            return false;
        }
    }
    private void normalize(){
        double maxPollution = 0.0, maxDistance = 0.0;
        for(Object obj : edges.values()){
            Edge cur_edge = (Edge)obj;
            maxPollution = Math.max(maxPollution, cur_edge.Pollution);
            maxDistance = Math.max(maxDistance, cur_edge.Distance);
        }
        for(Object obj : edges.values()){
            Edge cur_edge = (Edge)obj;
            cur_edge.Pollution *= maxDistance / maxPollution;
        }
    }
    public static double latLongDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return c;
        /*double r = 6371000;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double lambda1 = Math.toRadians(lon1);
        double lambda2 = Math.toRadians(lon2);
        double deltaSigma = Math.acos(Math.sin(phi1)*Math.sin(phi2) + Math.cos(phi1) * Math.cos(phi2) * Math.cos(lambda1 - lambda2));
        return r * deltaSigma;*/
    }
    public Long findClosestNode(Double latitude, Double longitude){
        Long best = -1L;
        Double bestDistance = Double.POSITIVE_INFINITY;
        for(Node node : nodes.values()){
            Double curDistance = latLongDistance(latitude, longitude, node.Latitude, node.Longitude);
            if(curDistance < bestDistance) {
                bestDistance = curDistance;
                best = node.ID;
            }
        }
        return best;
    }

    public static Object deserialize(InputStream is) throws IOException, ClassNotFoundException{
        ObjectInputStream objectInputStream = new ObjectInputStream(is);
        Object object = objectInputStream.readObject();
        long serialVersionID = ObjectStreamClass.lookup(object.getClass()).getSerialVersionUID();
        System.out.println("UID: " + serialVersionID);
        objectInputStream.close();
        return object;
    }
}
