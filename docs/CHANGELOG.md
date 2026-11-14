# Changelog

## 0.4.7

Downloads for Minecraft 1.21.3: Fabric.

- New: Crosshair module: centre dot, inner and outer lines (length, thickness, gap, opacity), outline, colour or rainbow, T shape, spread while moving, attack indicator
- New: crosshair presets with live previews, and a preview in the settings that stays visible while you adjust it (zoom, outline toggle)
- New: settings can be grouped into sections you can open and close
- Faster: entity culling only checks what is on screen, uses its own block copy instead of reading the world from another thread, works from the real camera (also in third person) and reaches further
- Faster: nametags and health indicator gather their entities once per tick instead of every frame
- Faster: dynamic lights now default to your own light only, with a limit for light sources and fewer updates at low FPS

## 0.4.6

Downloads for Minecraft 1.21.3: Fabric.

- Fixed: health indicator and nametags were far too big at range; they now shrink with distance like the entity itself
- Changed: enchantments in nametags are off by default (also for existing settings, once)

## 0.4.5

Downloads for Minecraft 1.21.3: Fabric.

- Fixed: a file for a different Minecraft version is no longer loaded and crashes the game; Fabric now names the required version instead

## 0.4.4

Downloads for Minecraft 1.21.3: Fabric.

- New: Friends module: rules by name (is, contains, starts with, ends with), each with its own colour, so tags in names work too
- New: friends are marked with an outline, a tinted skin or both
- Fixed: nametags no longer show raw formatting codes such as §7 for NPCs

## 0.4.3

Downloads for Minecraft 1.21.3: Fabric.

- New: all modules of the main version, nothing switched off any more
- New: No View Bobbing, No Speed FOV and a shake slider for No Hurtcam
- New: notice in the title screen when an update is available (can be switched off)
- Fixed: some features had no effect (chat timestamps, hidden vanilla name tags, small items, particles)
