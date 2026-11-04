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
        Log.info(NAME + " gestartet mit " + modules.all().size() + " Modulen (Profil: " + config.activeProfile() + ")");
    }

    public void shutdown() {
        config.save();
    }

    // ---- Eingaben ----

    private void onKey(Events.Key e) {
        if (!e.isPress() || e.screenOpen()) return;
        handleKeybind(e.key());
    }

    private void handleKeybind(String key) {
        if (Keys.isNone(key)) return;
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
        if (m.isBlocked() && !m.isEnabled()) {
            m.setEnabled(true);
            platform.notify(m.name() + ": auf diesem Server gesperrt (Kompatibilitätsmodus)");
            return;
        }
        m.toggle();
        if (clientSettings.toggleMessages.on()) {
            platform.notify(m.name() + ": " + (m.isEnabled() ? (m.isBlocked() ? "an (gesperrt)" : "an") : "aus"));
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

    private void applyCompat(boolean announce) {
        int blocked = compat.apply(clientSettings.compatMode.get(), clientSettings.allowList.get());
        if (announce && blocked > 0) {
            platform.notify(blocked + (blocked == 1 ? " Modul" : " Module") + " auf diesem Server gesperrt (Kompatibilitätsmodus)");
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
