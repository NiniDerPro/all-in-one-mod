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
        bus.subscribe(Events.RenderHud.class, e -> render(e.renderer));
    }

    private void tick() {
        for (HudElement el : elements) if (el.isActive()) el.update();
    }

    private void render(Renderer r) {
        if (editorOpen) return; // der Editor zeichnet die Elemente selbst
        for (HudElement el : elements) if (el.isActive()) el.renderOverlay(r);
        for (HudElement el : elements) if (el.isActive() && el.editable()) el.render(r, false);
    }

    public List<HudElement> elements() { return elements; }

    public void setEditorOpen(boolean open) { editorOpen = open; }

    public boolean isEditorOpen() { return editorOpen; }
}
