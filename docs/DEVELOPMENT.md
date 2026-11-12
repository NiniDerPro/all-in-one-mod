# AIO ClientSide – developer notes

A modular Minecraft **26.3** (Fabric) client mod with a slim core. Every feature is a module that only
hooks in through the event bus.

## Layout

```
core/                 Version-independent core, NO Minecraft dependency (Gson only)
  event/              Event bus + events (Tick, RenderHud, Key, MouseButton, MouseScroll, ServerJoin/Leave)
  setting/            Boolean, Number, Enum, Color, Keybind, String (JSON-serialisable)
  module/             Module base class, categories, registry
  config/             JSON per module, profiles, export/import, delayed saving
  compat/             Server compatibility mode (blocks advantage modules)
  hud/                HudElement, TextHudElement, HudManager, HUD editor, snapping
  ui/                 Settings GUI (ClickGuiScreen), theme, UiScreen interface
  platform/           Version abstraction: Platform, Renderer, ItemRef, ...
  modules/            The actual features
fabric-26.3/          Thin adapter for MC 26.3 (Fabric/Quilt): Platform/Renderer implementation + mixins
neoforge-26.3/        Placeholder for a NeoForge adapter (no build.gradle yet, skipped by the build)
forge-26.3/           Placeholder for a Forge adapter (same)
server/stats-worker/  Cloudflare Worker for the optional anonymous statistics
```

**Version abstraction:** modules, HUD and GUI only talk to `Platform` and `Renderer`. For a new
Minecraft version only an adapter folder (`fabric-<version>/`) is copied and adapted:
`FabricPlatform`, `FabricRenderer`, `FabricItemRef`, `AioScreen`, `InputState` and a few small mixins.
Keys are stored by their Minecraft names (`key.keyboard.right.shift`, `key.mouse.left`), not numeric
codes, so configs stay valid when the game switches between GLFW and SDL.

Branches: `main` holds the overview; the code lives in one branch per Minecraft version (`26.3`, later `26.2`, ...).
Inside a version branch there is one adapter folder per loader (`fabric-<mc>`, `neoforge-<mc>`, `forge-<mc>`).
`settings.gradle` includes every such folder that contains a `build.gradle`; folders without one are placeholders and
are skipped, so the build keeps working. CI builds all included adapters on every push and attaches all jars
(`aio-clientside-<loader>-<version>+mc<mc>.jar`) to one GitHub release per version. To add a loader or Minecraft
version: copy an adapter folder, adapt the adapter classes, add its `build.gradle`; nothing else has to change.

## Core principles

- **Modules cost nothing while off:** listeners are only *declared* in the constructor (`on(...)`) and
  attached to the bus while the module is active. Shared services such as the `ClickTracker` only listen
  while a module uses them (reference counting).
- **Caching:** `TextHudElement` asks `getText()` once per tick (not per frame) and only re-measures the
  text width when the text changed. A test covers this.
- **Colours:** every HUD element has a colour mode: rainbow (default), gradient or fixed.
- **Config:** `config/aio/profiles/<profile>/<module>.json`. Changes are collected and written at the
  earliest 1 s after the last change. Damaged files fall back to defaults.
- **Server compatibility:** modules with `gameplayAffecting = true` (freelook, zoom, fullbright, toggle
  sprint, ...) can be blocked depending on the mode: *Off* (default), *Auto* (on multiplayer servers
  unless the server is on the allow list) or *Always*. The player's choice stays stored; when the block
  ends the modules run again. Purely visual HUD modules are never blocked.
- **No input automation.** No autoclicker or similar. Toggle sprint follows the vanilla option.
- **Other mods win:** `ModConflicts` maps module ids to mod ids that do the same job. If such a mod is
  installed, only the affected module is switched off. Mixins that cannot be applied are detected
  (`HookStatus`) and their module is disabled instead of crashing the game.

## A new HUD module

```java
public final class DayTimeModule extends TextHudElement {
    public DayTimeModule() {
        super("daytime", "Clock", "Shows the real time", 0.0, 0.25);
    }

    @Override protected String getText() {
        return java.time.LocalTime.now().withNano(0).toString();
    }
}
```

Register it in `AioCore.registerDefaultModules()`. Position, size, colours, background, shadow, saving and
HUD-editor support come from `HudElement`. New `Lang.tr` keys need a regenerated language file:

```bash
./gradlew :core:test -Paio.coreOnly=true -Daio.writeLang=true --tests '*LangFileTest*'
```

## Building

```bash
./gradlew :core:test -Paio.coreOnly=true   # core + tests only (no Minecraft)
./gradlew build                            # mod jar: fabric-26.3/build/libs/
./gradlew :fabric-26.3:runClient           # start Minecraft with the mod
```

Needs **Java 25** for the adapter (the core runs on Java 21). Versions are in `gradle.properties`.
The official jars are built by GitHub Actions; the stats server address is injected from the secret
`AIO_STATS_URL` at build time (see `docs/TELEMETRY.md`), local builds do not send statistics.

## Releases and versions

- `0.x.y` are pre-releases, `1.0.0` is the first release. Every build raises the version in `gradle.properties`.
- Every push to a `26.*` branch builds the jar and publishes it as a GitHub release `v<version>-mc<minecraft>`
  (the jar only, no zip).

## Verification status

- **Core:** compiles, unit tests cover regressions of every fixed bug (start without loaded options,
  config loading, waypoints surviving resets and profile changes, freelook without camera hook, mod conflicts, ...).
- **Adapter 26.3:** compiles against Minecraft 26.3 in CI. Mixin targets are only validated by Minecraft
  at load time; hooks that could not be confirmed use `require = 0` so that only their feature is lost.
- **In-game testing** is done by hand; there is a dev-only self test (`-Daio.selftest=<folder>`) that opens the
  main screens and takes screenshots.
- **Recipe sources:** singleplayer uses the integrated server's recipes; servers with Fabric recipe sync use the
  server's recipes; otherwise the default recipes from the Minecraft jar (`data/minecraft/recipe`).
- **Data:** waypoints live in `config/aio/waypoints.json` (independent of profiles and of the export).
- **Compatibility:** with JEI, REI or EMI the built-in item list hides itself; with Mouse Tweaks, Mouse Wheelie or
  Inventory Profiles Next the built-in mouse tweaks do.
- **Limits:** the bee count is only known to the server, so it is missing on servers.
