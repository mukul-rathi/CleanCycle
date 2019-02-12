public class Edge {
    public final long ID;
    /* These are the IDs of the nodes at each end of the edge. */
    public final long Node1ID;
    public final long Node2ID;
    public final double Distance;
    public double Pollution;

    public Edge(long id, long node1id_, long node2id_, double dist_) {
        ID = id;
        Node1ID = node1id_;
        Node2ID = node2id_;
        Distance = dist_;
    }
}
