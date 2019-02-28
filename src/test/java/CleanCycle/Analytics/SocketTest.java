package CleanCycle.Analytics;

import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SocketTest {
    @Test
    public void testEdgeSocketTransmission() {
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
