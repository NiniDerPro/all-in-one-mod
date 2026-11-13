# AIO ClientSide – All in One

> One mod instead of thirty: HUD, PvP and quality-of-life features in a single menu.

Zoom, Freecam, HUD, waypoints, item browser, villager info, chat tools and more are normally spread over
a pile of separate mods, each with its own menu, keybinds and updates. AIO ClientSide puts them in
**one download** with **one menu**, and you switch on only what you want.

- 50+ modules, each on/off on its own. A module costs nothing while it is off.
- One menu, one search, one HUD editor, one config with profiles.
- Already have a favourite mod for something? **It wins.** AIO turns only the overlapping module off.
- Client-side only. Fabric, NeoForge and Forge, Minecraft 26.3.

## What it replaces

| Instead of a separate mod for… | …use the AIO module |
|---|---|
| FPS / CPS / Keystrokes / Armor HUD | HUD modules |
| Zoom, Freelook, Freecam, Fullbright | Camera modules |
| Waypoints, death point, compass | Navigation modules |
| Look-at info, villager info, food values | Info modules |
| Item list (recipe browser), mouse tweaks, sorting | Inventory modules |
| Chat timestamps, copy, chat heads, resizable chat | Chat modules |
| Entity culling, particle control, XP clumping | Performance modules |

AIO is its own implementation; it does not contain the code of those mods.

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
- **Menu**: search, profiles, config export/import, rainbow or custom accent colour, language selection (English, Deutsch, Español, Français).

## Versions

Each Minecraft version has its own branch; this page belongs to **26.3**. Downloads are on the [Releases](https://github.com/NiniDerPro/all-in-one-mod/releases) page.

| Minecraft | Branch | Fabric | NeoForge | Forge |
|---|---|---|---|---|
| 26.3 | [`26.3`](https://github.com/NiniDerPro/all-in-one-mod/tree/26.3) | ✓ | ✓ | ✓ |
| 26.2 | [`26.2`](https://github.com/NiniDerPro/all-in-one-mod/tree/26.2) | ✓ | ✓ | ✓ |
| 26.1.x | [`26.1`](https://github.com/NiniDerPro/all-in-one-mod/tree/26.1) | ✓ | ✓ | ✓ |
| 1.21.11 | [`1.21.11`](https://github.com/NiniDerPro/all-in-one-mod/tree/1.21.11) | ✓ | – | – |
| 1.21.8 | [`1.21.8`](https://github.com/NiniDerPro/all-in-one-mod/tree/1.21.8) | ✓ | – | – |
| 1.21.3 | [`1.21.3`](https://github.com/NiniDerPro/all-in-one-mod/tree/1.21.3) | ✓ | – | – |
| 1.21.1 | [`1.21.1`](https://github.com/NiniDerPro/all-in-one-mod/tree/1.21.1) | ✓ | – | – |
| 1.20.1 | [`1.20.1`](https://github.com/NiniDerPro/all-in-one-mod/tree/1.20.1) | ✓ | – | – |

Changes per release: [`CHANGELOG.md`](CHANGELOG.md).

## Install

1. Install a loader for Minecraft 26.3 (Java 25): **Fabric** Loader with **Fabric API**, **NeoForge** or **Forge**.
2. Put the matching `aio-clientside-<loader>-*.jar` into your `mods` folder.
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
chat are ever sent. Full privacy information: [`PRIVACY.md`](PRIVACY.md). Technical details: [`TELEMETRY.md`](TELEMETRY.md).

## Building

```
./gradlew build
```

The jar is written to `fabric-26.3/build/libs/`. Architecture and how to add a module:
[`DEVELOPMENT.md`](DEVELOPMENT.md).

## License

MIT — see [`LICENSE`](../LICENSE). Parts of the Freecam module are adapted from Freecam (MIT); the food
overlay follows AppleSkin's look (public domain). See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
Not affiliated with Mojang or Microsoft.
