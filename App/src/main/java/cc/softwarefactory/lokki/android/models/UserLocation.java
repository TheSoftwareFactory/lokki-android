package cc.softwarefactory.lokki.android.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

/**
 * Created by panchamu on 18.11.2015.
 */
public class UserLocation {
    private double lat;
    private double lon;
    private int acc;
    private Date time;

    public UserLocation() {}

    public UserLocation(LatLng latLng, int rad) {
        this.lat = latLng.latitude;
        this.lon = latLng.longitude;
        this.acc = rad;
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
        this.acc = (int)acc;
    }

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
}