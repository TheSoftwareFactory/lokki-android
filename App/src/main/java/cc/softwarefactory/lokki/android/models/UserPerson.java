package cc.softwarefactory.lokki.android.models;

import android.location.Location;

import java.util.Date;

// TODO: This is a bad name. Once User can be removed, we should decide better names for Person and UserPerson.
public class UserPerson {
    private String userId;
    private String email;
    private UserLocation location;

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

    public void setLocation(Location location) {
        UserLocation userLocation = new UserLocation();
        userLocation.setLat(location.getLatitude());
        userLocation.setLon(location.getLongitude());
        userLocation.setAcc(location.getAccuracy());
        userLocation.setTime(new Date(location.getTime()));
        this.location = userLocation;
    }
}
