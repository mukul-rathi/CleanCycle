package uk.ac.cam.cl.cleancyclegui;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.google.android.gms.common.api.Status;
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
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.cam.cl.cleancyclegraph.Edge;
import uk.ac.cam.cl.cleancyclegraph.Node;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    private List<Polyline> polylines = new ArrayList<>();

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

    // this activity (for use in listeners etc.)
    private MainActivity main;

    private AutocompleteSupportFragment searchAutoComplete;

    private PlacesClient placesClient;
    Map<Long, Node> nodes = new HashMap<>();
    Map<Long, List<Edge>> edges = new HashMap<>();


    /**
     * Hide the provided fragment using a fragment transaction
     * @param fragment The fragment to be hidden.
     */
    private void hideFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.hide(fragment);
        ft.commit();
    }

    /**
     * Show the provided fragment using a fragment transation.
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
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    main.showFragment(mapFragment);
                    main.hideFragment(dashboardFragment);
                    main.hideFragment(notificationsFragment);
                    return true;

                case R.id.navigation_dashboard:
                    main.hideFragment(mapFragment);
                    main.showFragment(dashboardFragment);
                    main.hideFragment(notificationsFragment);
                    return true;

                case R.id.navigation_notifications:
                    main.hideFragment(mapFragment);
                    main.hideFragment(dashboardFragment);
                    main.showFragment(notificationsFragment);
                    return true;

                default:
                    return false;
            }
        }
    };

    private LatLng getCurrentLocation() {
        return new LatLng(52.211472, 0.10381);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);

        setContentView(R.layout.activity_main);

        // START initialise class variables
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        dashboardFragment = getSupportFragmentManager().findFragmentById(R.id.dashboard);
        notificationsFragment = getSupportFragmentManager().findFragmentById(R.id.notifications);

        navigationView = (BottomNavigationView) findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        searchAutoComplete = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        searchAutoComplete.getView().setBackgroundColor(Color.WHITE);
        searchAutoComplete.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        searchAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                plotRoute(getCurrentLocation(), place.getLatLng());
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });

        main = this;
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

    /**
     * Plot a route on the map, given a series of points
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

    public void plotRoute(List<Long> nodeIds) {
        LatLng[] points = new LatLng[nodeIds.size()];
        for (int i=0; i<nodeIds.size(); ++i) {
            Node node = nodes.get(nodeIds.get(i));
            points[i] = new LatLng(node.Latitude, node.Longitude);
        }
        plotRoute(points);
    }

    /**
     * TODO: normalise pollution input based on maximum and minimum values (i.e. find maximum and minimum pollution values).
     * TODO: think of a better scaling than linear as it has the effect that as pollution gets worse, areas with previously red pollution start fading to green
     *       maybe just use thresholding?
     * @param pollution The amount of pollution
     * @return The rgba colour represented as an integer
     */
    private int getPollutionColour(double pollution) {
        double maxPollution = 1.0;
        double minPollution = 0.0;

        double normalisedPollution = (pollution - minPollution) / (maxPollution - minPollution);

        if (normalisedPollution < 0.5) {
            float red = 1.0f;
            float green = 2 * (float) normalisedPollution;
            float blue = 0f;
            return Color.valueOf(red, green, blue).toArgb();
        } else {
            float red = 1f - 2 * ((float) normalisedPollution - 0.5f);
            float green = 1f;
            float blue = 0f;
            return Color.valueOf(red, green, blue).toArgb();
        }
    }

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
    }

    public void plotWay(List<Edge> way) {
        for (Edge edge : way) plotEdge(edge);
    }

    /**
     * Called in response to a call to mapFragment.getMapAsync()
     * @param googleMap The google map received.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        LatLng[] points = {
                new LatLng(52.212999 ,0.103973),
                new LatLng(52.213307 ,0.104217),
                new LatLng(52.213942 ,0.101882),
                new LatLng(52.213996 ,0.101585),
                new LatLng(52.214159 ,0.101145),
                new LatLng(52.215133,0.097625 ),
                new LatLng(52.215220 ,0.097727),
                new LatLng(52.215484 ,0.096807),
                new LatLng(52.215432 ,0.096644),
                new LatLng(52.215871 ,0.095333),
                new LatLng(52.214515 ,0.094014),
                new LatLng(52.214431 ,0.093070),
                new LatLng(52.212603 ,0.092226),
                new LatLng(52.212618 ,0.091671),
                new LatLng(52.211163 ,0.091127)
        };

        CameraUpdate centre = CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(new LatLng(52.211472, 0.10381), 15));
        map.animateCamera(centre);
        plotRoute(points);
    }
}
