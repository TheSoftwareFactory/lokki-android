package cc.softwarefactory.lokki.android.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model Class for Tracking Place on Map
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Place implements Comparable<Place> {

    private String id;
    private String name;
    private String img;
    private UserLocation location;

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
