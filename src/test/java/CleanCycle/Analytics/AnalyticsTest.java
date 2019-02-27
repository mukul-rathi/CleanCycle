package CleanCycle.Analytics;

import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        try {
            Main.loadPointsFromDatabase(points);
        }
        catch(IOException | ParseException e) {
            e.printStackTrace();
            fail();
        }

        assertTrue("Database pull error, found " + points.size() + " points, should have been > 0.", points.size() > 0);
    }

    @Test public void testJSONMapNodeCount() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();

        writeJSONMap();

        Main.readDataFromJSON("testMap.json", nodes, edges);

        assertEquals("Incorrect number of nodes, should be 7, found " + nodes.size() + ".", 7, nodes.size());
    }

    @Test public void testJSONMapEdgeCount() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();

        writeJSONMap();

        Main.readDataFromJSON("testMap.json", nodes, edges);

        assertEquals("Incorrect number of edges, should be 5, found " + edges.size() + ".", 5, edges.size());
    }

    @Test public void testComponentElimination() {

        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();
        final List<Point> points = new ArrayList<>();

        try {
            Main.loadPointsFromDatabase(points);
        }
        catch(IOException | ParseException e) {
            e.printStackTrace();
            fail();
        }

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
        assertEquals("Unexpected number of components: " + componentCount, 1, componentCount);
    }

    @Test public void testValidEdges() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();
        final List<Point> points = new ArrayList<>();

        try {
            Main.loadPointsFromDatabase(points);
        }
        catch(IOException | ParseException e) {
            e.printStackTrace();
            fail();
        }

        writeJSONMap();
        Main.readDataFromJSON("testMap.json", nodes, edges);

        List<Set<Long>> components = Main.getComponents(nodes, edges);
        Collections.sort(components, new Main.SizeComparator());
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

        Main.pointToEdge(points, edges, nodes);

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

    @Test public void testEdgeSocketTransmission() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();
        final List<Point> points = new ArrayList<>();

        try {
            Main.loadPointsFromDatabase(points);
        }
        catch(IOException | ParseException e) {
            e.printStackTrace();
            fail();
        }

        writeJSONMap();
        Main.readDataFromJSON("testMap.json", nodes, edges);

        List<Set<Long>> components = Main.getComponents(nodes, edges);
        Collections.sort(components, new Main.SizeComparator());
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

        Main.pointToEdge(points, edges, nodes);

        Thread sendEdgesThread = new Thread() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(54333);
                    while (true) {
                        Socket outputSocket = serverSocket.accept();
                        ObjectOutputStream out = new ObjectOutputStream(outputSocket.getOutputStream());

                        synchronized (nodes) {
                            synchronized (edges) {
                                out.writeObject(nodes);
                                out.writeObject(edges);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("There was an error transmitting analytics data.");
                    e.printStackTrace();
                }
            }
        };
        sendEdgesThread.setDaemon(true);
        sendEdgesThread.start();

        try {
            Thread.sleep(1000);
            Socket s = new Socket(InetAddress.getLocalHost(), 54333);
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());

            Map<Long, Node> newNodes = (Map<Long, Node>) in.readObject();
            Map<Long, Edge> newEdges = (Map<Long, Edge>) in.readObject();

            assertTrue("Node map size was not > 0.", newNodes.size() > 0);
            assertTrue("Edge map size was not > 0.", newEdges.size() > 0);
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.out.println("There was an error receiving analytics data.");
            e.printStackTrace();
            fail();
        }
    }

    @Test public void testPointSocketTransmission() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();
        final List<Point> points = new ArrayList<>();

        try {
            Main.loadPointsFromDatabase(points);
        }
        catch(IOException | ParseException e) {
            e.printStackTrace();
            fail();
        }

        writeJSONMap();
        Main.readDataFromJSON("testMap.json", nodes, edges);

        List<Set<Long>> components = Main.getComponents(nodes, edges);
        Collections.sort(components, new Main.SizeComparator());
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

        Main.pointToEdge(points, edges, nodes);

        Thread sendPointsThread = new Thread() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(54334);
                    while (true) {
                        Socket outputSocket = serverSocket.accept();
                        ObjectOutputStream out = new ObjectOutputStream(outputSocket.getOutputStream());

                        synchronized (points) {
                            out.writeObject(points);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("There was an error transmitting heatmap data.");
                    e.printStackTrace();
                }
            }
        };
        sendPointsThread.setDaemon(true);
        sendPointsThread.start();

        try {
            Thread.sleep(1000);
            Socket s = new Socket(InetAddress.getLocalHost(), 54334);
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());

            List<Point> newPoints = (List<Point>)in.readObject();

            assertTrue("Point list size was not > 0.", newPoints.size() > 0);
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.out.println("There was an error receiving heatmap data.");
            e.printStackTrace();
            fail();
        }
    }
}