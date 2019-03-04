package CleanCycle.Analytics;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import static CleanCycle.Analytics.AnalyticsUtils.haversineDistance;

/**
 * Class for reading and writing data from files. Not all of these functions
 * are used in the final product, but readDataFromJSON() is.
 */
public class FileUtils {
    /**
     * Read in the large, uncondensed CSV file and fills up the list of pollution
     * data points.
     *
     * @param filename the name of the CSV file.
     */
    public static void getPointsFromBigCSV(String filename, List<Point> points) {

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {

            /* Read in the first line without doing anything as it's just a key */
            String line;
            reader.readLine();

            /*
             * Read in the line as a CSV record and extract the relevant data into a new
             * Point object
             */
            while ((line = reader.readLine()) != null) {
                String[] record = line.split(",");
                points.add(new Point(Double.parseDouble(record[15]), Double.parseDouble(record[16]),
                        Double.parseDouble(record[19]), Double.parseDouble(record[20])));
            }

        } catch (IOException e) {
            System.out.println("There was an error processing the CSV file.");
            e.printStackTrace();
        }
    }

    /**
     * Write out the current list of points in CSV format.
     *
     * @param filename the name of the file to write to.
     * @param points   the list of points.
     */
    public static void writePointsToCSV(String filename, List<Point> points) {
        /*
         * Because the original CSV file has obvious precision errors, we round the data
         * to the intended six decimal places.
         */
        DecimalFormat df = new DecimalFormat("#0.000000");

        try {
            /*
             * Write out the first line as a key, then each point as a separate CSV record.
             */
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            writer.println("KEY: Latitude, Longitude, PM10, PM2.5");
            for (Point p : points) {
                writer.println(df.format(p.Latitude) + "," + df.format(p.Longitude) + "," + p.Pollution10 + ","
                        + p.Pollution2_5);
            }

        } catch (IOException e) {
            System.out.println("There was an error writing the new CSV file.");
            e.printStackTrace();
        }
    }

    /**
     * Read in the OSM data from a JSON file. The two argument maps are filled with
     * the results.
     *
     * @param filename the name of the JSON file to be parsed.
     * @param nodes    the map of Node WayID -> Node to be filled.
     * @param edges    the map of Edge WayID -> List of edges with that WayID to be
     *                 filled.
     */
    public static void readDataFromJSON(String filename, Map<Long, Node> nodes, Map<Long, Edge> edges) {

        try {
            /* First we initialize the JSON objects to be used. */

            JSONParser parser = new JSONParser();
            JSONObject container = (JSONObject) parser.parse(new FileReader(filename));
            JSONArray array = (JSONArray) container.get("elements");

            /*
             * The first pass over the array finds all the nodes in the JSON and stores
             * their latitude and longitude.
             */
            for (Object obj : array) {
                JSONObject jobj = (JSONObject) obj;

                String type = (String) jobj.get("type");

                if (type.equals("node")) {
                    long id = (long) jobj.get("id");
                    double latitude = (double) jobj.get("lat");
                    double longitude = (double) jobj.get("lon");

                    nodes.put(id, new Node(id, latitude, longitude));
                }
            }

            /*
             * The next pass finds all the edges in the JSON and appends them to the nodes
             * at both ends of each edge. For example, if we have an edge (1,2) with WayID
             * 3, then we store an entry with (node WayID 2, edge WayID 3) with node 1, and
             * (node WayID 1, edge WayID 3) with node 2.
             */
            long currentEdgeID = 0;

            for (Object obj : array) {
                JSONObject jobj = (JSONObject) obj;

                String type = (String) jobj.get("type");

                if (type.equals("way")) {
                    /* This is the WayID of the edge */
                    JSONArray subarray = (JSONArray) jobj.get("nodes");

                    /* For each part of the whole way */
                    for (int i = 0; i < subarray.size() - 1; i++) {
                        /*
                         * For the nodes at each end of the edge, put the edge ID into the edges map on
                         * the node.
                         */
                        long node1 = (long) subarray.get(i);
                        long node2 = (long) subarray.get(i + 1);

                        nodes.get(node1).Edges.add(currentEdgeID);
                        nodes.get(node2).Edges.add(currentEdgeID);

                        double distance = haversineDistance(nodes.get(node1), nodes.get(node2));

                        edges.put(currentEdgeID, new Edge(currentEdgeID, node1, node2, distance));

                        currentEdgeID++;
                    }
                }
            }
        }

        catch (IOException | ParseException e) {
            System.out.println("Error parsing JSON file.");
            e.printStackTrace();
        }
    }
}
