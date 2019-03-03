package uk.ac.cam.cl.cleancyclegui;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

import CleanCycle.Analytics.EdgeComplete;

public class LoadSavedRoutesAsync extends AsyncTask<Void,Void,Void> {
    private SavedRoutes savedRoutes;
    private SavedRoutesUtil savedRoutesUtil;

    public LoadSavedRoutesAsync(SavedRoutes savedRoutes, SavedRoutesUtil savedRoutesUtil) {
        this.savedRoutes = savedRoutes;
        this.savedRoutesUtil = savedRoutesUtil;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Map<String,List<EdgeComplete>> routes = getSavedRoutes();
        if (routes != null) {
            for (Map.Entry<String,List<EdgeComplete>> entry : routes.entrySet()) {
                savedRoutes.addRoute(entry.getKey(), entry.getValue());
            }
        }
        return null;
    }

    private Map<String,List<EdgeComplete>> getSavedRoutes() {
        try {
            InputStream fis = savedRoutesUtil.getInputStream();
            if (fis == null) return null;
            ObjectInputStream objStream = new ObjectInputStream(fis);
            Object obj = objStream.readObject();

            fis.close();

            return (Map<String,List<EdgeComplete>>) obj;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
        }
        return null;
    }
}
