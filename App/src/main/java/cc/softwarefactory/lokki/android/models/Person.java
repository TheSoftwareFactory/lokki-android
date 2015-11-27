package cc.softwarefactory.lokki.android.models;

import android.graphics.Bitmap;
import android.location.Location;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Date;

public abstract class Person implements ClusterItem {
    private String userId;
    private String email;
    private UserLocation location;
    private Bitmap photo;
    private Bitmap markerPhoto;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserLocation getLocation() {
        return location;
    }

    public void setLocation(UserLocation location) {
        this.location = location;
    }

    public void setLocationFromAndroidLocation(Location location) {
        UserLocation userLocation = new UserLocation();
        userLocation.setLat(location.getLatitude());
        userLocation.setLon(location.getLongitude());
        userLocation.setAcc(location.getAccuracy());
        userLocation.setTime(new Date(location.getTime()));
        this.location = userLocation;
    }

    @Override
    @JsonIgnore
    public LatLng getPosition() {
        if (location == null) return null;
        return new LatLng(location.getLat(), location.getLon());
    }

    public Bitmap getPhoto() {
        return photo;
    }

    public void setPhoto(Bitmap photo) {
        this.photo = photo;
    }

    public Bitmap getMarkerPhoto() {
        return markerPhoto;
    }

    public void setMarkerPhoto(Bitmap markerPhoto) {
        this.markerPhoto = markerPhoto;
    }
}
