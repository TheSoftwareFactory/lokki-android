package cc.softwarefactory.lokki.android.models;

/**
 * Model class for User
 * @author panchamukhi
 */
public class User {


    private UserLocation location; // track user location
    private boolean visibility;   // define visibility scope
    private String battery;      // battery status house keeping info

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
