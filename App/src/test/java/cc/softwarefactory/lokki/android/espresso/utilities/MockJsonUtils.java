package cc.softwarefactory.lokki.android.espresso.utilities;


import com.android.support.test.deps.guava.hash.Hashing;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.models.Place;
import cc.softwarefactory.lokki.android.models.UserLocation;


public class MockJsonUtils {


    public static String getContactsJson() throws JSONException, JsonProcessingException {
        Contact contact = new Contact();
        contact.setEmail("test.friend@example.com");
        contact.setName("Test Friend");
        contact.setCanSeeMe(true);

        Contact contact2 = new Contact();
        contact2.setEmail("family.member@example.com");
        contact2.setName("Family Member");
        contact2.setCanSeeMe(true);

        Contact contact3 = new Contact();
        contact3.setEmail("work.buddy@example.com");
        contact3.setName("Work Buddy");
        contact3.setCanSeeMe(true);

        return getContactsJsonWith(new Contact[] {contact, contact2, contact3});
    }


    public static List<Place> getPlaces() {
        List<Place> places = new ArrayList<>();

        Place place = new Place();
        place.setId("cb693820-3ce7-4c95-af2f-1f079d2841b1");
        UserLocation location = new UserLocation();
        location.setLat(37.483477313364574);
        location.setLon(-122.14838393032551);
        location.setAcc(100);
        place.setUserLocation(location);
        place.setName("Testplace1");
        place.setImg("");

        Place place2 = new Place();
        place2.setId("105df9a7-33cc-4880-9001-66aab110c3dd");
        UserLocation location2 = new UserLocation();
        location2.setLat(40.2290817553899);
        location2.setLon(-116.64331555366516);
        location2.setAcc(100);
        place2.setUserLocation(location2);
        place2.setName("Testplace2");
        place2.setImg("");

        places.add(place);
        places.add(place2);

        return places;
    }

    public static String getPlacesJson() throws JSONException, JsonProcessingException {
        return new ObjectMapper().writeValueAsString(getPlaces());
    }

    public static String getEmptyDashboardJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject
                .put("battery", "")
                .put("canseeme", new JSONArray())
                .put("icansee", new JSONObject())
                .put("idmapping", new JSONObject().put(TestUtils.VALUE_TEST_USER_ID, TestUtils.VALUE_TEST_USER_ACCOUNT))
                .put("location", new JSONObject())
                .put("visibility", true);

        return jsonObject.toString();
    }

    public static String getContactsJsonWith(Contact... contacts) throws JSONException, JsonProcessingException {
        JSONArray array = new JSONArray();
        for (Contact contact : contacts) {
            JSONObject contactJSON = new JSONObject();
            contactJSON.put("email", contact.getEmail());
            contactJSON.put("userId", contact.getUserId());
            contactJSON.put("name", contact.getName());
            contactJSON.put("canSeeMe", contact.isCanSeeMe());
            contactJSON.put("isIgnored", contact.isIgnored());
            if (contact.getLocation() != null) {
                UserLocation userLocation = contact.getLocation();
                JSONObject location = new JSONObject();
                location.put("acc", userLocation.getAcc());
                location.put("lat", userLocation.getLat());
                location.put("lon", userLocation.getLon());
                location.put("time", userLocation.getTime());
                contactJSON.put("location", location);
            }
            array.put(contactJSON);
        }

        return array.toString();

    }

    public static Contact createContact(String email) {
        Contact contact = new Contact();
        contact.setEmail(email);
        contact.setUserId(Hashing.sha1().hashString(email).toString());
        contact.setCanSeeMe(true);
        return contact;
    }

    public static String getDashboardJsonContactsUserLocation(String[] contactEmails, JSONObject[] locations, JSONObject userLocation) throws JSONException {
        if (contactEmails.length != locations.length) {
            return "parameters must be equal";
        }
        JSONArray canseemeJsonArray = new JSONArray();
        JSONObject icanseeJsonObject = new JSONObject();
        JSONObject idmappingJsonObject = new JSONObject().put(TestUtils.VALUE_TEST_USER_ID, TestUtils.VALUE_TEST_USER_ACCOUNT);

        for (String contactEmail : contactEmails) {
            String contactId = Hashing.sha1().hashString(contactEmail).toString();

            canseemeJsonArray.put(contactId);

            icanseeJsonObject.put(contactId, new JSONObject()
                    .put("battery", "")
                    .put("location", userLocation)
                    .put("visibility", true));

            idmappingJsonObject.put(contactId, contactEmail);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject
                .put("battery", "")
                .put("canseeme", canseemeJsonArray)
                .put("icansee", icanseeJsonObject)
                .put("idmapping", idmappingJsonObject)
                .put("location", locations[0])
                .put("visibility", true);

        return jsonObject.toString();
    }

    public static String getEmptyPlacesJson() {
        return "{}";
    }

    public static String getSignUpResponse(String id, String[] canSeeMe, String[] iCanSee, String authorizationToken) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject
                .put("id", id)
                .put("canseeme", canSeeMe)
                .put("icansee", iCanSee)
                .put("authorizationtoken", authorizationToken);

        return jsonObject.toString();
    }

}
