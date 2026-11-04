package dev.aio.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** Ein Knopf in den Moduleinstellungen, der eine Aktion auslöst. Wird nicht gespeichert. */
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
