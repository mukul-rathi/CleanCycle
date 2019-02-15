package uk.ac.cam.cl.cleancyclegui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // START initialise class variables
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        dashboardFragment = getSupportFragmentManager().findFragmentById(R.id.dashboard);
        notificationsFragment = getSupportFragmentManager().findFragmentById(R.id.notifications);

        navigationView = (BottomNavigationView) findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        main = this;
        // END initialise class variables

        // show home fragment and hide others
        showFragment(mapFragment);
        hideFragment(dashboardFragment);
        hideFragment(notificationsFragment);
    }

    /**
     * Plot a route on the map, given a series of points
     * @param points The array of points to plot
     */
    public void plotRoute(LatLng... points) {
        Polyline line = map.addPolyline(
                new PolylineOptions()
                    .add(points)
                .width(5)
                .color(0xFF005331)
        );
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
