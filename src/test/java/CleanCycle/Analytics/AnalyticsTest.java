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

public class AnalyticsTest {
    @Test public void testDatabaseQuery() {
        final List<Point> points = new ArrayList<>();

        Main.loadPointsFromDatabase(points);

        assertTrue("Database pull error, found " + points.size() + " points, should have been > 0.", points.size() > 0);
    }

    @Test public void testJSONMapNodeCount() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();

        writeJSONMap();

        readDataFromJSON("testMap.json", nodes, edges);

        assertEquals("Incorrect number of nodes, should be 7, found " + nodes.size() + ".", 7, nodes.size());
    }

    @Test public void testJSONMapEdgeCount() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();

        writeJSONMap();

        readDataFromJSON("testMap.json", nodes, edges);

        assertEquals("Incorrect number of edges, should be 5, found " + edges.size() + ".", 5, edges.size());
    }

    @Test public void testComponentElimination() {

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

        /* Now we remove all nodes and edges that reference non-main components */

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