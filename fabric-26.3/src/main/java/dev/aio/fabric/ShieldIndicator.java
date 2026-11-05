package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.modules.combat.ShieldIndicatorModule;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shield Indicator: bestimmt die Farbe eines Schilds und reicht sie bis zum Zeichnen durch.
 * Beim Erfassen des Items (mit Besitzer) wird die Farbe an eine eigene Kopie der Item-Daten
 * gehängt; beim späteren Zeichnen des Schildmodells wird sie wieder ausgelesen.
 */
public final class ShieldIndicator {
    private ShieldIndicator() {}

    /** Besitzer des Items, das gerade erfasst wird (verschachtelt möglich). */
    private static final ArrayList<LivingEntity> OWNERS = new ArrayList<>();
    /**
     * Item-Daten mit angehängter Farbe. Eigene Klasse statt Zuordnungstabelle: Minecrafts
     * Item-Daten vergleichen nach Inhalt, zwei gleiche Schilde wären sonst nicht zu unterscheiden.
     */
    public static final class Tinted implements DataComponentMap {
        private final DataComponentMap base;
        final int tint;

        Tinted(DataComponentMap base, int tint) {
            this.base = base;
            this.tint = tint;
        }

        @Override public Set<DataComponentType<?>> keySet() { return base.keySet(); }

        @Override public <T> T get(DataComponentType<? extends T> type) { return base.get(type); }
    }
    /** Vorhersage: Spieler → Zeitpunkt (ms), bis zu dem das Schild als deaktiviert gilt. */
    private static final Map<UUID, Long> PREDICTED = new ConcurrentHashMap<>();
    private static final long DISABLE_MS = 5000;

    /** Farbe, mit der das aktuell gezeichnete Schild eingefärbt wird (-1 = normal). */
    public static int current = -1;

    private static ShieldIndicatorModule module() {
        AioCore core = AioCore.get();
        return core == null ? null : core.module(ShieldIndicatorModule.class);
    }

    public static void pushOwner(LivingEntity owner) { OWNERS.add(owner); }

    public static void popOwner() { if (!OWNERS.isEmpty()) OWNERS.removeLast(); }

    /** Beim Erfassen: ggf. eingefärbte Kopie der Item-Daten zurückgeben. */
    public static DataComponentMap tag(DataComponentMap map) {
        if (map == null || OWNERS.isEmpty()) return map;
        int tint = tintFor(OWNERS.getLast());
        if (tint == -1) return map;
        return new Tinted(map instanceof Tinted t ? t.base : map, tint);
    }

    public static int tintOf(DataComponentMap map) {
        return map instanceof Tinted t ? t.tint : -1;
    }

    private static int tintFor(LivingEntity owner) {
        ShieldIndicatorModule m = module();
        if (m == null || owner == null || !m.isActive()) return -1;
        Minecraft mc = Minecraft.getInstance();
        boolean self = owner == mc.player;
        if (self ? !m.own() : !m.others()) return -1;

        // Deaktiviert: eigener Cooldown (mit Ausblenden) bzw. Vorhersage für andere
        ItemStack shield = shieldOf(owner);
        if (self && shield != null && mc.player.getCooldowns().isOnCooldown(shield)) {
            float left = mc.player.getCooldowns().getCooldownPercent(shield, 0);
            return ShieldIndicatorModule.fade(m.disabledColor(), 0.35f + 0.65f * left);
        }
        if (!self) {
            Long until = PREDICTED.get(owner.getUUID());
            if (until != null) {
                long now = System.currentTimeMillis();
                if (now < until && !owner.isBlocking()) {
                    return ShieldIndicatorModule.fade(m.disabledColor(), 0.35f + 0.65f * (until - now) / (float) DISABLE_MS);
                }
                if (now >= until || owner.isBlocking()) PREDICTED.remove(owner.getUUID());
            }
        }
        if (owner.isBlocking()) return m.blockingColor() | 0xFF000000;
        if (m.showRaising() && owner.isUsingItem() && owner.getUseItem().is(Items.SHIELD)) return m.raisingColor() | 0xFF000000;
        return -1;
    }

    private static ItemStack shieldOf(LivingEntity e) {
        if (e.getOffhandItem().is(Items.SHIELD)) return e.getOffhandItem();
        if (e.getMainHandItem().is(Items.SHIELD)) return e.getMainHandItem();
        return null;
    }

    /** Eigener Angriff: Axt gegen einen blockenden Spieler von vorne → Schild 5 s deaktiviert. */
    public static void onAttack(Player attacker, Entity target) {
        ShieldIndicatorModule m = module();
        if (m == null || !m.isActive() || !m.predict()) return;
        if (!(target instanceof Player p) || !p.isBlocking()) return;
        if (!attacker.getMainHandItem().is(ItemTags.AXES)) return;
        // Schilde wehren nur von vorne ab
        Vec3 look = p.getViewVector(1f);
        Vec3 toAttacker = attacker.position().subtract(p.position()).normalize();
        if (look.x * toAttacker.x + look.z * toAttacker.z <= 0) return;
        PREDICTED.put(p.getUUID(), System.currentTimeMillis() + DISABLE_MS);
    }

    public static void clear() { PREDICTED.clear(); }
}
