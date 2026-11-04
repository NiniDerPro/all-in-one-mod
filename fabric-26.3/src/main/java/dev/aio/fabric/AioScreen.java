package dev.aio.fabric;

import dev.aio.core.ui.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Verpackt einen versionsunabhängigen {@link UiScreen} in einen MC-Screen. */
public final class AioScreen extends Screen {
    private final UiScreen ui;
    private final Screen parent;
    private final FabricRenderer renderer;

    /** @param parent Screen, zu dem bei {@link UiScreen#returnsToPrevious()} zurückgekehrt wird */
    public AioScreen(UiScreen ui, Screen parent) {
        super(Component.literal(ui.title()));
        this.ui = ui;
        this.parent = collapse(parent, ui);
        this.renderer = new FabricRenderer(Minecraft.getInstance());
    }

    /** Direkt derselbe Screen-Typ (z. B. Rezept → Rezept) ersetzt sich, statt die Kette zu verlängern. */
    private static Screen collapse(Screen parent, UiScreen ui) {
        return parent instanceof AioScreen a && a.ui.getClass() == ui.getClass() ? a.parent : parent;
    }

    /** Wird bei jedem Anzeigen aufgerufen, auch bei der Rückkehr zu diesem Screen. */
    @Override protected void init() {
        super.init();
        ui.onOpen();
    }

    @Override public void onClose() {
        if (ui.returnsToPrevious() && parent != null) Minecraft.getInstance().gui.setScreen(parent);
        else super.onClose();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        renderer.begin(graphics);
        ui.render(renderer, mouseX, mouseY, delta);
    }

    /** Kein Vanilla-Hintergrund/Blur: Die UiScreens zeichnen ihren eigenen. */
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {}

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return ui.mouseClicked(event.x(), event.y(), FabricPlatform.button(event.button()));
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        return ui.mouseReleased(event.x(), event.y(), FabricPlatform.button(event.button()));
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return ui.mouseDragged(event.x(), event.y(), FabricPlatform.button(event.button()), deltaX, deltaY);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return ui.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (ui.keyPressed(FabricPlatform.keyName(event))) return true;
        return super.keyPressed(event); // Escape schließt
    }

    @Override public boolean charTyped(CharacterEvent event) {
        return ui.charTyped(event.codepointAsString());
    }

    @Override public boolean isPauseScreen() { return false; }

    /** Wird bei jedem Verlassen aufgerufen (Escape, anderer Screen, ...). */
    @Override public void removed() { ui.onClose(); }

    public UiScreen ui() { return ui; }
}
