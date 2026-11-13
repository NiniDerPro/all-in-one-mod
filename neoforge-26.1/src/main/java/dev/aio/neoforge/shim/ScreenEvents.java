package dev.aio.neoforge.shim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Screen hooks for the adapter, backed by NeoForge's screen events. Listeners are registered per screen in
 * {@link #AFTER_INIT} and are dropped when the screen is initialised again or closed.
 */
public final class ScreenEvents {
    private ScreenEvents() {}

    public interface AfterInit { void afterInit(Minecraft client, Screen screen, int width, int height); }
    public interface AfterExtract { void afterExtract(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta); }
    public interface AfterTick { void afterTick(Screen screen); }
    public interface Remove { void onRemove(Screen screen); }
    public interface AllowMouse { boolean allow(Screen screen, MouseButtonEvent event); }
    public interface AllowDrag { boolean allow(Screen screen, MouseButtonEvent event, double dx, double dy); }
    public interface AfterRelease { boolean after(Screen screen, MouseButtonEvent event, boolean consumed); }
    public interface AllowScroll { boolean allow(Screen screen, double mouseX, double mouseY, double horizontal, double vertical); }
    public interface AllowKey { boolean allow(Screen screen, KeyEvent event); }
    public interface AllowChar { boolean allow(Screen screen, CharacterEvent event); }

    /** A list of listeners with the {@code register} call the adapter code uses. */
    public static final class Hook<T> {
        final List<T> list = new ArrayList<>();
        public void register(T listener) { list.add(listener); }
    }

    static final class PerScreen {
        final Hook<AfterExtract> extract = new Hook<>();
        final Hook<AfterTick> tick = new Hook<>();
        final Hook<Remove> remove = new Hook<>();
        final Hook<AllowMouse> click = new Hook<>(), release = new Hook<>();
        final Hook<AllowDrag> drag = new Hook<>();
        final Hook<AfterRelease> afterRelease = new Hook<>();
        final Hook<AllowScroll> scroll = new Hook<>();
        final Hook<AllowKey> key = new Hook<>();
        final Hook<AllowChar> chr = new Hook<>();
        Consumer<GuiEventListener> addWidget;
    }

    private static final Map<Screen, PerScreen> SCREENS = new WeakHashMap<>();
    private static int heldButton;

    public static final Hook<AfterInit> AFTER_INIT = new Hook<>();

    static PerScreen of(Screen screen) { return SCREENS.computeIfAbsent(screen, s -> new PerScreen()); }

    public static Hook<AfterExtract> afterExtract(Screen screen) { return of(screen).extract; }
    public static Hook<AfterTick> afterTick(Screen screen) { return of(screen).tick; }
    public static Hook<Remove> remove(Screen screen) { return of(screen).remove; }

    /** Connects the hooks with NeoForge once at start. */
    public static void install() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(ScreenEvent.Init.Post.class, e -> {
            Screen screen = e.getScreen();
            SCREENS.remove(screen);
            PerScreen p = of(screen);
            p.addWidget = e::addListener;
            Minecraft mc = Minecraft.getInstance();
            for (AfterInit l : AFTER_INIT.list) l.afterInit(mc, screen, screen.width, screen.height);
            p.addWidget = null;
        });
        bus.addListener(ScreenEvent.Render.Post.class, e -> {
            PerScreen p = SCREENS.get(e.getScreen());
            if (p != null) for (AfterExtract l : p.extract.list) l.afterExtract(e.getScreen(), e.getGuiGraphics(), e.getMouseX(), e.getMouseY(), e.getPartialTick());
        });
        bus.addListener(ClientTickEvent.Post.class, e -> {
            Screen screen = Minecraft.getInstance().screen;
            PerScreen p = screen == null ? null : SCREENS.get(screen);
            if (p != null) for (AfterTick l : p.tick.list) l.afterTick(screen);
        });
        bus.addListener(ScreenEvent.Closing.class, e -> {
            PerScreen p = SCREENS.remove(e.getScreen());
            if (p != null) for (Remove l : p.remove.list) l.onRemove(e.getScreen());
        });
        bus.addListener(ScreenEvent.MouseButtonPressed.Pre.class, e -> {
            heldButton = e.getButton();
            PerScreen p = SCREENS.get(e.getScreen());
            if (p == null) return;
            for (AllowMouse l : p.click.list) if (!l.allow(e.getScreen(), e.getMouseButtonEvent())) { e.setCanceled(true); return; }
        });
        bus.addListener(ScreenEvent.MouseButtonReleased.Pre.class, e -> {
            PerScreen p = SCREENS.get(e.getScreen());
            if (p == null) return;
            for (AllowMouse l : p.release.list) if (!l.allow(e.getScreen(), e.getMouseButtonEvent())) { e.setCanceled(true); return; }
        });
        bus.addListener(ScreenEvent.MouseButtonReleased.Post.class, e -> {
            PerScreen p = SCREENS.get(e.getScreen());
            if (p == null) return;
            for (AfterRelease l : p.afterRelease.list) l.after(e.getScreen(), e.getMouseButtonEvent(), e.wasReleaseHandled());
        });
        bus.addListener(ScreenEvent.MouseDragged.Pre.class, e -> {
            PerScreen p = SCREENS.get(e.getScreen());
            if (p == null) return;
            for (AllowDrag l : p.drag.list) if (!l.allow(e.getScreen(), e.getMouseButtonEvent(), e.getDragX(), e.getDragY())) { e.setCanceled(true); return; }
        });
        bus.addListener(ScreenEvent.MouseScrolled.Pre.class, e -> {
            PerScreen p = SCREENS.get(e.getScreen());
            if (p == null) return;
            for (AllowScroll l : p.scroll.list) if (!l.allow(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getScrollDeltaX(), e.getScrollDeltaY())) { e.setCanceled(true); return; }
        });
        bus.addListener(ScreenEvent.KeyPressed.Pre.class, e -> {
            PerScreen p = SCREENS.get(e.getScreen());
            if (p == null) return;
            for (AllowKey l : p.key.list) if (!l.allow(e.getScreen(), e.getKeyEvent())) { e.setCanceled(true); return; }
        });
        bus.addListener(ScreenEvent.CharacterTyped.Pre.class, e -> {
            PerScreen p = SCREENS.get(e.getScreen());
            if (p == null) return;
            for (AllowChar l : p.chr.list) if (!l.allow(e.getScreen(), e.getCharacterEvent())) { e.setCanceled(true); return; }
        });
    }
}
