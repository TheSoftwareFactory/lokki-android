package cc.softwarefactory.lokki.android.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.androidquery.AQuery;

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
    public static final String TAG ="SearchActivity";
    // The string used to fetch the search query from the launching intent
    public final static String QUERY_MESSAGE="SEARCH_QUERY";

    /**
     * The different possible result types:
     * NONE: invalid result or no results
     * CONTACT: This result is a contact
     * PLACE: This result is a user-defined place
     * GOOGLE_LOCATION: This result is a Google maps location
     */
    public enum ResultType {NONE, CONTACT, PLACE, GOOGLE_LOCATION}

    /**
     * Helper class for storing and manipulating search results
     */
    public class SearchResult{
        //The type of the result
        public ResultType type;
        //The string that is used to represent this result to the user
        public String displayName;
        //Additional metadata used to show this result on the map
        public String extraData;

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
        queryMessage = queryMessage.toLowerCase();
        Log.d(TAG, "User searched for: " + queryMessage );

        resultList = new ArrayList<>();

        //Perform searches
        searchContacts(queryMessage);
        searchPlaces(queryMessage);

        //If no results, show a message to the user
        if(resultList.isEmpty())
            resultList.add(new SearchResult(ResultType.NONE, getString(R.string.no_search_results), null));
        //Show the results
        setListAdapter(this);
    }

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
     * Converts the list of results into a list of buttons visible to the user
     * @param listActivity
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
                        case PLACE: {
                            Log.d(TAG, "result type: place");
                            //Broadcast back place coordinates
                            Intent intent = new Intent(MapViewFragment.BROADCAST_GO_TO);
                            intent.putExtra(MapViewFragment.GO_TO_COORDS, clickedResult.extraData);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            finish();
                            break;
                        }
                        default: {
                            Log.d(TAG, "result type: other");
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
                    // Pick icon based on result type
                    switch (clickedResult.type){
                        case CONTACT:{
                            Log.d(TAG, "setting button for " + clickedResult.displayName);
                            setButtonIcon(resultButton, R.drawable.ic_people_grey600_48dp);
                            break;
                        }
                        case PLACE:{
                            Log.d(TAG, "setting button for " + clickedResult.displayName);
                            setButtonIcon(resultButton, R.drawable.ic_place_grey600_48dp);
                            break;
                        }
                    }
                }
            });

            return convertView;
        }

        };
        getListView().setAdapter(adapter);
    }

    /**
     * Helper method for computing proper sizes for button icons and setting them
     * @param btn   The button to be configured
     * @param icon  The resource ID of the icon to be shown on the button
     */
    private void setButtonIcon(Button btn, int icon){
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
            return;
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
        btn.setCompoundDrawables(drawable, null, null, null);

    }

}
