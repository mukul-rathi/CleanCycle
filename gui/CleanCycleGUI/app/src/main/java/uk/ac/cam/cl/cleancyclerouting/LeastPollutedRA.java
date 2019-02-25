package uk.ac.cam.cl.cleancyclerouting;

import java.util.*;

import uk.ac.cam.cl.cleancyclegraph.Edge;
import uk.ac.cam.cl.cleancyclegraph.Node;

public class LeastPollutedRA implements RouteAlgorithm {
    Map<Long, Double> value;
    Map<Long, Double> dist;
    Map<Long, Long> previous;
    public LeastPollutedRA(){
        value = new HashMap<Long, Double>();
        dist = new HashMap<Long, Double>();
        previous = new HashMap<Long, Long>();
    }
    public class PQEntry implements Comparable<PQEntry> {
        public Long ID;
        public Double d;
        PQEntry(Long ID_, Double d_){
            ID = ID_;
            d = d_;
        }
        @Override
        public int compareTo(PQEntry other){
            return Double.compare(d, other.d);
        }
    }
    private Double normalize(double part, Double pollution, Double distance){
        return part * pollution + (1.0 - part) * distance;
    }
    private Double getValue(Algorithm type, Edge edge){
        if(type == Algorithm.POLLUTION_ONLY) return edge.Pollution;
        else if(type == Algorithm.DISTANCE_ONLY) return edge.Distance;
        else if(type == Algorithm.MIXED_1) return normalize(0.25, edge.Pollution, edge.Distance);
        else if(type == Algorithm.MIXED_2) return normalize(0.5, edge.Pollution, edge.Distance);
        else return normalize(0.75, edge.Pollution, edge.Distance);
    }
    public List<Long> getBestPath(Algorithm type, Map<Long, Node> nodes, Map<Long, Edge> edges, Long from, Long to) throws GraphNotConnectedException {
        for (Map.Entry<Long, Edge> e : edges.entrySet()) {
            Long key = e.getKey();
            Edge edge = e.getValue();
            value.put(key, getValue(type, edge));
            nodes.get(edge.Node1ID).Edges.add(edge.ID);
        }
        for (Map.Entry<Long, Node> entry : nodes.entrySet()) {
            Long k = entry.getKey();
            Node v = entry.getValue();
            dist.put(k, Double.POSITIVE_INFINITY);
        }
        dist.put(from, 0.0);
        PriorityQueue<PQEntry> pq = new PriorityQueue<PQEntry>();
        pq.add(new PQEntry(from, 0.0));
        int nodesVisited = 0;
        while(!pq.isEmpty()){
            nodesVisited += 1;
            PQEntry tEntry = pq.remove();
            Long cur = tEntry.ID;
            Double curPollution = tEntry.d;
            Node curNode = nodes.get(cur);
            for (Long edgeID : curNode.Edges) {
                if (!edges.containsKey(edgeID)) continue;
                Long xt = edges.get(edgeID).Node1ID + edges.get(edgeID).Node2ID - cur;
                Double tryPollution = curPollution + value.get(edgeID);
                if (tryPollution < dist.get(xt)) {
                    dist.put(xt, tryPollution);
                    previous.put(xt, cur);
                    pq.add(new PQEntry(xt, tryPollution));
                }
            }
            if(cur == to) break;
        }
        previous.put(from, -1L);
        if(dist.get(to).equals(Double.POSITIVE_INFINITY)) {
            System.out.println(nodes);
            System.out.println(edges);
            throw new GraphNotConnectedException(String.format(Locale.ENGLISH, "%d %d", from, to));
        }
        Long lastNode = to;
        List<Long> returned = new ArrayList<>();
        while(!lastNode.equals(-1L)){
            returned.add(lastNode);
            lastNode = previous.get(lastNode);
            if(lastNode == null){
                System.out.println(String.format("last node: %d", lastNode));
                throw new GraphNotConnectedException(String.format(Locale.ENGLISH, "%d %d", from, to));
            }
        }
        Collections.reverse(returned);
        return returned;
    }
}