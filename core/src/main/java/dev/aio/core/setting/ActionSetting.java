package dev.aio.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** A button in the module settings that triggers an action. Is not saved. */
public class ActionSetting extends Setting<Boolean> {
    private final Runnable action;

    public ActionSetting(String id, String name, Runnable action) {
        super(id, name, false);
        this.action = action;
    }

    public void run() { action.run(); }

    @Override public JsonElement toJson() { return JsonNull.INSTANCE; }

    @Override public void fromJson(JsonElement json) {}
}
