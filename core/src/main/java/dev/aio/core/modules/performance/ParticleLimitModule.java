package dev.aio.core.modules.performance;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.NumberSetting;

/** Partikel-Limit: nur ein Teil der Partikel wird erzeugt (Explosionen, Farmen, Regen, …). */
public final class ParticleLimitModule extends Module {
    private final NumberSetting percent;
    private final java.util.Random random = new java.util.Random();

    public ParticleLimitModule() {
        super("particlelimit", "Partikel-Limit", "Weniger Partikel für mehr FPS bei Explosionen und Farmen", Category.PERFORMANCE, false);
        percent = add(new NumberSetting("percent", "Partikel behalten", 50, 0, 100, 5, " %"));
    }

    /** Ob ein neuer Partikel erzeugt werden soll. */
    public boolean allow() {
        if (!isActive()) return true;
        double p = percent.get() / 100.0;
        return p >= 1 || random.nextDouble() < p;
    }
}
