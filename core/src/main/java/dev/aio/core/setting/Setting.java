package dev.aio.core.setting;

import dev.aio.core.i18n.Lang;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Eine einzelne, persistierbare Einstellung eines Moduls. */
public abstract class Setting<T> {
    private final String id;
    private final String name;
    private final T defaultValue;
    protected T value;
    private String key;
    private boolean hidden;
    private BooleanSupplier visibleWhen = () -> true;
    private final List<Consumer<T>> listeners = new ArrayList<>(1);

    protected Setting(String id, String name, T defaultValue) {
        this.id = id;
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String id() { return id; }

    /** Label in the menu, translated if the language file has it. */
    public String name() { return key == null ? name : Lang.tr(key, name); }

    /** English label as written in code (source for the language file). */
    public String defaultName() { return name; }

    /** Translation key, set when the setting is added to a module. */
    public String translationKey() { return key; }

    public void bindKey(String key) { this.key = key; }

    public T get() { return value; }

    public T defaultValue() { return defaultValue; }

    public void set(T newValue) {
        T sanitized = sanitize(newValue);
        if (sanitized == null || sanitized.equals(value)) return;
        value = sanitized;
        for (Consumer<T> l : listeners) l.accept(sanitized);
    }

    public void reset() { set(defaultValue); }

    /** Normiert einen Wert (z. B. Clamping). Null heißt: Wert ablehnen. */
    protected T sanitize(T v) { return v; }

    public Setting<T> onChange(Consumer<T> listener) {
        listeners.add(listener);
        return this;
    }

    /** Versteckt die Einstellung in der GUI. Sie wird trotzdem gespeichert, z. B. für die HUD-Position. */
    public Setting<T> hidden() {
        hidden = true;
        return this;
    }

    /** Zeigt die Einstellung nur an, wenn die Bedingung erfüllt ist. */
    public Setting<T> visibleWhen(BooleanSupplier condition) {
        visibleWhen = condition;
        return this;
    }

    public boolean isVisible() { return !hidden && visibleWhen.getAsBoolean(); }

    /** Interne Einstellung (Position, Daten), die nie im Menü erscheint und nicht zurückgesetzt wird. */
    public boolean isHidden() { return hidden; }

    public abstract JsonElement toJson();

    /** Liest den Wert aus JSON. Bei ungültigem Inhalt bleibt der aktuelle Wert stehen. */
    public abstract void fromJson(JsonElement json);
}
