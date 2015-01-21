/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package com.fsecure.lokki;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.androidquery.AQuery;
import com.fsecure.lokki.avatar.AvatarLoader;
import com.fsecure.lokki.utils.Utils;
import com.makeramen.RoundedImageView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;


public class PlacesFragment extends Fragment {

    private static final String TAG = "PlacesFragment";
    private AQuery aq;
    private Context context;
    private ArrayList<String> placesList;
    private JSONObject peopleInsidePlace;
    private ListView listView;
    private AvatarLoader avatarLoader;

    public PlacesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        context = getActivity().getApplicationContext();
        View rootView = inflater.inflate(R.layout.fragment_places, container, false);
        listView = (ListView) rootView.findViewById(R.id.listView1);
        avatarLoader = new AvatarLoader(context);
        return rootView;

        //listView = new ListView(getActivity());
        //return listView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        Log.e(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        DataService.getPlaces(context);
        showPlaces();
    }

    @Override
    public void onResume() {

        Log.e(TAG, "onResume");
        super.onResume();
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, new IntentFilter("PLACES-UPDATE"));
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, new IntentFilter("LOCATION-UPDATE"));
        //showPlaces();
    }

    @Override
    public void onPause() {

        super.onPause();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.e(TAG, "BroadcastReceiver onReceive");
            showPlaces();
        }
    };

    private void setListAdapter() {

        Log.e(TAG, "setListAdapter");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.places_row_layout, placesList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                convertView = getActivity().getLayoutInflater().inflate(R.layout.places_row_layout, null);
                AQuery aq = new AQuery(getActivity(), convertView);

                final String placeName = getItem(position);
                aq.id(R.id.place_name).text(placeName).longClicked(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Log.e(TAG, "Place long clicked: " + placeName);
                        deletePlaceDialog(placeName);
                        return false;
                    }
                });

                String bgImage = "place_0" + (position + 1);
                //Log.e(TAG, "bgImage: " + bgImage);
                int resourceId = getResources().getIdentifier(bgImage, "drawable", "com.fsecure.lokki");
                aq.id(R.id.background).background(resourceId);
                //Log.e(TAG, "Resource id: " + resourceId);

                Log.e(TAG, "Plane name: " + placeName);
                Log.e(TAG, "peopleInsidePlace? " + peopleInsidePlace.has(placeName));

                if (peopleInsidePlace.has(placeName)) { // People are inside this place
                    Log.e(TAG, "Inside loop");
                    try {
                        JSONArray people = peopleInsidePlace.getJSONArray(placeName);
                        LinearLayout avatarRow = (LinearLayout) convertView.findViewById(R.id.avatar_row);
                        avatarRow.removeAllViewsInLayout(); // Deletes old avatars, if any.

                        for (int i = 0; i < people.length(); i++) {
                            final String email = people.getString(i);
                            if (!MainApplication.iDontWantToSee.has(email)) {

                                RoundedImageView image = createAvatar(email);
                                //image.setImageResource(R.drawable.default_avatar);
                                //avatarLoader.load(email, image);


                                if (MainApplication.avatarCache.get(email) != null) {
                                    image.setImageBitmap(MainApplication.avatarCache.get(email));
                                }
                                else {
                                    Log.e(TAG, "Avatar not in cache, email: " + email);
                                    image.setImageResource(R.drawable.default_avatar);
                                }

                                avatarRow.addView(image);
                            }
                        }

                    } catch (Exception ex) {
                        Log.e(TAG, "Error in adding avatars");
                    }
                }

                return convertView;
            }
        };

        listView.setAdapter(adapter);

    }

    private void deletePlaceDialog(final String name) {

        Log.e(TAG, "deletePlaceDialog");
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(context.getResources().getString(R.string.delete_place))
                .setMessage(name + " " + context.getResources().getString(R.string.will_be_deleted_from_places))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deletePlace(name);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Not now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        alertDialog.show();

    }

    private void deletePlace(String name) {

        Log.e(TAG, "deletePlace");
        try {
            Iterator keys = MainApplication.places.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                JSONObject placeObj = MainApplication.places.getJSONObject(key);
                if (name.equals(placeObj.getString("name"))) {
                    Log.e(TAG, "Place ID to be deleted: " + key);
                    ServerAPI.removePlace(context, key);
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private RoundedImageView createAvatar(final String email) {

        int sizeinDip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) 65, getResources().getDisplayMetrics());
        RoundedImageView image = new RoundedImageView(getActivity());
        image.setTag(email);
        image.setCornerRadius(100);
        image.setBorderWidth(0);
        image.setPadding(20, 0, 0, 0);
        image.setLayoutParams(new LinearLayout.LayoutParams(sizeinDip, sizeinDip));
        image.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //((MainActivity) getActivity()).showUserInMap(email);
                MainApplication.emailBeingTracked = email;
                Intent intentLocation = new Intent("LOCATION-UPDATE");
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intentLocation);
                Intent intentMapTab = new Intent("GO_TO_MAP_TAB");
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intentMapTab);
            }
        });

        return image;
    }

    private void showPlaces() {

        Log.e(TAG, "showPlaces");
        placesList = new ArrayList<String>();
        peopleInsidePlace = new JSONObject();

        try {
            if (MainApplication.places == null) { // Read them from cache
                if (!Utils.getValue(context, "places").equals(""))
                    MainApplication.places = new JSONObject(Utils.getValue(context, "places"));
                else return;
            }

            Log.e(TAG, "Places json: " + MainApplication.places);
            Iterator keys = MainApplication.places.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                JSONObject placeObj = MainApplication.places.getJSONObject(key);
                String placeName = placeObj.getString("name");
                placesList.add(placeName);

                calculatePeopleInside(placeObj);
            }

            Log.e(TAG, "peopleInsidePlace: " + peopleInsidePlace);
            Collections.sort(placesList);
            setListAdapter();

        } catch (Exception ex) {
            Log.e(TAG, "ERROR: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void calculatePeopleInside(JSONObject placeObj) {

        try {
            if (MainApplication.dashboard == null) return;

            JSONObject iCanSeeObj = MainApplication.dashboard.getJSONObject("icansee");
            JSONObject idMappingObj = MainApplication.dashboard.getJSONObject("idmapping");
            JSONArray peopleInThisPlace = new JSONArray();

            Location placeLocation = new Location(placeObj.getString("name"));
            placeLocation.setLatitude(placeObj.getDouble("lat"));
            placeLocation.setLongitude(placeObj.getDouble("lon"));
            placeLocation.setAccuracy(placeObj.getInt("rad"));

            // Check myself
            JSONObject userLocationObj = MainApplication.dashboard.getJSONObject("location");
            Location myLocation = new Location(MainApplication.userAccount);
            myLocation.setLatitude(userLocationObj.getDouble("lat"));
            myLocation.setLongitude(userLocationObj.getDouble("lon"));
            //Log.e(TAG, "userLocation: " + userLocation);

            // Compare location
            float myDistance = placeLocation.distanceTo(myLocation);
            if (myDistance < placeLocation.getAccuracy()) {
                //Log.e(TAG, email + " is in place: " + placeLocation.getProvider());
                peopleInThisPlace.put(MainApplication.userAccount);
            }

            // Check for my contacts
            Iterator keys = iCanSeeObj.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String email = (String) idMappingObj.get(key);

                JSONObject locationObj = iCanSeeObj.getJSONObject(key).getJSONObject("location");
                Location userLocation = new Location(email);
                userLocation.setLatitude(locationObj.getDouble("lat"));
                userLocation.setLongitude(locationObj.getDouble("lon"));
                //Log.e(TAG, "userLocation: " + userLocation);

                // Compare location
                float distance = placeLocation.distanceTo(userLocation);
                if (distance < placeLocation.getAccuracy()) {
                    //Log.e(TAG, email + " is in place: " + placeLocation.getProvider());
                    peopleInThisPlace.put(email);
                }
            }

            if (peopleInThisPlace.length() > 0) {
                //Log.e(TAG, "peopleInThisPlace: " + peopleInThisPlace);
                peopleInsidePlace.put(placeObj.getString("name"), peopleInThisPlace);
            }


        } catch (Exception ex) {
            Log.e(TAG, "Error");
            ex.printStackTrace();

        }
    }

}
