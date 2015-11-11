package cc.softwarefactory.lokki.android.models;


import java.util.Date;

public class User extends JSONModel {
    public static class Location extends JSONModel {
        private double lat;
        private double lon;
        private double acc;
        private Date time;

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

        public double getAcc() {
            return acc;
        }

        public void setAcc(double acc) {
            this.acc = acc;
        }

        public Date getTime() {
            return time;
        }

        public void setTime(Date time) {
            this.time = time;
        }
    }

    private String battery;
    private Location location;
    private boolean visibility;

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public boolean isVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }
}
