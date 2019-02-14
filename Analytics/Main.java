import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import org.json.simple.*;
import org.json.simple.parser.*;

public class Main {
    /**
     * This function uses the haversine formula to calculate the great circle distance in metres between two nodes.
     * @param n1 The first node.
     * @param n2 The second node.
     * @return the distance in metres from node 1 to node 2.
     */
    public static double haversineDistance(Node n1, Node n2) {
        /* The radius of the earth in metres. */
        final int r = 6517219;

        /* First we convert the latitudes and differences into radians. */
        double phi1 = Math.toRadians(n1.Latitude);
        double phi2 = Math.toRadians(n2.Latitude);
        double deltaphi = Math.toRadians(n2.Latitude - n1.Latitude);
        double deltalambda = Math.toRadians(n2.Longitude - n1.Longitude);

        /* The value a is the square of half the chord length between the points. */
        double a = Math.sin(deltaphi/2) * Math.sin(deltaphi/2) +
                        Math.cos(phi1) * Math.cos(phi2) *
                        Math.sin(deltalambda/2) * Math.sin(deltalambda/2);

        /* The value c is the angular distance in radians. */
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        /* Finally return the distance in metres. */
        return r * c;
    }

    /**
     * Read in the large, uncondensed CSV file and fills up the list of pollution data points.
     * @param filename the name of the CSV file.
     */
    public static List<Point> getPointsFromBigCSV(String filename) {
        List<Point> points = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {

            /* Read in the first line without doing anything as it's just a key */
            String line;
            reader.readLine();

            /* Read in the line as a CSV record and extract the relevant data into a new Point object */
            while ((line = reader.readLine()) != null) {
                String[] record = line.split(",");
                points.add(new Point(Double.parseDouble(record[15]), Double.parseDouble(record[16]), Double.parseDouble(record[19]), Double.parseDouble(record[20])));
            }

        } catch (IOException e) {
            System.out.println("There was an error processing the CSV file.");
            e.printStackTrace();
        }

        return points;
    }

    /**
     * Write out the current list of points in CSV format.
     * @param filename the name of the file to write to.
     * @param points the list of points.
     */
    public static void writePointsToCSV(String filename, List<Point> points) {
        /* Because the original CSV file has obvious precision errors, we round the data to the intended six decimal places. */
        DecimalFormat df = new DecimalFormat("#0.000000");

        try {
            /* Write out the first line as a key, then each point as a separate CSV record. */
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            writer.println("KEY: Latitude, Longitude, PM10, PM2.5");
            for (Point p : points) {
                writer.println(df.format(p.Latitude) + "," + df.format(p.Longitude) + "," + p.Pollution10 + "," + p.Pollution2_5);
            }

        } catch (IOException e) {
            System.out.println("There was an error writing the new CSV file.");
            e.printStackTrace();
        }
    }

    /**
     * Read in the OSM data from a JSON file. The two argument maps are filled with the results.
     * @param filename the name of the JSON file to be parsed.
     * @param nodes the map of Node WayID -> Node to be filled.
     * @param edges the map of Edge WayID -> List of edges with that WayID to be filled.
     */
    public static Map<Long, Node> readDataFromJSON(String filename, Map<Long, Node> nodes, Map<Long, List<Edge>> edges) {

        try {
            /* First we initialize the JSON objects to be used. */

            JSONParser parser = new JSONParser();

            JSONObject container = (JSONObject)parser.parse(new FileReader(filename));

            JSONArray array = (JSONArray)container.get("elements");

            /* The first pass over the array finds all the nodes in the JSON and stores their latitude and longitude. */
            for (Object obj : array) {
                JSONObject jobj = (JSONObject)obj;

                String type = (String)jobj.get("type");

                if (type.equals("node")) {
                    long id = (long)jobj.get("id");
                    double latitude = (double)jobj.get("lat");
                    double longitude = (double)jobj.get("lon");

                    nodes.put(id, new Node(id, latitude, longitude));
                }
            }

            /* The next pass finds all the edges in the JSON and appends them to the nodes at both ends of each edge.
            For example, if we have an edge (1,2) with WayID 3, then we store an entry with (node WayID 2, edge WayID 3) with node 1,
            and (node WayID 1, edge WayID 3) with node 2.
             */
            for (Object obj : array) {
                JSONObject jobj = (JSONObject)obj;

                String type = (String)jobj.get("type");

                long currentEdgeID = 0;

                if (type.equals("way")) {
                    /* This is the WayID of the edge */
                    long wayID = (long)jobj.get("id");
                    JSONArray subarray = (JSONArray)jobj.get("nodes");

                    edges.put(wayID, new ArrayList<>());

                    /* For each part of the whole way */
                    for (int i = 0; i < subarray.size() - 1; i++) {
                        /* For the nodes at each end of the edge, put a record pairing the neighbouring node and the edge WayID
                        into the node's Edges map. */
                        long node1 = (long)subarray.get(i);
                        long node2 = (long)subarray.get(i+1);

                        nodes.get(node1).Edges.add(node2);
                        nodes.get(node2).Edges.add(node1);

                        double distance = haversineDistance(nodes.get(node1), nodes.get(node2));

                        edges.get(wayID).add(new Edge(currentEdgeID, wayID, node1, node2, distance));
                        currentEdgeID++;
                    }
                }
            }
        }
        catch(IOException | ParseException e) {
            System.out.println("Error parsing JSON file.");
            e.printStackTrace();
        }

        return nodes;
    }

    /**
     * This function finds the distance between a point and an edge with a node at each end.
     * @param point The pollution point to find the distance from
     * @param node1 One end of the edge
     * @param node2 Other end of the edge
     */
    private static double pointToEdgeDistance(Point point, Node node1, Node node2) {
        /* A - the standalone point (point)
         * B - start point of the line segment (node1)
         * C - end point of the line segment (node2)
         * D - the crossing point between line from A to BC */

        double AB = pythagoras(point.Longitude, point.Latitude, node1.Longitude, node1.Latitude);
        double BC = pythagoras(node1.Longitude, node1.Latitude, node2.Longitude, node2.Latitude);
        double AC = pythagoras(point.Longitude, point.Latitude, node2.Longitude, node2.Latitude);

        /* This is Heron's formula to find the area of a triangle */
        double s = (AB + BC + AC) / 2;
        double area = Math.sqrt(s * (s - AB) * (s - BC) * (s - AC));

        /* area == (BC * AD) / 2
         * BC * AD == 2 * area
         * AD == (2 * area) / BC */
        double AD = (2 * area) / BC;
        return AD;
    }

    /**
     * Finds the pythagorean distance between two points.
     * @param x The x-coordinate of the first point
     * @param y The y-coordinate of the first point
     * @param x2 The x-coordinate of the second point
     * @param y2 The y-coordinate of the second point
     * @return the distance between the points
     */
    private static double pythagoras(double x, double y, double x2, double y2) {
        double deltax = x2 - x;
        double deltay = y2 - y;

        return Math.sqrt(deltax * deltax + deltay * deltay);
    }

    /**
     * Iterates through the huge list of edges and finds the average of all the pollution readings within about 20 metres of each one.
     * @param points The list of pollution data points
     * @param edges The list of map edge lists
     * @param nodes The list of map nodes
     */
    public static void pointToEdge(List<Point> points, Map<Long, List<Edge>> edges, Map<Long, Node> nodes) {
        long numPoints = 0;
        long numEdges = 0;
        long numEdgeLists = 0;

        /* Iterate through every edge */
        for (Long listID : edges.keySet()) {
            for (Edge edge : edges.get(listID)) {
                int counter = 0;
                double cumulative = 0;

                Node node1 = nodes.get(edge.Node1ID);
                Node node2 = nodes.get(edge.Node2ID);

                /* Iterate through every point */
                for (Point point : points) {

                    /* 0.0003 units at latitude 52 is about a 20 metre radius.
                     * We take the mean of all the pollution points in this radius, treating PM2.5 and PM10 as equally weighted. */
                    if (pointToEdgeDistance(point, node1, node2) < 0.0003) {
                        cumulative += point.Pollution2_5 + point.Pollution10;
                        counter++;
                        numPoints++;
                    }
                }

                numEdges++;
                edge.Pollution = cumulative / counter;
            }
            numEdgeLists++;
            System.out.println("Edge lists completed: " + numEdgeLists + "/" + edges.size());
        }
        System.out.println("Average number of points per edge: " + (double)numPoints / numEdges);
    }

    /**
     * The main function is currently used for testing purposes.
     * @param args the command line arguments of the program.
     */
    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();

        List<Point> points = getPointsFromBigCSV("data.csv");

        Map<Long, Node> nodes = new HashMap<>();
        Map<Long, List<Edge>> edges = new HashMap<>();
        readDataFromJSON("map.json", nodes, edges);

        pointToEdge(points, edges, nodes);

        System.out.println("Completed in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");

        Map<Long, Edge> edgeList = new HashMap<>();
        for (long l : edges.keySet()) {
            for (Edge e : edges.get(l)) {
                edgeList.put(e.ID, e);
            }
        }

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("nodes.obj"));
        oos.writeObject(nodes);
        oos.close();

        oos = new ObjectOutputStream(new FileOutputStream("edges.obj"));
        oos.writeObject(edgeList);
        oos.close();
    }
}