package cc.softwarefactory.lokki.android.utilities.map;

/**
 * Created by verne_000 on 16.2.2015.
 */
public enum MapUserTypes {
    User(0),
    Others(1),
    All(2);
    // 0 = user, 1 = others, 3 = all.   <- huh? in the code 2 is used for all

    private int value;

    private MapUserTypes(int value) {
        this.value = value;
    }
}
