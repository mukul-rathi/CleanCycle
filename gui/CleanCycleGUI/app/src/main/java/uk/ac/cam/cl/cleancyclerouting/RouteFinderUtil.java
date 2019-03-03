package uk.ac.cam.cl.cleancyclerouting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import CleanCycle.Analytics.Edge;
import CleanCycle.Analytics.Node;

public interface RouteFinderUtil {
    InputStream getNodesInputStream() throws IOException;
    InputStream getEdgesInputStream() throws IOException;
    OutputStream getNodesOutputStream() throws IOException;
    OutputStream getEdgesOutputStream() throws IOException;
}
