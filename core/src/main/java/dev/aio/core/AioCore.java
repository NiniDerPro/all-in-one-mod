package dev.aio.core;

import dev.aio.core.compat.ServerCompat;
import dev.aio.core.config.ConfigManager;
import dev.aio.core.event.EventBus;
import dev.aio.core.event.Events;
import dev.aio.core.hud.HudEditorScreen;
import dev.aio.core.hud.HudManager;
import dev.aio.core.i18n.Lang;
import dev.aio.core.module.Module;
import dev.aio.core.module.ModuleRegistry;
import dev.aio.core.modules.chat.ChatModule;
import dev.aio.core.modules.chat.NameAlertModule;
import dev.aio.core.modules.client.ClientSettingsModule;
import dev.aio.core.modules.client.KeyBindsModule;
import dev.aio.core.modules.combat.AutoToolModule;
import dev.aio.core.modules.combat.AutoWeaponModule;
import dev.aio.core.modules.combat.HealthTagsModule;
import dev.aio.core.modules.combat.ShieldIndicatorModule;
import dev.aio.core.modules.hud.ArmorHudModule;
import dev.aio.core.modules.hud.ArrowCounterModule;
import dev.aio.core.modules.hud.CoordinatesModule;
import dev.aio.core.modules.hud.CpsModule;
import dev.aio.core.modules.hud.FpsModule;
import dev.aio.core.modules.hud.HeldItemInfoModule;
import dev.aio.core.modules.hud.KeystrokesModule;
import dev.aio.core.modules.hud.LookAtModule;
import dev.aio.core.modules.hud.PingModule;
import dev.aio.core.modules.hud.PotionStatusModule;
import dev.aio.core.modules.hud.TotemCounterModule;
import dev.aio.core.modules.info.BiomeModule;
import dev.aio.core.modules.info.DateTimeModule;
import dev.aio.core.modules.info.DayCounterModule;
import dev.aio.core.modules.info.PlaytimeModule;
import dev.aio.core.modules.info.ServerInfoModule;
import dev.aio.core.modules.info.VillagerInfoModule;
import dev.aio.core.modules.info.WorldTimeModule;
import dev.aio.core.modules.inventory.InventoryTweaksModule;
import dev.aio.core.modules.inventory.ItemListModule;
import dev.aio.core.modules.inventory.ShulkerPreviewModule;
import dev.aio.core.modules.navigation.CompassModule;
import dev.aio.core.modules.navigation.DeathPointModule;
import dev.aio.core.modules.navigation.PortalCalculatorModule;
import dev.aio.core.modules.navigation.WaypointsModule;
import dev.aio.core.modules.performance.DynamicFpsModule;
import dev.aio.core.modules.performance.EntityCullingModule;
import dev.aio.core.modules.performance.FpsBoostModule;
import dev.aio.core.modules.performance.ParticleLimitModule;
import dev.aio.core.modules.performance.ProfilerChartModule;
import dev.aio.core.modules.performance.RenderDistanceModule;
import dev.aio.core.modules.performance.XpClumpsModule;
import dev.aio.core.modules.player.ElytraSwapModule;
import dev.aio.core.modules.player.FoodInfoModule;
import dev.aio.core.modules.player.ToggleSprintModule;
import dev.aio.core.modules.player.ToolSaverModule;
import dev.aio.core.modules.render.BlockOutlineModule;
import dev.aio.core.modules.render.DynamicLightsModule;
import dev.aio.core.modules.render.FreecamModule;
import dev.aio.core.modules.render.FreelookModule;
import dev.aio.core.modules.render.FullbrightModule;
import dev.aio.core.modules.render.LightOverlayModule;
import dev.aio.core.modules.render.NametagsModule;
import dev.aio.core.modules.render.NoFogModule;
import dev.aio.core.modules.render.NoHurtcamModule;
import dev.aio.core.modules.render.NoPumpkinModule;
import dev.aio.core.modules.render.SmallItemsModule;
import dev.aio.core.modules.render.ZoomModule;
import dev.aio.core.platform.Hotkeys;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Platform;
import dev.aio.core.ui.ClickGuiScreen;
import dev.aio.core.util.ClickTracker;
import dev.aio.core.util.Log;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        modules.register(new NameAlertModule());
        // Info
        modules.register(new BiomeModule());
        modules.register(new DayCounterModule());
        modules.register(new WorldTimeModule());
        modules.register(new DateTimeModule());
        modules.register(new ProfilerChartModule());
        modules.register(new SmallItemsModule());
        modules.register(new ShieldIndicatorModule());
        modules.register(new ToolSaverModule());
        modules.register(new LightOverlayModule());
        modules.register(new HealthTagsModule());
        modules.register(new DynamicLightsModule());
        modules.register(new NametagsModule());
        modules.register(new PlaytimeModule());
        modules.register(new ServerInfoModule());
        modules.register(new VillagerInfoModule());
        // HUD
        modules.register(new TotemCounterModule());
        modules.register(new ArrowCounterModule());
        modules.register(new HeldItemInfoModule());
        // Navigation
        modules.register(new CompassModule());
        modules.register(new WaypointsModule());
        modules.register(new DeathPointModule());
        modules.register(new PortalCalculatorModule());
        // Grafik
        modules.register(new NoFogModule());
        modules.register(new FreecamModule());
        modules.register(new NoHurtcamModule());
        modules.register(new NoPumpkinModule());
        modules.register(new BlockOutlineModule());
        // Inventar
        modules.register(new ShulkerPreviewModule());
        modules.register(new InventoryTweaksModule());
        modules.register(new ItemListModule());
        // Spieler & Kampf
        modules.register(new AutoToolModule());
        modules.register(new AutoWeaponModule());
        modules.register(new ElytraSwapModule());
        modules.register(new KeyBindsModule());
        modules.register(new FoodInfoModule());
        modules.register(new FpsBoostModule());
        modules.register(new EntityCullingModule());
        modules.register(new RenderDistanceModule());
        modules.register(new DynamicFpsModule());
        modules.register(new ParticleLimitModule());
        modules.register(new XpClumpsModule());
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
    private final Hotkeys.Tap tap = new Hotkeys.Tap();

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
        if (Hotkeys.otherComboHeld(platform, key)) return;
        if (Hotkeys.isModifier(key) && isBoundAnywhere(key)) {
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
            platform.notify(Lang.tr("aio.ui.aio_core.blocked_on_this_server_compatibility", "%s: blocked on this server (compatibility mode)", m.name()));
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
    private final Map<String, String> brokenHooks = new HashMap<>();
    private final Set<String> announcedBroken = new HashSet<>();

    /**
     * Setzt für jedes Modul, ob es gerade laufen kann: Eingriff fehlt (immer aus) oder eine andere
     * Mod mit derselben Funktion ist installiert (aus, solange "Prefer other mods" an ist).
     */
    public void applyAvailability() {
        Map<String, String> other = clientSettings.preferOtherMods.on() ? platform.conflictingMods() : Map.of();
        for (Module m : modules.all()) {
            String reason = null;
            if (brokenHooks.containsKey(m.id())) reason = "Not available: " + brokenHooks.get(m.id());
            else if (other.containsKey(m.id())) reason = other.get(m.id()) + " is installed and takes priority";
            m.setUnavailable(reason);
        }
    }

    private void checkHooks() {
        Map<String, String> now;
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
        List<String> names = new ArrayList<>();
        for (String id : now.keySet()) {
            if (!announcedBroken.add(id)) continue;
            Module m = modules.get(id);
            String name = m == null ? id : m.name();
            names.add(name);
            Log.warn("[aio] Module " + name + " turned off: " + now.get(id));
        }
        if (!names.isEmpty()) {
            platform.notify(Lang.tr("aio.ui.aio_core.turned_off_because_another_mod", "%s%s turned off because another mod changes the same part of the game", String.join(", ", names), (names.size() == 1 ? " is" : " are")));
        }
    }

    private void applyCompat(boolean announce) {
        int blocked = compat.apply(clientSettings.compatMode.get(), clientSettings.allowList.get());
        if (announce && blocked > 0) {
            platform.notify(Lang.tr("aio.ui.aio_core.blocked_on_this_server_compatibility_2", "%s%s blocked on this server (compatibility mode)", blocked, (blocked == 1 ? " module" : " modules")));
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
