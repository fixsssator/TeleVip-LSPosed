package com.my.televip.obfuscate.struct;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClientObfuscationData {

    private final Map<String, String> classMap = new HashMap<>();
    private final Map<String, String> fieldMap = new HashMap<>();
    private final Map<String, String> methodMap = new HashMap<>();
    private final Map<String, String> methodAliasMap = new HashMap<>();

    public static ClientObfuscationData parse(String clientJson, String clientAliasJson) {
        ClientObfuscationData data = new ClientObfuscationData();
        try {
            JSONObject root = new JSONObject(clientJson);
            data.parseClasses(root.optJSONArray("classes"));
            data.parseFields(root.optJSONArray("fields"));
            data.parseMethods(root.optJSONArray("methods"));
            JSONObject aliasRoot = new JSONObject(clientAliasJson);
            data.parseMethodAlias(aliasRoot.optJSONObject("methodAlias"));
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return data;
    }

    private void parseClasses(JSONArray arr) {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String original = o.optString("o", null);
            String resolved = o.optString("r", null);
            classMap.put(original, resolved);
        }
    }

    private void parseFields(JSONArray arr) {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String className = o.optString("c", null);
            String original = o.optString("o", null);
            String resolved = o.optString("r", null);
            fieldMap.put(className + "#" + original, resolved);
        }
    }

    private void parseMethods(JSONArray arr) {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String className = o.optString("c", null);
            String original = o.optString("o", null);
            String resolved = o.optString("r", null);
            methodMap.put(className + "#" + original, resolved);
        }
    }

    private void parseMethodAlias(JSONObject obj) {
        if (obj == null) return;
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = obj.optString(key, null);
            methodAliasMap.put(key, value);
        }
    }

    public String resolveClass(String original) {
        return classMap.getOrDefault(original, original);
    }

    public String resolveField(String className, String original) {
        return fieldMap.getOrDefault(className + "#" + original, original);
    }

    public String resolveMethod(String className, String original) {
        String aliased = methodAliasMap.getOrDefault(className + "#" + original, original);
        return methodMap.getOrDefault(className + "#" + aliased, aliased);
    }
}