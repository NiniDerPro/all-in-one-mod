package dev.aio.core.compat;

import dev.aio.core.module.Module;
import dev.aio.core.module.ModuleRegistry;

import java.util.Locale;

/**
 * Server compatibility switch: blocks modules that may count as a gameplay advantage
 * ({@link Module#isGameplayAffecting()}), automatically depending on mode and server.
 *
 * <p>The player's desired state is kept. When leaving the server,
 * the modules run as before again.
 */
public final class ServerCompat {
    private final ModuleRegistry modules;
    private boolean singleplayer = true;
    private String address;

    public ServerCompat(ModuleRegistry modules) {
        this.modules = modules;
    }

    public void setServer(boolean singleplayer, String address) {
        this.singleplayer = singleplayer;
        this.address = address;
    }

    /** Applies the rules. Returns the number of modules that were newly blocked by it. */
    public int apply(CompatMode mode, String allowList) {
        boolean block = shouldBlock(mode, singleplayer, address, allowList);
        int newlyBlocked = 0;
        for (Module m : modules.all()) {
            if (!m.isGameplayAffecting()) continue;
            if (block && !m.isBlocked() && m.isEnabled()) newlyBlocked++;
            m.setBlocked(block);
        }
        return newlyBlocked;
    }

    public static boolean shouldBlock(CompatMode mode, boolean singleplayer, String address, String allowList) {
        return switch (mode) {
            case OFF -> false;
            case ALWAYS -> true;
            case AUTO -> !singleplayer && address != null && !isAllowed(address, allowList);
        };
    }

    /**
     * Checks the comma-separated allow list. An entry {@code example.net} matches
     * {@code example.net} and all subdomains such as {@code play.example.net}. Ports are ignored.
     */
    public static boolean isAllowed(String address, String allowList) {
        if (address == null || allowList == null || allowList.isBlank()) return false;
        String host = normalizeHost(address);
        if (host.isEmpty()) return false;
        for (String raw : allowList.split(",")) {
            String entry = normalizeHost(raw);
            if (entry.isEmpty()) continue;
            if (host.equals(entry) || host.endsWith("." + entry)) return true;
        }
        return false;
    }

    static String normalizeHost(String s) {
        String h = s.trim().toLowerCase(Locale.ROOT);
        if (h.startsWith("[")) {
            int end = h.indexOf(']');
            return end > 0 ? h.substring(1, end) : h;
        }
        int colon = h.indexOf(':');
        if (colon >= 0 && h.indexOf(':', colon + 1) < 0) h = h.substring(0, colon);
        while (h.endsWith(".")) h = h.substring(0, h.length() - 1);
        return h;
    }

    public boolean isSingleplayer() { return singleplayer; }

    public String address() { return address; }
}
