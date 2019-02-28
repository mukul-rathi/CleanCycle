package CleanCycle.Analytics;

import java.io.Serializable;
import java.util.Locale;

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

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Edge(%d, %d, n1=%d, n2=%d, d=%f, p=%f)", ID, WayID, Node1ID, Node2ID, Distance, Pollution);
    }
}
