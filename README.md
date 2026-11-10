# AIO Client

An all-in-one **client-side** mod for **Minecraft 26.3 (Fabric)**: 50+ small modules in one clean menu —
HUD, PvP helpers, quality-of-life tweaks and information overlays. Every module can be switched on or
off on its own, and a module costs nothing while it is off.

> Client-side only. Install it on your own game, not on a server.

## Highlights

- **HUD**: FPS, CPS, keystrokes, armor, potion effects, coordinates, ping, server info, time/date,
  arrow and totem counters — all movable in the in-game **HUD editor**.
- **Camera**: zoom, freelook, freecam, fullbright, no hurt-cam, clearer fog/water.
- **Navigation**: compass, waypoints (with a waypoint menu), death points, portal calculator.
  Waypoints shared in chat in the Xaero format are recognised and can be added with one click.
- **Info**: look-at info (blocks and mobs), villager trades info, food values, item info, shulker previews.
- **Inventory**: item browser with recipes and uses, mouse tweaks, sorting, tool saver, auto tool/sword.
- **Chat**: timestamps, long history, copy, resizable chat, chat heads, name alerts.
- **Performance**: entity culling, particle control, XP orb clumping, more FPS options.
- **Menu**: search, profiles, config export/import, rainbow or custom accent colour.

## Install

1. Install Fabric Loader (0.16+) and **Fabric API** for Minecraft 26.3 (Java 25).
2. Put the `aio-client-*.jar` into your `mods` folder.
3. Start the game. Open the menu with **Right Shift** (or the **AIO** button in the title and pause menu).

## Works with your other mods

If another installed mod already provides a feature (for example a minimap/waypoint mod), AIO turns the
overlapping module **off** and tells you. A conflict only disables the affected module; it never crashes
the game.

## Fair play / servers

Some modules (zoom, freecam, fullbright, nametags, light level, auto tool …) are considered an advantage
on some servers. **Check the rules of the server you play on.** The *Server compatibility* setting
(Client settings) can block such modules, automatically or only on servers you list.

## Privacy

AIO keeps a small statistics file **on your computer only** (`config/aio/stats.json`) and never shows or
sends it by itself. On the first start AIO asks whether you want to share **anonymous** usage statistics
(via bStats): **Okay** turns it on, **No thanks** keeps it off. It is **off by default** and can be changed
anytime under *Client → Send anonymous usage statistics*. No names, accounts, IPs, servers, coordinates or
chat are ever sent. Details: [`docs/TELEMETRY.md`](docs/TELEMETRY.md).

## Building

```
./gradlew build
```

The jar is written to `fabric-26.3/build/libs/`. Architecture and how to add a module:
[`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md).

## License

MIT — see [`LICENSE`](LICENSE). Parts of the Freecam module are adapted from Freecam (MIT); the food
overlay follows AppleSkin's look (public domain). See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
Not affiliated with Mojang or Microsoft.
