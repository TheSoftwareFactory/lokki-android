package cc.softwarefactory.lokki.android.models;

public class BuzzPlace {
    private String placeId;
    private int buzzCount;
    private boolean activated;

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

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

