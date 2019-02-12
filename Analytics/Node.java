import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class Node implements Serializable {
    public final long ID;
    public final double Latitude;
    public final double Longitude;
    /* Each adjacent edge entry is of the form <Neighbouring Node ID, Connecting Edge ID>. */
    public Map<Long, Long> Edges;

    public Node(long id_, double lat_, double long_) {
        ID = id_;
        Latitude = lat_;
        Longitude = long_;
        Edges = new TreeMap<>();
    }
}
