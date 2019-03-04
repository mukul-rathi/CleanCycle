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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnalyticsUtils {
    static class SizeComparator implements Comparator<Set<?>> {
        @Override
        public int compare(Set<?> o1, Set<?> o2) {
            return Integer.valueOf(o1.size()).compareTo(o2.size());
        }
    }

    /**
     * This function uses the haversine formula to calculate the great circle
     * distance in metres between two nodes.
     *
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
        double a = Math.sin(deltaphi / 2) * Math.sin(deltaphi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltalambda / 2) * Math.sin(deltalambda / 2);

        /* The value c is the angular distance in radians. */
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        /* Finally return the distance in metres. */
        return r * c;
    }

    /**
     * This function finds the distance between a point and an edge with a node at
     * each end.
     *
     * @param point The pollution point to find the distance from
     * @param node1 One end of the edge
     * @param node2 Other end of the edge
     */
    private static double pointToEdgeDistance(Point point, Node node1, Node node2) {
        /*
         * A - the standalone point (point) B - start point of the line segment (node1)
         * C - end point of the line segment (node2) D - the crossing point between line
         * from A to BC
         */

        double AB = pythagoras(point.Longitude, point.Latitude, node1.Longitude, node1.Latitude);
        double BC = pythagoras(node1.Longitude, node1.Latitude, node2.Longitude, node2.Latitude);
        double AC = pythagoras(point.Longitude, point.Latitude, node2.Longitude, node2.Latitude);

        /* This is Heron's formula to find the area of a triangle */
        double s = (AB + BC + AC) / 2;
        double area = Math.sqrt(s * (s - AB) * (s - BC) * (s - AC));

        /*
         * area == (BC * AD) / 2 BC * AD == 2 * area AD == (2 * area) / BC
         */
        double AD = (2 * area) / BC;
        return AD;
    }

    /**
     * Finds the pythagorean distance between two points.
     *
     * @param x  The x-coordinate of the first point
     * @param y  The y-coordinate of the first point
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
     * Iterates through the huge list of edges and finds the average of all the
     * pollution readings within about 20 metres of each one.
     *
     * @param points The list of pollution data points
     * @param edges  The list of map edge lists
     * @param nodes  The list of map nodes
     */
    public static void pointToEdge(List<Point> points, Map<Long, Edge> edges, Map<Long, Node> nodes) {
        long numPoints = 0;
        long numEdges = 0;

        /* Iterate through every edge */
        for (long key : edges.keySet()) {
            int counter = 0;
            double cumulative = 0;
            Edge edge = edges.get(key);

            Node node1 = nodes.get(edge.Node1ID);
            Node node2 = nodes.get(edge.Node2ID);

            /* Iterate through every point */
            for (Point point : points) {

                /*
                 * 0.0003 units at latitude 52 is about a 20 metre radius. We take the mean of
                 * all the pollution points in this radius, treating PM2.5 and PM10 as equally
                 * weighted.
                 */
                if (pointToEdgeDistance(point, node1, node2) < 0.0003) {
                    cumulative += point.Pollution2_5 + point.Pollution10;
                    counter++;
                    numPoints++;
                }
            }

            numEdges++;
            edge.Pollution = cumulative / counter;
            if (numEdges % 1000 == 0)
                System.out.println("Completed " + numEdges + " edges");
        }
        System.out.println("Average number of points per edge: " + (double) numPoints / numEdges);
    }
}
