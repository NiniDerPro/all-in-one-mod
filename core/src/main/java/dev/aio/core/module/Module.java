package dev.aio.core.module;

import dev.aio.core.AioCore;
import dev.aio.core.event.EventBus;
import dev.aio.core.event.Subscription;
import dev.aio.core.i18n.Lang;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Platform;
import dev.aio.core.setting.KeybindSetting;
import dev.aio.core.setting.Setting;
import dev.aio.core.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Base class for every feature.
 *
 * <p>A module registers its listeners in the constructor via {@link #on}. They are only attached to
 * the bus while the module is <b>active</b>, i.e. switched on and not blocked by the
 * server compatibility mode. Switched-off modules therefore cost nothing.
 */
public abstract class Module {
    private record ListenerDecl<E>(Class<E> type, Consumer<E> handler, int priority) {
        Subscription subscribe(EventBus bus) { return bus.subscribe(type, handler, priority); }
    }

    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final boolean gameplayAffecting;

    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<ListenerDecl<?>> listenerDecls = new ArrayList<>();
    private final List<Subscription> subscriptions = new ArrayList<>();

    protected final KeybindSetting toggleKey;

    private boolean enabled;
    private boolean blocked;
    private boolean active;
    protected AioCore core;

    /**
     * @param gameplayAffecting true for modules that may count as an advantage on servers
     *                          (Freelook, Zoom, Fullbright, Toggle Sprint). The server blocks these
     *                          server compatibility mode.
     */
    protected Module(String id, String name, String description, Category category, boolean gameplayAffecting) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = Category.of(id, category);
        this.gameplayAffecting = gameplayAffecting;
        this.toggleKey = add(new KeybindSetting("toggleKey", "Toggle key", Keys.NONE));
        if (!canToggle()) toggleKey.hidden();
    }

    // ---- Registration (in the constructor of subclasses) ----

    protected <S extends Setting<?>> S add(S setting) {
        for (Setting<?> s : settings) {
            if (s.id().equals(setting.id())) throw new IllegalArgumentException("Doppelte Setting-ID " + setting.id() + " in " + id);
        }
        settings.add(setting);
        setting.bindKey("aio." + id + "." + setting.id());
        setting.onChange(v -> markDirty());
        return setting;
    }

    protected <E> void on(Class<E> type, Consumer<E> handler) {
        on(type, handler, EventBus.PRIORITY_NORMAL);
    }

    protected <E> void on(Class<E> type, Consumer<E> handler, int priority) {
        ListenerDecl<E> decl = new ListenerDecl<>(type, handler, priority);
        listenerDecls.add(decl);
        if (active && core != null) subscriptions.add(decl.subscribe(core.bus()));
    }

    // ---- Lebenszyklus ----

    /** Called by the registry. */
    public final void attach(AioCore core) {
        this.core = core;
        onAttach();
        updateActive();
    }

    /** Once after registering, before the module can become active. */
    protected void onAttach() {}

    /** Called when the module becomes active (listeners are already attached then). */
    protected void onActivate() {}

    /** Called when the module becomes inactive (listeners are already gone then). */
    protected void onDeactivate() {}

    public void setEnabled(boolean value) {
        if (!canToggle()) value = true;
        if (enabled == value) return;
        enabled = value;
        markDirty();
        updateActive();
    }

    public void toggle() { setEnabled(!enabled); }

    /** Reason why the module cannot run right now (another mod takes over / hook missing), otherwise null. */
    private String unavailable;

    /**
     * Makes the module ineffective independent of the on/off switch, e.g. because another mod has the same
     * feature or the needed place in the game was changed by another mod.
     * {@code null} undoes that. The stored on/off state stays untouched.
     */
    public void setUnavailable(String reason) {
        if (Objects.equals(unavailable, reason)) return;
        unavailable = reason;
        updateActive();
    }

    public String unavailableReason() { return unavailable; }

    public void setBlocked(boolean value) {
        if (blocked == value) return;
        blocked = value;
        updateActive();
    }

    private void updateActive() {
        boolean shouldBeActive = enabled && !blocked && unavailable == null && core != null;
        if (shouldBeActive == active) return;
        active = shouldBeActive;
        if (active) {
            for (ListenerDecl<?> d : listenerDecls) subscriptions.add(d.subscribe(core.bus()));
            try {
                onActivate();
            } catch (RuntimeException e) {
                // A faulty module must never abort the game: detach the module again.
                Log.error("Module " + id + " could not be activated", e);
                active = false;
                for (Subscription s : subscriptions) s.cancel();
                subscriptions.clear();
            }
        } else {
            for (Subscription s : subscriptions) s.cancel();
            subscriptions.clear();
            try {
                onDeactivate();
            } catch (RuntimeException e) {
                Log.error("Module " + id + " could not be deactivated cleanly", e);
            }
        }
    }

    protected void markDirty() {
        if (core != null) core.config().markDirty();
    }

    protected Platform platform() { return core.platform(); }

    // ---- Abfragen ----

    public String id() { return id; }

    public String name() { return Lang.tr("aio." + id, name); }

    public String description() { return Lang.tr("aio." + id + ".desc", description); }

    /** English name/description as written in code (source for the language file). */
    public String defaultName() { return name; }

    public String defaultDescription() { return description; }

    public Category category() { return category; }

    public boolean isGameplayAffecting() { return gameplayAffecting; }

    /** State wanted by the player (is stored). */
    public boolean isEnabled() { return enabled; }

    /** Blocked by the compatibility mode. */
    public boolean isBlocked() { return blocked; }

    /** Actually running: switched on and not blocked. */
    public boolean isActive() { return active; }

    /** Modules like the client settings cannot be switched off. */
    public boolean canToggle() { return true; }

    /** On the one-time switch to new defaults (see ConfigManager.DEFAULTS_VERSION). */
    public void applyNewDefaults() {}

    /** Called after loading the config, e.g. to migrate old default values. */
    public void afterLoad() {}

    /** After loading: adjust the stored on/off once (e.g. when the default changed). */
    public boolean adjustLoadedEnabled(boolean enabled) { return enabled; }

    public KeybindSetting toggleKey() { return toggleKey; }

    public List<Setting<?>> settings() { return Collections.unmodifiableList(settings); }

    public Setting<?> setting(String settingId) {
        for (Setting<?> s : settings) if (s.id().equals(settingId)) return s;
        return null;
    }

    /** Whether the module is on by default when no config exists yet. */
    public boolean enabledByDefault() { return !canToggle() || DEFAULT_ON.contains(id); }

    /** Modules that are on in a new installation (everything else is off). */
    public static final Set<String> DEFAULT_ON = Set.of(
            "armorhud", "lookat", "flexchat", "helditeminfo", "dynamicfps", "elytraswap", "entityculling", "foodinfo",
            "freelook", "fullbright", "itemlist", "invtweaks", "coordinates", "namealert", "shulkerpreview",
            "keybinds", "xpclumps", "zoom", "potions");

    /** Resets everything to default (without triggering a save, the caller does that). */
    public void resetToDefaults() {
        resetSettings();
        setEnabled(enabledByDefault());
    }

    /** Only resets the settings, the on/off state stays. */
    public void resetSettings() {
        for (Setting<?> s : settings) s.reset();
    }
}
