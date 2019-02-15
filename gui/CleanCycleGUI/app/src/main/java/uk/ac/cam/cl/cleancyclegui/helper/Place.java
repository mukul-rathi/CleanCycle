package uk.ac.cam.cl.cleancyclegui.helper;

import com.google.android.gms.maps.model.LatLng;

/**
 * A class to store information about a place.
 */
public class Place {
    private String name;
    private LatLng pos;

    public Place(String name, LatLng pos) {
        this.name = name;
    }

    public LatLng getPos() {
        return pos;
    }

    public String getName() {
        return name;
    }
}
