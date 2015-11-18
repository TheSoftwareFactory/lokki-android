/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.models.Place;
import cc.softwarefactory.lokki.android.models.User;
import cc.softwarefactory.lokki.android.models.UserLocation;
import cc.softwarefactory.lokki.android.utilities.AnalyticsUtils;
import cc.softwarefactory.lokki.android.utilities.DialogUtils;
import cc.softwarefactory.lokki.android.utilities.Utils;
import cc.softwarefactory.lokki.android.utilities.map.MapUserTypes;
import cc.softwarefactory.lokki.android.utilities.map.MapUtils;


public class MapViewFragment extends Fragment {

    private static final String TAG = "MapViewFragment";
    public static final String BROADCAST_GO_TO = "GO_TO_LOCATION";
    public static final String GO_TO_COORDS = "GO_TO_COORDS";
    private static final int DEFAULT_ZOOM = 16;
    private SupportMapFragment fragment;
    private GoogleMap map;
    private HashMap<String, Marker> markerMap;
    private AQuery aq;
    private static Boolean cancelAsyncTasks = false;
    private Context context;
    private ArrayList<Circle> placesOverlay;
    private double radiusMultiplier = 0.9;  // Dont want to fill the screen from edge to edge...
    private TextView placeAddingTip;
    private final static String BUNDLE_KEY_MAP_STATE ="mapdata";
    private LatLng startLocation = null;

    public MapViewFragment() {
        markerMap = new HashMap<>();
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
        Double lat = map.getCameraPosition().target.latitude;
        Double lon =  map.getCameraPosition().target.longitude;

        SharedPreferences prefs = context.getSharedPreferences(BUNDLE_KEY_MAP_STATE, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lat", Double.toString(lat));
        editor.putString("lon", Double.toString(lon));
        editor.commit();

    }

    //load current map state from SharedPreferences
    public void loadMapState(){
        SharedPreferences prefs = context.getSharedPreferences(BUNDLE_KEY_MAP_STATE, Activity.MODE_PRIVATE);
        Double lat, lon;
        try {
            lat = Double.parseDouble(prefs.getString("lat", "0.0"));
            lon = Double.parseDouble(prefs.getString("lon", "0.0"));
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

        removeMarkers();

        map.setMapType(MainApplication.mapTypes[MainApplication.mapType]);
        map.setInfoWindowAdapter(new MyInfoWindowAdapter()); // Set the windowInfo view for each marker
        map.setMyLocationEnabled(true);
        map.setIndoorEnabled(true);
        map.setBuildingsEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(false);

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (!marker.isInfoWindowShown()) {
                    marker.showInfoWindow();
                    MainApplication.emailBeingTracked = marker.getTitle();

                }
                return true;
            }
        });

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

    private void removeMarkers() {

        Log.d(TAG, "removeMarkers");
        for (Iterator<Marker> it = markerMap.values().iterator(); it.hasNext();) {
            Marker m = it.next();
            m.remove();
        }
        markerMap.clear();
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
                    .radius(place.getLocation().getAcc())
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

    private class UpdateMap extends AsyncTask<MapUserTypes, Void, HashMap<String, Location>> {

        @Override
        protected HashMap<String, Location> doInBackground(MapUserTypes... params) {

            MainApplication.Dashboard dashboard = MainApplication.dashboard;

            if (dashboard == null) {
                return null;
            }

            MapUserTypes who = params[0];
            Log.d(TAG, "UpdateMap update for all users: " + who);

            HashMap<String, Location> markerData = new HashMap<>();

            if (who == MapUserTypes.User || who == MapUserTypes.All) {
                markerData.put(MainApplication.userAccount, dashboard.getUserLocation().convertToAndroidLocation()); // User himself
            }

            if (who == MapUserTypes.Others || who == MapUserTypes.All) {
                for (String userId : dashboard.getUserIdsICanSee()) {
                    User user = dashboard.getUserICanSeeByUserId(userId);
                    UserLocation location = user.getUserLocation();
                    String email = dashboard.getEmailByUserId(userId);
                    Log.d(TAG, "I can see: " + email + " => " + user);

                    if (MainApplication.iDontWantToSee != null && MainApplication.iDontWantToSee.has(email)) {
                        Log.d(TAG, "I dont want to see: " + email);
                    } else {
                        Location loc = location.convertToAndroidLocation();
                        if (loc == null) {
                            Log.w(TAG, "No location could be parsed for: " + email);
                        }
                        markerData.put(email, loc);
                    }
                }
            }
            return markerData;
        }

        @Override
        protected void onPostExecute(HashMap<String, Location> markerDataResult) {

            Log.d(TAG, "cancelAsyncTasks: " + cancelAsyncTasks);
            super.onPostExecute(markerDataResult);
            if (markerDataResult != null && !cancelAsyncTasks && isAdded()) {
                for (String email : markerDataResult.keySet()) {
                    Log.d(TAG, "marker to update: " + email);
                    if (markerDataResult.get(email) != null) {
                        new LoadMarkerAsync(markerDataResult.get(email), email).execute();
                    }
                }
            }
        }
    }

    private class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {

            if (!aq.isExist() || cancelAsyncTasks || !isAdded()) {
                return null;
            }

            View myContentsView = getActivity().getLayoutInflater().inflate(R.layout.map_info_window, null);
            AQuery aq = new AQuery(myContentsView);

            String name = Utils.getNameFromEmail(context, marker.getTitle());
            aq.id(R.id.contact_name).text(name);
            aq.id(R.id.timestamp).text(Utils.timestampText(marker.getSnippet()));

            return myContentsView;
        }
    }

    private Bitmap getMarkerBitmap(String email, Boolean accurate, Boolean recent) {

        Log.d(TAG, "getMarkerBitmap");

        // Add cache checking logic
        Bitmap markerImage = MainApplication.avatarCache.get(email + ":" + accurate + ":" + recent);
        if (markerImage != null) {
            Log.d(TAG, "Marker IN cache: " + email + ":" + accurate + ":" + recent);
            return markerImage;
        } else {
            Log.d(TAG, "Marker NOT in cache. Processing: " + email + ":" + accurate + ":" + recent);
        }

        Log.d(TAG, "AvatarLoader not in cache. Fetching it. Email: " + email);
        // Get avatars
        Bitmap userImage = Utils.getPhotoFromEmail(context, email);
        if (userImage == null) {
            userImage = BitmapFactory.decodeResource(getResources(), R.drawable.default_avatar);
        } else {
            userImage = Utils.getRoundedCornerBitmap(userImage, 50);
        }

        // Marker colors, etc.
        Log.d(TAG, "userImage size: " + userImage);
        View markerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.map_marker, null);

        aq = new AQuery(markerView);
        aq.id(R.id.user_image).image(userImage);
        Log.d(TAG, "aq in place");

        if (email.equals(MainApplication.userAccount)) {
            aq.id(R.id.marker_frame).image(R.drawable.pointers_android_pointer_green);
        } else if (!recent || !accurate) {
            aq.id(R.id.marker_frame).image(R.drawable.pointers_android_pointer_orange);
        }

        Log.d(TAG, "Image set. Calling createDrawableFromView");

        markerImage = createDrawableFromView(markerView);
        MainApplication.avatarCache.put(email + ":" + accurate + ":" + recent, markerImage);
        return markerImage;
    }

    // Convert a view to bitmap
    private Bitmap createDrawableFromView(View view) {

        Log.d(TAG, "createDrawableFromView");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }


    class LoadMarkerAsync extends AsyncTask<Void, Void, Bitmap> {

        Location position;
        LatLng latLng;
        String email;
        String time;
        Boolean accurate;
        Boolean recent;

        public LoadMarkerAsync(Location position, String email) {

            this.email = email;
            this.position = position;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {

            if (position == null || email == null) {
                return null;
            }
            Log.d(TAG, "LoadMarkerAsync - Email: " + email + ", Position: " + position);
            latLng = new LatLng(position.getLatitude(), position.getLongitude());
            time = String.valueOf(position.getTime());
            accurate = Math.round(position.getAccuracy()) < 100;
            recent = (System.currentTimeMillis() - position.getTime()) < 60 * 60 * 1000;

            try {
                return getMarkerBitmap(email, accurate, recent);
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
            Marker marker = markerMap.get(email);
            Boolean isNew = false;
            if (marker != null) {
                Log.d(TAG, "onPostExecute - updating marker: " + email);
                marker.setPosition(latLng);
                marker.setSnippet(time);
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmapResult));

            } else {
                Log.d(TAG, "onPostExecute - creating marker: " + email);
                marker = map.addMarker(new MarkerOptions().position(latLng).title(email).snippet(time).icon(BitmapDescriptorFactory.fromBitmap(bitmapResult)));
                Log.d(TAG, "onPostExecute - marker created");
                markerMap.put(email, marker);
                Log.d(TAG, "onPostExecute - marker in map stored. markerMap: " + markerMap.size());
                isNew = true;
            }

            if (marker.getTitle().equals(MainApplication.emailBeingTracked)) {
                marker.showInfoWindow();
                Log.d(TAG, "onPostExecute - showInfoWindow open");
                if (isNew) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), DEFAULT_ZOOM));
                } else {
                    map.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                }
            } else if (MainApplication.firstTimeZoom && MainApplication.emailBeingTracked == null && MainApplication.userAccount != null && marker.getTitle().equals(MainApplication.userAccount)) {
                MainApplication.firstTimeZoom = false;
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), DEFAULT_ZOOM));
            }
        }

    }

    @Override
    public void onDestroy() {
        // TODO: Cancel ALL Async tasks
        cancelAsyncTasks = true;
        super.onDestroy();
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
