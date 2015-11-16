package cc.softwarefactory.lokki.android.models;

import com.google.android.gms.maps.model.LatLng;

public class Place extends JSONModel implements Comparable<Place> {

    private String id;
    private String name;
    private String img;
    private Location location;

    public static class Location {
        private double lat;
        private double lon;
        private int rad;

        public Location() {}

        public Location(LatLng latLng, int rad) {
            this.lat = latLng.latitude;
            this.lon = latLng.longitude;
            this.rad = rad;
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

        public int getRad() {
            return rad;
        }

        public void setRad(int rad) {
            this.rad = rad;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(Place another) {
        return (this.getName().compareTo(another.getName()));
    }
}
