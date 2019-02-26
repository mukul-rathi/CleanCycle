package CleanCycle.Analytics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class AnalyticsTest {
    private void writeJSONMap() {
        try {
            PrintWriter writer = new PrintWriter("testMap.json");
            writer.write("{\n" +
                    "\"elements\": [\n" +
                    " {\n" +
                    "  \"type\": \"way\",\n" +
                    "\"id\": 1,\n" +
                    "\"nodes\": [1, 2, 3, 4, 5]\n" +
                    "},\n" +
                    "\n" +
                    "{\n" +
                    "\"type\": \"way\", \n" +
                    "\"id\": 2,\n" +
                    "\"nodes\": [6, 7]\n" +
                    "},\n" +
                    "\n" +
                    "{\n" +
                    "\"type\": \"node\",\n" +
                    "\"id\": 1,\n" +
                    "\"lat\": 52.2,\n" +
                    "\"lon\": 0.15\n" +
                    "},\n" +
                    "\n" +
                    "{\n" +
                    "\"type\": \"node\",\n" +
                    "\"id\": 2,\n" +
                    "\"lat\": 52.21,\n" +
                    "\"lon\": 0.151\n" +
                    "},\n" +
                    "\n" +
                    "{\n" +
                    "\"type\": \"node\",\n" +
                    "\"id\": 3,\n" +
                    "\"lat\": 52.22,\n" +
                    "\"lon\": 0.152\n" +
                    "},\n" +
                    "\n" +
                    "{\n" +
                    "\"type\": \"node\",\n" +
                    "\"id\": 4,\n" +
                    "\"lat\": 52.23,\n" +
                    "\"lon\": 0.153\n" +
                    "},\n" +
                    "\n" +
                    "{\n" +
                    "\"type\": \"node\",\n" +
                    "\"id\": 5,\n" +
                    "\"lat\": 52.24,\n" +
                    "\"lon\": 0.154\n" +
                    "},\n" +
                    "\n" +
                    "{\n" +
                    "\"type\": \"node\",\n" +
                    "\"id\": 6,\n" +
                    "\"lat\": 52.25,\n" +
                    "\"lon\": 0.155\n" +
                    "},\n" +
                    "\n" +
                    "{\n" +
                    "\"type\": \"node\",\n" +
                    "\"id\": 7,\n" +
                    "\"lat\": 52.26,\n" +
                    "\"lon\": 0.156\n" +
                    "}\n" +
                    "\n" +
                    "]\n" +
                    "}");
            writer.close();
        }
        catch(IOException e) {
            System.out.println("Error writing test map JSON file.");
            e.printStackTrace();
        }
    }

    @Test public void testDatabaseQuery() {
        final List<Point> points = new ArrayList<>();

        Main.loadPointsFromDatabase(points);

        Assertions.assertTrue(points.size() > 100000, "Database pull error, only found " + points.size() + " points, should have been ~130k.");
    }

    @Test public void testJSONMapNodeCount() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();

        writeJSONMap();

        Main.readDataFromJSON("testMap.json", nodes, edges);

        Assertions.assertEquals(7, nodes.size(), "Incorrect number of nodes, should be 7, found " + nodes.size() + ".");
    }

    @Test public void testJSONMapEdgeCount() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();

        writeJSONMap();

        Main.readDataFromJSON("testMap.json", nodes, edges);

        Assertions.assertEquals(5, edges.size(), "Incorrect number of edges, should be 5, found " + edges.size() + ".");
    }

    @Test public void testComponentElimination() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();
        final List<Point> points = new ArrayList<>();

        Main.loadPointsFromDatabase(points);

        writeJSONMap();
        Main.readDataFromJSON("testMap.json", nodes, edges);

        List<Set<Long>> components = Main.getComponents(nodes, edges);
        Collections.sort(components, new Main.SizeComparator());
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
        Assertions.assertEquals(1, componentCount, "Unexpected number of components: " + componentCount);
    }

    @Test public void testValidEdges() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();
        final List<Point> points = new ArrayList<>();

        Main.loadPointsFromDatabase(points);

        writeJSONMap();
        Main.readDataFromJSON("testMap.json", nodes, edges);

        List<Set<Long>> components = Main.getComponents(nodes, edges);
        Collections.sort(components, new Main.SizeComparator());
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

        Main.pointToEdge(points, edges, nodes);

        boolean testResult = true;

        for (Long edgeID : edges.keySet()) {
            Edge edge = edges.get(edgeID);
            if (!nodes.keySet().contains(edge.Node1ID))
                testResult = false;
            if (!nodes.keySet().contains(edge.Node2ID))
                testResult = false;
        }

        Assertions.assertTrue(testResult, "Invalid edges exist");
    }
}