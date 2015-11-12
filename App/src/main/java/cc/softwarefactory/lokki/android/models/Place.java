package cc.softwarefactory.lokki.android.models;

public class Place extends JSONModel implements Comparable<Place> {
    private String id;
    private String name;
    private double lat;
    private double lon;
    private int rad;
    private String img;

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
