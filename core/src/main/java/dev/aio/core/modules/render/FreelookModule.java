package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Perspective;
import dev.aio.core.setting.BooleanSetting;

/**
 * Freelook: Solange aktiv, dreht die Maus nur die Kamera, nicht den Spieler.
 * Wechselt dabei auf die 3. Person (hinten) und springt danach zur vorherigen Perspektive zurück.
 *
 * <p>Der Adapter fragt {@link #consumeMouse} (Mausbewegung) und {@link #cameraYaw()}/{@link #cameraPitch()} ab.
 */
public final class FreelookModule extends KeyActivatedModule {
    private final BooleanSetting thirdPerson;
    private final BooleanSetting invertPitch;

    private float yaw;
    private float pitch;
    private Perspective previous;
    private boolean changedPerspective;

    public FreelookModule() {
        super("freelook", "Freelook", "Kamera frei drehen, ohne dass sich der Spieler mitdreht", Category.RENDER, Keys.LEFT_ALT);
        thirdPerson = add(new BooleanSetting("thirdPerson", "In 3. Person wechseln", true));
        invertPitch = add(new BooleanSetting("invertPitch", "Vertikal invertieren", false));
    }

    @Override protected void onEngage() {
        yaw = platform().playerYaw();
        pitch = platform().playerPitch();
        previous = platform().perspective();
        changedPerspective = false;
        if (thirdPerson.on() && previous != Perspective.THIRD_PERSON_BACK) {
            platform().setPerspective(Perspective.THIRD_PERSON_BACK);
            changedPerspective = true;
        }
    }

    @Override protected void onDisengage() {
        // Nur zurückspringen, wenn der Spieler die Perspektive nicht inzwischen selbst geändert hat.
        if (changedPerspective && platform().perspective() == Perspective.THIRD_PERSON_BACK) {
            platform().setPerspective(previous);
        }
        changedPerspective = false;
    }

    /**
     * Vom Adapter aufgerufen, bevor die Mausbewegung auf den Spieler angewendet wird.
     *
     * @return true, wenn Freelook die Bewegung verbraucht hat (Spieler nicht drehen)
     */
    public boolean consumeMouse(double dx, double dy) {
        if (!isEngaged()) return false;
        yaw += (float) dx * 0.15f;
        pitch += (float) (invertPitch.on() ? -dy : dy) * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch));
        return true;
    }

    public float cameraYaw() { return yaw; }

    public float cameraPitch() { return pitch; }
}
