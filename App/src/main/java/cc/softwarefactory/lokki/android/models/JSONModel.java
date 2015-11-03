package cc.softwarefactory.lokki.android.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Base class for models that are parsed from JSON.
 */
public class JSONModel {

    public static <T> T createFromJson(String json, Class<T> clazz) throws IOException {
        return (T) new ObjectMapper().readValue(json, clazz);
    }

    public String serialize() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    public JSONObject toJSONObject() throws JsonProcessingException, JSONException {
        return new JSONObject(this.serialize());
    }
}
