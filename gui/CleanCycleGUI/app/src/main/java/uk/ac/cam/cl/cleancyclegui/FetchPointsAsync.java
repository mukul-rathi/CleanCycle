package uk.ac.cam.cl.cleancyclegui;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import CleanCycle.Analytics.Point;

public class FetchPointsAsync extends AsyncTask<Void,Void,List<Point>> {
    // a handler to do something with the fetched points, possibly updating the GUI
    private PointsHandler pointsHandler;

    public FetchPointsAsync(PointsHandler pointsHandler) {
        this.pointsHandler = pointsHandler;
    }

    @Override
    protected List<Point> doInBackground(Void... voids) {
        try {
            Socket socket = new Socket("b96d3eac.ngrok.io", 54334);
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            List<Point> points = (List<Point>) ois.readObject();
            return points;
        } catch (UnknownHostException e) {
            // should never happen
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onPostExecute(List<Point> points) {
        pointsHandler.handlePoints(points);
    }
}
