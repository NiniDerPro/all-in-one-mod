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
    private final FabricRenderer renderer;

    public AioScreen(UiScreen ui) {
        super(Component.literal(ui.title()));
        this.ui = ui;
        this.renderer = new FabricRenderer(Minecraft.getInstance());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        renderer.begin(graphics);
        ui.render(renderer, mouseX, mouseY, delta);
    }

    /** Kein Vanilla-Hintergrund/Blur: Die UiScreens zeichnen ihren eigenen. */
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {}

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return ui.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        return ui.mouseReleased(event.x(), event.y(), event.button());
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return ui.mouseDragged(event.x(), event.y(), event.button(), deltaX, deltaY);
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
