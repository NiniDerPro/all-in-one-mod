package dev.aio.core.modules.info;

import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Category;
import dev.aio.core.modules.navigation.Projection;
import dev.aio.core.platform.CameraView;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.WorldLabel;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * Dorfbewohner-Info: zeigt über Dorfbewohnern, welche Verzauberungen (z. B. Reparatur) sie als
 * Buch anbieten und zu welchem Preis. Angebote sind bekannt, sobald man das Handelsfenster einmal
 * geöffnet hat (Server schicken sie erst dann).
 */
public final class VillagerInfoModule extends HudElement {
    private final BooleanSetting booksOnly;
    private final BooleanSetting firstOnly;
    private final BooleanSetting hideSoldOut;
    private final NumberSetting distance;

    public VillagerInfoModule() {
        super("villagerinfo", "Villager Info", "Enchanted books and prices above villagers", Category.QOL, false, 0, 0);
        booksOnly = add(new BooleanSetting("booksOnly", "Only enchanted books", true));
        firstOnly = add(new BooleanSetting("firstOnly", "Only the first book", false));
        hideSoldOut = add(new BooleanSetting("hideSoldOut", "Hide sold out", false));
        distance = add(new NumberSetting("distance", "Range", 24, 4, 64, 1, " blocks"));
    }


    @Override public boolean editable() { return false; }

    @Override protected boolean drawsDefaultBackground() { return false; }

    @Override public int width(Renderer r) { return 1; }

    @Override public int height(Renderer r) { return 1; }

    @Override protected void renderContent(Renderer r, boolean preview) {}

    @Override public boolean hasContent() { return platform().inWorld() && !platform().isScreenOpen(); }

    @Override public void renderOverlay(Renderer r, float partialTick) {
        if (!isActive() || !hasContent()) return;
        List<WorldLabel> labels = platform().villagerLabels(distance.get(), partialTick);
        if (labels.isEmpty()) return;
        CameraView cam = platform().camera(partialTick);
        float w = r.screenWidth(), h = r.screenHeight();
        for (WorldLabel label : labels) {
            List<WorldLabel.Line> lines = filter(label.lines());
            if (lines.isEmpty()) continue;
            Projection.Point pt = Projection.project(label.x(), label.y(), label.z(), cam.x(), cam.y(), cam.z(),
                    cam.yaw(), cam.pitch(), cam.fov(), w, h);
            if (!pt.onScreen()) continue;
            // Weiter weg etwas kleiner, wie Namensschilder
            float scale = (float) Math.max(0.6, Math.min(1.0, 8.0 / Math.max(1, label.distance())));
            int lh = r.fontHeight() + 1;
            float total = lines.size() * lh;
            r.push();
            r.translate(pt.x(), pt.y());
            r.scale(scale);
            float y = -total;
            for (WorldLabel.Line line : lines) {
                float tw = r.textWidth(line.text());
                r.fill(-tw / 2 - 2, y - 1, tw / 2 + 2, y + lh - 1, 0x80000000);
                r.text(line.text(), -tw / 2, y, line.color(), shadow());
                y += lh;
            }
            r.pop();
        }
    }

    private List<WorldLabel.Line> filter(List<WorldLabel.Line> lines) {
        List<WorldLabel.Line> out = new ArrayList<>();
        for (WorldLabel.Line l : lines) {
            if (booksOnly.on() && !l.book()) continue;
            if (hideSoldOut.on() && l.color() == SOLD_OUT_COLOR) continue;
            out.add(l);
            if (firstOnly.on() && l.book()) break;
        }
        return out;
    }

    /** Farbe, mit der der Adapter ausverkaufte Angebote kennzeichnet. */
    public static final int SOLD_OUT_COLOR = 0xFF888888;
}
