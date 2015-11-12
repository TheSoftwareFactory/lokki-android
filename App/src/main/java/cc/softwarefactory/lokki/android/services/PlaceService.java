package cc.softwarefactory.lokki.android.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.errors.PlaceError;
import cc.softwarefactory.lokki.android.models.JSONMap;
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

    /**
     * User's places is a map, where key is ID and value is the place.
     * Places in the format they are received from the server.
     */
    private static class PlacesResponse extends JSONMap<Place> {

        private Map<String, Place> places = new HashMap<>();

        @Override
        protected Map<String, Place> getMap() {
            return places;
        }

        public Collection<Place> getPlaces() {
            return places.values();
        }

    }

    private static class AddResponse {
        public String id;
    }

    public List<Place> placesResponseToPlaces(PlacesResponse placesResponse) {
        List<Place> places = new ArrayList<>();

        for (Map.Entry<String, Place> placeResponseEntry : placesResponse.entrySet()) {
            Place place = placeResponseEntry.getValue();
            place.setId(placeResponseEntry.getKey());
            places.add(place);
        }
        return places;
    }

    public PlacesResponse placesToPlacesResponse(List<Place> places) {
        PlacesResponse placesResponse = new PlacesResponse();
        for (Place place : places) {
            String id = place.getId();
            place.setId(null);
            placesResponse.put(id, place);
        }
        return placesResponse;
    }


    private static final String TAG = "ServerApi";

    public void getPlaces() {
        Log.d(TAG, "getPlaces");

        get("places", new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject json, AjaxStatus status) {
                Log.d(TAG, "placesCallback");

                if (json == null) {
                    Log.e(TAG, "Error: " + status.getCode() + " - " + status.getMessage());
                    return;
                }
                Log.d(TAG, "json returned: " + json);
                try {
                    PlacesResponse placesResponse = JSONModel.createFromJson(json.toString(), PlacesResponse.class);
                    MainApplication.places = placesResponseToPlaces(placesResponse);
                    updateCache(placesResponse);
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
        return placesResponseToPlaces(JSONModel.createFromJson(PreferenceUtils.getString(context, PreferenceUtils.KEY_PLACES), PlacesResponse.class));
    }

    private void updateCache() {
        updateCache(placesToPlacesResponse(MainApplication.places));
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
        place.setLat(latLng.latitude);
        place.setLon(latLng.longitude);
        place.setRad(radius);

        post("place", place, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
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

        delete("place/" + placeId, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject result, AjaxStatus status) {
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

        put("place/" + placeId, place, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject result, AjaxStatus status) {
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
