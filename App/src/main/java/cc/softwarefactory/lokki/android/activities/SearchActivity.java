package cc.softwarefactory.lokki.android.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SearchRecentSuggestionsProvider;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import com.androidquery.AQuery;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.fragments.ContactsFragment;

public class SearchActivity extends ListActivity {

    ArrayList<String> listresult;
    private Context context;
    public static final String TAG ="searchActivity";
    private ListView listView;
    public final static String QUERY_MESSAGE="send query message";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d(TAG, "searchActivityOnCreate");
        setContentView(R.layout.activity_search);
        context= getApplicationContext();
        Intent intent = getIntent();
        String query_message=intent.getStringExtra(QUERY_MESSAGE);
        Log.d(TAG, "searchActivityOnCreate1"+query_message );
        doMySearch(query_message);


    }
    protected void doMySearch(String query)
    {
        listresult=new ArrayList<String>();

        try {
            JSONObject icansee = MainApplication.dashboard.getJSONObject("icansee");
            JSONObject idmapping =  MainApplication.dashboard.getJSONObject("idmapping");
            JSONObject contacts = MainApplication.contacts;
            Iterator<String> it =icansee.keys();
            while(it.hasNext())
            {
                String id =it.next();
                String email=idmapping.getString(id);
                JSONObject contact =contacts.optJSONObject(email);
                String name ="";

                if(contact!=null){
                    name=contact.optString("name");
                }
                if(email.contains(query)||name.contains(query))
                {
                    if(!name.isEmpty())
                        listresult.add(name);
                    else
                        listresult.add(email);

                }
            }
            if(listresult.isEmpty())
                listresult.add("No matching contacts");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        setListAdapter(this);
    }
    private void setListAdapter(final Activity listActivity)
    {
        Log.d(TAG, "setListAdapter");
        ArrayAdapter<String>adapter= new ArrayAdapter<String>(context,R.layout.listresult_layout,listresult){

        public View getView(int position, View unusedView, ViewGroup parent)
        {
            View convertView = getLayoutInflater().inflate(R.layout.listresult_layout, parent, false);
            AQuery aq = new AQuery(listActivity,convertView);
            final String namelist= listresult.get(position);
            Log.d(TAG,"SearchButtonClick1");
            aq.id(R.id.list_result).text(namelist).clicked(new View.OnClickListener(){
                @Override
            public void onClick(View view)
                {
                   String email=MainApplication.mapping.optString(namelist);
                    if(email.isEmpty())
                        email=namelist;
                    MainApplication.emailBeingTracked=email;
                    Log.d(TAG,"SearchButtonClick2");
                    finish();

                }
            });
            return convertView;
        }

        };
        getListView().setAdapter(adapter);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        MenuItem searchItem=menu.findItem(R.id.search);
        SearchView searchview=(SearchView) MenuItemCompat.getActionView(searchItem);

        return super.onCreateOptionsMenu(menu);


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
       /* if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }
}
