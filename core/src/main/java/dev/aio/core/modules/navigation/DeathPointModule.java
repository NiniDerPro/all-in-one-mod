package dev.aio.core.modules.navigation;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Vec3;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

/** Setzt beim Tod automatisch einen Wegpunkt und entfernt ihn, sobald man wieder dort ist. */
public final class DeathPointModule extends Module {
    private final NumberSetting arriveDistance;
    private final BooleanSetting chatMessage;
    private boolean wasDead;

    public DeathPointModule() {
        super("deathpoint", "Death Waypoint", "Waypoint where you died, removed when you get there", Category.NAVIGATION, false);
        arriveDistance = add(new NumberSetting("arrive", "Remove within", 4, 1, 20, 1, " blocks"));
        chatMessage = add(new BooleanSetting("message", "Announce coordinates", true));
        on(Events.Tick.class, e -> tick());
    }


    private WaypointsModule waypoints() { return core.module(WaypointsModule.class); }

    private void tick() {
        if (!platform().inWorld()) return;
        boolean dead = platform().isDead();
        if (dead && !wasDead) onDeath();
        wasDead = dead;
        if (!dead) checkArrival();
    }

    private void onDeath() {
        WaypointsModule wp = waypoints();
        if (wp == null) return;
        // nur ein Todespunkt pro Welt: alten ersetzen
        for (Waypoint w : wp.inThisWorld()) if (w.death()) wp.remove(w);
        Vec3 p = platform().playerPos();
        int x = (int) Math.floor(p.x()), y = (int) Math.floor(p.y()), z = (int) Math.floor(p.z());
        wp.add(new Waypoint("Death", x, y, z, platform().worldKey(), platform().dimension(), 0xFF5555, true, true));
        if (chatMessage.on()) platform().notify("Died at " + x + " " + y + " " + z);
    }

    private void checkArrival() {
        WaypointsModule wp = waypoints();
        if (wp == null) return;
        Vec3 p = platform().playerPos();
        String dim = platform().dimension();
        for (Waypoint w : wp.inThisWorld()) {
            if (w.death() && w.dimension().equals(dim) && w.distanceTo(p.x(), p.y(), p.z()) <= arriveDistance.get()) {
                wp.remove(w);
                platform().notify("Death waypoint reached");
            }
        }
    }
}
