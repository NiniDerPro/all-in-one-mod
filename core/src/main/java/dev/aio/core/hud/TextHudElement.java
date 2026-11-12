package dev.aio.core.hud;

import dev.aio.core.module.Category;
import dev.aio.core.platform.Renderer;

/**
 * HUD element with one line of text. Subclasses only implement {@link #getText()}.
 *
 * <p>The text is queried once per tick (not per frame). The width is only measured again
 * when the text changed.
 */
public abstract class TextHudElement extends HudElement {
    private String cachedText = "";
    private int cachedWidth = -1;

    protected TextHudElement(String id, String name, String description, double defaultX, double defaultY) {
        super(id, name, description, defaultX, defaultY);
    }

    protected TextHudElement(String id, String name, String description, Category category, boolean gameplayAffecting,
                             double defaultX, double defaultY) {
        super(id, name, description, category, gameplayAffecting, defaultX, defaultY);
    }

    /** The text to show. Empty means: show nothing. */
    protected abstract String getText();

    /** Placeholder for the HUD editor when {@link #getText()} is empty. */
    protected String previewText() { return name(); }

    @Override public void update() { setText(getText()); }

    @Override protected void onActivate() { update(); }

    private void setText(String text) {
        if (text == null) text = "";
        if (!text.equals(cachedText)) {
            cachedText = text;
            cachedWidth = -1;
        }
    }

    protected String displayedText(boolean preview) {
        return preview && cachedText.isEmpty() ? previewText() : cachedText;
    }

    @Override public boolean hasContent() { return !cachedText.isEmpty(); }

    @Override public int width(Renderer r) {
        if (cachedText.isEmpty()) return r.textWidth(previewText()) + PADDING * 2;
        if (cachedWidth < 0) cachedWidth = r.textWidth(cachedText);
        return cachedWidth + PADDING * 2;
    }

    @Override public int height(Renderer r) { return r.fontHeight() + PADDING * 2 - 2; }

    @Override protected void renderContent(Renderer r, boolean preview) {
        drawText(r, displayedText(preview), PADDING, PADDING);
    }
}
