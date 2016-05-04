package cc.softwarefactory.lokki.android.junit.modelTests;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Test;

import cc.softwarefactory.lokki.android.models.User;
import cc.softwarefactory.lokki.android.models.UserLocation;

import static org.junit.Assert.assertEquals;

/**
 * Created on 4.5.2016 for the unit testing on functions of User class
 */

public class UserTests {
    private boolean visibility;   // define visibility scope

    User userObj = new User();
    private Double lat = 2.123;
    private Double lng = 1.234;
    private LatLng Uslocation = new LatLng(lat,lng);
    private UserLocation location = new UserLocation(Uslocation,1);

    @Test
    public void setGetVisibilityTest() {    // Test to check the setVisibility() & isVisibility() methods
        boolean visible = true;
        userObj.setVisibility(visible);
        assertEquals(userObj.isVisibility(),visible);
    }

    @Test
    public void setGetLocation(){           // Test to check the setLocation() & getLocation() methods
        userObj.setLocation(location);
        assertEquals(userObj.getLocation(),location);
    }
}
