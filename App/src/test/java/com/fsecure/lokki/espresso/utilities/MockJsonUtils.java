package com.fsecure.lokki.espresso.utilities;


import com.android.support.test.deps.guava.hash.Hashing;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MockJsonUtils {


    public static String getContactsJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject
                .put("test.friend@example.com", new JSONObject().put("id", 1).put("name", "Test Friend"))
                .put("family.member@example.com", new JSONObject().put("id", 2).put("name", "Family Member"))
                .put("work.buddy@example.com", new JSONObject().put("id", 3).put("name", "Work Buddy"))
                .put("mapping", new JSONObject().put("Test Friend", "test.friend@example.com")
                                                .put("Family Member", "family.member@example.com")
                                                .put("Work Buddy", "work.buddy@example.com"));


        return jsonObject.toString();
    }

    public static String getOneContact() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject
                .put("test.friend@example.com", new JSONObject().put("id", 1).put("name", "Test Friend"))
                .put("family.member@example.com", new JSONObject().put("id", 2).put("name", "Family Member"))
                .put("work.buddy@example.com", new JSONObject().put("id", 3).put("name", "Work Buddy"))
                .put("mapping", new JSONObject().put("Test Friend", "test.friend@example.com")
                                                .put("Family Member", "family.member@example.com")
                                                .put("Work Buddy", "work.buddy@example.com"));


        return jsonObject.toString();
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

    public static String getDashboardJsonWithContacts(String... contactEmails) throws JSONException {
        JSONArray canseemeJsonArray = new JSONArray();
        JSONObject icanseeJsonObject = new JSONObject();
        JSONObject idmappingJsonObject = new JSONObject().put(TestUtils.VALUE_TEST_USER_ID, TestUtils.VALUE_TEST_USER_ACCOUNT);

        for (String contactEmail : contactEmails) {
            String contactId = Hashing.sha1().hashString(contactEmail).toString();

            canseemeJsonArray.put(contactId);

            icanseeJsonObject.put(contactId, new JSONObject()
                    .put("battery", "")
                    .put("location", new JSONObject())
                    .put("visibility", true));

            idmappingJsonObject.put(contactId, contactEmail);
        }


        JSONObject jsonObject = new JSONObject();
        jsonObject
                .put("battery", "")
                .put("canseeme", canseemeJsonArray)
                .put("icansee", icanseeJsonObject)
                .put("idmapping", idmappingJsonObject)
                .put("location", new JSONObject())
                .put("visibility", true);

        return jsonObject.toString();
    }

    public static String getEmptyPlacesJson() throws JSONException {
        return "{}";
    }

}
