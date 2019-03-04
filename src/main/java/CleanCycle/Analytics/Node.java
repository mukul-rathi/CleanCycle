package CleanCycle.Analytics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for a node in the map graph.
 */
public class Node implements Serializable {
    /* ID is the OSM ID of the node.
    Latitude and Longitude are the physical location of the node.
    Edges is the unique list of Edges coming out of the node.
     */
    public final long ID;
    public final double Latitude;
    public final double Longitude;
    public List<Long> Edges;

    static final long serialVersionUID = 1L;

    public Node(long id_, double lat_, double long_) {
        ID = id_;
        Latitude = lat_;
        Longitude = long_;
        Edges = new ArrayList<>();
    }
}
