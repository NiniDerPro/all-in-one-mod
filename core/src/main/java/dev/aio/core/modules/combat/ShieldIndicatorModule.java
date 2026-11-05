package dev.aio.core.modules.combat;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;

/**
 * Shield Indicator (wie "Shield Statuses"): Das Schild färbt sich je nach Zustand.
 * <ul>
 *   <li>Rot: deaktiviert (von einer Axt getroffen). Beim eigenen Schild blendet die Farbe aus,
 *       während der Cooldown abläuft.</li>
 *   <li>Grün: blockt gerade (Treffer von vorne werden abgewehrt).</li>
 *   <li>Gelb: wird gerade hochgenommen, blockt aber noch nicht.</li>
 * </ul>
 * Für andere Spieler schickt der Server den Cooldown nicht mit: Trifft man selbst jemanden, der
 * blockt, mit einer Axt, wird "deaktiviert" für 5 Sekunden vorhergesagt.
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

    /** Mischt die Farbe mit Weiß: t = 1 volle Farbe, t = 0 normales Schild. */
    public static int fade(int argb, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (255 + (((argb >> 16) & 0xFF) - 255) * t);
        int g = (int) (255 + (((argb >> 8) & 0xFF) - 255) * t);
        int b = (int) (255 + ((argb & 0xFF) - 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
