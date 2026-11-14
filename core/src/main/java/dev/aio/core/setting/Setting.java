package dev.aio.core.setting;

import dev.aio.core.i18n.Lang;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** A single, persistable setting of a module. */
public abstract class Setting<T> {
    private final String id;
    private final String name;
    private final T defaultValue;
    protected T value;
    private String key;
    private boolean hidden;
    private String group;
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

    /** Normalises a value (e.g. clamping). Null means: reject the value. */
    protected T sanitize(T v) { return v; }

    public Setting<T> onChange(Consumer<T> listener) {
        listeners.add(listener);
        return this;
    }

    /** Hides the setting in the GUI. It is still saved, e.g. for the HUD position. */
    public Setting<T> hidden() {
        hidden = true;
        return this;
    }

    /** Puts the setting into a collapsible group in the menu (id, translated by the module). */
    public Setting<T> group(String groupId) {
        group = groupId;
        return this;
    }

    public String group() { return group; }

    /** Shows the setting only if the condition is met. */
    public Setting<T> visibleWhen(BooleanSupplier condition) {
        visibleWhen = condition;
        return this;
    }

    public boolean isVisible() { return !hidden && visibleWhen.getAsBoolean(); }

    /** Internal setting (position, data) that never appears in the menu and is not reset. */
    public boolean isHidden() { return hidden; }

    public abstract JsonElement toJson();

    /** Reads the value from JSON. With invalid content the current value stays. */
    public abstract void fromJson(JsonElement json);
}
