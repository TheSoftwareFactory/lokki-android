package cc.softwarefactory.lokki.android.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.fragments.MapViewFragment;

/**
 * An activity for performing searches and displaying their results
 */
public class SearchActivity extends ListActivity {

    // A list of search results
    ArrayList<SearchResult> resultList;
    private Context context;
    // The debug tag for this activity
    public static final String TAG = "SearchActivity";
    // The string used to fetch the search query from the launching intent
    public final static String QUERY_MESSAGE = "SEARCH_QUERY";
    public static final String GOOGLE_MAPS_API_URL = "http://maps.googleapis.com/maps/api/geocode/json?address=";

    // Icons need to be resized to fit buttons, so cache them to prevent them from hogging the UI thread
    private Drawable contactIcon = null;
    private Drawable placeIcon = null;
    private Drawable mapIcon = null;

    /**
     * The different possible result types:
     * CONTACT: This result is a contact
     * PLACE: This result is a user-defined place
     * GOOGLE_LOCATION: This result is a Google maps location
     */
    public enum ResultType {CONTACT, PLACE, GOOGLE_LOCATION}

    /**
     * Helper class for storing and manipulating search results
     */
    public class SearchResult{
        //The type of the result
        public final ResultType type;
        //The string that is used to represent this result to the user
        public final String displayName;
        //Additional metadata used to show this result on the map
        public final String extraData;

        /**
         * Create a new search result
         * @param type          The ResultType of this result
         * @param displayName   The string that is used to represent this result to the user
         * @param extraData     Additional metadata used to show this result on the map
         */
        SearchResult(ResultType type, String displayName, String extraData){
            this.type = type;
            this.displayName = displayName;
            this.extraData = extraData;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d(TAG, "searchActivityOnCreate");
        setContentView(R.layout.activity_search);
        context= getApplicationContext();
        Intent intent = getIntent();
        //Get the user's query from the intent
        String queryMessage = intent.getStringExtra(QUERY_MESSAGE);
        Log.d(TAG, "User searched for: " + queryMessage);

        resultList = new ArrayList<>();
        setHeader(queryMessage);

        setListAdapter(this);
        new PerformSearch().execute(queryMessage);
        Log.d(TAG, "end of onCreate");
    }

    //------------------Background tasks------------------

    /**
     * Helper class to perform the search in an asynchronous background task.
     * (Prevents the search from hogging the UI thread)
     */
    private class PerformSearch extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... query) {
            if (query.length < 1){
                Log.w(TAG, "No search parameters");
                return null;
            }
            String queryMessage = query[0];
            queryMessage = queryMessage.toLowerCase();

            //Perform searches
            searchContacts(queryMessage);
            searchPlaces(queryMessage);

            return queryMessage;
        }

        @Override
        protected void onPostExecute(String query) {
            //Show the results
            setListAdapter(SearchActivity.this);
            setHeader(query);
            //Start Google Maps search (separate task so that we can show local results before online search finishes)
            new AddressSearch().execute(query);
            Log.d(TAG, "end of performSearch");
        }
    }

    /**
     * Helper class for performing a Google Maps search
     */
    private class AddressSearch extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... query) {
            if (query.length < 1){
                Log.w(TAG, "No search parameters");
                return null;
            }
            String queryMessage = query[0];
            searchGoogleMaps(queryMessage);

            return queryMessage;
        }

        @Override
        protected void onPostExecute(String query) {
            Log.d(TAG, "end of addressSearch");
        }
    }

    //------------------Search Methods------------------

    /**
     * Attempts to find a match from contact names or emails.
     * @param query The string being searched
     */
    protected void searchContacts(String query)
    {
        try {
            JSONObject icansee = MainApplication.dashboard.getJSONObject("icansee");
            JSONObject idmapping =  MainApplication.dashboard.getJSONObject("idmapping");
            JSONObject contacts = MainApplication.contacts;
            Iterator<String> it = icansee.keys();

            // Loop through everyone we can see
            while(it.hasNext())
            {
                String id = it.next();
                String email = idmapping.getString(id);
                JSONObject contact = (contacts != null)? contacts.optJSONObject(email) : null;
                String name ="";

                if(contact!=null){
                    name=contact.optString("name");
                }
                if(email.toLowerCase().contains(query)||name.toLowerCase().contains(query))
                {
                    //Display either name or email depending on whether a name exists
                    //Store contact data in the result's extra data for easy access
                    if(!name.isEmpty())
                        resultList.add(new SearchResult(ResultType.CONTACT, name, email));
                    else
                        resultList.add(new SearchResult(ResultType.CONTACT, email, email));

                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing contacts: " + e);
        }
    }

    /**
     * Attempts to find a match from place names.
     * @param query The string being searched
     */
    protected void searchPlaces(String query){
        // Loop through all user places
        Iterator<String> it = MainApplication.places.keys();
        Log.d(TAG, MainApplication.places.toString());
        while (it.hasNext()){
            String id = it.next();
            try {
                JSONObject location = MainApplication.places.getJSONObject(id);
                String name = location.getString("name");
                Log.d(TAG, "place: " + name);
                if (name.toLowerCase().contains(query)){
                    //Store place coordinates in the result's extra data for easy access
                    String coords = location.getDouble("lat") + "," + location.getDouble("lon");
                    resultList.add(new SearchResult(ResultType.PLACE, name, coords));
                }

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing places: " + e);
            }
        }
    }

    /**
     * Sends a search request to Google Maps and shows the results
     * @param query The string being searched
     */
    private void searchGoogleMaps(final String query){
        final AQuery aq = new AQuery(this);
        String url = GOOGLE_MAPS_API_URL + query;

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject json, AjaxStatus status) {
                if (status.getError() != null){
                    Log.e(TAG, "Error accessing Google Maps API: " + status.getError());
                    return;
                }
                //TODO: Move result parsing to an async task
                try {
                    //Loop through the results in the Google Maps response
                    JSONArray results = json.getJSONArray("results");
                    if (results.length() > 0)
                        for (int i = 0; i < results.length(); i++){
                            //Create a SearchResult from each result in the response
                            resultList.add(parseGoogleMapsResult(results.getJSONObject(i)));
                        }

                } catch (JSONException e){
                    Log.e(TAG, "Error parsing Google Maps JSON: " + e);
                }
                Log.d(TAG, "Geocoding result: " + json.toString());

                //Show the results
                setListAdapter(SearchActivity.this);
                setHeader(query);
            }

        };
        aq.ajax(url, JSONObject.class, cb);
    }

    //------------------Private helper methods------------------

    /**
     * Converts the list of results into a list of buttons visible to the user
     * @param listActivity  The activity containing the list
     */
    private void setListAdapter(final Activity listActivity)
    {
        Log.d(TAG, "setListAdapter");
        ArrayAdapter<SearchResult>adapter= new ArrayAdapter<SearchResult>(context,R.layout.listresult_layout, resultList){

        public View getView(int position, View unusedView, ViewGroup parent)
        {
            View convertView = getLayoutInflater().inflate(R.layout.listresult_layout, parent, false);
            AQuery aq = new AQuery(listActivity,convertView);
            final SearchResult clickedResult = resultList.get(position);
            String buttonLabel = clickedResult.displayName;

            // Configure search result button
            aq.id(R.id.list_result).text(buttonLabel).clicked(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "user clicked search result");
                    switch (clickedResult.type) {
                        case CONTACT: {
                            Log.d(TAG, "result type: contact");
                            //Set app to track contact's email
                            MainApplication.emailBeingTracked = clickedResult.extraData;
                            finish();
                            break;
                        }
                        case PLACE:
                            Log.d(TAG, "result type: place");
                            //Maps locations and places have the same behavior, so fall through
                        case GOOGLE_LOCATION: {
                            //Recheck the result type in case we fell through from PLACE
                            if (clickedResult.type == ResultType.GOOGLE_LOCATION)
                                Log.d(TAG, "result type: Google Maps location");
                            //Broadcast back place coordinates
                            Intent intent = new Intent(MapViewFragment.BROADCAST_GO_TO);
                            intent.putExtra(MapViewFragment.GO_TO_COORDS, clickedResult.extraData);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            //Go back to the map
                            finish();
                            break;
                        }
                        default: {
                            Log.d(TAG, "invalid search type");
                            //Close the search
                            finish();
                        }
                    }

                }
            });
            final Button resultButton = aq.id(R.id.list_result).getButton();
            //Set button icons in a callback so that the button already exists and we can find out its size
            resultButton.addOnLayoutChangeListener(new View.OnLayoutChangeListener(){
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom){
                    setButtonIcon(resultButton, clickedResult);
                    //Only set the icon once
                    resultButton.removeOnLayoutChangeListener(this);
                }
            });

            return convertView;
        }

        };
        getListView().setAdapter(adapter);
    }

    /**
     * Set a button icon depending on the type of result
     * @param btn   The button
     * @param res   The search result associated with the button
     */
    private void setButtonIcon(Button btn, SearchResult res){
        Drawable icon;

        switch (res.type){
            case CONTACT:{
                //If the icon has already been scaled, use the cached icon
                if (contactIcon == null){
                    //If the icon hasn't been scaled, scale and cache it
                    contactIcon = scaleIconToButton(btn, R.drawable.ic_people_grey600_48dp);
                }
                icon = contactIcon;
                break;
            }
            case PLACE:{
                if (placeIcon == null){
                    placeIcon = scaleIconToButton(btn, R.drawable.ic_place_grey600_48dp);
                }
                icon = placeIcon;
                break;
            }
            case GOOGLE_LOCATION:{
                if (mapIcon == null){
                    mapIcon = scaleIconToButton(btn, R.drawable.ic_map_grey600_48dp);
                }
                icon = mapIcon;
                break;
            }
            default:{
                //Unknown result type, do nothing
                return;
            }
        }
        //Set the scaled icon
        btn.setCompoundDrawables(icon, null, null, null);
    }

    /**
     * Scales icons down to fit search result buttons
     * @param btn   The button that will contain the icon
     * @param icon  The resource ID of the icon to be shown on the button
     * @return      The icon scaled down to fit the button
     */
    private Drawable scaleIconToButton(Button btn, int icon){
        Log.d(TAG, "Setting button icon");
        Drawable drawable;
        //getResources().getDrawable is deprecated, but context.getDrawable doesn't work below API level 21
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            drawable = context.getDrawable(icon);
        } else {
            drawable = getResources().getDrawable(icon);
        }
        if (drawable == null){
            Log.e(TAG, "Could not find drawable resource " + icon);
            return null;
        }
        //Scale the drawable if it's too big
        //"Too big" = forces app to resize button
        if (drawable.getIntrinsicHeight() > btn.getHeight() / 2){
            Log.d(TAG, "Intrinsic size: " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight());
            int newHeight = btn.getHeight() / 2;
            int newWidth = (int)(drawable.getIntrinsicWidth() * ((float)newHeight / (float)drawable.getIntrinsicHeight()));

            drawable.setBounds(0, 0, newWidth, newHeight);
            Log.d(TAG, "new drawable size: " + newWidth + "x" + newHeight);
        } else {
            //Call setBounds so that setCompoundDrawables can be called safely
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
        return drawable;

    }

    /**
     * Sets the search page header depending on whether or not there are search results to display
     * @param query The string that was searched for
     */
    private void setHeader(String query){
        AQuery aq = new AQuery(this);
        String headerText = getString(R.string.search_results) + " " + query;
        if (resultList.size() < 1){
            headerText = getString(R.string.no_search_results);
        }
        aq.id(R.id.search_header).text(headerText);
    }

    /**
     * Generates a new Google Maps SearchResult from a single result JSON object contained in a Google Maps geocoding result
     * @param locationObject    The JSON object containing a single Google Maps geocoding search result
     * @return                  A SearchResult object pointing to the location of the Google Maps result
     * @throws JSONException    If the Google Maps result is malformed
     */
    private SearchResult parseGoogleMapsResult(JSONObject locationObject) throws JSONException {
        String name = locationObject.getString("formatted_address");
        JSONObject coords = locationObject.getJSONObject("geometry").getJSONObject("location");
        String coordString = coords.getString("lat") + "," + coords.getString("lng");
        return new SearchResult(ResultType.GOOGLE_LOCATION, name, coordString);
    }

}
