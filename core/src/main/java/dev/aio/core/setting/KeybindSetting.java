package dev.aio.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.aio.core.platform.Keys;

/** Tastenbelegung als GLFW-Code; Maustasten siehe {@link Keys#mouse(int)}; {@link Keys#NONE} = keine. */
public class KeybindSetting extends Setting<Integer> {
    public KeybindSetting(String id, String name, int defaultKey) {
        super(id, name, defaultKey);
    }

    public int key() { return value; }

    public boolean isBound() { return value != Keys.NONE; }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) set(json.getAsInt());
    }
}
