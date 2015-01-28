package com.fsecure.lokki.utils;

import android.location.Location;

/**
 * Created by verne_000 on 28.1.2015.
 */
public class MapUtils {
    /**
     * Determines if the location should be used or discarded depending on the current accuracy and
     * the distance to the last location
     * @param newLocation New location to be checked
     * @return true if the new location should be used
     */
    public static boolean useNewLocation(Location newLocation, Location lastLocation, long timeInterval ) {
        return (lastLocation == null || (newLocation.getTime() - lastLocation.getTime() > timeInterval) ||
                lastLocation.distanceTo(newLocation) > 5 || lastLocation.getAccuracy() - newLocation.getAccuracy() > 5);
    }
}