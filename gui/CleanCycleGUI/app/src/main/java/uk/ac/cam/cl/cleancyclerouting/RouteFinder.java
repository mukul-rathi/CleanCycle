package uk.ac.cam.cl.cleancyclerouting;

import android.annotation.SuppressLint;

import java.io.*;
import java.lang.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

import CleanCycle.Analytics.Edge;
import CleanCycle.Analytics.EdgeComplete;
import CleanCycle.Analytics.MapInfoContainer;
import CleanCycle.Analytics.Node;

public class RouteFinder {
    private Map<Long, Node> nodes = null;
    private Map<Long, Edge> edges = null;
    private RouteAlgorithm algorithm;

    private ObjectInputStream graphInput;
    private Socket graphSocket;

    private MapInfoContainer mapInfoContainer;

    private RouteFinderUtil routeFinderUtil;

    public RouteFinder(String address, int nodePort, MapInfoContainer mapInfoContainer, RouteFinderUtil routeFinderUtil) throws NotSetUpException {
        this.routeFinderUtil = routeFinderUtil;
        try {
            graphSocket = new Socket(address, nodePort);
            graphInput = new ObjectInputStream(graphSocket.getInputStream());
            nodes = (Map<Long, Node>)deserialize(graphInput);
            edges = (Map<Long, Edge>)deserialize(graphInput);
            graphInput.close();
        } catch (ConnectException e) {
            try {
                nodes = loadCachedNodes();
                edges = loadCachedEdges();
            } catch (IOException e1) {
                e1.printStackTrace();
                System.err.println("NO GRAPH CACHE FOUND");
                throw new NotSetUpException(e1);
            }
        } catch(IOException | ClassNotFoundException e){
            throw new NotSetUpException(e);
        }
        algorithm = new LeastPollutedRA();
        this.mapInfoContainer = mapInfoContainer;
    }

    public void cacheNodes() throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(routeFinderUtil.getNodesOutputStream());
        oos.writeObject(nodes);
        oos.flush();
        oos.close();
    }

    public void cacheEdges() throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(routeFinderUtil.getEdgesOutputStream());
        oos.writeObject(edges);
        oos.flush();
        oos.close();
    }

    Map<Long, Node> loadCachedNodes() throws IOException {
        ObjectInputStream ois = new ObjectInputStream(routeFinderUtil.getNodesInputStream());
        Map<Long, Node> ns;
        try {
             ns = (Map<Long, Node>) ois.readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException(e);
        }
        ois.close();
        return ns;
    }

    Map<Long, Edge> loadCachedEdges() throws IOException {
        ObjectInputStream ois = new ObjectInputStream(routeFinderUtil.getEdgesInputStream());
        Map<Long,Edge> es;
        try {
            es = (Map<Long,Edge>) ois.readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
        ois.close();
        return es;
    }

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
                } else if (first.ID == edge.Node2ID && second.ID == edge.Node1ID) {
                    Edge e = new Edge(edge.ID, edge.WayID, edge.Node2ID, edge.Node1ID, edge.Distance);
                    e.Pollution = edge.Pollution;
                    edgs.add(e);
                    done = true;
                    break;
                }
            }
            if (!done) System.err.println(String.format(Locale.ENGLISH, "Failed to find edge containing nodes: %d and %d", first.ID, second.ID));
        }
        return edgs.stream().map(x -> {
            EdgeComplete ec = new EdgeComplete(x.ID, x.WayID, nodes.get(x.Node1ID), nodes.get(x.Node2ID), x.Distance, x.Pollution);
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
        if(edges == null || nodes == null){
            throw new NotSetUpException();
        }
        List<Double> pollution = new ArrayList<>();
        for (Edge edge : edges.values()) {
            pollution.add(edge.Pollution);
        }
        Collections.sort(pollution);

        int percentile5  =      pollution.size() / 20;
        int percentile10 =      pollution.size() / 10;
        int percentile90 =  9 * pollution.size() / 10;
        int percentile95 = 19 * pollution.size() / 20;

        mapInfoContainer.setPollutionPercentile5(pollution.get(percentile5));
        mapInfoContainer.setPollutionPercentile10(pollution.get(percentile10));
        mapInfoContainer.setPollutionPercentile90(pollution.get(percentile90));
        mapInfoContainer.setPollutionPercentile95(pollution.get(percentile95));

        /*for (Edge edge : edges.values()) {
            if (edge.Pollution > 1000f) continue;
            if (mapInfoContainer.getMaxPollution() < edge.Pollution) mapInfoContainer.setMaxPollution(edge.Pollution);
            if (mapInfoContainer.getMinPollution() > edge.Pollution) mapInfoContainer.setMinPollution(edge.Pollution);
        }*/
        Long from = findClosestNode(lat1, long1);
        Long to = findClosestNode(lat2, long2);
        return algorithm.getBestPath(algo, nodes, edges, from, to);
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

    public static Object deserialize(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException{
        Object object = objectInputStream.readObject();
        long serialVersionID = ObjectStreamClass.lookup(object.getClass()).getSerialVersionUID();
        //System.out.println("UID: " + serialVersionID);
        return object;
    }
}
