package ru.feryafox.wakatimewrapper.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

public class Parser {
    public static Map parseUrlEncoded(String response) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        String[] pairs = response.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            map.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return map;
    }

    public static Map<String, Object> parseJson(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        return parseJsonObject(jsonObject);
    }

    private static Map<String, Object> parseJsonObject(JSONObject jsonObject) throws JSONException {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                value = parseJsonObject((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = parseJsonArray((JSONArray) value);
            }
            map.put(key, value);
        }
        return map;
    }

    private static List<Object> parseJsonArray(JSONArray jsonArray) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                value = parseJsonObject((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = parseJsonArray((JSONArray) value);
            }
            list.add(value);
        }
        return list;
    }
}
