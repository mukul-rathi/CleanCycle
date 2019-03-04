package CleanCycle.Analytics;

import org.junit.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Collections;

import static CleanCycle.Analytics.AnalyticsUtils.pointToEdge;
import static CleanCycle.Analytics.FileUtils.readDataFromJSON;
import static CleanCycle.Analytics.SpoofJSON.writeJSONMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The AnalyticsTest class contains most of the unit tests for this module.
 */
public class AnalyticsTest {
    /**
     * Test for connecting to Mukul's database.
     */
    @Test public void testDatabaseQuery() {
        final List<Point> points = new ArrayList<>();

        Main.loadPointsFromDatabase(points);

        assertTrue("Database pull error, found " + points.size() + " points, should have been > 0.", points.size() > 0);
    }

    /**
     * Test for reading in a list of nodes from a spoof OSM JSON file.
     */
    @Test public void testJSONMapNodeCount() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();

        writeJSONMap();

        readDataFromJSON("testMap.json", nodes, edges);

        assertEquals("Incorrect number of nodes, should be 7, found " + nodes.size() + ".", 7, nodes.size());
    }

    /**
     * Test for reading in a list of edges from a spoof OSM JSON file.
     */
    @Test public void testJSONMapEdgeCount() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();

        writeJSONMap();

        readDataFromJSON("testMap.json", nodes, edges);

        assertEquals("Incorrect number of edges, should be 5, found " + edges.size() + ".", 5, edges.size());
    }

    /**
     * Test for the use of DFS to remain small connected components left over by the bounding box
     * of the Overpass API.
     */
    @Test public void testComponentElimination() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();
        final List<Point> points = new ArrayList<>();

        Main.loadPointsFromDatabase(points);

        writeJSONMap();
        readDataFromJSON("testMap.json", nodes, edges);

        /* After reading in the spoof OSM JSON, we get its components via DFS,
        then sort the components by size using a custom comparator. Take a set of
        all the nodes in the largest component (the one we will use), then remove all
        nodes and edges that reference nodes not in the set. The remaining set of nodes
        should only be one component.
         */
        List<Set<Long>> components = Main.getComponents(nodes, edges);
        Collections.sort(components, new AnalyticsUtils.SizeComparator());
        Collections.reverse(components);
        Set<Long> IDsOfMainComponent = components.get(0);

        Iterator nodeIt = nodes.keySet().iterator();
        while (nodeIt.hasNext()) {
            Long nodeID = (Long)nodeIt.next();
            if (!IDsOfMainComponent.contains(nodeID)) {
                nodeIt.remove();
            }
        }

        Iterator edgeIt = edges.keySet().iterator();
        while (edgeIt.hasNext()) {
            Long edgeID = (Long)edgeIt.next();
            Edge edge = edges.get(edgeID);
            if (!IDsOfMainComponent.contains(edge.Node1ID) || !IDsOfMainComponent.contains(edge.Node2ID)) {
                edgeIt.remove();
            }
        }

        int componentCount = Main.getComponents(nodes, edges).size();
        assertEquals("Unexpected number of components: " + componentCount, 1, componentCount);
    }

    /**
     * Test to ensure all edges have nodes that exist listed in their end fields.
     */
    @Test public void testValidEdges() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();
        final List<Point> points = new ArrayList<>();

        Main.loadPointsFromDatabase(points);

        writeJSONMap();
        readDataFromJSON("testMap.json", nodes, edges);

        List<Set<Long>> components = Main.getComponents(nodes, edges);
        Collections.sort(components, new AnalyticsUtils.SizeComparator());
        Collections.reverse(components);
        Set<Long> IDsOfMainComponent = components.get(0);

        /* First do the component elimination as before. */

        Iterator nodeIt = nodes.keySet().iterator();
        while (nodeIt.hasNext()) {
            Long nodeID = (Long)nodeIt.next();
            if (!IDsOfMainComponent.contains(nodeID)) {
                nodeIt.remove();
            }
        }

        Iterator edgeIt = edges.keySet().iterator();
        while (edgeIt.hasNext()) {
            Long edgeID = (Long)edgeIt.next();
            Edge edge = edges.get(edgeID);
            if (!IDsOfMainComponent.contains(edge.Node1ID) || !IDsOfMainComponent.contains(edge.Node2ID)) {
                edgeIt.remove();
            }
        }

        /* Perform the point to edge algorithm and check all edges still
        contain nodes that are in the node list. */

        pointToEdge(points, edges, nodes);

        boolean testResult = true;

        for (Long edgeID : edges.keySet()) {
            Edge edge = edges.get(edgeID);
            if (!nodes.keySet().contains(edge.Node1ID))
                testResult = false;
            if (!nodes.keySet().contains(edge.Node2ID))
                testResult = false;
        }

        assertTrue("Invalid edges exist", testResult);
    }
}