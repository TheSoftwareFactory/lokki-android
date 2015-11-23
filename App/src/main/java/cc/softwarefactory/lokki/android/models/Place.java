package cc.softwarefactory.lokki.android.models;

/**
 * Model Class for Tracking Place on Map
 */
public class Place extends JSONModel implements Comparable<Place> {

    private String id; // place id
    private String name; // place name
    private String img;  // Place image
    private UserLocation location; // place location


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


    public UserLocation getLocation() {
        return location;
    }

    public void setLocation(UserLocation location) {
        this.location = location;
    }

    public void setUserLocation(UserLocation location) {
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
