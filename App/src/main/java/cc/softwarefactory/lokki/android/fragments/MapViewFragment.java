/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.models.Person;
import cc.softwarefactory.lokki.android.models.Place;
import cc.softwarefactory.lokki.android.models.UserLocation;
import cc.softwarefactory.lokki.android.services.ContactService;
import cc.softwarefactory.lokki.android.utilities.AnalyticsUtils;
import cc.softwarefactory.lokki.android.utilities.DialogUtils;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import cc.softwarefactory.lokki.android.utilities.Utils;
import cc.softwarefactory.lokki.android.utilities.map.CustomNonHierarchicalDistanceBasedAlgorithm;
import cc.softwarefactory.lokki.android.utilities.map.MapUserTypes;
import cc.softwarefactory.lokki.android.utilities.map.MapUtils;
import cc.softwarefactory.lokki.android.utilities.map.PersonRenderer;


public class MapViewFragment extends Fragment {

    public static final String BROADCAST_GO_TO = "GO_TO_LOCATION";
    public static final String GO_TO_COORDS = "GO_TO_COORDS";

    private static Boolean cancelAsyncTasks = false;
    private static final int DEFAULT_ZOOM = 16;
    private static final String TAG = "MapViewFragment";
    private static final String BUNDLE_KEY_MAP_STATE ="mapdata";

    private AQuery aq;
    private ArrayList<Circle> placesOverlay;
    private ClusterManager <Person> clusterManager;
    private Context context;
    private double radiusMultiplier = 0.9;  // Dont want to fill the screen from edge to edge...
    private GoogleMap map;
    private List<Person> markers;
    private LatLng startLocation = null;
    private SupportMapFragment fragment;
    private TextView placeAddingTip;

    private ContactService contactService;

    public MapViewFragment() {
        markers = new ArrayList<>();
        placesOverlay = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_map, container, false);
        placeAddingTip = (TextView) rootView.findViewById(R.id.place_adding_tip);
        placeAddingTip.setText(R.string.place_adding_tip);
        updatePlaceAddingTipVisibility();

        aq = new AQuery(getActivity(), rootView);
        context = getActivity().getApplicationContext();
        contactService = new ContactService(context);
        LocalBroadcastManager.getInstance(context).registerReceiver(goToReceiver, new IntentFilter(BROADCAST_GO_TO));
        return rootView;
    }

    public void updatePlaceAddingTipVisibility() {
        boolean placeIsBeingAdded = getView() != null && getView().findViewById(R.id.addPlaceCircle) != null &&
            ((ImageView) getView().findViewById(R.id.addPlaceCircle)).getDrawable() != null;

        boolean noPlacesAdded = MainApplication.places != null && MainApplication.places.size() > 0;

        placeAddingTip.setAlpha(noPlacesAdded && !placeIsBeingAdded ? 1 : 0);
    }

    @Override
    public void onDestroyView() {
        // Trying to clean up properties (not to hold anything coming from the map (and avoid mem leaks)).
        fragment = null;
        map = null;
        aq = null;
        LocalBroadcastManager.getInstance(context).unregisterReceiver(goToReceiver);
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) { // This method guarantees that the fragment is loaded in the parent activity!

        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        FragmentManager fm = getChildFragmentManager();
        fragment = (SupportMapFragment) fm.findFragmentById(R.id.map);
        if (fragment == null) {
            fragment = SupportMapFragment.newInstance();
            fm.beginTransaction().replace(R.id.map, fragment).commit();
        }
    }

    //store current map state on SharedPreferences
    public void storeMapState(){
        if (map == null){
            Log.w(TAG, "No map, can't save current location");
            return;
        }
        PreferenceUtils.setDouble(context, PreferenceUtils.KEY_LAT, map.getCameraPosition().target.latitude);
        PreferenceUtils.setDouble(context, PreferenceUtils.KEY_LON, map.getCameraPosition().target.longitude);
    }

    //load current map state from SharedPreferences
    public void loadMapState(){
        Double lat, lon;
        try {
            lat = PreferenceUtils.getDouble(context, PreferenceUtils.KEY_LAT);
            lon = PreferenceUtils.getDouble(context, PreferenceUtils.KEY_LON);
        } catch(Exception e){
            Log.d(TAG, "Error Parsing saved coordinates" + e );
            lat = 0.0;
            lon = 0.0;
        }
        startLocation = new LatLng(lat, lon);
    }



    @Override
    public void onResume() { // onResume is called after onActivityCreated, when the fragment is loaded 100%

        Log.d(TAG, "onResume");
        super.onResume();
        if (map == null) {
            Log.w(TAG, "Map null. creating it.");
            setUpMap();
            setupAddPlacesOverlay();
        } else {
            Log.d(TAG, "Map already exists. Nothing to do.");
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, new IntentFilter("LOCATION-UPDATE"));
        LocalBroadcastManager.getInstance(context).registerReceiver(placesUpdateReceiver, new IntentFilter("PLACES-UPDATE"));

        checkLocationServiceStatus();

        new UpdateMap().execute(MapUserTypes.All);
        cancelAsyncTasks = false;
        if (MainApplication.places != null) {
            updatePlaces();
        }
        AnalyticsUtils.screenHit(getString(R.string.analytics_screen_map));


        if(MainApplication.emailBeingTracked == null && map != null){
            if(startLocation == null){
              loadMapState();
            }
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, DEFAULT_ZOOM));
            //Don't move again on the next resume
            startLocation = null;
        }
        setUpClusterManager();
        markers.clear();
    }


    private void checkLocationServiceStatus() {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gps && !network && !MainApplication.locationDisabledPromptShown) {
            promptLocationService();
            MainApplication.locationDisabledPromptShown = true;
        }
    }

    private void promptLocationService() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.location_services_disabled)
                .setMessage(R.string.gps_disabled)
                .setCancelable(true)
                .setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                getString(R.string.analytics_action_click),
                                getString(R.string.analytics_label_open_settings_from_location_disabled_dialog));
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(R.string.ignore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                getString(R.string.analytics_action_click),
                                getString(R.string.analytics_label_ignore_location_disabled_dialog));
                    }
                })
                .show();
    }

    private void setUpMap() {

        map = fragment.getMap();

        if (map == null) {
            Log.e(TAG, "Could not create map!");
            return;
        }

        map.setMapType(MainApplication.mapTypes[MainApplication.mapType]);
        map.setMyLocationEnabled(true);
        map.setIndoorEnabled(true);
        map.setBuildingsEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                MainApplication.emailBeingTracked = null;
            }
        });

        // Set long click to add a place
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_long_click),
                        getString(R.string.analytics_label_add_place_overlay_activated));
                setAddPlacesVisible(true);
            }
        });

        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_my_location_button));
                MainApplication.locationDisabledPromptShown = false;
                MainApplication.emailBeingTracked = null;
                checkLocationServiceStatus();
                return false;
            }
        });
    }

    private void setupAddPlacesOverlay() {
        // todo these should probably be initialized once...
        Button cancelButton = (Button) getView().findViewById(R.id.cancel_add_place_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_cancel_add_place_button));
                setAddPlacesVisible(false);
            }
        });

        Button addPlaceButton = (Button) getView().findViewById(R.id.add_place_button);
        addPlaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_confirm_add_place_button));
                int mapWidth = fragment.getView().getWidth();
                int mapHeight = fragment.getView().getHeight() - getView().findViewById(R.id.add_place_buttons).getHeight();

                Location middleSideLocation;
                if (mapWidth > mapHeight) {
                    middleSideLocation = MapUtils.convertToLocation(map.getProjection().fromScreenLocation(new Point(mapWidth / 2, 0)), "middleSide");
                } else {
                    middleSideLocation = MapUtils.convertToLocation(map.getProjection().fromScreenLocation(new Point(0, mapHeight / 2)), "middleSide");
                }

                LatLng centerLatLng = map.getProjection().fromScreenLocation(getAddPlaceCircleCenter());
                int radius = (int) middleSideLocation.distanceTo(MapUtils.convertToLocation(centerLatLng, "center"));
                DialogUtils.addPlace(getActivity(), centerLatLng, (int) (radius * radiusMultiplier));
            }
        });
    }

    private void setAddPlacesVisible(boolean visible) {
        if (visible) {
            ((ImageView) getView().findViewById(R.id.addPlaceCircle)).setImageDrawable(new AddPlaceCircleDrawable());
            showAddPlaceButtons();
        } else {
            ((ImageView) getView().findViewById(R.id.addPlaceCircle)).setImageDrawable(null);
            hideAddPlaceButtons();
        }
        updatePlaceAddingTipVisibility();
    }

    private void showAddPlaceButtons() {
        Animation slideUp = AnimationUtils.loadAnimation(this.getActivity().getApplicationContext(), R.anim.add_place_buttons_show);
        getView().findViewById(R.id.add_place_buttons).startAnimation(slideUp);
        getView().findViewById(R.id.add_place_overlay).setVisibility(View.VISIBLE);
    }

    private void hideAddPlaceButtons() {
        Animation slideDown = AnimationUtils.loadAnimation(this.getActivity().getApplicationContext(), R.anim.add_place_buttons_hide);
        slideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                getView().findViewById(R.id.add_place_overlay).setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        getView().findViewById(R.id.add_place_buttons).startAnimation(slideDown);
    }

    private Point getAddPlaceCircleCenter() {
        int mapCenterX = fragment.getView().getWidth() / 2;
        int mapCenterY = (fragment.getView().getHeight() - getView().findViewById(R.id.add_place_buttons).getHeight()) / 2;

        return new Point(mapCenterX, mapCenterY);
    }

    @Override
    public void onPause() {
        super.onPause();
        storeMapState();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(placesUpdateReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BroadcastReceiver onReceive");
            Bundle extras = intent.getExtras();
            if (extras == null || !extras.containsKey("current-location")) {
                new UpdateMap().execute(MapUserTypes.All);
            }
        }
    };

    private BroadcastReceiver placesUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "placesUpdateReceiver onReceive");
            updatePlaces();
        }
    };

    /**
     * Receives intents that cause the map to move to a specific location
     */
    private BroadcastReceiver goToReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (map == null){
                Log.w(TAG, "null map, not moving camera");
                return;
            }
            Log.d(TAG, "goToReceiver onReceive");
            //Parse coordinates from extra data
            String coords = intent.getStringExtra(GO_TO_COORDS);
            int separator = coords.indexOf(',');
            if (separator == -1){
                Log.e(TAG, "Invalid coordinates, no separator");
                return;
            }
            double lat, lon;
            try {
                lat = Double.parseDouble(coords.substring(0 , separator));
                lon = Double.parseDouble(coords.substring(separator +1));
            }
            catch (NumberFormatException e){
                Log.e(TAG, "Could not parse coordinates");
                return;
            }
            //If we're tracking a contact, untrack them to prevent the camera from focusing on them
            MainApplication.emailBeingTracked = null;
            if (MapViewFragment.this.isVisible()){
                //If the map is already visible, just move the map
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat,lon), DEFAULT_ZOOM));
            }
            //If the map is not visible, move the camera the next time the map is shown
            startLocation = new LatLng(lat, lon);
        }
    };

    private void updatePlaces() {

        Log.d(TAG, "updatePlaces");
        if (map == null) {
            return;
        }

        removePlaces();

        for (Place place : MainApplication.places) {
            Circle circle = map.addCircle(new CircleOptions()
                    .center(new LatLng(place.getLocation().getLat(), place.getLocation().getLon()))
                    .radius(place.getLocation().getAcc()) //updated to getAcc from getRad
                    .strokeWidth(0)
                    .fillColor(getResources().getColor(R.color.place_circle)));
            placesOverlay.add(circle);
        }

        updatePlaceAddingTipVisibility();
    }

    private void removePlaces() {

        Log.d(TAG, "removePlaces");
        for (Iterator<Circle> it = placesOverlay.iterator(); it.hasNext();) {
            Circle circle = it.next();
            circle.remove();
        }
        placesOverlay.clear();
    }

    private class UpdateMap extends AsyncTask<MapUserTypes, Void, List<Person>> {

        @Override
        protected List<Person> doInBackground(MapUserTypes... params) {
            List<Contact> visibleContacts = contactService.getContactsVisibleToMe();

            MapUserTypes who = params[0];
            Log.d(TAG, "UpdateMap update for all users: " + who);

            List<Person> markerData = new ArrayList<>();

            if (who == MapUserTypes.User || who == MapUserTypes.All) {
                if (MainApplication.user != null && MainApplication.user.getEmail() != null) markerData.add(MainApplication.user);
            }

            if (who == MapUserTypes.Others || who == MapUserTypes.All) {
                for (Contact contact : visibleContacts) {
                    markerData.add((contact));
                }
            }
            return markerData;
        }

        @Override
        protected void onPostExecute(List<Person> markerDataResult) {

            Log.d(TAG, "cancelAsyncTasks: " + cancelAsyncTasks);
            super.onPostExecute(markerDataResult);
            if (markerDataResult != null && !cancelAsyncTasks && isAdded()) {
                for (Person person : markerDataResult) {
                    Log.d(TAG, "marker to update: " + person.toString());
                    if (person != null) {
                        new LoadMarkerAsync(person).execute();
                    }
                }
            }
        }
    }

    private Bitmap getMarkerBitmap(Person person) {
        if (person.getMarkerPhoto() != null) return person.getMarkerPhoto();

        Bitmap userImage = person.getPhoto();
        if (userImage == null) {
            userImage = BitmapFactory.decodeResource(getResources(), R.drawable.default_avatar);
        }

        Log.d(TAG, "userImage setting borders ");
        if (person == MainApplication.user) {
           userImage = Utils.addBorderToBitMap(userImage, 10, Color.GREEN);
        } else {
            boolean accurate = Math.round(person.getLocation().getAcc()) < 100;
            boolean recent = (System.currentTimeMillis() - person.getLocation().getTime().getTime()) < 60 * 60 * 1000;
            if (!recent || !accurate) {
                userImage = Utils.addBorderToBitMap(userImage, 10, Color.YELLOW);
            }
        }

        person.setMarkerPhoto(userImage);
        return userImage;

    }


    class LoadMarkerAsync extends AsyncTask<Void, Void, Bitmap> {

        Person person;

        public LoadMarkerAsync(Person person) {
            this.person = person;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                UserLocation location = person.getLocation();
                if (location == null || location.isEmpty()) return null;
                return getMarkerBitmap(person);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmapResult) {

            super.onPostExecute(bitmapResult);
            if (bitmapResult == null || cancelAsyncTasks || !isAdded() || map == null) {
                return;
            }
            Person marker = person;

            if (!markers.contains(marker)) {
                markers.add(marker);
                clusterManager.addItem(marker);
            } else {
                clusterManager.removeItem(marker);
                clusterManager.addItem(marker);
            }

            if (marker.getEmail().equals(MainApplication.emailBeingTracked)) {
                Log.d(TAG, "onPostExecute - showInfoWindow open");
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), DEFAULT_ZOOM));
            } else if (MainApplication.firstTimeZoom && MainApplication.emailBeingTracked == null && MainApplication.user.getEmail() != null && marker.getEmail().equals(MainApplication.user.getEmail()) && marker.getPosition() != null) {
                MainApplication.firstTimeZoom = false;
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), DEFAULT_ZOOM));
            }
            clusterManager.cluster();
        }

    }

    @Override
    public void onDestroy() {
        // TODO: Cancel ALL Async tasks
        cancelAsyncTasks = true;
        super.onDestroy();
    }
    private void setUpClusterManager(){
        Log.v(TAG, "setUpClusterManager()");
        GoogleMap googleMap = fragment.getMap();
        if(null == googleMap){
            Log.v(TAG, "Map null");
            setUpMap();
        }
        clusterManager = new ClusterManager<>(context, googleMap);
        clusterManager.setAlgorithm(new CustomNonHierarchicalDistanceBasedAlgorithm<Person>());
        clusterManager.setRenderer(new PersonRenderer(context, map, clusterManager));
        clusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<Person>() {
            @Override
            public boolean onClusterClick(Cluster<Person> cluster) {
                Person person = cluster.getItems().iterator().next();
                Toast.makeText(context, cluster.getSize() + " people including " + person.toString() , Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        clusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<Person>() {
            @Override
            public boolean onClusterItemClick(Person person) {
                MainApplication.emailBeingTracked = person.getEmail();
                return false;
            }
        });
        googleMap.setOnCameraChangeListener(clusterManager);
        googleMap.setOnMarkerClickListener(clusterManager);
        googleMap.setOnInfoWindowClickListener(clusterManager);
        googleMap.setInfoWindowAdapter(clusterManager.getMarkerManager());
    }

    private class AddPlaceCircleDrawable extends Drawable {

        public static final int STROKE_WIDTH = 12;
        public final float[] DASH_INTERVALS = new float[]{49, 36};

        @Override
        public void draw(Canvas canvas) {
            Point mapCenter = getAddPlaceCircleCenter();
            int radius = Math.min(mapCenter.x, mapCenter.y);

            Paint circlePaint = new Paint();
            circlePaint.setColor(getResources().getColor(R.color.add_place_circle));
            circlePaint.setAntiAlias(true);
            circlePaint.setStrokeWidth(STROKE_WIDTH);
            DashPathEffect dashPath = new DashPathEffect(DASH_INTERVALS, 1.0f);
            circlePaint.setPathEffect(dashPath);
            circlePaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(mapCenter.x, mapCenter.y, (int) (radius * radiusMultiplier - STROKE_WIDTH), circlePaint);
        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(ColorFilter cf) {

        }

        @Override
        public int getOpacity() {
            return 0;
        }
    }
}
