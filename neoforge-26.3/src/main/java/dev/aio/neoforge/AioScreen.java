package dev.aio.neoforge;

import dev.aio.core.ui.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Wraps a version-independent {@link UiScreen} in an MC screen. */
public final class AioScreen extends Screen {
    private final UiScreen ui;
    private final Screen parent;
    private final NeoForgeRenderer renderer;

    /** @param parent screen to return to with {@link UiScreen#returnsToPrevious()} */
    public AioScreen(UiScreen ui, Screen parent) {
        super(Component.literal(ui.title()));
        this.ui = ui;
        this.parent = collapse(parent, ui);
        this.renderer = new NeoForgeRenderer(Minecraft.getInstance());
    }

    /** The same screen type directly (e.g. recipe → recipe) replaces itself instead of lengthening the chain. */
    private static Screen collapse(Screen parent, UiScreen ui) {
        return parent instanceof AioScreen a && a.ui.getClass() == ui.getClass() ? a.parent : parent;
    }

    /** Called on every display, also when returning to this screen. */
    @Override protected void init() {
        super.init();
        // Invisible text field with focus: since 26.3 (SDL) Minecraft only delivers typed characters
        // while a text field is focused. The characters still go to our screen (charTyped).
        EditBox textInput = new EditBox(font, -1000, -1000, 1, 1, Component.empty());
        textInput.setFocused(true);
        setFocused(textInput);
        ui.onOpen();
    }

    @Override public void onClose() {
        if (ui.replacesParent() && parent != null) {
            parent.onClose(); // to where the replaced vanilla screen would have led
            return;
        }
        if (ui.returnsToPrevious() && parent != null) Minecraft.getInstance().gui.setScreen(parent);
        else super.onClose();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        renderer.begin(graphics);
        ui.render(renderer, mouseX, mouseY, delta);
    }

    /** No vanilla background/blur: the UiScreens draw their own. */
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {}

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return ui.mouseClicked(event.x(), event.y(), NeoForgePlatform.button(event.button()));
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        return ui.mouseReleased(event.x(), event.y(), NeoForgePlatform.button(event.button()));
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return ui.mouseDragged(event.x(), event.y(), NeoForgePlatform.button(event.button()), deltaX, deltaY);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return ui.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (ui.keyPressed(NeoForgePlatform.keyName(event))) return true;
        return super.keyPressed(event); // Escape closes
    }

    @Override public boolean charTyped(CharacterEvent event) {
        return ui.charTyped(event.codepointAsString());
    }

    @Override public boolean isPauseScreen() { return false; }

    /** Called on every leave (Escape, another screen, ...). */
    @Override public void removed() { ui.onClose(); }

    public UiScreen ui() { return ui; }
}
