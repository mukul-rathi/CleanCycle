package uk.ac.cam.cl.cleancyclegui;

import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

public class Util {
    // https://en.wikipedia.org/wiki/Great-circle_distance
    public static double distance(LatLng one, LatLng two) {
        double r = 6371000;
        double phi1 = Math.toRadians(one.latitude);
        double phi2 = Math.toRadians(two.latitude);
        double lambda1 = Math.toRadians(one.longitude);
        double lambda2 = Math.toRadians(two.longitude);
        double deltaSigma = Math.acos(Math.sin(phi1)*Math.sin(phi2) + Math.cos(phi1) * Math.cos(phi2) * Math.cos(lambda1 - lambda2));
        return r * deltaSigma;
    }

    public static double dot(double[] xs, double[] ys) {
        double tot = 0.0;
        for (int i=0; i<xs.length; ++i) tot += xs[i] * ys[i];
        return tot;
    }

    public static double magnitude(double... xs) {
        double tot = 0.0;
        for (double x : xs) tot += x;
        return Math.sqrt(tot);
    }
}

