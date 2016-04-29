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
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.Button;
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
import cc.softwarefactory.lokki.android.androidServices.DataService;
import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.models.Place;
import cc.softwarefactory.lokki.android.models.Person;
import cc.softwarefactory.lokki.android.services.ContactService;
import cc.softwarefactory.lokki.android.services.PlaceService;
import cc.softwarefactory.lokki.android.utilities.AnalyticsUtils;


public class PlacesFragment extends Fragment {

    private static final String TAG = "PlacesFragment";
    private Context context;
    private List<Place> placesList;
    private Map<Place, List<Person>> peopleInsidePlace;
    private ListView listView;
    private PlaceService placeService;
    private ContactService contactService;
    private ArrayAdapter<Place> adapter;
    private EditText inputSearch;
    private Button clearFilter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        context = getActivity().getApplicationContext();
        placeService = new PlaceService(context);
        contactService = new ContactService(context);
        View rootView = inflater.inflate(R.layout.fragment_places, container, false);
        listView = (ListView) rootView.findViewById(R.id.listView1);
        registerForContextMenu(listView);
        inputSearch = (EditText) rootView.findViewById(R.id.place_search);
        inputSearch.setEnabled(false);
        inputSearch.setAlpha(0);
        clearFilter = (Button) rootView.findViewById(R.id.clear_place_filter);
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
        enableSearchFilter();
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
            showPlaces(); //commented for testing
        }
    };

    /**
     * Places search filter
     */

    private void enableSearchFilter() {

        inputSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // When user changed the Text
                if (adapter != null) {
                    adapter.getFilter().filter(cs);
                }
                clearFilter.setVisibility(cs.length() == 0 ? View.INVISIBLE : View.VISIBLE);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
            }
        });

        inputSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_search_places_textbox));
            }
        });

        clearFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                        getString(R.string.analytics_action_click),
                        getString(R.string.analytics_label_clear_search_places_textbox_button));
                inputSearch.setText("");
            }
        });
    }

    private void setListAdapter() {

        Log.d(TAG, "setListAdapter");

        final PlacesFragment placesFragment = this;
        adapter = new ArrayAdapter<Place>(context, R.layout.places_row_layout, placesList) {

            @Override
            public View getView(int position, View unusedView, ViewGroup parent) {

                View convertView = getActivity().getLayoutInflater().inflate(R.layout.places_row_layout, parent, false);
                AQuery aq = new AQuery(getActivity(), convertView);

                final Place place = getItem(position);
                inputSearch.setEnabled(true);
                inputSearch.setAlpha(1);
                aq.id(R.id.place_name).text(place.getName());

                aq.id(R.id.places_context_menu_button).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.showContextMenu();
                    }
                });
                Log.d(TAG, "Setting up checkbox callback");

                aq.id(R.id.buzz_checkBox).checked(place.isBuzz());

                aq.id(R.id.buzz_checkBox).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {

                        if (((CheckBox) view).isChecked()) {
                            Dialog dialog = new AlertDialog.Builder(getActivity())
                                    .setMessage(R.string.confirm_buzz)
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int which) {
                                            AnalyticsUtils.eventHit(getString(R.string.analytics_category_ux),
                                                getString(R.string.analytics_action_click),
                                                getString(R.string.analytics_label_buzz_turn_on));
                                            placeService.setBuzz(place, true);
                                            ((CheckBox) view).setChecked(true);
                                        }
                                    })
                                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int which) {
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
                            placeService.setBuzz(place, false);
                        }

                    }
                });

                Log.d(TAG, "Place name: " + place.getName());
                Log.d(TAG, "peopleInsidePlace? " + peopleInsidePlace.containsKey(place));

                if (peopleInsidePlace.containsKey(place)) { // People are inside this place
                    Log.d(TAG, "Inside loop");
                    try {
                        List<Person> people = peopleInsidePlace.get(place);
                        LinearLayout avatarRow = (LinearLayout) convertView.findViewById(R.id.avatar_row);
                        avatarRow.removeAllViewsInLayout(); // Deletes old avatars, if any.

                        for (int i = 0; i < people.size(); i++) {

                            final Person person = people.get(i);

                            if (person instanceof Contact) {
                                final Contact contact = (Contact) person;
                                if (contact.isIgnored() || !contact.isVisibleToMe()) {
                                    continue;
                                }
                            }

                            RoundedImageView image = createAvatar(person);

                            Bitmap photo = person.getPhoto();
                            if (photo != null) {
                                image.setImageBitmap(photo);
                            } else {
                                Log.d(TAG, "Person didn't have photo stored. " + person.toString());
                                image.setImageResource(R.drawable.default_avatar);
                            }
                            image.setContentDescription(person.getEmail());

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

    private RoundedImageView createAvatar(final Person person) {

        int sizeInDip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) 65, getResources().getDisplayMetrics());
        RoundedImageView image = new RoundedImageView(getActivity());
        image.setTag(person.getEmail());
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
                MainApplication.emailBeingTracked = person.getEmail();
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
            List<Person> peopleInThisPlace = new ArrayList<>();

            Location placeLocation = place.getLocation().convertToAndroidLocation();

            // Check myself
            Location userLocation = MainApplication.user.getLocation().convertToAndroidLocation();

            // Compare location
            float myDistance = placeLocation.distanceTo(userLocation);
            if (myDistance < placeLocation.getAccuracy()) {
                peopleInThisPlace.add(MainApplication.user);
            }

            // Check for my contacts
            for (Contact contact : contactService.getContactsVisibleToMe()) {
                Location contactLocation = contact.getLocation().convertToAndroidLocation();

                if (contactLocation.getLatitude() == 0 || contactLocation.getLongitude() == 0) {
                    continue;
                }

                // Compare location
                float distance = placeLocation.distanceTo(contactLocation);
                if (distance < placeLocation.getAccuracy()) {
                    peopleInThisPlace.add(contact);
                }
            }

            if (peopleInThisPlace.size() > 0) {
                peopleInsidePlace.put(place, peopleInThisPlace);
            }


        } catch (Exception ex) {
            Log.e(TAG, "Error" + ex.getMessage());
            ex.printStackTrace();

        }
    }

}
