package cc.softwarefactory.lokki.android.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.errors.PlaceError;
import cc.softwarefactory.lokki.android.models.JSONModel;
import cc.softwarefactory.lokki.android.models.Place;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import cc.softwarefactory.lokki.android.utilities.ServerApi;

public class PlaceService extends ApiService {

    public PlaceService(Context context) {
        super(context);
    }

    @Override
    String getTag() {
        return "PlaceService";
    }

    @Override
    String getCacheKey() {
        return PreferenceUtils.KEY_PLACES;
    }

    private static class AddResponse {
        public String id;
    }

    private static final String TAG = "ServerApi";

    private String restPath = "places";

    public void getPlaces() {
        Log.d(TAG, "getPlaces");

        get(restPath, new AjaxCallback<String>() {
            @Override
            public void callback(String url, String json, AjaxStatus status) {
                Log.d(TAG, "placesCallback");

                if (json == null) {
                    Log.e(TAG, "Error: " + status.getCode() + " - " + status.getMessage());
                    return;
                }
                Log.d(TAG, "json returned: " + json);
                try {
                    MainApplication.places = JSONModel.createListFromJson(json.toString(), Place.class);
                    updateCache();
                    Intent intent = new Intent("PLACES-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } catch (IOException e) {
                    Log.e(TAG, "Error: Failed to parse places JSON.");
                    e.printStackTrace();
                }
            }
        });
    }

    public List<Place> getFromCache() throws IOException {
        return JSONModel.createListFromJson(PreferenceUtils.getString(context, PreferenceUtils.KEY_PLACES), Place.class);
    }

    private void updateCache() {
        try {
            updateCache(new ObjectMapper().writeValueAsString(MainApplication.places));
        } catch (JsonProcessingException e) {
            Log.e(TAG, "Serializing places to JSON failed");
            e.printStackTrace();
        }
    }

    private void displayPlaceError(final AjaxStatus status) {
        PlaceError error = PlaceError.getEnum(status.getError());
        if (error != null) {
            Toast.makeText(context, context.getString(error.getErrorMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    public void addPlace(String name, LatLng latLng, int radius) throws JSONException, JsonProcessingException {


        Log.d(TAG, "addPlace");
        String cleanName = name.trim();
        cleanName = cleanName.substring(0, 1).toUpperCase() + cleanName.substring(1).toLowerCase();

        final Place place = new Place();
        place.setName(cleanName);
        place.setImg("");
        place.setLocation(new Place.Location(latLng, radius));

        post("place", place, new AjaxCallback<String>() {
            @Override
            public void callback(String url, String object, AjaxStatus status) {
                ServerApi.logStatus("addPlace", status);

                if (status.getError() != null) {
                    displayPlaceError(status);
                    return;
                }

                Log.d(TAG, "No error, place created.");
                Toast.makeText(context, context.getString(R.string.place_created), Toast.LENGTH_SHORT).show();

                try {
                    AddResponse addResponse = JSONModel.createFromJson(object.toString(), AddResponse.class);
                    place.setId(addResponse.id);
                    MainApplication.places.add(place);
                    updateCache();
                    Intent intent = new Intent("PLACES-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } catch (IOException e) {
                    Log.e(TAG, "add place response JSON deserializing failed. Going to update places from the server.");
                    getPlaces();
                    e.printStackTrace();
                }
            }
        });
    }

    public void removePlace(Place place) {

        Log.d(TAG, "removePlace");

        final String placeId = place.getId();

        delete(restPath + "/" + placeId, new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                ServerApi.logStatus("removePlace", status);
                if (status.getError() == null) {
                    Log.d(TAG, "No error, continuing deletion.");

                    for (Iterator<Place> it = MainApplication.places.iterator(); it.hasNext(); ) {
                        Place place = it.next();
                        if (place.getId().equals(placeId)) {
                            it.remove();
                            break;
                        }
                    }
                    updateCache();

                    Toast.makeText(context, context.getString(R.string.place_removed), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent("PLACES-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            }
        });
    }

    public void renamePlace(Place place, final String newName) throws JSONException, IOException {

        final String placeId = place.getId();
        Log.d(TAG, "renamePlace");

        String cleanName = newName.trim();
        cleanName = cleanName.substring(0, 1).toUpperCase() + cleanName.substring(1).toLowerCase();
        place.setName(cleanName);

        put(restPath + "/" + placeId, place, new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                ServerApi.logStatus("renamePlace", status);

                if (status.getError() != null) {
                    displayPlaceError(status);
                    getPlaces();
                    return;
                }

                Intent intent = new Intent("PLACES-UPDATE");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                Toast.makeText(context, R.string.place_renamed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public Place getPlaceById(String id) {
        for (Place place : MainApplication.places) {
            if (place.getId() == id) return place;
        }
        Log.e(TAG, "couldn't find place by id: " + id);
        return null;
    }
}
