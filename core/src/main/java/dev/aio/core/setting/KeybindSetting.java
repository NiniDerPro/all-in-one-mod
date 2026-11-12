package dev.aio.core.setting;

import dev.aio.core.platform.Hotkeys;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Platform;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Key binding as a Minecraft key name (see {@link Keys}); empty = none. */
public class KeybindSetting extends Setting<String> {
    public KeybindSetting(String id, String name, String defaultKey) {
        super(id, name, defaultKey);
    }

    public String key() { return value; }

    public boolean isBound() { return !Keys.isNone(value); }

    public boolean matches(String key) { return isBound() && value.equals(key); }

    /** Like {@link #matches}, but only without an additionally held combo key (F3+F4 does not count as F4). */
    public boolean triggeredBy(Platform platform, String key) {
        return matches(key) && !Hotkeys.otherComboHeld(platform, key);
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) set(json.getAsString());
    }
}
