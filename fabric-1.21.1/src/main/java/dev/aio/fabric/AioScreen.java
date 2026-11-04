package dev.aio.fabric;

import dev.aio.core.ui.UiScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Verpackt einen versionsunabhängigen {@link UiScreen} in einen MC-Screen. */
public final class AioScreen extends Screen {
    private final UiScreen ui;
    private final FabricRenderer renderer;

    public AioScreen(UiScreen ui) {
        super(Text.literal(ui.title()));
        this.ui = ui;
        this.renderer = new FabricRenderer(MinecraftClient.getInstance());
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderer.begin(context);
        ui.render(renderer, mouseX, mouseY, delta);
    }

    /** Kein Vanilla-Hintergrund/Blur: Die UiScreens zeichnen ihren eigenen. */
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return ui.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return ui.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return ui.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return ui.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ui.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers); // Escape schließt
    }

    @Override public boolean charTyped(char chr, int modifiers) {
        return ui.charTyped(chr, modifiers);
    }

    @Override public boolean shouldPause() { return false; }

    /** Wird bei jedem Verlassen aufgerufen (Escape, setScreen auf einen anderen Screen, ...). */
    @Override public void removed() { ui.onClose(); }

    public UiScreen ui() { return ui; }
}
