package dev.aio.core.module;

import dev.aio.core.AioCore;
import dev.aio.core.event.EventBus;
import dev.aio.core.event.Subscription;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Platform;
import dev.aio.core.setting.KeybindSetting;
import dev.aio.core.setting.Setting;
import dev.aio.core.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Basisklasse für jedes Feature.
 *
 * <p>Ein Modul meldet seine Listener im Konstruktor über {@link #on} an. Am Bus hängen
 * sie aber nur, solange das Modul <b>aktiv</b> ist, also eingeschaltet und nicht vom
 * Server-Kompatibilitätsmodus gesperrt. Ausgeschaltete Module kosten dadurch nichts.
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
     * @param gameplayAffecting true für Module, die auf Servern als Vorteil gelten können
     *                          (Freelook, Zoom, Fullbright, Toggle Sprint). Diese sperrt der
     *                          Server-Kompatibilitätsmodus.
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

    // ---- Registrierung (im Konstruktor von Unterklassen) ----

    protected <S extends Setting<?>> S add(S setting) {
        for (Setting<?> s : settings) {
            if (s.id().equals(setting.id())) throw new IllegalArgumentException("Doppelte Setting-ID " + setting.id() + " in " + id);
        }
        settings.add(setting);
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

    /** Wird von der Registry aufgerufen. */
    public final void attach(AioCore core) {
        this.core = core;
        onAttach();
        updateActive();
    }

    /** Einmal nach dem Registrieren, bevor das Modul aktiv werden kann. */
    protected void onAttach() {}

    /** Wird aufgerufen, wenn das Modul aktiv wird (Listener hängen dann schon). */
    protected void onActivate() {}

    /** Wird aufgerufen, wenn das Modul inaktiv wird (Listener sind dann schon weg). */
    protected void onDeactivate() {}

    public void setEnabled(boolean value) {
        if (!canToggle()) value = true;
        if (enabled == value) return;
        enabled = value;
        markDirty();
        updateActive();
    }

    public void toggle() { setEnabled(!enabled); }

    public void setBlocked(boolean value) {
        if (blocked == value) return;
        blocked = value;
        updateActive();
    }

    private void updateActive() {
        boolean shouldBeActive = enabled && !blocked && core != null;
        if (shouldBeActive == active) return;
        active = shouldBeActive;
        if (active) {
            for (ListenerDecl<?> d : listenerDecls) subscriptions.add(d.subscribe(core.bus()));
            try {
                onActivate();
            } catch (RuntimeException e) {
                // Ein fehlerhaftes Modul darf nie das Spiel abbrechen: Modul wieder aushängen.
                Log.error("Modul " + id + " konnte nicht aktiviert werden", e);
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
                Log.error("Modul " + id + " konnte nicht sauber deaktiviert werden", e);
            }
        }
    }

    protected void markDirty() {
        if (core != null) core.config().markDirty();
    }

    protected Platform platform() { return core.platform(); }

    // ---- Abfragen ----

    public String id() { return id; }

    public String name() { return name; }

    public String description() { return description; }

    public Category category() { return category; }

    public boolean isGameplayAffecting() { return gameplayAffecting; }

    /** Vom Spieler gewünschter Zustand (wird gespeichert). */
    public boolean isEnabled() { return enabled; }

    /** Vom Kompatibilitätsmodus gesperrt. */
    public boolean isBlocked() { return blocked; }

    /** Tatsächlich laufend: eingeschaltet und nicht gesperrt. */
    public boolean isActive() { return active; }

    /** Module wie die Client-Einstellungen lassen sich nicht ausschalten. */
    public boolean canToggle() { return true; }

    /** Bei der einmaligen Umstellung auf neue Standards (siehe ConfigManager.DEFAULTS_VERSION). */
    public void applyNewDefaults() {}

    /** Nach dem Laden der Config aufgerufen, z. B. um alte Standardwerte umzustellen. */
    public void afterLoad() {}

    public KeybindSetting toggleKey() { return toggleKey; }

    public List<Setting<?>> settings() { return Collections.unmodifiableList(settings); }

    public Setting<?> setting(String settingId) {
        for (Setting<?> s : settings) if (s.id().equals(settingId)) return s;
        return null;
    }

    /** Ob das Modul standardmäßig eingeschaltet ist, wenn noch keine Config existiert. */
    public boolean enabledByDefault() { return !canToggle() || DEFAULT_ON.contains(id); }

    /** Module, die bei einer neuen Installation an sind (alles andere ist aus). */
    public static final java.util.Set<String> DEFAULT_ON = java.util.Set.of(
            "armorhud", "lookat", "chat", "helditeminfo", "dynamicfps", "elytraswap", "entityculling", "foodinfo",
            "freelook", "fullbright", "itemlist", "invtweaks", "coordinates", "namealert", "shulkerpreview",
            "keybinds", "xpclumps", "zoom", "potions");

    /** Setzt alles auf Standard (ohne Speichern auszulösen, das übernimmt der Aufrufer). */
    public void resetToDefaults() {
        resetSettings();
        setEnabled(enabledByDefault());
    }

    /** Setzt nur die Einstellungen zurück, der Ein/Aus-Zustand bleibt. */
    public void resetSettings() {
        for (Setting<?> s : settings) s.reset();
    }
}
