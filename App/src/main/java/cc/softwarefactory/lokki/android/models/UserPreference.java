package cc.softwarefactory.lokki.android.models;

/**
 * Created by panchamu on 16.11.2015.
 */

public class UserPreference  {

    private String battery;
    private UserLocation location;
    private boolean visibility;
    private  int mapMode;
    private boolean shareUsageData;
    private boolean receiveExpirimentalFeature;

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    public UserLocation getLocation() {
        return location;
    }

    public void setLocation(UserLocation location) {
        this.location = location;
    }

    public boolean isVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public int getMapMode() {
        return mapMode;
    }

    public void setMapMode(int mapMode) {
        this.mapMode = mapMode;
    }

    public boolean isShareUsageData() {
        return shareUsageData;
    }

    public void setShareUsageData(boolean shareUsageData) {
        this.shareUsageData = shareUsageData;
    }

    public boolean isReceiveExpirimentalFeature() {
        return receiveExpirimentalFeature;
    }

    public void setReceiveExpirimentalFeature(boolean receiveExpirimentalFeature) {
        this.receiveExpirimentalFeature = receiveExpirimentalFeature;
    }


}

