package uk.ac.cam.cl.cleancyclegui;

import java.io.InputStream;
import java.io.OutputStream;

public interface SavedRoutesUtil {
    /**
     * Action to be performed after the routes have been saved,
     */
    void postSave();

    /**
     * Get the input stream from which the saved routes should be read.
     *
     * @return The input stream from which saved routes should be read.
     */
    InputStream getInputStream();

    /**
     * Get the output stream to which saved routes should be written.
     *
     * @return The output stream to which saved routes should be written.
     */
    OutputStream getOutputStream();

    /**
     * Pass a message on to the user (i.e. through the UI).
     *
     * @param msg The message to be displayed.
     */
    void updateStatus(String msg);
}
