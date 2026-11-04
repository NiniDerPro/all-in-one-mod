package dev.aio.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.aio.core.platform.Keys;

/** Tastenbelegung als Minecraft-Tastenname (siehe {@link Keys}); leer = keine. */
public class KeybindSetting extends Setting<String> {
    public KeybindSetting(String id, String name, String defaultKey) {
        super(id, name, defaultKey);
    }

    public String key() { return value; }

    public boolean isBound() { return !Keys.isNone(value); }

    public boolean matches(String key) { return isBound() && value.equals(key); }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) set(json.getAsString());
    }
}
