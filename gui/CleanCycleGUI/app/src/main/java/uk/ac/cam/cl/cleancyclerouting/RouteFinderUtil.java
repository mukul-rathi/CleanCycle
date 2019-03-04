package uk.ac.cam.cl.cleancyclerouting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import CleanCycle.Analytics.Edge;
import CleanCycle.Analytics.Node;

public interface RouteFinderUtil {

    /**
     * Get an input stream from which a Map: Long -> Node can be read.
     *
     * @return The input stream.
     * @throws IOException Thrown if there is a failure to open an input stream.
     */
    InputStream getNodesInputStream() throws IOException;

    /**
     * Get an input stream from which a Map: Long -> Edge can be read.
     *
     * @return The input stream;
     * @throws IOException Thrown when there is a failure to open an input stream.
     */
    InputStream getEdgesInputStream() throws IOException;

    /**
     * Get an output stream to which the Map: Long -> Node can be written for later caching.
     *
     * @return The output stream.
     * @throws IOException Thrown if there is a failure to access the output stream.
     */
    OutputStream getNodesOutputStream() throws IOException;

    /**
     * Get an output stream to which the Map: Long -> Edge can be written for later caching.
     *
     * @return The output stream.
     * @throws IOException Thrown if there is a failure to access the output stream.
     */
    OutputStream getEdgesOutputStream() throws IOException;
}
