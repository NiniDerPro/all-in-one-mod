package dev.aio.core.setting;

import dev.aio.core.i18n.Lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

/** Auswahl aus einer Enum. Die Enum kann {@link Named} implementieren, um einen Anzeigenamen zu liefern. */
public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    public interface Named {
        String displayName();
    }

    private final E[] values;

    public EnumSetting(String id, String name, E defaultValue) {
        super(id, name, defaultValue);
        this.values = defaultValue.getDeclaringClass().getEnumConstants();
    }

    public E[] values() { return values; }

    public void cycle(int direction) {
        int i = Math.floorMod(value.ordinal() + direction, values.length);
        set(values[i]);
    }

    public String display() { return label(value); }

    /** Translated name of an enum constant ({@code aio.enum.<Type>.<constant>}). */
    public static String label(Enum<?> e) {
        String fallback = e instanceof Named n ? n.displayName() : e.name();
        return Lang.tr(key(e), fallback);
    }

    public static String key(Enum<?> e) {
        return "aio.enum." + e.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT)
                + "." + e.name().toLowerCase(Locale.ROOT);
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value.name()); }

    @Override public void fromJson(JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) return;
        String s = json.getAsString();
        for (E e : values) {
            if (e.name().equals(s)) { set(e); return; }
        }
    }
}
