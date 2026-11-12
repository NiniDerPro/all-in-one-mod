package dev.aio.core.modules.render;

import dev.aio.core.i18n.Lang;

import dev.aio.core.module.Category;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Perspective;
import dev.aio.core.setting.BooleanSetting;

/**
 * Freelook: while active, the mouse only turns the camera, not the player.
 * Switches to third person (behind) and jumps back to the previous perspective afterwards.
 *
 * <p>The adapter queries {@link #consumeMouse} (mouse movement) and {@link #cameraYaw()}/{@link #cameraPitch()}.
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
        // Only jump back if the player has not changed the perspective in the meantime.
        if (changedPerspective && platform().perspective() == Perspective.THIRD_PERSON_BACK) {
            platform().setPerspective(previous);
        }
        changedPerspective = false;
    }

    /**
     * Called by the adapter before the mouse movement is applied to the player.
     *
     * @return true if freelook consumed the movement (do not turn the player)
     */
    public boolean consumeMouse(double dx, double dy) {
        // Without a working camera hook the view would "freeze"
        if (!isEngaged() || !platform().freelookSupported()) return false;
        yaw += (float) dx * 0.15f;
        pitch += (float) (invertPitch.on() ? -dy : dy) * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch));
        return true;
    }

    public float cameraYaw() { return yaw; }

    public float cameraPitch() { return pitch; }
}
