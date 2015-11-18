package cc.softwarefactory.lokki.android.models;


public class User extends JSONModel {


    private UserLocation location;
    private boolean visibility;
    private String battery;

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    public UserLocation getUserLocation() {
        return location;
    }

    public void setUserLocation(UserLocation location) {
        this.location = location;
    }

    public boolean isVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public UserLocation getLocation() {
        return location;
    }

    public void setLocation(UserLocation location) {
        this.location = location;
    }
}
