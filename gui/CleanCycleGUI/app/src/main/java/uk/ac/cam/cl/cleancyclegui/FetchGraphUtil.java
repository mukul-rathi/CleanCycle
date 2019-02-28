package uk.ac.cam.cl.cleancyclegui;

import java.io.InputStream;

public interface FetchGraphUtil {
    void updateLabel(String msg);
    InputStream getEdgesStream();
    InputStream getNodesStream();
}
