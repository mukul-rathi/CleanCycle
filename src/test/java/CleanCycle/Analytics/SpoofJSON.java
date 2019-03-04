package CleanCycle.Analytics;

import java.io.IOException;
import java.io.PrintWriter;

public class SpoofJSON {
    public static void writeJSONMap() {
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
}
