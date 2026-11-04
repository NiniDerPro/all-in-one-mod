package dev.aio.core.ui;

import dev.aio.core.platform.Renderer;

/**
 * Versionsunabhängiger Screen. Der Adapter verpackt ihn in einen MC-{@code Screen} und
 * reicht Zeichnen und Eingaben 1:1 durch. Koordinaten sind skalierte GUI-Pixel.
 */
public interface UiScreen {
    void render(Renderer r, int mouseX, int mouseY, float delta);

    default boolean mouseClicked(double x, double y, int button) { return false; }

    default boolean mouseReleased(double x, double y, int button) { return false; }

    default boolean mouseDragged(double x, double y, int button, double dx, double dy) { return false; }

    default boolean mouseScrolled(double x, double y, double amount) { return false; }

    /** @param key MC-Tastenname. @return true, wenn die Taste verbraucht wurde. Escape schließt sonst den Screen. */
    default boolean keyPressed(String key) { return false; }

    default boolean charTyped(String text) { return false; }

    default void onClose() {}

    /** Wird bei jedem Anzeigen aufgerufen, auch wenn man zu diesem Screen zurückkehrt. */
    default void onOpen() {}

    /** Ob der Screen beim Schließen zum vorherigen Screen zurückkehrt (z. B. Rezepte → Inventar). */
    default boolean returnsToPrevious() { return false; }

    /**
     * Ersetzt dieser Screen einen Vanilla-Screen (z. B. die Tastenbelegung)? Dann führt Schließen
     * dorthin zurück, wohin der ersetzte Screen geführt hätte.
     */
    default boolean replacesParent() { return false; }

    default String title() { return "AIO"; }
}
