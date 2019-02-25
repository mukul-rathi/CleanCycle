package uk.ac.cam.cl.cleancyclegui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import uk.ac.cam.cl.cleancyclegraph.Edge;
import uk.ac.cam.cl.cleancyclegraph.EdgeComplete;
import uk.ac.cam.cl.cleancyclegraph.Node;
import uk.ac.cam.cl.cleancyclerouting.Algorithm;
import uk.ac.cam.cl.cleancyclerouting.GraphNotConnectedException;
import uk.ac.cam.cl.cleancyclerouting.NotSetUpException;
import uk.ac.cam.cl.cleancyclerouting.RouteFinder;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    class FetchGraphAsync extends AsyncTask<LatLng, Void, List<Node>> {
        @Override
        protected List<Node> doInBackground(LatLng... points) {
            if (points.length != 2) return null;
            try {
                updateDistanceLabel("I: finding route");
                if (routeFinder == null) {
                    InputStream nodes = getResources().openRawResource(R.raw.nodes_updated);
                    InputStream edges = getResources().openRawResource(R.raw.edges_updated);
                    routeFinder = new RouteFinder(nodes, edges);
                }
                //return routeFinder.findRouteEdges(Algorithm.POLLUTION_ONLY, points[0].latitude, points[0].longitude, points[1].latitude, points[1].longitude);
                return routeFinder.findRoute(Algorithm.POLLUTION_ONLY, points[0].latitude, points[0].longitude, points[1].latitude, points[1].longitude);

            } catch (NotSetUpException e) {
                e.printStackTrace();
                updateDistanceLabel("E: Graph not set up.");
            } catch (GraphNotConnectedException e) {
                e.printStackTrace();
                updateDistanceLabel("E: Graph not connected.");
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Node> route) {
            if (route == null) {
                return;
            }

            LatLng[] points = new LatLng[route.size()];
            for (int i=0; i<points.length; ++i) {
                Node n = route.get(i);
                points[i] = new LatLng(n.Latitude, n.Longitude);
            }
            updateDistanceLabel("I: plotting route");
            plotRoute(points);
            updateDistanceLabel("I: route plotted");
            System.out.println(route.stream().map(x -> String.format(Locale.ENGLISH, "%f,%f", x.Latitude, x.Longitude)).collect(Collectors.toList()));
        }
    }

    private List<Polyline> polylines = new ArrayList<>();

    private LatLng defaultLocation = new LatLng(52.211105, 0.091527);

    // fragment which contains the GoogleMap
    private SupportMapFragment mapFragment;

    // dashboard fragment
    private Fragment dashboardFragment;

    // notification fragment
    private Fragment notificationsFragment;

    // the GoogleMap on which routes are plotted
    private GoogleMap map;

    // the bottom navigation bar
    private BottomNavigationView navigationView;

    private TextView distanceLabel;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private AutocompleteSupportFragment searchAutoComplete;

    private RouteFinder routeFinder = null;

    private PlacesClient placesClient;

    private final int locationRequestInt = 2;


    enum Pages {PAGE_MAP, PAGE_DASHBOARD, PAGE_NOTIFICATIONS};

    /**
     * Show the appropriate page. Will require showing.hiding multiple components.
     *
     * @param page The page to be shown.
     */
    private void showPage(Pages page) {
        View searchView = searchAutoComplete.getView();
        switch (page) {
            // map page
            case PAGE_MAP:
                showFragment(mapFragment);
                hideFragment(dashboardFragment);
                hideFragment(notificationsFragment);
                distanceLabel.setVisibility(View.VISIBLE);
                if (searchView != null) searchView.setVisibility(View.VISIBLE);
                break;

                // dashboard page
            case PAGE_DASHBOARD:
                hideFragment(mapFragment);
                showFragment(dashboardFragment);
                hideFragment(notificationsFragment);
                distanceLabel.setVisibility(View.GONE);
                if (searchView != null) searchView.setVisibility(View.GONE);
                break;

            // notifications page
            case PAGE_NOTIFICATIONS:
                hideFragment(mapFragment);
                hideFragment(dashboardFragment);
                showFragment(notificationsFragment);
                distanceLabel.setVisibility(View.GONE);
                if (searchView != null) searchView.setVisibility(View.GONE);
                break;
        }
    }


    /**
     * Hide the provided fragment using a fragment transaction
     *
     * @param fragment The fragment to be hidden.
     */
    private void hideFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.hide(fragment);
        ft.commit();
    }

    /**
     * Show the provided fragment using a fragment transation.
     *
     * @param fragment The fragment to be shown.
     */
    private void showFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.show(fragment);
        ft.commit();
    }

    /**
     * A listener to deal with button presses from the bottom navigation view.
     */
    private BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

        /**
         * Show the appropriate page depending on which navigation button was pressed.
         *
         * @param item The menu item selected.
         * @return A boolean describing whether the selected item has been displayed.
         */
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    showPage(Pages.PAGE_MAP);
                    return true;

                case R.id.navigation_dashboard:
                    showPage(Pages.PAGE_DASHBOARD);
                    return true;

                case R.id.navigation_notifications:
                    showPage(Pages.PAGE_NOTIFICATIONS);
                    return true;

                default:
                    return false;
            }
        }
    };

    /**
     * A shorthand way to request the location permission.
     */
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, locationRequestInt);
    }

    /**
     * Check whether we have the appropriate location permissions. If we do, then return true.
     * Otherwise, request the permissions and return false (the request will call a callback function
     * when the result is returned so until that happens, we do not have the appropriate permission).
     *
     * @return Value describing whether we have the permission right now.
     */
    private boolean hasLocationPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestLocationPermission();
            return false;
        }
    }

    /**
     * This is an exemplar function demonstrating how to make use of the getLastLocation function.
     * Once I have Vlad's code I can use a similar function to actually plot routes.
     *
     * @param goal The goal destination.
     */
    // suppression added because i check the permission with the hasLocationPermission() function
    @SuppressLint("MissingPermission")
    private void plotFromCurrentLocation(LatLng goal) {
        if (hasLocationPermission()) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    defaultLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    panToLocation(map, defaultLocation);
                }
                new FetchGraphAsync().execute(defaultLocation, goal);
                /*
                List<Node> route;
                try {
                    // get a List<Node> of points on the route from defaultLocation to goal
                    route = routeFinder.findRoute(Algorithm.POLLUTION_ONLY, defaultLocation.latitude, defaultLocation.longitude, goal.latitude, goal.longitude);

                    // convert to an array
                    LatLng[] points = new LatLng[route.size()];
                    for (int i=0; i<points.length; ++i) {
                        Node n = route.get(i);
                        points[i] = new LatLng(n.Latitude, n.Longitude);
                    }

                    // calculate the distance of the route
                    double dist = 0.0;
                    for (int i=0; i<points.length-1; ++i) {
                        dist += Util.distance(points[i], points[i+1]);
                    }

                    // plot teh route
                    plotRoute(points);

                    // display the route distance
                    updateDistanceLabel(dist / 1000.0);
                } catch (NotSetUpException e) {
                    // assume connection failure
                    e.printStackTrace();
                    updateDistanceLabel("E: failed to setup graph.");
                } catch (GraphNotConnectedException e) {
                    // impossible to get to the destination
                    e.printStackTrace();
                    updateDistanceLabel("E: Failed to find a route.");
                }*/
            });
        } else {
            plotRoute(defaultLocation, goal);
        }
    }

    // suppression added because i check the permission with the hasLocationPermission() function
    @SuppressLint("MissingPermission")
    private void panToCurrentLocation() {
        if (hasLocationPermission()) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                defaultLocation = new LatLng(location.getLatitude(), location.getLongitude());
                panToLocation(map, defaultLocation);
                updateBoundsPreference(searchAutoComplete, defaultLocation);
            });
        } else panToLocation(map, defaultLocation);
    }

    /**
     * Pan the camera to the location specified.
     *
     * @param loc The location to pan to.
     */
    private void panToLocation(GoogleMap gMap, LatLng loc) {
        CameraUpdate centre = CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(loc, 15));
        if (gMap != null) gMap.animateCamera(centre);
    }

    private void updateBoundsPreference(AutocompleteSupportFragment autocompleteSupportFragment, LatLng location) {
        if (location == null || autocompleteSupportFragment == null) return;

        double theta = Math.toRadians(location.longitude);
        double r = 6371000; // radius of Earth in metres
        double latAdjust = 900000.0/(Math.PI * r);
        double lonAdjust = latAdjust / Math.cos(theta);
        autocompleteSupportFragment.setLocationBias(RectangularBounds.newInstance(
                new LatLng(location.latitude - latAdjust, location.longitude - lonAdjust),
                new LatLng(location.latitude + latAdjust, location.longitude + lonAdjust)
        ));
    }

    /**
     * Update the distance label with a specified distance.
     *
     * @param distance The distance.
     */
    private void updateDistanceLabel(double distance) {
        if (distanceLabel != null) {
            distanceLabel.setText(String.format(Locale.ENGLISH, "Distance to destination: %.2fkm", distance));
        }
    }

    /**
     * Directly update the distance label with text (mainly used for error messages).
     *
     * @param text The text to be displayed.
     */
    private void updateDistanceLabel(String text) {
        if (distanceLabel != null) {
            distanceLabel.setText(text);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // the route finder will be used to get a List<Node> representing the route between the
        // current location and the destination
        //routeFinder = new RouteFinder();

        // initialise the Google Places API
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);

        // set the content view to the main activity
        setContentView(R.layout.activity_main);

        // START initialise class variables
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        dashboardFragment = getSupportFragmentManager().findFragmentById(R.id.dashboard);
        notificationsFragment = getSupportFragmentManager().findFragmentById(R.id.notifications);

        navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        searchAutoComplete = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        if (searchAutoComplete != null) {
            View searchView = searchAutoComplete.getView();
            if (searchView != null) searchAutoComplete.getView().setBackgroundColor(Color.WHITE);
            searchAutoComplete.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            searchAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                /**
                 * The operation to be performed when a user selects a place using the autocomplete search bar.
                 *
                 * @param place The place selected by the user.
                 */
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    //plotRoute(getCurrentLocation(), place.getLatLng());
                    plotFromCurrentLocation(place.getLatLng());
                }

                /**
                 * The operation to be performed if there is an error.
                 *
                 * @param status The error status.
                 */
                @Override
                public void onError(@NonNull Status status) {
                    int code = status.getStatusCode();
                    String msg = status.getStatusMessage();
                    updateDistanceLabel(String.format(Locale.ENGLISH, "E %d: %s", code, msg));
                }
            });
        }

        /*
         * Check that we have the appropriate location permissions and, if we do, obtain a location
         * provider.
         */
        if (hasLocationPermission()) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        } else {
            fusedLocationProviderClient = null;
        }

        /*
         * Initialise the distance label.
         */
        distanceLabel = findViewById(R.id.distance_label);
        if (distanceLabel != null) {
            distanceLabel.setBackgroundColor(0xFFFFFFFF);
            distanceLabel.setPadding(2,2,2,2);
        }

        //updateDistanceLabel(defaultLocation, defaultLocation);

        // END initialise class variables

        // show home fragment and hide others
        showFragment(mapFragment);
        hideFragment(dashboardFragment);
        hideFragment(notificationsFragment);
    }

    private void clearMap() {
        while (!polylines.isEmpty()) {
            Polyline pl = polylines.get(0);
            pl.remove();
            polylines.remove(0);
        }
    }

    public void plotRoute(List<EdgeComplete> edges) {
        clearMap();

        double minPollution = Double.MAX_VALUE;
        double maxPollution = Double.MIN_VALUE;

        for (EdgeComplete edge : edges) {
            Polyline line = map.addPolyline(new PolylineOptions().add(
                new LatLng(edge.node1.Latitude, edge.node1.Longitude), new LatLng(edge.node2.Latitude, edge.node2.Longitude)
            ).width(5).color(
                    //getPollutionColour(edge.Pollution)
                    Color.BLUE
            ));
            polylines.add(line);
            minPollution = minPollution < edge.Pollution ? minPollution : edge.Pollution;
            maxPollution = maxPollution > edge.Pollution ? maxPollution : edge.Pollution;
        }

        System.err.println(String.format(Locale.ENGLISH, "%f < pollution < %f", minPollution, maxPollution));
    }

    /**
     * Plot a route on the map, given a series of points
     *
     * @param points The array of points to plot
     */
    public void plotRoute(LatLng... points) {
        clearMap();
        Polyline line = map.addPolyline(
                new PolylineOptions()
                    .add(points)
                .width(5)
                .color(0xFF005331)
        );
        polylines.add(line);
    }

    /**
     * TODO: normalise pollution input based on maximum and minimum values (i.e. find maximum and minimum pollution values).
     * TODO: think of a better scaling than linear as it has the effect that as pollution gets worse, areas with previously red pollution start fading to green
     *       maybe just use thresholding?
     *
     * @param pollution The amount of pollution
     * @return The rgba colour represented as an integer
     */
    private int getPollutionColour(double pollution) {
        double maxPollution = 300;
        double minPollution = 90;

        double normalisedPollution = (pollution - minPollution) / (maxPollution - minPollution);

        if (normalisedPollution < 0.5) {
            float red = 1.0f;
            float green = 2 * (float) normalisedPollution;
            green = green > 1.0f ? 1.0f : green;
            green = green < 0.0f ? 0.0f : green;
            float blue = 0f;
            return Color.valueOf(red, green, blue).toArgb();
        } else {
            float red = 1f - 2 * ((float) normalisedPollution - 0.5f);
            red = red > 1.0f ? 1.0f : red;
            red = red < 0.0f ? 0.0f : red;
            float green = 1f;
            float blue = 0f;
            return Color.valueOf(red, green, blue).toArgb();
        }
    }

    /*
    public void plotEdge(Edge edge) {
        Node start = nodes.get(edge.Node1ID);
        Node end = nodes.get(edge.Node2ID);
        if (start == null || end == null) return;
        Polyline line = map.addPolyline(
                new PolylineOptions()
                        .add(new LatLng(start.Latitude, start.Longitude), new LatLng(end.Latitude, end.Longitude))
                        .width(5)
                        .color(getPollutionColour(edge.Pollution))
        );
        polylines.add(line);
    }*/

    /*
    public void plotWay(List<Edge> way) {
        for (Edge edge : way) plotEdge(edge);
    }*/

    /**
     * Called in response to a call to mapFragment.getMapAsync()
     *
     * @param googleMap The google map received.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        if (hasLocationPermission()) map.setMyLocationEnabled(true);
        else map.setMyLocationEnabled(false);

        panToCurrentLocation();
    }

    /**
     * Function called as a callback for when some permission has been requested.
     *
     * @param requestCode The app-specified request code.
     * @param permissions The permissions.
     * @param grantResults The grant results.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case locationRequestInt:
                // have only requested the location permission so this array will only contain the
                // results for the location permission thus only need to check the first item
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted, use permission
                    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                    if (map != null) {
                        map.setMyLocationEnabled(true);
                    }
                } else {
                    // disable permission functionality
                    if (map != null) {
                        map.setMyLocationEnabled(false);
                    }
                }
                break;
        }
    }
}
