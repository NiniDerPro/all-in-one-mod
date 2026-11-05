package dev.aio.core.modules.player;

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
 * Tool Saver: Werkzeuge und Waffen mit wenig Haltbarkeit werden geschützt. Wahlweise lassen sie
 * sich nicht mehr benutzen (Abbauen, Schlagen, Benutzen) und/oder es kommt ein Warnton.
 * Rüstung lässt sich nicht "nicht benutzen", dafür gibt es nur die Warnung.
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
    /** Items, für die schon gewarnt wurde (ID + Name + Schaden), damit es nicht dauernd piept. */
    private final Set<String> warned = new HashSet<>();
    private long lastBlockedNotice;

    public ToolSaverModule() {
        super("toolsaver", "Tool Saver", "Stops using tools and weapons before they break, with a warning sound", Category.QOL, false);
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

    /** Ist das Item im geschützten Bereich? */
    public boolean isLow(ItemRef item) {
        if (item == null || item.isEmpty() || !item.isDamageable()) return false;
        int left = item.durability();
        if (unit.get() == Unit.PERCENT) return left * 100.0 / item.maxDamage() <= threshold.get();
        return left <= threshold.intValue();
    }

    /** Soll die Benutzung dieses Items gerade verhindert werden? */
    public boolean shouldBlock(ItemRef item) {
        if (!isActive() || !block.on() || !isLow(item)) return false;
        return !(sneakOverride.on() && platform().isGameKeyDown(GameKey.SNEAK));
    }

    /** Auto Tool / Auto Weapon sollen geschützte Items gar nicht erst wählen. */
    public boolean avoid(ItemRef item) { return isActive() && block.on() && isLow(item); }

    /** Wurde blockiert: kurz Bescheid geben (höchstens alle 2 s). */
    public void onBlocked(ItemRef item) {
        long now = System.currentTimeMillis();
        if (now - lastBlockedNotice < 2000) return;
        lastBlockedNotice = now;
        platform().notify(item.name() + " protected: only " + item.durability() + " durability left");
        if (warn.on()) platform().playSound(sound.get().id, (float) (volume.get() / 100.0), 0.8f);
    }

    private void tick() {
        if (!platform().inWorld()) return;
        check(platform().mainHand(), true);
        check(platform().offHand(), true);
        if (armor.on()) for (ItemRef a : platform().armor()) check(a, false);
    }

    /** Beim Unterschreiten der Grenze einmal warnen. */
    private void check(ItemRef item, boolean tool) {
        if (item == null || item.isEmpty() || !item.isDamageable()) return;
        String key = item.id() + "|" + item.name();
        if (!isLow(item)) {
            warned.remove(key);
            return;
        }
        if (!warned.add(key)) return;
        platform().notify((tool ? "" : "Armor: ") + item.name() + " is almost broken (" + item.durability() + " left)");
        if (warn.on()) platform().playSound(sound.get().id, (float) (volume.get() / 100.0), 1.0f);
    }
}
