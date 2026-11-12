package dev.aio.core.modules.combat;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;

/**
 * Shield Indicator (like "Shield Statuses"): the shield changes colour depending on its state.
 * <ul>
 *   <li>Red: disabled (hit by an axe). On the own shield the colour fades out
 *       while the cooldown runs down.</li>
 *   <li>Green: currently blocking (hits from the front are blocked).</li>
 *   <li>Yellow: being raised, but not blocking yet.</li>
 * </ul>
 * For other players the server does not send the cooldown: if you yourself hit someone who is
 * blocking with an axe, "disabled" is predicted for 5 seconds.
 */
public final class ShieldIndicatorModule extends Module {
    private final BooleanSetting own, others, predict, showRaising;
    private final ColorSetting disabled, blocking, raising;

    public ShieldIndicatorModule() {
        super("shieldindicator", "Shield Indicator", "Colors shields by state: disabled, blocking, raising", Category.PVP, false);
        own = add(new BooleanSetting("own", "Your shield", true));
        others = add(new BooleanSetting("others", "Other players' shields", true));
        predict = add(new BooleanSetting("predict", "Predict disable on axe hits", true));
        showRaising = add(new BooleanSetting("raising", "Show raising (not blocking yet)", true));
        disabled = add(new ColorSetting("disabledColor", "Disabled", 0xFFFF4040));
        blocking = add(new ColorSetting("blockingColor", "Blocking", 0xFF55FF55));
        raising = add(new ColorSetting("raisingColor", "Raising", 0xFFFFD040));
        raising.visibleWhen(showRaising::on);
    }

    public boolean own() { return isActive() && own.on(); }

    public boolean others() { return isActive() && others.on(); }

    public boolean predict() { return predict.on(); }

    public boolean showRaising() { return showRaising.on(); }

    public int disabledColor() { return disabled.argb(); }

    public int blockingColor() { return blocking.argb(); }

    public int raisingColor() { return raising.argb(); }

    /** Mixes the colour with white: t = 1 full colour, t = 0 normal shield. */
    public static int fade(int argb, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (255 + (((argb >> 16) & 0xFF) - 255) * t);
        int g = (int) (255 + (((argb >> 8) & 0xFF) - 255) * t);
        int b = (int) (255 + ((argb & 0xFF) - 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
