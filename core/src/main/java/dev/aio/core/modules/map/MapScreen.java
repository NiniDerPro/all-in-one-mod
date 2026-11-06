package dev.aio.core.modules.map;

import dev.aio.core.AioCore;
import dev.aio.core.i18n.Lang;
import dev.aio.core.modules.navigation.Waypoint;
import dev.aio.core.modules.navigation.WaypointsModule;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.Vec3;
import dev.aio.core.ui.Draw;
import dev.aio.core.ui.Theme;
import dev.aio.core.ui.UiScreen;

/** Vollbild-Karte: ziehen, zoomen, Rechtsklick setzt einen Wegpunkt, Tab wechselt flach/Diorama. */
public final class MapScreen implements UiScreen {
    private final AioCore core;
    private final MapModule module;
    private double cx, cz;
    private float ppb = 2f;
    private MapView view;

    public MapScreen(AioCore core, MapModule module) {
        this.core = core;
        this.module = module;
        Vec3 p = core.platform().playerPos();
        cx = p.x();
        cz = p.z();
    }

    @Override public String title() { return "Map"; }

    @Override public void render(Renderer r, int mx, int my, float delta) {
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, 0xFF05070A);
        MapStore s = module.store();
        Vec3 p = core.platform().playerPos();
        int refH = s.rgb((int) Math.floor(cx), (int) Math.floor(cz)) == 0 ? (int) p.y()
                : s.height((int) Math.floor(cx), (int) Math.floor(cz));
        view = new MapView(0, 0, w, h, cx, cz, ppb, refH, module.dioramaOn());
        MapRenderer.draw(r, s, view, false);
        module.drawWaypoints(r, view, s, p);
        int ph = s.rgb((int) Math.floor(p.x()), (int) Math.floor(p.z())) == 0 ? (int) p.y() : s.height((int) Math.floor(p.x()), (int) Math.floor(p.z()));
        MapRenderer.player(r, view, view.sx(p.x()), view.sy(p.z(), ph), core.platform().playerYaw());

        int[] at = view.worldAt(s, mx, my);
        String info = at[0] + ", " + at[1];
        int accent = Theme.accent(core);
        float bw = w - 16;
        Draw.round(r, 8, h - 20, 8 + bw, h - 6, 3, 0xE80E1015);
        Draw.roundOutline(r, 8, h - 20, 8 + bw, h - 6, Theme.LINE_STRONG);
        r.text(info, 14, h - 16, accent, false);
        String hint = Lang.tr("aio.ui.map.hint", "Drag: move · Scroll: zoom · Right click: waypoint · Tab: look · Space: center");
        r.text(r.trim(hint, (int) bw - r.textWidth(info) - 30), 14 + r.textWidth(info) + 12, h - 16, Theme.TEXT_DIM, false);
    }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button != 0) return false;
        cx -= dx / ppb;
        cz -= dy / (ppb * (module.dioramaOn() ? MapView.TILT : 1f));
        return true;
    }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        ppb = (float) Math.max(0.25, Math.min(16, ppb * (amount > 0 ? 1.25 : 0.8)));
        return true;
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button != 1 || view == null) return false;
        WaypointsModule wp = core.module(WaypointsModule.class);
        if (wp == null) return false;
        MapStore s = module.store();
        int[] at = view.worldAt(s, x, y);
        int height = s.rgb(at[0], at[1]) == 0 ? view.refH() : s.height(at[0], at[1]) + 1;
        wp.add(new Waypoint(Lang.tr("aio.ui.map.waypoint", "Map %s", wp.inThisWorld().size() + 1), at[0], height, at[1],
                core.platform().worldKey(), core.platform().dimension(), 0x55AAFF, true, false));
        return true;
    }

    @Override public boolean keyPressed(String key) {
        if ("key.keyboard.tab".equals(key)) {
            module.setDiorama(!module.dioramaOn());
            return true;
        }
        if ("key.keyboard.space".equals(key)) {
            Vec3 p = core.platform().playerPos();
            cx = p.x();
            cz = p.z();
            return true;
        }
        return false;
    }
}
