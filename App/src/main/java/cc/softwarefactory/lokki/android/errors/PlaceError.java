package cc.softwarefactory.lokki.android.errors;

import cc.softwarefactory.lokki.android.R;

public enum PlaceError {
    PLACE_LIMIT("place_limit_reached", R.string.place_limit_reached),
    NAME_IN_USE("place_name_already_in_use", R.string.place_name_already_in_use),
    TOO_LONG_NAME("place_name_too_long", R.string.place_name_too_long);

    private String name;
    private int errorMessage;

    PlaceError(String name, int errorMessage) {
        this.name = name;
        this.errorMessage = errorMessage;
    }

    private String getName() {
        return name;
    }

    public int getErrorMessage() {
        return errorMessage;
    }

    public static PlaceError getEnum(String serverMessage) {
        for (PlaceError ape : PlaceError.values()) {
            if (ape.getName().equals(serverMessage)) {
                return ape;
            }
        }
        return null;
    }

}
