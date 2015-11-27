package cc.softwarefactory.lokki.android.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

/**
 * Created by panchamu on 18.11.2015.
 * Model Class for Tracking Locatin/Places
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLocation {
    private double lat; //latitude
    private double lon; //longitude
    private int acc;    //Accuracy(dist) rounded to nearby integer
    private Date time;  //time required for converting to Andriod Location

    public UserLocation() {}

    /**
     * creates UserLocation from LatLng and accuracy
     * @param latLng
     * @param acc
     */
    public UserLocation(LatLng latLng, int acc) {
        this.lat = latLng.latitude;
        this.lon = latLng.longitude;
        this.acc = acc;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public int getAcc() {
        return acc;
    }

    public void setAcc(float acc) {
        this.acc = (int) acc;
    }

    /**
     * create fused location using AndriodLocation
     * @return convertedLocation
     */
    public android.location.Location convertToAndroidLocation() {
        android.location.Location convertedLocation = new android.location.Location("fused");
        double lat = this.lat;
        double lon = this.lon;
        float acc = (float) this.acc;
        Long time = (this.time == null) ? 0 : this.time.getTime();
        convertedLocation.setLatitude(lat);
        convertedLocation.setLongitude(lon);
        convertedLocation.setAccuracy(acc);
        convertedLocation.setTime(time);
        return convertedLocation;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public boolean isEmpty() {
        return (this.lat == 0 && this.lon == 0 && this.acc == 0  && this.time == null);
    }
}