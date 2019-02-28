package uk.ac.cam.cl.cleancyclegui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.cam.cl.cleancyclegraph.Edge;
import uk.ac.cam.cl.cleancyclegraph.EdgeComplete;
import uk.ac.cam.cl.cleancyclegraph.MapInfoContainer;
import uk.ac.cam.cl.cleancyclegraph.Node;
import uk.ac.cam.cl.cleancyclegraph.Point;
import uk.ac.cam.cl.cleancyclerouting.Algorithm;
import uk.ac.cam.cl.cleancyclerouting.GraphNotConnectedException;
import uk.ac.cam.cl.cleancyclerouting.NotSetUpException;
import uk.ac.cam.cl.cleancyclerouting.RouteFinder;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {


    private List<Polyline> polylines = new ArrayList<>();
    private List<Polygon> polygons = new ArrayList<>();

    private LatLng defaultLocation = new LatLng(52.211105, 0.091527);

    // fragment which contains the GoogleMap
    private SupportMapFragment mapFragment;

    // settings fragment
    private Fragment settingsFragment;

    // notification fragment
    private Fragment notificationsFragment;

    // the GoogleMap on which routes are plotted
    private GoogleMap map;

    // the bottom navigation bar
    private BottomNavigationView navigationView;

    private TextView distanceLabel;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private AutocompleteSupportFragment searchAutoComplete;
    private final RouteFinderContainer routeFinderContainer = new RouteFinderContainer(null);
    private PlacesClient placesClient;
    private final AlgorithmContainer algorithmContainer = new AlgorithmContainer(Algorithm.POLLUTION_ONLY);
    private RadioGroup algorithmSelectGroup;
    private final int locationRequestInt = 2;
    private RecyclerView savedRoutesListView;
    private MapInfoContainer mapInfoContainer = new MapInfoContainer();
    private final RouteContainer currentRoute = new RouteContainer(null);
    private SavedRoutes savedRoutes = new SavedRoutes();
    private Button saveRouteButton;
    private RecyclerView.Adapter savedRoutesAdapter;
    private RecyclerView.LayoutManager savedRoutesLayoutManager;

    private MainActivity main = this;

    private boolean showSaveRouteButton = false;


    enum Pages {PAGE_MAP, PAGE_SETTINGS, PAGE_NOTIFICATIONS};

    class WriteSavedRoutesAsync extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (!saveRoutes()) updateDistanceLabel("E: failed to save routes");
            else updateDistanceLabel("I: routes saved");
            return null;
        }

        private boolean saveRoutes() {
            try {
                FileOutputStream fos = getApplicationContext().openFileOutput("saved_routes.obj", Context.MODE_PRIVATE);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(savedRoutes.getRoutes());
                fos.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public void onPostExecute(Void v) {
            savedRoutesAdapter.notifyDataSetChanged();
            System.out.println(savedRoutes.getRoutes());
        }
    }

    class LoadSavedRoutesAsync extends AsyncTask<Void,Void,Void> {
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
                FileInputStream fis = getApplicationContext().openFileInput("saved_routes.obj");
                ObjectInputStream objStream = new ObjectInputStream(fis);
                Object obj = objStream.readObject();

                fis.close();

                return (Map<String,List<EdgeComplete>>) obj;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    /**
     * An asynchronous task used to draw a heat map visualising the pollution data
     */
    class DrawHeatMapAsync extends AsyncTask<Set<Point>,Void,Void> {
        private void drawHeatMap(Set<Point> points) {
            //TileOverlay tileOverlay = gMap.addTileOverlay();
            Projection projection = map.getProjection();
            VisibleRegion visibleRegion = projection.getVisibleRegion();
            LatLng southwest = visibleRegion.latLngBounds.southwest;
            LatLng northeast = visibleRegion.latLngBounds.northeast;

            double[][] pollution = new double[100][100];
            int[][] count = new int[100][100];
            double unitX = (northeast.longitude - southwest.longitude) / 100;
            double unitY = (northeast.latitude - southwest.latitude) / 100;
            double yAxis = southwest.longitude;
            double xAxis = southwest.latitude;


            for (Point point : points) {
                int x = (int) Math.floor(((yAxis - point.Longitude) / unitX) % 100);
                int y = (int) Math.floor(((xAxis - point.Longitude) / unitY) % 100);
                pollution[y][x] += point.Pollution10;
                ++count[y][x];
            }

            for (int y=0; y<100; ++y) {
                for (int x=0; x<100; ++x) {
                    pollution[y][x] /= count[y][x];
                }
            }

            for (int y=0; y<100; ++y) {
                for (int x=0; x<100; ++x) {
                    if (count[y][x] > 0) {
                        Polygon p = map.addPolygon(new PolygonOptions().add(
                                new LatLng(southwest.latitude + y * unitY, southwest.longitude + x * unitX),
                                new LatLng(southwest.latitude + y * unitY + unitY, southwest.longitude + x * unitX + unitX)

                        ).fillColor(getPollutionColour(pollution[y][x], mapInfoContainer,0.2f)));
                        polygons.add(p);
                    }
                }
            }
        }

        @Override
        protected Void doInBackground(Set<Point>... sets) {
            Set<Point> S = new HashSet<>();
            for (Set<Point> s : sets) S.addAll(s);
            drawHeatMap(S);
            return null;
        }
    }


    /**
     * An asynchronous task to fetch the open street maps graph and plot a route etc
     */
    class FetchGraphAsync extends AsyncTask<LatLng, Void, List<EdgeComplete>> {
        private RouteFinderContainer routeFinderContainer;
        private MapInfoContainer mapInfoContainer;
        private AlgorithmContainer algorithmContainer;
        private Context context;
        private RouteContainer routeContainer;
        private TextView statusLabel;

        public FetchGraphAsync(Context context, RouteContainer routeContainer, RouteFinderContainer routeFinderContainer, MapInfoContainer mapInfoContainer, AlgorithmContainer algorithmContainer, TextView statusLabel) {
            this.context = context;
            this.routeFinderContainer = routeFinderContainer;
            this.mapInfoContainer = mapInfoContainer;
            this.routeContainer = routeContainer;
            this.algorithmContainer = algorithmContainer;
            this.statusLabel = statusLabel;
        }

        @Override
        protected List<EdgeComplete> doInBackground(LatLng... points) {
            if (points.length != 2) return null;
            try {
                updateLabel(statusLabel,"I: finding route");
                if (routeFinderContainer.getRouteFinder() == null) {
                    InputStream nodes = context.getResources().openRawResource(R.raw.nodes_updated);
                    InputStream edges = context.getResources().openRawResource(R.raw.edges_updated);
                    routeFinderContainer.setRouteFinder(new RouteFinder(nodes, edges, mapInfoContainer));
                }

                List<EdgeComplete> cur = routeFinderContainer.getRouteFinder().findRouteEdges(
                        algorithmContainer.getAlgorithm(),
                        points[0].latitude, points[0].longitude,
                        points[1].latitude, points[1].longitude);



                // perform Chaikin here because this keeps the computation asynchronous and prevents
                // frames from being missed (the UI is the main thread)
                for (int i=0; i<3; ++i) cur = chaikin(cur);

                routeContainer.setRoute(cur);
                return cur;

            } catch (NotSetUpException e) {
                e.printStackTrace();
                updateLabel(statusLabel, "E: Graph not set up.");
            } catch (GraphNotConnectedException e) {
                e.printStackTrace();
                updateLabel(statusLabel, "E: Graph not connected.");
            }
            return null;
        }

        private void updateLabel(TextView label, String text) {
            if (label != null) {
                label.setText(text);
            }
        }

        @Override
        protected void onPostExecute(List<EdgeComplete> route) {
            if (route == null) return;
            updateLabel(statusLabel, "I: plotting route");
            plotRoute(map, route);
            updateLabel(statusLabel,"I: route plotted");
        }


        /**
         * Not particularly difficult (see further graphics).
         * Just a basic function to smooth out the routes. Makes sharp corners less sharp and fades
         * splits changes in edge colour across multiple edges.
         *
         * @param edgesIn The edges to be smoothed.
         * @return The smoothed edges.
         */
        private List<EdgeComplete> chaikin(List<EdgeComplete> edgesIn) {
            if (edgesIn.size() < 2) return edgesIn;
            LinkedList<EdgeComplete> edgesOut = new LinkedList<>();
            List<Node> nodes = new ArrayList<>();
            for (int i=0; i<edgesIn.size()-1; ++i) {
                EdgeComplete edgeOne = edgesIn.get(i);
                EdgeComplete edgeTwo = edgesIn.get(i+1);
                Node one = edgeOne.node1;
                Node two = edgeOne.node2;
                Node p2i  = new Node(0, 0.9375 * one.Latitude + 0.0625 * two.Latitude, 0.9375 * one.Longitude + 0.0625 * two.Longitude);
                Node p2i1 = new Node(0, 0.0625 * one.Latitude + 0.9375 * two.Latitude, 0.0625 * one.Longitude + 0.9375 * two.Longitude);
                nodes.add(p2i);
                nodes.add(p2i1);
            }

            for (int i=0; i<nodes.size()-1; ++i) {
                Node one = nodes.get(i);
                Node two = nodes.get(i+1);

                double pollution = (i & 2) == 2
                        ? edgesIn.get(i/2).Pollution
                        : (edgesIn.get(i/2).Pollution + edgesIn.get(i/2+1).Pollution) / 2;

                EdgeComplete edge = new EdgeComplete(0,0,one,two,0, pollution);
                edgesOut.addLast(edge);
            }
            EdgeComplete first = edgesOut.getFirst();
            EdgeComplete last = edgesOut.getLast();
            edgesOut.addFirst(new EdgeComplete(0,0, edgesIn.get(0).node1, first.node1, 0, edgesIn.get(0).Pollution));
            edgesOut.addLast(new EdgeComplete(0,0, last.node2, edgesIn.get(edgesIn.size()-1).node2, 0, edgesIn.get(edgesIn.size()-1).Pollution));
            return new ArrayList<>(edgesOut);
        }
    }


    class SavedRouteItemClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(String key) {
            try {
                List<EdgeComplete> route = savedRoutes.getRoute(key);
                plotRoute(map, route);
            } catch (RoutesNotLoadedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onItemLongClick(String key) {
            AlertDialog.Builder builder = new AlertDialog.Builder(main);
            builder.setTitle(R.string.saved_route_delete_title);
            builder.setMessage(key);

            builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    savedRoutes.removeRoute(key);
                    new WriteSavedRoutesAsync().execute();
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
    }


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
                hideFragment(settingsFragment);
                hideFragment(notificationsFragment);
                distanceLabel.setVisibility(View.VISIBLE);
                if (searchView != null) searchView.setVisibility(View.VISIBLE);
                algorithmSelectGroup.setVisibility(View.GONE);
                if (showSaveRouteButton) saveRouteButton.setVisibility(View.VISIBLE);
                else saveRouteButton.setVisibility(View.GONE);
                savedRoutesListView.setVisibility(View.GONE);
                break;

            // settings page
            case PAGE_SETTINGS:
                hideFragment(mapFragment);
                showFragment(settingsFragment);
                hideFragment(notificationsFragment);
                distanceLabel.setVisibility(View.GONE);
                if (searchView != null) searchView.setVisibility(View.GONE);
                algorithmSelectGroup.setVisibility(View.VISIBLE);
                saveRouteButton.setVisibility(View.GONE);
                savedRoutesListView.setVisibility(View.GONE);
                break;

            // notifications page
            case PAGE_NOTIFICATIONS:
                hideFragment(mapFragment);
                hideFragment(settingsFragment);
                showFragment(notificationsFragment);
                distanceLabel.setVisibility(View.GONE);
                if (searchView != null) searchView.setVisibility(View.GONE);
                algorithmSelectGroup.setVisibility(View.GONE);
                saveRouteButton.setVisibility(View.GONE);
                savedRoutesListView.setVisibility(View.VISIBLE);
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

                case R.id.navigation_settings:
                    showPage(Pages.PAGE_SETTINGS);
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
                // context, route container, route finder container, map info container, algorithm container
                new FetchGraphAsync(this, currentRoute, routeFinderContainer, mapInfoContainer, algorithmContainer, distanceLabel).execute(defaultLocation, goal);
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

        // load any saved routes
        new LoadSavedRoutesAsync().execute();

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

        settingsFragment = getSupportFragmentManager().findFragmentById(R.id.settings);
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

        algorithmSelectGroup = findViewById(R.id.algorithm_group);

        saveRouteButton = findViewById(R.id.save_route_button);
        savedRoutesListView = findViewById(R.id.saved_routes_list);

        savedRoutesLayoutManager = new LinearLayoutManager(this);
        savedRoutesListView.setLayoutManager(savedRoutesLayoutManager);
        savedRoutesAdapter = new SavedRoutesAdapter(savedRoutes, new SavedRouteItemClickListener());
        savedRoutesListView.setAdapter(savedRoutesAdapter);


        //updateDistanceLabel(defaultLocation, defaultLocation);

        // END initialise class variables

        showPage(Pages.PAGE_MAP);
    }

    private void clearMap() {
        while (!polylines.isEmpty()) {
            Polyline pl = polylines.get(0);
            pl.remove();
            polylines.remove(0);
        }
    }

    private void clearHeatMap() {
        while (!polygons.isEmpty()) {
            Polygon poly = polygons.get(0);
            poly.remove();
            polygons.remove(0);
        }
    }

    public void plotRoute(GoogleMap map, List<EdgeComplete> edges) {
        clearMap();

        for (EdgeComplete edge : edges) {
            Polyline line = map.addPolyline(new PolylineOptions().add(
                    new LatLng(edge.node1.Latitude, edge.node1.Longitude), new LatLng(edge.node2.Latitude, edge.node2.Longitude)
            ).width(7).color(
                    getPollutionColour(edge.Pollution, mapInfoContainer, 1f)
                    //Color.BLUE
            ).endCap(new RoundCap()).startCap(new RoundCap()));
            polylines.add(line);
        }

        currentRoute.setRoute(edges);

        showSaveRouteButton = true;
        showPage(Pages.PAGE_MAP);

        //System.err.println(String.format(Locale.ENGLISH, "%f < pollution < %f", minPollution, maxPollution));
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
    @SuppressLint("NewApi")
    private int getPollutionColour(double pollution, MapInfoContainer mapInfoContainer, float alpha) {

        float darkFactor = 0.9f;

        double minPollution = mapInfoContainer.getPollutionPercentile5();
        double maxPollution = mapInfoContainer.getPollutionPercentile95();

        double normalisedPollution = (pollution - minPollution) / (maxPollution - minPollution);
        normalisedPollution *= 2;
        normalisedPollution = normalisedPollution > 1f ? 1f : normalisedPollution;
        normalisedPollution = normalisedPollution < 0f ? 0f : normalisedPollution;

        float red;
        float green;
        float blue;

        if (normalisedPollution < 0.5) {
            red = (float) (2 * normalisedPollution);
            green = 1f;
            blue = 0f;
        } else {
            red = 1f;
            green = 1f - (float) (2 * (normalisedPollution - 0.5f));
            blue = 0f;
        }

        return Color.valueOf(darkFactor * red, darkFactor * green, darkFactor * blue, alpha).toArgb();
    }


    /**
     * Called in response to a call to mapFragment.getMapAsync()
     *
     * @param googleMap The google map received.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        map.setMaxZoomPreference(25f);

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
    @SuppressLint("MissingPermission")
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

    /**
     * The callback for when the user changes their preferred algorithm.
     *
     * @param view The view clicked.
     */
    public void onAlgorithmButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        /*
         * We check which button was clicked by checking the id of the view that was clicked
         * (and hence passed to this function).
         *
         * We then take the appropriate action by setting the routing algorithm to be that specified
         * by the user.
         */
        switch (view.getId()) {
            case R.id.radio_pollution_only:
                algorithmContainer.setAlgorithm(Algorithm.POLLUTION_ONLY);
                break;

            case R.id.radio_distance_only:
                algorithmContainer.setAlgorithm(Algorithm.DISTANCE_ONLY);
                break;

            case R.id.radio_mixed_1:
                algorithmContainer.setAlgorithm(Algorithm.MIXED_1);
                break;

            case R.id.radio_mixed_2:
                algorithmContainer.setAlgorithm(Algorithm.MIXED_2);
                break;

            case R.id.radio_mixed_3:
                algorithmContainer.setAlgorithm(Algorithm.MIXED_3);
                break;

            case R.id.radio_avoid_pollution:
                algorithmContainer.setAlgorithm(Algorithm.AVOID_POLLUTED);
                break;
        }
    }

    public void onSaveButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.save_route_button:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.route_name_alert_title);
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString();
                        if (savedRoutes.hasRoute(name)) {
                            dialog.dismiss();
                            // handle clash
                            AlertDialog.Builder errBuilder = new AlertDialog.Builder(main);
                            errBuilder.setTitle(R.string.route_name_alert_title);
                            errBuilder.setMessage(R.string.save_error_same_name);
                            errBuilder.show();
                        } else {
                            savedRoutes.addRoute(name, new ArrayList<>(currentRoute.getRoute()));
                            new WriteSavedRoutesAsync().execute();
                        }
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
                break;
        }
    }
}
