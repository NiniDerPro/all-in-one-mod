package dev.aio.core.modules.render;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Hotkeys;
import dev.aio.core.platform.Keys;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.KeybindSetting;

/**
 * Modul, das (wenn eingeschaltet) über eine eigene Taste kurzzeitig aktiviert wird,
 * z. B. Freelook und Zoom. Modus: halten oder umschalten.
 */
public abstract class KeyActivatedModule extends Module {
    protected final KeybindSetting activationKey;
    protected final EnumSetting<ActivationMode> mode;
    private boolean engaged;

    protected KeyActivatedModule(String id, String name, String description, Category category, String defaultKey) {
        this(id, name, description, category, defaultKey, ActivationMode.HOLD);
    }

    protected KeyActivatedModule(String id, String name, String description, Category category, String defaultKey,
                                 ActivationMode defaultMode) {
        super(id, name, description, category, true);
        activationKey = add(new KeybindSetting("key", "Key", defaultKey));
        mode = add(new EnumSetting<>("mode", "Mode", defaultMode));
        on(Events.Key.class, e -> onInput(e.key(), e.isPress(), e.isRelease(), e.screenOpen()));
        on(Events.MouseButton.class, e -> onInput(Keys.mouse(e.button()), e.isPress(), e.action() == 0, e.screenOpen()));
        on(Events.Tick.class, e -> tickEngaged());
    }


    private final Hotkeys.Tap tap = new Hotkeys.Tap();

    private void onInput(String key, boolean press, boolean release, boolean screenOpen) {
        if (press) tap.pressed(key);
        if (!activationKey.matches(key)) {
            // Kombi dazugedrückt (z. B. Alt halten, dann Tab): gehaltene Aktivierung sofort beenden
            if (press && engaged && mode.get() == ActivationMode.HOLD && Hotkeys.isCombo(key)
                    && platform().isKeyDown(activationKey.key())) {
                setEngaged(false);
            }
            return;
        }
        if (press && !screenOpen) {
            if (Hotkeys.otherComboHeld(platform(), key)) return; // F3+F4 ist nicht F4
            if (mode.get() == ActivationMode.TOGGLE) {
                // Strg/Alt/Windows erst beim Loslassen, damit Alt+Tab nichts umschaltet
                if (Hotkeys.isModifier(key)) tap.arm(key);
                else setEngaged(!engaged);
            } else {
                setEngaged(true);
            }
        } else if (release) {
            if (mode.get() == ActivationMode.HOLD) setEngaged(false);
            else if (tap.released(key, platform().windowFocused()) && !screenOpen) setEngaged(!engaged);
        }
    }

    private void tickEngaged() {
        if (!engaged) return;
        // Loslassen verpasst (z. B. in einem Menü) oder Welt verlassen: sauber beenden.
        if (!platform().inWorld() || (mode.get() == ActivationMode.HOLD && !platform().isKeyDown(activationKey.key()))) {
            setEngaged(false);
        }
    }

    protected final void setEngaged(boolean value) {
        if (engaged == value) return;
        if (value && !platform().inWorld()) return;
        engaged = value;
        if (value) onEngage();
        else onDisengage();
    }

    public boolean isEngaged() { return engaged && isActive(); }

    @Override protected void onDeactivate() { setEngaged(false); }

    protected abstract void onEngage();

    protected abstract void onDisengage();
}
