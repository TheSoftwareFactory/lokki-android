package cc.softwarefactory.lokki.android.models;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by haider on 12.11.2015.
 */
public class Person implements ClusterItem{
    private final Bitmap mProfilePhoto;
    private final String mTitle;
    private final LatLng mPosition;
    private String mSnippet;

    public Person(LatLng position, String name,String time, Bitmap pictureResource) {
        mTitle        = name;
        mProfilePhoto = pictureResource;
        mPosition     = position;
        mSnippet      = time;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    public Bitmap getProfilePhoto(){
        return mProfilePhoto;
    }

    public String getSnippet(){
        return mSnippet;
    }

    public String getTitle(){
        return mTitle;
    }


}
