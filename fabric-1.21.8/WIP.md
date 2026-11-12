# Work in progress – Minecraft 1.21.11 (Fabric)

This adapter does **not compile yet** (about 180 compile errors). It is kept on a work-in-progress branch only.

Done:
- Obfuscated-game setup: Loom plugin `net.fabricmc.fabric-loom-remap`, `mappings loom.officialMojangMappings()`, `modImplementation`, Java 21.
- Versions: Minecraft 1.21.11, Fabric API 0.141.6+1.21.11.

Still to port (from the 26.x adapter): `GuiGraphicsExtractor` -> `GuiGraphics` rendering calls (about 56 places),
`GuiMessage`, `ContainerInput`, `LightCoordsUtil`, `ClientCommands`, tooltip/level-render Fabric API events,
`CameraRenderState`/`LevelRenderState`, recipe manager API, trade sets, and the mixin targets (check with the in-game
self test: `-Daio.selftest=<folder>`).
