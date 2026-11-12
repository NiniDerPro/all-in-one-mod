# NeoForge adapter for Minecraft 1.21.11 – planned, not implemented

This folder is a placeholder. It has no `build.gradle`, so Gradle skips it and the mod builds as before.

To implement it:
1. Copy `fabric-1.21.11/` here and replace the Fabric parts (Loom plugin, `fabric.mod.json`, Fabric API events)
   with the NeoForge equivalents (`neoforge.mods.toml`, NeoForge events, key mapping registration).
2. Keep `core/` unchanged. Only `Platform`, `Renderer`, `ItemRef`, the screen wrapper and the mixins are adapter code.
3. Add a `build.gradle` here. The folder is then picked up automatically by `settings.gradle` and CI will build
   `aio-clientside-neoforge-<version>+mc1.21.11.jar` and attach it to the release.
