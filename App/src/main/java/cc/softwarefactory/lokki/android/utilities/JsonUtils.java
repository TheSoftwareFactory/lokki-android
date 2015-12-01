package cc.softwarefactory.lokki.android.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class JsonUtils {
    public static <T> T createFromJson(String json, Class<T> clazz) throws IOException {
        return (T) new ObjectMapper().readValue(json, clazz);
    }

    public static <T> List<T> createListFromJson(String json, Class<T> clazz) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    public static String serialize(Object object) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(object);
    }

    public static JSONObject toJSONObject(Object object) throws JsonProcessingException, JSONException {
        return new JSONObject(serialize(object));
    }
}
