package uk.ac.cam.cl.cleancyclegraph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Node implements Serializable {
    public final long ID;
    public final double Latitude;
    public final double Longitude;
    /* List of unique edge IDs connected to this node. */
    public List<Long> Edges;

    static final long serialVersionUID = 1L;

    public Node(long id_, double lat_, double long_) {
        ID = id_;
        Latitude = lat_;
        Longitude = long_;
        Edges = new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Node(%d, <%f, %f>, %s)", ID, Latitude, Longitude, Edges);
    }
}