package dev.aio.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class StringSetting extends Setting<String> {
    private final int maxLength;

    public StringSetting(String id, String name, String defaultValue, int maxLength) {
        super(id, name, defaultValue);
        this.maxLength = maxLength;
    }

    public int maxLength() { return maxLength; }

    @Override protected String sanitize(String v) {
        if (v == null) return null;
        return v.length() > maxLength ? v.substring(0, maxLength) : v;
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) set(json.getAsString());
    }
}
