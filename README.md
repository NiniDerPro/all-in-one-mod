# AIO Client

Modularer Minecraft-Client (Fabric) mit schlankem Kern, ähnlich Lunar/Badlion.
Jedes Feature ist ein eigenes Modul, das sich nur über den Event-Bus einklinkt.

## Aufbau

```
core/                 Versionsunabhängiger Kern, KEINE Minecraft-Abhängigkeit (nur Gson)
  event/              Event-Bus + Events (Tick, RenderHud, Key, MouseButton, MouseScroll, ServerJoin/Leave)
  setting/            Boolean, Number, Enum, Color, Keybind, String (JSON-serialisierbar)
  module/             Module-Basisklasse, Kategorien, Registry
  config/             JSON pro Modul, Profile, Export/Import, verzögertes Speichern
  compat/             Server-Kompatibilitätsmodus (sperrt Vorteils-Module)
  hud/                HudElement, TextHudElement, HudManager, HUD-Editor, Snapping
  ui/                 Einstellungs-GUI (ClickGuiScreen), Theme, UiScreen-Schnittstelle
  platform/           Versions-Abstraktion: Platform, Renderer, ItemRef, ...
  modules/            Die eigentlichen Features
fabric-1.21.1/        Dünner Adapter für MC 1.21.1: Platform-/Renderer-Implementierung + Mixins
```

**Versions-Abstraktion:** Module, HUD und GUI sprechen nur mit `Platform` und `Renderer`.
Für eine neue MC-Version wird nur ein Adapter-Ordner (`fabric-<version>/`) kopiert und angepasst:
`FabricPlatform`, `FabricRenderer`, `FabricItemRef`, `AioScreen` und 8 kleine Mixins.

### Kern-Prinzipien

- **Module ohne Kosten, wenn aus:** Listener werden im Konstruktor nur *deklariert* (`on(...)`)
  und hängen nur am Bus, solange das Modul aktiv ist. Geteilte Dienste wie der `ClickTracker`
  lauschen nur, solange ein Modul sie nutzt (Referenzzählung).
- **Caching:** `TextHudElement` fragt `getText()` pro Tick ab (nicht pro Frame) und misst die
  Textbreite nur neu, wenn sich der Text geändert hat. Das deckt ein Test ab.
- **Config:** `config/aio/profiles/<profil>/<modul>.json`. Änderungen werden gesammelt und frühestens
  1 s nach der letzten Änderung geschrieben. Beschädigte Dateien fallen auf Standardwerte zurück.
- **Server-Kompatibilität:** Module mit `gameplayAffecting = true` (Freelook, Zoom, Fullbright,
  Toggle Sprint) werden je nach Modus gesperrt:
  - *Aus*: nie
  - *Auto* (Standard): auf Multiplayer-Servern, außer der Server steht auf der Erlaubt-Liste
  - *Immer*: überall

  Der Wunsch des Spielers bleibt gespeichert; nach dem Verlassen laufen die Module wieder.
  Rein visuelle HUD-Module werden nie gesperrt.
- **Keine Eingabe-Automatisierung.** Kein Autoclicker o. Ä. Toggle Sprint entspricht der Vanilla-Option.

## Module

| Modul | Kategorie | Server-Sperre | Inhalt |
|---|---|---|---|
| FPS | HUD | – | FPS, optional Ø und Min/Max über ein Zeitfenster |
| CPS | HUD | – | Klicks/s links und rechts (Queue, älter als 1 s fliegt raus) |
| Keystrokes | HUD | – | WASD, Maus (mit CPS), Leertaste; sanfter Farbwechsel; 3 Layouts |
| ArmorHUD | HUD | – | Rüstung, Hand/Zweithand; Haltbarkeit als Balken, Zahl oder % |
| Koordinaten | HUD | – | XYZ, Blickrichtung, Biom |
| Ping | HUD | – | Latenz laut Spielerliste |
| Trank-Effekte | HUD | – | Effekte mit Stufe und Restzeit |
| Freelook | Grafik | ja | Kamera frei drehen (Halten/Umschalten), 3. Person und zurück |
| Zoom | Grafik | ja | Faktor einstellbar, sanft, Mausrad, Maus verlangsamen |
| Fullbright | Grafik | ja | Gamma-Override, landet nicht in `options.txt` |
| Toggle Sprint | Spieler | ja | Sprint-Taste schaltet Dauersprinten, mit Statusanzeige |
| Chat | Chat | – | Zeitstempel, Verlauf bis 2000 Zeilen |
| Client | Client | – | Menü-Taste, HUD-Editor-Taste, Kompatibilitätsmodus, Akzentfarbe |

## Bedienung

- **Rechts-Shift**: Einstellungsmenü (Kategorien links, Module als Karten, Schalter, Slider,
  Tastenbelegung, Farben, Profile unten links: wechseln, neu, löschen, Export/Import über die Zwischenablage).
- **HUD bearbeiten** (Button im Menü oder eigene Taste): Elemente ziehen, Mausrad = Größe,
  Einrasten an Rändern, Mitte und anderen Elementen (Shift = frei), Rechtsklick = Einstellungen, R = zurücksetzen.
- **Linke Alt-Taste** (halten): Freelook. **C** (halten): Zoom.

## Ein neues HUD-Modul

```java
public final class DayTimeModule extends TextHudElement {
    public DayTimeModule() {
        super("daytime", "Uhrzeit", "Zeigt die echte Uhrzeit", 0.0, 0.25);
    }

    @Override protected String getText() {
        return java.time.LocalTime.now().withNano(0).toString();
    }
}
```

Danach in `AioCore.registerDefaultModules()` registrieren. Position, Größe, Farben, Hintergrund,
Schatten, Speichern und Editor-Unterstützung kommen von `HudElement`.

## Bauen

```bash
./gradlew :core:test -Paio.coreOnly=true   # nur Kern + Tests (ohne Minecraft)
./gradlew build                            # Mod-Jar: fabric-1.21.1/build/libs/
./gradlew :fabric-1.21.1:runClient         # Minecraft mit Mod starten
```

Benötigt Java 21. Versionen für den Adapter stehen in `gradle.properties`.
