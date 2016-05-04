package cc.softwarefactory.lokki.android.junit.modelTests;

/**
 * Created on 02.05.2016 for unit testing of User Location class.
 */

import com.google.android.gms.maps.model.LatLng;
import org.junit.Test;
import java.util.Date;
import cc.softwarefactory.lokki.android.models.UserLocation;
import static org.junit.Assert.*;

public class UserLocationTests {

    private double lat = 25.748151;
    private double lon = 61.924110;
    private int acc = 500;
    private Date time = new Date();

    UserLocation ul = new UserLocation(new LatLng(lat, lon), acc);
    android.location.Location cl = new android.location.Location("fused");

    @Test
    public void setLatitudeTest() {     // Test to check the setLat() & getLat() methods
        ul.setLat(lat);
        assertEquals(ul.getLat(), lat, 0.01);
    }

    @Test
    public void setLongitudeTest() {     // Test to check the setLon() & getLon() methods
        ul.setLon(lon);
        assertEquals(ul.getLon(), lon, 0.01);
    }

    @Test
    public void setAccTest() {     // Test to check the setAcc() & getAcc() methods
        ul.setAcc(acc);
        assertEquals(ul.getAcc(), acc);
    }

    @Test
    public void setTimeTest() {     // Test to check the setTime() & getTime() methods
        ul.setTime(time);
        assertEquals(ul.getTime(), time);
    }

    @Test
    public void isEmptyTest() {     // Test to check the isEmpty() methods
        assertEquals(ul.isEmpty(), false);
    }

    @Test
    public void convertedLocationTest() {     // Test to check the convertedLocation() method
        cl.setLatitude(lat);
        cl.setLongitude(lon);
        cl.setAccuracy(acc);
        assertEquals(ul.convertToAndroidLocation().getLatitude(), cl.getLatitude(), 0.01);
        assertEquals(ul.convertToAndroidLocation().getLongitude(), cl.getLongitude(), 0.01);
        assertEquals(ul.convertToAndroidLocation().getAccuracy(), cl.getAccuracy(), 0.01);
    }

}

