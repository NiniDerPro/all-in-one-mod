package dev.aio.core;

import dev.aio.core.compat.ServerCompat;
import dev.aio.core.config.ConfigManager;
import dev.aio.core.event.EventBus;
import dev.aio.core.event.Events;
import dev.aio.core.hud.HudEditorScreen;
import dev.aio.core.hud.HudManager;
import dev.aio.core.module.Module;
import dev.aio.core.module.ModuleRegistry;
import dev.aio.core.modules.chat.ChatModule;
import dev.aio.core.modules.client.ClientSettingsModule;
import dev.aio.core.modules.hud.ArmorHudModule;
import dev.aio.core.modules.hud.CoordinatesModule;
import dev.aio.core.modules.hud.CpsModule;
import dev.aio.core.modules.hud.FpsModule;
import dev.aio.core.modules.hud.KeystrokesModule;
import dev.aio.core.modules.hud.LookAtModule;
import dev.aio.core.modules.hud.PingModule;
import dev.aio.core.modules.hud.PotionStatusModule;
import dev.aio.core.modules.player.ToggleSprintModule;
import dev.aio.core.modules.render.FreelookModule;
import dev.aio.core.modules.render.FullbrightModule;
import dev.aio.core.modules.render.ZoomModule;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Platform;
import dev.aio.core.ui.ClickGuiScreen;
import dev.aio.core.util.ClickTracker;
import dev.aio.core.util.Log;

import java.nio.file.Path;

/**
 * Zentrale des Clients: verbindet Event-Bus, Module, Config, HUD und Server-Kompatibilität.
 * Der Adapter erzeugt genau eine Instanz über {@link #init(Platform, Path)}.
 */
public final class AioCore {
    public static final String NAME = "AIO Client";

    private static AioCore instance;

    private final Platform platform;
    private final EventBus bus = new EventBus();
    private final ModuleRegistry modules = new ModuleRegistry();
    private final ConfigManager config;
    private final HudManager hud;
    private final ServerCompat compat;
    private final ClickTracker clicks;
    private ClientSettingsModule clientSettings;

    public AioCore(Platform platform, Path configDir) {
        this.platform = platform;
        this.config = new ConfigManager(configDir, modules, System::currentTimeMillis);
        this.hud = new HudManager(modules);
        this.compat = new ServerCompat(modules);
        this.clicks = new ClickTracker(bus, System::nanoTime);
    }

    public static AioCore init(Platform platform, Path configDir) {
        if (instance != null) throw new IllegalStateException("AioCore ist bereits initialisiert");
        AioCore core = new AioCore(platform, configDir);
        core.registerDefaultModules();
        core.start();
        instance = core;
        return core;
    }

    /** Zugriff für Mixins. Null, bevor der Client initialisiert ist. */
    public static AioCore get() { return instance; }

    /** Registriert die eingebauten Module. Reihenfolge = Reihenfolge in der GUI. */
    public void registerDefaultModules() {
        clientSettings = modules.register(new ClientSettingsModule());
        modules.register(new FpsModule());
        modules.register(new CpsModule());
        modules.register(new KeystrokesModule());
        modules.register(new ArmorHudModule());
        modules.register(new CoordinatesModule());
        modules.register(new PingModule());
        modules.register(new PotionStatusModule());
        modules.register(new LookAtModule());
        modules.register(new FreelookModule());
        modules.register(new ZoomModule());
        modules.register(new FullbrightModule());
        modules.register(new ToggleSprintModule());
        modules.register(new ChatModule());
        modules.register(new dev.aio.core.modules.chat.NameAlertModule());
        // Info
        modules.register(new dev.aio.core.modules.info.BiomeModule());
        modules.register(new dev.aio.core.modules.info.DayCounterModule());
        modules.register(new dev.aio.core.modules.info.WorldTimeModule());
        modules.register(new dev.aio.core.modules.info.DateTimeModule());
        modules.register(new dev.aio.core.modules.performance.ProfilerChartModule());
        modules.register(new dev.aio.core.modules.render.SmallItemsModule());
        modules.register(new dev.aio.core.modules.combat.ShieldIndicatorModule());
        modules.register(new dev.aio.core.modules.info.PlaytimeModule());
        modules.register(new dev.aio.core.modules.info.ServerInfoModule());
        modules.register(new dev.aio.core.modules.info.VillagerInfoModule());
        // HUD
        modules.register(new dev.aio.core.modules.hud.TotemCounterModule());
        modules.register(new dev.aio.core.modules.hud.ArrowCounterModule());
        modules.register(new dev.aio.core.modules.hud.HeldItemInfoModule());
        // Navigation
        modules.register(new dev.aio.core.modules.navigation.CompassModule());
        modules.register(new dev.aio.core.modules.navigation.WaypointsModule());
        modules.register(new dev.aio.core.modules.navigation.DeathPointModule());
        modules.register(new dev.aio.core.modules.navigation.PortalCalculatorModule());
        // Grafik
        modules.register(new dev.aio.core.modules.render.NoFogModule());
        modules.register(new dev.aio.core.modules.render.FreecamModule());
        modules.register(new dev.aio.core.modules.render.NoHurtcamModule());
        modules.register(new dev.aio.core.modules.render.NoPumpkinModule());
        modules.register(new dev.aio.core.modules.render.BlockOutlineModule());
        // Inventar
        modules.register(new dev.aio.core.modules.inventory.ShulkerPreviewModule());
        modules.register(new dev.aio.core.modules.inventory.InventoryTweaksModule());
        modules.register(new dev.aio.core.modules.inventory.ItemListModule());
        // Spieler & Kampf
        modules.register(new dev.aio.core.modules.combat.AutoToolModule());
        modules.register(new dev.aio.core.modules.combat.AutoWeaponModule());
        modules.register(new dev.aio.core.modules.player.ElytraSwapModule());
        modules.register(new dev.aio.core.modules.client.KeyBindsModule());
        modules.register(new dev.aio.core.modules.player.FoodInfoModule());
        modules.register(new dev.aio.core.modules.performance.FpsBoostModule());
        modules.register(new dev.aio.core.modules.performance.EntityCullingModule());
        modules.register(new dev.aio.core.modules.performance.RenderDistanceModule());
        modules.register(new dev.aio.core.modules.performance.DynamicFpsModule());
        modules.register(new dev.aio.core.modules.performance.ParticleLimitModule());
        modules.register(new dev.aio.core.modules.performance.XpClumpsModule());
    }

    /** Hängt alle Module an, lädt die Config und registriert die Kern-Listener. */
    public void start() {
        if (clientSettings == null) clientSettings = modules.register(new ClientSettingsModule());
        for (Module m : modules.all()) m.attach(this);
        config.load();
        hud.init(bus);

        bus.subscribe(Events.Tick.class, e -> config.tick());
        bus.subscribe(Events.Key.class, this::onKey, EventBus.PRIORITY_HIGH);
        bus.subscribe(Events.MouseButton.class, e -> {
            if (e.isPress()) tap.pressed(Keys.mouse(e.button()));
            if (e.isPress() && !e.screenOpen()) handleKeybind(Keys.mouse(e.button()));
        });
        bus.subscribe(Events.ServerJoin.class, e -> {
            compat.setServer(e.address() == null, e.address());
            applyCompat(true);
        });
        bus.subscribe(Events.ServerLeave.class, e -> {
            compat.setServer(true, null);
            applyCompat(false);
        });
        clientSettings.compatMode.onChange(v -> applyCompat(true));
        clientSettings.allowList.onChange(v -> applyCompat(false));
        applyCompat(false);
        clientSettings.preferOtherMods.onChange(v -> applyAvailability());
        applyAvailability();
        // Ob alle Eingriffe greifen, steht erst fest, wenn die Welt geladen ist: kurz danach prüfen
        bus.subscribe(Events.ServerJoin.class, e -> hookCheckIn = 60);
        bus.subscribe(Events.Tick.class, e -> {
            if (hookCheckIn > 0 && --hookCheckIn == 0) checkHooks();
        });
        Log.info(NAME + " started with " + modules.all().size() + " modules (profile: " + config.activeProfile() + ")");
    }

    /**
     * Alles auf Standard: jedes Modul erst aus (damit z. B. FPS Boost seine gemerkten Vanilla-Werte
     * zurückschreibt), dann Einstellungen zurück und Standard-Ein/Aus. Wegpunkte bleiben erhalten.
     */
    public void resetAll() {
        for (Module m : modules.all()) if (m != clientSettings) m.setEnabled(false);
        for (Module m : modules.all()) {
            m.resetSettings();
            m.afterLoad();
        }
        for (Module m : modules.all()) m.setEnabled(m.enabledByDefault());
        config.save();
        Log.info("All settings reset to default");
    }

    public void shutdown() {
        config.save();
    }

    // ---- Eingaben ----

    /** Strg/Alt/Windows als Hotkey: erst beim Loslassen auslösen, wenn nichts dazwischen kam. */
    private final dev.aio.core.platform.Hotkeys.Tap tap = new dev.aio.core.platform.Hotkeys.Tap();

    private void onKey(Events.Key e) {
        if (e.isRelease()) {
            if (tap.released(e.key(), platform.windowFocused()) && !e.screenOpen()) fireKeybind(e.key());
            return;
        }
        if (!e.isPress()) return;
        tap.pressed(e.key());
        if (e.screenOpen()) return;
        handleKeybind(e.key());
    }

    private void handleKeybind(String key) {
        if (Keys.isNone(key)) return;
        // Nur allein gedrückte Hotkeys zählen (F3+F4 löst F4 nicht aus)
        if (dev.aio.core.platform.Hotkeys.otherComboHeld(platform, key)) return;
        if (dev.aio.core.platform.Hotkeys.isModifier(key) && isBoundAnywhere(key)) {
            tap.arm(key);
            return;
        }
        fireKeybind(key);
    }

    private boolean isBoundAnywhere(String key) {
        if (clientSettings.guiKey.matches(key) || clientSettings.hudEditorKey.matches(key)) return true;
        for (Module m : modules.all()) if (m.canToggle() && m.toggleKey().matches(key)) return true;
        return false;
    }

    private void fireKeybind(String key) {
        if (clientSettings.guiKey.matches(key)) {
            openClickGui(null);
            return;
        }
        if (clientSettings.hudEditorKey.matches(key)) {
            openHudEditor();
            return;
        }
        for (Module m : modules.all()) {
            if (!m.canToggle() || !m.toggleKey().matches(key)) continue;
            toggleWithFeedback(m);
        }
    }

    /** Schaltet ein Modul um und meldet das dem Spieler (oder warum es gesperrt ist). */
    public void toggleWithFeedback(Module m) {
        if (m.unavailableReason() != null) {
            platform.notify(m.name() + ": " + m.unavailableReason());
            return;
        }
        if (m.isBlocked() && !m.isEnabled()) {
            m.setEnabled(true);
            platform.notify(m.name() + ": blocked on this server (compatibility mode)");
            return;
        }
        m.toggle();
        if (clientSettings.toggleMessages.on()) {
            platform.notify(m.name() + ": " + (m.isEnabled() ? (m.isBlocked() ? "on (blocked)" : "on") : "off"));
        }
    }

    // ---- Screens ----

    public void openClickGui(Module focus) {
        platform.openScreen(new ClickGuiScreen(this, focus));
    }

    public void openHudEditor() {
        if (!platform.inWorld()) return;
        platform.openScreen(new HudEditorScreen(this));
    }

    // ---- Server-Kompatibilität ----

    // ---- Verträglichkeit mit anderen Mods ----

    private int hookCheckIn;
    private final java.util.Map<String, String> brokenHooks = new java.util.HashMap<>();
    private final java.util.Set<String> announcedBroken = new java.util.HashSet<>();

    /**
     * Setzt für jedes Modul, ob es gerade laufen kann: Eingriff fehlt (immer aus) oder eine andere
     * Mod mit derselben Funktion ist installiert (aus, solange "Prefer other mods" an ist).
     */
    public void applyAvailability() {
        java.util.Map<String, String> other = clientSettings.preferOtherMods.on() ? platform.conflictingMods() : java.util.Map.of();
        for (Module m : modules.all()) {
            String reason = null;
            if (brokenHooks.containsKey(m.id())) reason = "Not available: " + brokenHooks.get(m.id());
            else if (other.containsKey(m.id())) reason = other.get(m.id()) + " is installed and takes priority";
            m.setUnavailable(reason);
        }
    }

    private void checkHooks() {
        java.util.Map<String, String> now;
        try {
            now = platform.brokenHooks();
        } catch (RuntimeException e) {
            Log.error("Hook check failed", e);
            return;
        }
        boolean changed = !now.equals(brokenHooks);
        brokenHooks.clear();
        brokenHooks.putAll(now);
        if (changed) applyAvailability();
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String id : now.keySet()) {
            if (!announcedBroken.add(id)) continue;
            Module m = modules.get(id);
            String name = m == null ? id : m.name();
            names.add(name);
            Log.warn("[aio] Module " + name + " turned off: " + now.get(id));
        }
        if (!names.isEmpty()) {
            platform.notify(String.join(", ", names) + (names.size() == 1 ? " is" : " are")
                    + " turned off because another mod changes the same part of the game");
        }
    }

    private void applyCompat(boolean announce) {
        int blocked = compat.apply(clientSettings.compatMode.get(), clientSettings.allowList.get());
        if (announce && blocked > 0) {
            platform.notify(blocked + (blocked == 1 ? " module" : " modules") + " blocked on this server (compatibility mode)");
        }
    }

    // ---- Zugriff ----

    public Platform platform() { return platform; }

    public EventBus bus() { return bus; }

    public ModuleRegistry modules() { return modules; }

    public ConfigManager config() { return config; }

    public HudManager hud() { return hud; }

    public ServerCompat compat() { return compat; }

    public ClickTracker clicks() { return clicks; }

    public ClientSettingsModule clientSettings() { return clientSettings; }

    public <M extends Module> M module(Class<M> type) { return modules.get(type); }
}
