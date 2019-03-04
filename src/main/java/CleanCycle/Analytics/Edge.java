package CleanCycle.Analytics;

import java.io.Serializable;

/**
 * Class for an edge in the map graph.
 */
public class Edge implements Serializable {
    /* ID is an ID assigned by the program.
    Node1ID/Node2ID are the OSM IDs of the nodes at each end of the edge.
    Distance is the physical distance in metres from one end to the other.
    Pollution is the mean of all the PM2.5 and PM10 measurements within a given radius of the edge. */
    public final long ID;
    public final long Node1ID;
    public final long Node2ID;
    public final double Distance;
    public double Pollution;

    static final long serialVersionUID = 1L;

    public Edge(long ID_, long node1id_, long node2id_, double dist_) {
        ID = ID_;
        Node1ID = node1id_;
        Node2ID = node2id_;
        Distance = dist_;
    }
}
