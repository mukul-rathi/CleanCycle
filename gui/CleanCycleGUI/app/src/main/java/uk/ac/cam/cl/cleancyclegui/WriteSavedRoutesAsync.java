package uk.ac.cam.cl.cleancyclegui;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * An asynchronous task to write the saved routes to a file.
 */
public class WriteSavedRoutesAsync extends AsyncTask<Void,Void,Void> {
    private SavedRoutes savedRoutes;
    private SavedRoutesUtil savedRoutesUtil;

    public WriteSavedRoutesAsync(SavedRoutes savedRoutes, SavedRoutesUtil savedRoutesUtil) {
        this.savedRoutes = savedRoutes;
        this.savedRoutesUtil = savedRoutesUtil;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (!saveRoutes()) savedRoutesUtil.updateStatus("E: failed to save routes");
        else savedRoutesUtil.updateStatus("I: routes saved");
        return null;
    }

    private boolean saveRoutes() {
        try {
            OutputStream fos = savedRoutesUtil.getOutputStream();
            if (fos == null) return false;
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(savedRoutes.getRoutes());
            fos.flush();
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onPostExecute(Void v) {
        savedRoutesUtil.postSave();
    }
}
