package uk.ac.cam.cl.cleancyclegui;

import java.io.InputStream;
import java.io.OutputStream;

public interface SavedRoutesUtil {
    void postSave();
    InputStream getInputStream();
    OutputStream getOutputStream();
    void updateStatus(String msg);
}
