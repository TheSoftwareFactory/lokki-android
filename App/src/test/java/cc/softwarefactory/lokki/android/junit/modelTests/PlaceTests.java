package cc.softwarefactory.lokki.android.junit.modelTests;

/**
 * Created on 28.4.2016 for the unit testing on functions of Place class
 */

import com.google.android.gms.maps.model.LatLng;
import org.junit.Test;
import cc.softwarefactory.lokki.android.models.Place;
import cc.softwarefactory.lokki.android.models.UserLocation;
import static org.junit.Assert.*;

public class PlaceTests {

    Place placeObj = new Place();
    private String id = "QWERTY";
    private String name = "Kumpula";
    private Double lat = 2.123;
    private Double lng = 1.234;
    private LatLng Uslocation = new LatLng(lat,lng);
    private UserLocation location = new UserLocation(Uslocation,1);
    Place.Buzz BuzzObj = new Place.Buzz();

    @Test
    public void setIdGetIdTest() {          // Test to check the setID() & getID() methods
        placeObj.setId(id);
        assertEquals(placeObj.getId(), id);
    }

    @Test
    public void setNameGetNameTest() {      // Test to check the setName() & getName() methods
        placeObj.setName(name);
        assertEquals(placeObj.getName(), name);
    }

    @Test
    public void setLocationGetLocationTest() { // Test to check the setLocation() & getLocation() methods
        placeObj.setLocation(location);
        assertEquals(placeObj.getLocation(), location);
    }

    @Test
    public void setBuzzIsBuzzTest() {       // Test to check the setBuzz() & isBuzz() methods
        boolean buzzStatus = true;
        placeObj.setBuzz(buzzStatus);
        assertEquals(placeObj.isBuzz(), buzzStatus);
    }

    @Test
    public void buzzObjectTest() {          // Test to check the setBuzz() & getBuzz() methods
        Place.Buzz BuzzObj = new Place.Buzz();
        placeObj.setBuzzObject(BuzzObj);
        assertEquals(placeObj.getBuzzObject(), BuzzObj);
    }

    @Test
    public void setGetBuzzCountTest(){      // Test to check the setBuzzCount() & getBuzzCount() methods
        int buzzCount = 5;
        BuzzObj.setBuzzCount(buzzCount);
        assertEquals(BuzzObj.getBuzzCount(), buzzCount);
    }

    @Test
    public void activateBuzzTest(){         // Test to check the setActivated() & isActivated() methods
        BuzzObj.setActivated(true);
        assertEquals(BuzzObj.isActivated(), true);
    }
}
