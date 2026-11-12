package dev.aio.core.modules.navigation;

import dev.aio.core.i18n.Lang;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Vec3;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * Automatically sets a waypoint on death and removes it as soon as you are there again.
 * The point is always saved, even when the module is off; it is only shown while the module is
 * on. That way a death from before also appears after switching on.
 */
public final class DeathPointModule extends Module {
    private final NumberSetting arriveDistance;
    private final BooleanSetting chatMessage;
    private boolean wasDead;

    public DeathPointModule() {
        super("deathpoint", "Death Waypoint", "Marks where you died (works without Waypoints), removed when you get there", Category.QOL, false);
        arriveDistance = add(new NumberSetting("arrive", "Remove within", 4, 1, 20, 1, " blocks"));
        chatMessage = add(new BooleanSetting("message", "Announce coordinates", true));
        // Also works without the Waypoints module: then this module draws the death point itself
        on(Events.RenderHud.class, e -> {
            WaypointsModule wp = waypoints();
            if (wp != null && !wp.isActive()) wp.drawMarkers(e.renderer, e.tickDelta);
        });
    }


    /** Always notice the death, not only when the module is switched on. */
    @Override protected void onAttach() {
        core.bus().subscribe(Events.Tick.class, e -> tick(), 0);
    }

    private WaypointsModule waypoints() { return core.module(WaypointsModule.class); }

    private void tick() {
        if (!platform().inWorld()) return;
        boolean dead = platform().isDead();
        if (dead && !wasDead) onDeath();
        wasDead = dead;
        if (!dead && isActive()) checkArrival();
    }

    /** How many death points stay per world: the newest and the two before it. */
    static final int KEEP = 3;
    static final int OLD_COLOR = 0xAA5555;

    private void onDeath() {
        WaypointsModule wp = waypoints();
        if (wp == null) return;
        // The newest is called "Death", the two before "Old death" (paler); older ones are dropped
        List<Waypoint> deaths = new ArrayList<>();
        for (Waypoint w : wp.inThisWorld()) if (w.death()) deaths.add(w);
        while (deaths.size() >= KEEP) wp.remove(deaths.remove(0));
        for (Waypoint w : deaths) {
            String old = Lang.tr("aio.ui.death_point.old_death", "Old death");
            if (!w.name().equals(old) || w.color() != OLD_COLOR) wp.replace(w, w.withName(old).withColor(OLD_COLOR));
        }
        Vec3 p = platform().playerPos();
        int x = (int) Math.floor(p.x()), y = (int) Math.floor(p.y()), z = (int) Math.floor(p.z());
        wp.add(new Waypoint(Lang.tr("aio.ui.death_point.death", "Death"), x, y, z, platform().worldKey(), platform().dimension(), 0xFF5555, true, true));
        if (isActive() && chatMessage.on()) platform().notify(Lang.tr("aio.ui.death_point.died_at", "Died at %s %s %s", x, y, z));
    }

    private void checkArrival() {
        WaypointsModule wp = waypoints();
        if (wp == null) return;
        Vec3 p = platform().playerPos();
        String dim = platform().dimension();
        for (Waypoint w : wp.inThisWorld()) {
            if (w.death() && w.dimension().equals(dim) && w.distanceTo(p.x(), p.y(), p.z()) <= arriveDistance.get()) {
                wp.remove(w);
                platform().notify(Lang.tr("aio.ui.death_point.death_waypoint_reached", "Death waypoint reached"));
            }
        }
    }
}
