package uk.ac.cam.cl.cleancyclegui.helper;

import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.cl.cleancyclegui.R;

public class PlaceSearch {
    /**
     * Get content of search result as a String (the string will contain JSON)
     * @param params The parameters to be passed as a string
     * @return The string containing the content or null on failure
     */
    private static String search(String params) {
        params = params.replace(' ', '+');
        try (InputStream is = new URL(R.string.places_api_base + params).openStream()) {
            StringBuilder sb = new StringBuilder();
            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer, "UTF-8");
            String theString = writer.toString();
            return theString;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Peform search and returns a JSON array of results.
     * @param params The parameters to be passed as a string.
     * @return The JSONArray or null on failure.
     */
    private static JSONArray searchJSON(String params) {
        String res = search(params);
        if (res != null) {
            try {
                return new JSONArray(res);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Get a list of places relevant to the search term.
     * @param name The name of the place.
     * @return The list of results or an empty list on failure.
     */
    public static List<Place> getPlaces(String name) {
        List<Place> places = new ArrayList<>();
        JSONArray placesJson = searchJSON("name=" + name);
        if (placesJson == null) return new ArrayList<>();
        else {
            int len = placesJson.length();
            for (int i=0; i<len; ++i) {
                JSONObject placeObj;
                double lat;
                double lon;
                try {
                    placeObj = placesJson.getJSONObject(i);
                    if (placeObj == null) continue;
                    String placeName = placeObj.getString("display_name");
                    String latString = placeObj.getString("lat");
                    String lonString = placeObj.getString("lon");
                    lat = Double.parseDouble(latString);
                    lon = Double.parseDouble(lonString);
                    places.add(new Place(placeName, new LatLng(lat, lon)));
                } catch (JSONException e) {
                    e.printStackTrace();
                    placeObj = null;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    placeObj = null;
                }
            }
        }
        return places;
    }
}
