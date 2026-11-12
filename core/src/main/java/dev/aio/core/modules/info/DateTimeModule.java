package dev.aio.core.modules.info;

import dev.aio.core.hud.HudElement;
import dev.aio.core.hud.HudPart;
import dev.aio.core.module.Category;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.EnumSetting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Real time and current date. Time 24 h (default) or 12 h, date in several formats.
 * Time and date can be moved individually in the HUD editor.
 */
public final class DateTimeModule extends HudElement {
    public enum TimeFormat implements EnumSetting.Named {
        H24("24 hours (13:37)"), H12("12 hours (1:37 PM)");

        private final String displayName;

        TimeFormat(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    public enum DateFormat implements EnumSetting.Named {
        DOTS("04.11.2026", "dd.MM.yyyy"),
        DOTS_SHORT("04.11.26", "dd.MM.yy"),
        SLASH_EU("04/11/2026", "dd/MM/yyyy"),
        SLASH_US("11/04/2026", "MM/dd/yyyy"),
        ISO("2026-11-04", "yyyy-MM-dd"),
        SHORT_TEXT("4 Nov 2026", "d MMM yyyy"),
        WEEKDAY_DOTS("Wed, 04.11.2026", "EEE, dd.MM.yyyy"),
        LONG("Wednesday, 4 November 2026", "EEEE, d MMMM yyyy");

        private final String displayName;
        final DateTimeFormatter formatter;

        DateFormat(String displayName, String pattern) {
            this.displayName = displayName;
            this.formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
        }

        @Override public String displayName() { return displayName; }
    }

    /** Replaceable for tests. */
    static Supplier<LocalDateTime> clock = LocalDateTime::now;

    private final BooleanSetting showTime, showDate, seconds;
    private final EnumSetting<TimeFormat> timeFormat;
    private final EnumSetting<DateFormat> dateFormat;
    private final HudPart timePart, datePart;
    private String time = "", date = "";

    public DateTimeModule() {
        super("datetime", "Time/Date", "Real time and today's date, each movable on its own", Category.HUD, false, 1.0, 0.0);
        showTime = add(new BooleanSetting("time", "Show time", true));
        timeFormat = add(new EnumSetting<>("timeFormat", "Time format", TimeFormat.H24));
        timeFormat.visibleWhen(showTime::on);
        seconds = add(new BooleanSetting("seconds", "Seconds", false));
        seconds.visibleWhen(showTime::on);
        showDate = add(new BooleanSetting("date", "Show date", true));
        dateFormat = add(new EnumSetting<>("dateFormat", "Date format", DateFormat.DOTS));
        dateFormat.visibleWhen(showDate::on);
        timePart = addPart("time", "Time");
        datePart = addPart("date", "Date");
        timeFormat.onChange(v -> update());
        seconds.onChange(v -> update());
        dateFormat.onChange(v -> update());
    }

    @Override protected void onActivate() { update(); }

    @Override public void update() {
        LocalDateTime now = clock.get();
        time = formatTime(now, timeFormat.get(), seconds.on());
        date = now.format(dateFormat.get().formatter);
    }

    static String formatTime(LocalDateTime now, TimeFormat format, boolean seconds) {
        String pattern = format == TimeFormat.H24 ? (seconds ? "HH:mm:ss" : "HH:mm") : (seconds ? "h:mm:ss a" : "h:mm a");
        return now.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
    }

    public String timeText() { return time; }

    public String dateText() { return date; }

    // ---- Connected: one line "13:37  04.11.2026" ----

    private String joined() {
        if (showTime.on() && showDate.on()) return time + "  " + date;
        return showTime.on() ? time : showDate.on() ? date : "";
    }

    @Override public boolean hasContent() { return !joined().isEmpty(); }

    @Override public int width(Renderer r) { return r.textWidth(joined()) + PADDING * 2; }

    @Override public int height(Renderer r) { return r.fontHeight() + PADDING * 2 - 2; }

    @Override protected void renderContent(Renderer r, boolean preview) { drawText(r, joined(), PADDING, PADDING); }

    // ---- Einzeln ----

    private String text(HudPart part) { return part == timePart ? time : date; }

    @Override protected boolean partEnabled(HudPart part) { return part == timePart ? showTime.on() : showDate.on(); }

    @Override protected boolean partHasContent(HudPart part) { return !text(part).isEmpty(); }

    @Override protected int partWidth(Renderer r, HudPart part) { return r.textWidth(text(part)) + PADDING * 2; }

    @Override protected int partHeight(Renderer r, HudPart part) { return height(r); }

    @Override protected float[] partOffset(Renderer r, HudPart part) {
        // Date below the time by default, right-aligned to the block
        if (part == timePart || !showTime.on()) return new float[]{width(r) - partWidth(r, part), 0};
        return new float[]{width(r) - partWidth(r, part), height(r) + 1};
    }

    @Override protected void renderPart(Renderer r, HudPart part, boolean preview) {
        drawText(r, text(part), PADDING, PADDING);
    }
}
