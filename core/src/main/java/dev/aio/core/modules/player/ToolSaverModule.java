package dev.aio.core.modules.player;

import dev.aio.core.i18n.Lang;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.GameKey;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.NumberSetting;

import java.util.HashSet;
import java.util.Set;

/**
 * Tool Saver: tools and weapons with little durability are protected. Optionally they can
 * no longer be used (breaking, hitting, using) and/or a warning sound plays.
 * Armor cannot be "not used", so there is only the warning.
 */
public final class ToolSaverModule extends Module {
    public enum Unit implements EnumSetting.Named {
        POINTS("Durability left"), PERCENT("Percent left");

        private final String displayName;

        Unit(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    public enum Sound implements EnumSetting.Named {
        PLING("Pling", "pling"), BELL("Bell", "bell"), ANVIL("Anvil", "anvil"), XP("Experience", "xp"),
        BASS("Bass", "bass"), BREAK("Item break", "break");

        private final String displayName;
        public final String id;

        Sound(String displayName, String id) {
            this.displayName = displayName;
            this.id = id;
        }

        @Override public String displayName() { return displayName; }
    }

    private final NumberSetting threshold;
    private final EnumSetting<Unit> unit;
    private final BooleanSetting block;
    private final BooleanSetting sneakOverride;
    private final BooleanSetting warn;
    private final EnumSetting<Sound> sound;
    private final NumberSetting volume;
    private final BooleanSetting armor;
    /** Items that were already warned about (id + name + damage), so that it does not beep constantly. */
    private final Set<String> warned = new HashSet<>();
    private long lastBlockedNotice;
    private boolean wasHeld;

    public ToolSaverModule() {
        super("toolsaver", "Tool Saver", "Low tools and weapons stop working instead of breaking: no mining or hitting, with a sound and message on every try", Category.QOL, false);
        threshold = add(new NumberSetting("threshold", "Protect at", 10, 1, 100, 1, ""));
        unit = add(new EnumSetting<>("unit", "Unit", Unit.POINTS));
        block = add(new BooleanSetting("block", "Stop the tool from working", true));
        sneakOverride = add(new BooleanSetting("sneakOverride", "Use anyway while sneaking", false));
        sneakOverride.visibleWhen(block::on);
        warn = add(new BooleanSetting("warn", "Warning sound", true));
        sound = add(new EnumSetting<>("sound", "Sound", Sound.PLING));
        sound.visibleWhen(warn::on);
        volume = add(new NumberSetting("volume", "Volume", 100, 10, 200, 10, " %"));
        volume.visibleWhen(warn::on);
        armor = add(new BooleanSetting("armor", "Warn for armor too", true));
        on(Events.Tick.class, e -> tick());
    }

    /** Is the item in the protected range? */
    public boolean isLow(ItemRef item) {
        if (item == null || item.isEmpty() || !item.isDamageable()) return false;
        int left = item.durability();
        if (unit.get() == Unit.PERCENT) return left * 100.0 / item.maxDamage() <= threshold.get();
        return left <= threshold.intValue();
    }

    /** Should the use of this item be prevented right now? */
    public boolean shouldBlock(ItemRef item) {
        if (!isActive() || !block.on() || !isLow(item)) return false;
        return !(sneakOverride.on() && platform().isGameKeyDown(GameKey.SNEAK));
    }

    /**
     * Was blocked: on every new attempt (clicking again) message and sound. When holding down
     * Minecraft asks again every tick, which counts as the same attempt.
     */
    public void onBlocked(ItemRef item) {
        long now = System.currentTimeMillis();
        boolean held = platform().isGameKeyDown(GameKey.ATTACK) || platform().isGameKeyDown(GameKey.USE);
        boolean sameAttempt = held && wasHeld && now - lastBlockedNotice < 1500;
        wasHeld = held;
        if (sameAttempt) return;
        lastBlockedNotice = now;
        platform().notify(Lang.tr("aio.ui.tool_saver.protected_only_durability_left", "%s protected: only %s durability left", item.name(), item.durability()));
        if (warn.on()) platform().playSound(sound.get().id, (float) (volume.get() / 100.0), 0.8f);
    }

    private void tick() {
        if (!platform().inWorld()) return;
        if (!platform().isGameKeyDown(GameKey.ATTACK) && !platform().isGameKeyDown(GameKey.USE)) wasHeld = false;
        check(platform().mainHand(), true);
        check(platform().offHand(), true);
        if (armor.on()) for (ItemRef a : platform().armor()) check(a, false);
    }

    /** Warn once when the limit is undershot. */
    private void check(ItemRef item, boolean tool) {
        if (item == null || item.isEmpty() || !item.isDamageable()) return;
        String key = item.id() + "|" + item.name();
        if (!isLow(item)) {
            warned.remove(key);
            return;
        }
        if (!warned.add(key)) return;
        platform().notify(Lang.tr("aio.ui.tool_saver.is_almost_broken_left", "%s%s is almost broken (%s left)", (tool ? "" : "Armor: "), item.name(), item.durability()));
        if (warn.on()) platform().playSound(sound.get().id, (float) (volume.get() / 100.0), 1.0f);
    }
}
