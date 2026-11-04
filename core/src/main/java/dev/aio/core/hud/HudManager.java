package dev.aio.core.hud;

import dev.aio.core.event.EventBus;
import dev.aio.core.event.Events;
import dev.aio.core.module.ModuleRegistry;
import dev.aio.core.platform.Renderer;

import java.util.List;

/** Zeichnet alle aktiven HUD-Elemente und versorgt sie pro Tick mit Updates. */
public final class HudManager {
    private final ModuleRegistry modules;
    private List<HudElement> elements = List.of();
    private boolean editorOpen;

    public HudManager(ModuleRegistry modules) {
        this.modules = modules;
    }

    public void init(EventBus bus) {
        elements = modules.ofType(HudElement.class);
        bus.subscribe(Events.Tick.class, e -> tick());
        bus.subscribe(Events.RenderHud.class, e -> render(e.renderer, e.tickDelta));
    }

    private void tick() {
        for (HudElement el : elements) if (el.isActive()) el.update();
    }

    private void render(Renderer r, float partialTick) {
        placeNew(r);
        if (editorOpen) return; // der Editor zeichnet die Elemente selbst
        for (HudElement el : elements) if (el.isActive()) el.renderOverlay(r, partialTick);
        for (HudElement el : elements) if (el.isActive() && el.editable()) el.render(r, false);
    }

    /**
     * Elemente, die zum ersten Mal eingeschaltet wurden, bekommen einen freien Platz nahe ihrer
     * Standardposition, damit sie nicht über bereits platzierten liegen.
     */
    public void placeNew(Renderer r) {
        for (HudElement el : elements) {
            if (!el.isActive() || !el.editable() || el.isPlaced()) continue;
            java.util.List<Snapper.Rect> others = new java.util.ArrayList<>();
            for (HudElement o : elements) {
                if (o != el && o.isActive() && o.editable() && o.isPlaced()) {
                    others.add(new Snapper.Rect(o.screenX(r), o.screenY(r), o.scaledWidth(r), o.scaledHeight(r)));
                }
            }
            float[] p = Placer.findFree(new Snapper.Rect(el.screenX(r), el.screenY(r), el.scaledWidth(r), el.scaledHeight(r)),
                    others, r.screenWidth(), r.screenHeight());
            el.setScreenPos(r, p[0], p[1]);
            el.markPlaced();
        }
    }

    public List<HudElement> elements() { return elements; }

    public void setEditorOpen(boolean open) { editorOpen = open; }

    public boolean isEditorOpen() { return editorOpen; }
}
