package cc.softwarefactory.lokki.android.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Model Class for Tracking Place on Map
 */
public class Place implements Comparable<Place> {
    public static class Buzz {
        private int buzzCount;
        private boolean activated;

        public int getBuzzCount() {
            return buzzCount;
        }

        public void setBuzzCount(int buzzCount) {
            this.buzzCount = buzzCount;
        }

        public void decBuzzCount() {
            if (this.buzzCount > 0)
                buzzCount--;
        }

        public boolean isActivated() {
            return activated;
        }

        public void setActivated(boolean activated) {
            this.activated = activated;
        }
    }

    private String id;
    private String name;
    private String img;
    private UserLocation location;
    private boolean buzz;

    @JsonIgnore
    private Buzz buzzObject;

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

    public boolean isBuzz() {
        return buzz;
    }

    public void setBuzz(boolean buzz) {
        this.buzz = buzz;
    }

    public Buzz getBuzzObject() {
        return buzzObject;
    }

    public void setBuzzObject(Buzz buzzObject) {
        this.buzzObject = buzzObject;
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
