import java.io.Serializable;

public class Edge implements Serializable {
    public final long ID;
    public final long WayID;
    /* These are the IDs of the nodes at each end of the edge. */
    public final long Node1ID;
    public final long Node2ID;
    public final double Distance;
    public double Pollution;

    static final long serialVersionUID = 1L;

    public Edge(long ID_, long wayID_, long node1id_, long node2id_, double dist_) {
        ID = ID_;
        WayID = wayID_;
        Node1ID = node1id_;
        Node2ID = node2id_;
        Distance = dist_;
    }
}
