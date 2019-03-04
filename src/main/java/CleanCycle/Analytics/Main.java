package CleanCycle.Analytics;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.Socket;
import java.net.ServerSocket;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Collections;
import java.util.HashSet;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import static CleanCycle.Analytics.AnalyticsUtils.*;

import static CleanCycle.Analytics.FileUtils.getPointsFromBigCSV;
import static CleanCycle.Analytics.FileUtils.readDataFromJSON;

/**
 * The main class for the analytics code.
 */
public class Main {
    /**
     * This function will load the set of points from the database
     * 
     * @param points the list of points to be parsed into
     */
    static void loadPointsFromDatabase(List<Point> points) {
        /* We use an exponential back off loop to make multiple attempts to connect to Mukul's endpoint.
           Every time we fail, we double the time waited until we try again. */
        int backOffTime = 1;
        boolean success = false;

        while(!success) {
            try {
                /* Open an HTTP connection to Mukul's endpoint. */
                HttpURLConnection connection = ((HttpURLConnection) new URL("http://endpoint/analytics").openConnection());
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    /* Get the entire response using a string builder. */
                    BufferedReader inReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder jsonStringBuilder = new StringBuilder();
                    String line;
                    while ((line = inReader.readLine()) != null) {
                        jsonStringBuilder.append(line).append("\n");
                    }

                    String jsonString = jsonStringBuilder.toString();
                    JSONParser parser = new JSONParser();
                    JSONArray bigArray = (JSONArray) parser.parse(jsonString); // "new FileReader(filename)" for file,
                    // jsonString for string

                    /* The lock on the list of points only activates here, meaning it only locks
                    if we've established a successful connection and parsed successfully.
                     */
                    synchronized (points) {

                        points.clear();
                        for (Object obj : bigArray) {
                            JSONArray littleArray = (JSONArray) obj;

                            points.add(new Point((double) littleArray.get(0), (double) littleArray.get(1),
                                    (double) littleArray.get(2), (double) littleArray.get(3)));
                        }

                    }

                    success = true;
                }
            }

            /* If we fail to connect during this time, we wait and then increase wait time. */
            catch(IOException | ParseException e) {
                try {
                    System.out.println("Error fetching data, increasing backoff time...");
                    Thread.sleep(new java.util.Random().nextInt(backOffTime * 100));
                    backOffTime *= 2;

                }
                catch(InterruptedException f) {
                    System.out.println("Interrupted while waiting for data.");
                    f.printStackTrace();
                }
            }
        }
    }

    static final Map<Long, Node> nodes = new HashMap<>();
    static final Map<Long, Edge> edges = new HashMap<>();
    static final List<Point> points = new ArrayList<>();

    /* This was a test to make sure use of sockets via ngrok TCP was working.
    public static void getPointsTest() {
        try {
            Socket socket = new Socket("f2d74e41.ngrok.io", 80);
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Map<Long, Node> inNodes = (Map<Long, Node>) ois.readObject();
            ois.close();
            socket.close();
        }
        catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    */

    /**
     * The main function is the main thread of the module.
     *
     * @param args the command line arguments of the program.
     */
    public static void main(String[] args) {
        /*getPointsTest();
        System.exit(0);*/

        loadPointsFromDatabase(points);

        readDataFromJSON("map.json", nodes, edges);

        /* Filter the largest component out of the slightly mangled OSM data. */
        List<Set<Long>> components = getComponents(nodes, edges);
        Collections.sort(components, new AnalyticsUtils.SizeComparator());
        Collections.reverse(components);
        Set<Long> IDsOfMainComponent = components.get(0);

        /* Now we remove all nodes and edges that reference non-main components. */

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

        /* Perform the analysis algorithm to find pollution amounts per edge. */
        pointToEdge(points, edges, nodes);

        /* This thread sends the graph of nodes and edges to the route planning backend. */
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

                        out.flush();
                    }
                } catch (IOException e) {
                    System.out.println("There was an error transmitting analytics data.");
                    e.printStackTrace();
                }
            }
        };
        sendEdgesThread.setDaemon(true);
        sendEdgesThread.start();

        /* This thread sends the points data set to the app frontend in case it needs to be used for a heatmap. */
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

                        out.flush();
                    }
                } catch (IOException e) {
                    System.out.println("There was an error transmitting heatmap data.");
                    e.printStackTrace();
                }
            }
        };
        sendPointsThread.setDaemon(true);
        sendPointsThread.start();

        /* This part wrote the edges and notes to an output file, from before sockets were set up.

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("nodes.obj"));
        oos.writeObject(nodes);
        oos.close();

        oos = new ObjectOutputStream(new FileOutputStream("edges.obj"));
        oos.writeObject(edges);
        oos.close();
        */

        /* The original thread loops, refreshing the data set every ten minutes. */

        while (true) {
            try {
                Thread.sleep(1000 * 60 * 10);
            } catch (InterruptedException e) {
                System.out.println("The data refresh thread was interrupted.");
                e.printStackTrace();
            }

            loadPointsFromDatabase(points);

            Map<Long, Node> newNodes = new HashMap<>(nodes);
            Map<Long, Edge> newEdges = new HashMap<>(edges);

            pointToEdge(points, newEdges, newNodes);

            /* Since pointToEdge effectively blocks this thread until it can get new points,
            this part won't run until there's a new set of data to make use of.
             */
            synchronized (edges) {
                synchronized (nodes) {
                    edges.clear();
                    nodes.clear();
                    for (long nodeID : newNodes.keySet()) {
                        nodes.put(nodeID, newNodes.get(nodeID));
                    }
                    for (long edgeID : newEdges.keySet()) {
                        edges.put(edgeID, newEdges.get(edgeID));
                    }
                }
            }
        }
    }

    /* This part of the code was mostly written by Vladimir - I just cleaned it up a bit and
    adapted it to allow a list of connected components to be returned. */

    static final Set<Long> visited = new HashSet<>();
    static final Set<Long> tempVisited = new HashSet<>();

    static void dfs(Long src, Map<Long, Node> nodes, Map<Long, Edge> edges) {
        tempVisited.add(src);
        visited.add(src);

        Node curNode = nodes.get(src);
        List<Long> curEdges = curNode.Edges;

        curEdges.forEach(edgeID -> {
            Edge curEdge = edges.get(edgeID);
            Long xt = curEdge.Node1ID + curEdge.Node2ID - src;
            if (visited.contains(xt)) return;
            dfs(xt, nodes, edges);
        });
    }

    static List<Set<Long>> getComponents (Map<Long, Node> nodes, Map<Long, Edge> edges) {
        List<Set<Long>> components = new ArrayList<>();
        visited.clear();

        nodes.forEach((nodeID, node) -> {
            if (visited.contains(nodeID)) return;

            tempVisited.clear();
            dfs(nodeID, nodes, edges);
            components.add(new HashSet<>(tempVisited));
        });

        return components;
    }

}