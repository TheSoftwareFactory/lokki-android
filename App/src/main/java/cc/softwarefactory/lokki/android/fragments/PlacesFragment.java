/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.fragments;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.androidquery.AQuery;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.activities.BuzzActivity;
import cc.softwarefactory.lokki.android.androidServices.DataService;
import cc.softwarefactory.lokki.android.models.BuzzPlace;
import cc.softwarefactory.lokki.android.models.Place;
import cc.softwarefactory.lokki.android.models.User;
import cc.softwarefactory.lokki.android.models.UserLocation;
import cc.softwarefactory.lokki.android.services.PlaceService;
import cc.softwarefactory.lokki.android.utilities.AnalyticsUtils;


public class PlacesFragment extends Fragment {

    private static final String TAG = "PlacesFragment";
    private Context context;
    private List<Place> placesList;
    private Map<String, List<String>> peopleInsidePlace;
    private ListView listView;
    private PlaceService placeService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        context = getActivity().getApplicationContext();
        placeService = new PlaceService(context);
        View rootView = inflater.inflate(R.layout.fragment_places, container, false);
        listView = (ListView) rootView.findViewById(R.id.listView1);
        registerForContextMenu(listView);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        DataService.getPlaces(context);
        showPlaces();
    }

    @Override
    public void onResume() {

        Log.d(TAG, "onResume");
        super.onResume();
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, new IntentFilter("PLACES-UPDATE"));
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, new IntentFilter("LOCATION-UPDATE"));
        AnalyticsUtils.screenHit(getString(R.string.analytics_screen_places));
    }

    @Override
    public void onPause() {

        super.onPause();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {


            Log.d(TAG, "BroadcastReceiver onReceive");
            showPlaces();
        }
    };

    private void setListAdapter() {

        Log.d(TAG, "setListAdapter");

        final PlacesFragment placesFragment = this;
        ArrayAdapter<Place> adapter = new ArrayAdapter<Place>(context, R.layout.places_row_layout, placesList) {

            @Override
            public View getView(int position, View unusedView, ViewGroup parent) {

                View convertView = getActivity().getLayoutInflater().inflate(R.layout.places_row_layout, parent, false);
                AQuery aq = new AQuery(getActivity(), convertView);

                final Place place = getItem(position);
                aq.id(R.id.place_name).text(place.getName());

                aq.id(R.id.places_context_menu_button).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.showContextMenu();
                    }
                });
                Log.d(TAG, "Setting up checkbox callback");
                final String placeId = place.getId();

                aq.id(R.id.buzz_checkBox).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {

                        if (((CheckBox) view).isChecked()) {
                            // This ensures that automatic UI refresh won't uncheck the checkbox
                            // while the the dialog is still open.
                            BuzzActivity.setBuzz(placeId, 0);

                            Dialog dialog = new AlertDialog.Builder(getActivity())
                                    .setMessage(R.string.confirm_buzz)
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int which) {
                                            AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                                getString(R.string.analytics_action_click),
                                                getString(R.string.analytics_label_buzz_turn_on));
                                            BuzzActivity.setBuzz(placeId, 5);
                                        }
                                    })
                                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int which) {
                                            BuzzActivity.removeBuzz(placeId);
                                            ((CheckBox) view).setChecked(false);
                                            placesFragment.showPlaces();  // Update UI for tests
                                            AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                                    getString(R.string.analytics_action_click),
                                                    getString(R.string.analytics_label_buzz_decline));
                                        }
                                    }).create();
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                        } else {
                            BuzzActivity.removeBuzz(placeId);
                        }

                    }
                });

                for (BuzzPlace buzzPlace : MainApplication.buzzPlaces) {
                    if (buzzPlace.getPlaceId().equals(placeId))
                        aq.id(R.id.buzz_checkBox).checked(true);
                }

                Log.d(TAG, "Place name: " + place.getName());
                Log.d(TAG, "peopleInsidePlace? " + peopleInsidePlace.containsKey(place.getId()));

                if (peopleInsidePlace.containsKey(place.getId())) { // People are inside this place
                    Log.d(TAG, "Inside loop");
                    try {
                        List<String> people = peopleInsidePlace.get(place.getId());
                        LinearLayout avatarRow = (LinearLayout) convertView.findViewById(R.id.avatar_row);
                        avatarRow.removeAllViewsInLayout(); // Deletes old avatars, if any.

                        for (int i = 0; i < people.size(); i++) {

                            final String email = people.get(i);
                            if (MainApplication.iDontWantToSee.has(email)) {
                                continue;
                            }
                            RoundedImageView image = createAvatar(email);

                            if (MainApplication.avatarCache.get(email) != null) {
                                image.setImageBitmap(MainApplication.avatarCache.get(email));
                            } else {
                                Log.d(TAG, "Avatar not in cache, email: " + email);
                                image.setImageResource(R.drawable.default_avatar);
                            }
                            image.setContentDescription(email);

                            avatarRow.addView(image);
                        }

                    } catch (Exception ex) {
                        Log.e(TAG, "Error in adding avatars" + ex.getMessage());
                    }
                }

                return convertView;
            }
        };

        listView.setAdapter(adapter);


    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.places_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        Place place = placesList.get(position);

        switch(item.getItemId()) {
            case R.id.places_context_menu_rename:
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_rename_place));
                renamePlaceDialog(place);
                return true;
            case R.id.places_context_menu_delete:
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_delete_place));
                deletePlaceDialog(place);
                return true;
        }

        return super.onContextItemSelected(item);
    }

    private void deletePlaceDialog(final Place place) {

        Log.d(TAG, "deletePlaceDialog");
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.delete_place))
                .setMessage(place + " " + getString(R.string.will_be_deleted_from_places))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                getString(R.string.analytics_action_click),
                                getString(R.string.analytics_label_confirm_delete_place_dialog));
                        deletePlace(place);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                getString(R.string.analytics_action_click),
                                getString(R.string.analytics_label_cancel_delete_place_dialog));
                    }
                })
                .show();

    }

    private void deletePlace(Place place) {
        Log.d(TAG, "deletePlace");
        Log.d(TAG, "Place ID to be deleted: " + place.getId());
        placeService.removePlace(place);
    }

    private void renamePlaceDialog(final Place place) {

        Log.d(TAG, "renamePlaceDialog");
        final EditText input = new EditText(getActivity());
        String titleFormat = getString(R.string.rename_prompt);
        String title = String.format(titleFormat, place.getName());

        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(R.string.write_place_name)
                .setView(input)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                getString(R.string.analytics_action_click),
                                getString(R.string.analytics_label_confirm_rename_place_dialog));
                        String newName = input.getText().toString();
                        renamePlace(place, newName);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                getString(R.string.analytics_action_click),
                                getString(R.string.analytics_label_cancel_rename_place_dialog));
                    }
                })
                .show();
    }

    private void renamePlace(Place place, final String newName) {
        Log.d(TAG, "renamePlace");
        try {
            placeService.renamePlace(place, newName);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private RoundedImageView createAvatar(final String email) {

        int sizeInDip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) 65, getResources().getDisplayMetrics());
        RoundedImageView image = new RoundedImageView(getActivity());
        image.setTag(email);
        image.setCornerRadius(100f);
        image.setBorderWidth(0f);
        image.setPadding(20, 0, 0, 0);
        image.setLayoutParams(new LinearLayout.LayoutParams(sizeInDip, sizeInDip));
        image.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_avatar_show_user));
                MainApplication.emailBeingTracked = email;
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("LOCATION-UPDATE"));
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("GO-TO-MAP"));
            }
        });

        return image;
    }

    private void showPlaces() {

        Log.d(TAG, "showPlaces");
        peopleInsidePlace = new HashMap<>();

        try {
            if (MainApplication.places == null) { // Read them from cache
                List<Place> places = placeService.getFromCache();
                if (places.isEmpty()) {
                    return;
                }
                MainApplication.places = places;
            }

            Log.d(TAG, "Places json: " + MainApplication.places);

            for (Place place : MainApplication.places) {
                calculatePeopleInside(place);
            }
            Log.d(TAG, "peopleInsidePlace: " + peopleInsidePlace);

            placesList = new ArrayList<>(MainApplication.places);
            Collections.sort(placesList);
            setListAdapter();

        } catch (Exception ex) {
            Log.e(TAG, "ERROR: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void calculatePeopleInside(Place place) {

        try {
            if (MainApplication.dashboard == null) {
                return;
            }

            Map<String, User> iCanSee = MainApplication.dashboard.getiCanSee();
            List<String> peopleInThisPlace = new ArrayList<>();

            Location placeLocation = new Location(place.getName());
            placeLocation.setLatitude(place.getLocation().getLat());
            placeLocation.setLongitude(place.getLocation().getLon());
            placeLocation.setAccuracy(place.getLocation().getAcc()); //updated to getAcc from getRad

            // Check myself
            UserLocation userLocation = MainApplication.dashboard.getUserLocation();
            Location myLocation = new Location(MainApplication.userAccount);
            myLocation.setLatitude(userLocation.getLat());
            myLocation.setLongitude(userLocation.getLon());
            //Log.d(TAG, "userLocation: " + userLocation);

            // Compare location
            float myDistance = placeLocation.distanceTo(myLocation);
            if (myDistance < placeLocation.getAccuracy()) {
                //Log.d(TAG, email + " is in place: " + placeLocation.getProvider());
                peopleInThisPlace.add(MainApplication.userAccount);
            }

            // Check for my contacts
            for (String userId : iCanSee.keySet()) {
                String email = MainApplication.dashboard.getEmailByUserId(userId);

                UserLocation userLocationObj = iCanSee.get(userId).getUserLocation();
                Location location = new Location(email);

                if (userLocationObj.getLat() == 0 || userLocationObj.getLon() == 0) {
                    continue;
                }

                location.setLatitude(userLocationObj.getLat());
                location.setLongitude(userLocationObj.getLon());
                //Log.d(TAG, "userLocation: " + userLocation);

                // Compare location
                float distance = placeLocation.distanceTo(location);
                if (distance < placeLocation.getAccuracy()) {
                    //Log.d(TAG, email + " is in place: " + placeLocation.getProvider());
                    peopleInThisPlace.add(email);
                }
            }

            if (peopleInThisPlace.size() > 0) {
                //Log.d(TAG, "peopleInThisPlace: " + peopleInThisPlace);
                peopleInsidePlace.put(place.getId(), peopleInThisPlace);
            }


        } catch (Exception ex) {
            Log.e(TAG, "Error" + ex.getMessage());
            ex.printStackTrace();

        }
    }

}
