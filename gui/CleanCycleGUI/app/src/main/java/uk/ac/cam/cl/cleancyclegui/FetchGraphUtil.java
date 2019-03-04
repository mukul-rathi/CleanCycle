package uk.ac.cam.cl.cleancyclegui;

import java.io.InputStream;

/**
 * Utility class to allow functionality to be passed to an instance of FetchGraphAsync.
 */
public interface FetchGraphUtil {
    /**
     * Cannot pass a TextView down, so need an anonymous function to allow displaying status
     * messages to the user.
     *
     * @param msg The message to be displayed.
     */
    void updateLabel(String msg);

    /**
     * Cannot pass contexts down, so need an anonymous function to access resources in MainActivity
     *
     * @return The input stream used to access the nodes.
     */
    InputStream getEdgesStream();

    /**
     * Cannot pass contexts down, so need an anonymous function to access resources in MainActivity
     * 
     * @return The input stream used to access the edges.
     */
    InputStream getNodesStream();
}
