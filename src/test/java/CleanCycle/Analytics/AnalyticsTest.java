package CleanCycle.Analytics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

public class AnalyticsTest {
    @Test public void testAnalytics() {
        final Map<Long, Node> nodes = new HashMap<>();
        final Map<Long, Edge> edges = new HashMap<>();
        final List<Point> points = new ArrayList<>();

        /* loadPointsFromDatabase(points); */

        Main.getPointsFromBigCSV("data.csv", points);

        Main.readDataFromJSON("map.json", nodes, edges);

        /* Filter the largest component out of the slightly mangled OSM data */
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

        /* Perform the analysis algorithm to find pollution amounts per edge */
        Main.pointToEdge(points, edges, nodes);

        Assertions.assertTrue(Main.unitTestValidEdges(nodes, edges), "Invalid edges exist");
        Assertions.assertEquals(1, Main.unitTestSingleComponent(nodes, edges), "Unexpected number of components");
    }
}