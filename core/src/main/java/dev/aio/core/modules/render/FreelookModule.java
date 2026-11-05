package dev.aio.core.modules.render;

import dev.aio.core.i18n.Lang;

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
        super("freelook", "Freelook", "Look around freely without turning your player", Category.VISUALS, Keys.LEFT_ALT);
        thirdPerson = add(new BooleanSetting("thirdPerson", "Switch to third person", true));
        invertPitch = add(new BooleanSetting("invertPitch", "Invert vertical", false));
    }

    private boolean warned;

    @Override protected void onEngage() {
        if (!platform().freelookSupported() && !warned) {
            warned = true;
            platform().notify(Lang.tr("aio.ui.freelook.freelook_camera_hook_not_available", "Freelook: camera hook not available in this version, disabled"));
        }
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
        // Ohne funktionierenden Kamera-Hook würde die Sicht sonst "einfrieren"
        if (!isEngaged() || !platform().freelookSupported()) return false;
        yaw += (float) dx * 0.15f;
        pitch += (float) (invertPitch.on() ? -dy : dy) * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch));
        return true;
    }

    public float cameraYaw() { return yaw; }

    public float cameraPitch() { return pitch; }
}
