# AIO ClientSide

Modularer, farbiger Minecraft-Client für **Minecraft 26.3** (Fabric) mit schlankem Kern,
ähnlich Lunar/Badlion. Jedes Feature ist ein eigenes Modul, das sich nur über den Event-Bus einklinkt.

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
fabric-26.3/          Dünner Adapter für MC 26.3: Platform-/Renderer-Implementierung + Mixins
```

**Versions-Abstraktion:** Module, HUD und GUI sprechen nur mit `Platform` und `Renderer`.
Für eine neue MC-Version wird nur ein Adapter-Ordner (`fabric-<version>/`) kopiert und angepasst:
`FabricPlatform`, `FabricRenderer`, `FabricItemRef`, `AioScreen`, `InputState` und 6 kleine Mixins.
Tasten werden über ihre MC-Namen gespeichert (`key.keyboard.right.shift`, `key.mouse.left`), nicht
über numerische Codes. Die Config bleibt so auch beim Wechsel von GLFW auf SDL (ab 26.x) gültig.

### Farben

- Jedes HUD-Element hat einen **Farbmodus**: *Regenbogen* (Standard, Welle über den Text),
  *Verlauf* zwischen zwei Farben oder *Fest*. Das Tempo ist einstellbar.
- Keystrokes leuchten beim Drücken im Farbmodus.
- Das Menü hat einen Regenbogen-Akzent (abschaltbar, dann eigene Akzentfarbe) und eine Farbe pro Kategorie.
- Die Zeichenpositionen werden pro Text gecacht, also nicht in jedem Frame neu gemessen.

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
  - *Aus* (Standard): nie
  - *Auto*: auf Multiplayer-Servern, außer der Server steht auf der Erlaubt-Liste
  - *Immer*: überall

  Der Wunsch des Spielers bleibt gespeichert; nach dem Verlassen laufen die Module wieder.
  Rein visuelle HUD-Module werden nie gesperrt.
- **Keine Eingabe-Automatisierung.** Kein Autoclicker o. Ä. Toggle Sprint entspricht der Vanilla-Option.

## Module

Server-Sperre = vom Server-Schutz sperrbar (Standard: Schutz aus).

| Kategorie | Modul | Sperre | Inhalt |
|---|---|---|---|
| HUD | FPS | – | FPS, optional Ø und Min/Max |
| HUD | CPS | – | Klicks/s links und rechts |
| HUD | Keystrokes | – | WASD, Maus (mit CPS), Leertaste; sanfter Farbwechsel; 3 Layouts |
| HUD | ArmorHUD | – | Rüstung, Hand/Zweithand; Haltbarkeit als Balken, Zahl oder % |
| HUD | Trank-Effekte | – | Original-Effekt-Icons, Stufe und Restzeit |
| HUD | Totem-Zähler / Pfeil-Zähler | – | Anzahl im Inventar mit Icon, rot bei 0 |
| HUD | Item-Info | – | Verzauberungen des Items in der Hand über der Hotbar, blendet aus |
| Info | Koordinaten, Ping | – | XYZ + Richtung + Biom; Latenz |
| Info | Biom, Tagzähler, Uhrzeit (Welt) | – | Biomname, „Tag 42“, Weltzeit mit ☀/☾ |
| Info | Spielzeit | – | Aus der Statistik der Welt, z. B. „2 T 3 Std 14 Min“ |
| Info | Server-Info | – | Name, IP, Ping, Spieler, Version, Software |
| Info | Blick-Info | – | Wie Jade (Reichweite für Blöcke einstellbar): Icon, Name, Werkzeug ✔/✘, Wachstum, Entity-Typ, Herzen + HP, Rüstung, Mod; Block-Details nur beim passenden Block (Honig/Bienen, Komposter, Kessel, Seelenanker, Ackerboden, Redstone, Notenblock, …) |
| Navigation | Kompass | – | Leiste oben oder rund, mit Wegpunkten |
| Navigation | Wegpunkte | – | B = setzen, N = verwalten; Marker mit Name + Entfernung im Bild und am Rand; pro Welt und Dimension |
| Navigation | Todespunkt | – | Wegpunkt beim Tod, verschwindet bei Ankunft |
| Navigation | Portalrechner | – | Nether-/Oberwelt-Koordinaten (÷8 / ×8) |
| Grafik | Freelook, Zoom, Fullbright | ja | wie bisher |
| Grafik | NoFog | ja | Klares Wasser, klare Lava, optional kein Sichtweiten-Nebel |
| Grafik | No Hurtcam | – | Kein Kamerawackeln bei Schaden |
| Grafik | No Pumpkin Overlay | ja | Kein Kürbis-Sichtrahmen |
| Grafik | Block-Outline | – | Farbe (auch Regenbogen), Dicke, Füllung |
| Inventar | Shulker-Vorschau | – | Inhalt als Raster im Tooltip (Shift), in Kistenfarbe |
| Inventar | Maus-Tweaks & Sortieren | – | Mausrad = einzelne Items verschieben (Shift = Stack), Shift + Ziehen, Mittelklick sortiert |
| Inventar | Item-Liste | – | Alle Items rechts im Inventar, Suche (`@mod`, `$id`, `-ausschließen`), Rezepte (Klick/R) und Verwendungen (Rechtsklick/U). **+** legt das Rezept ins 2×2, die Werkbank, den Ofen/Schmelz-/Räucherofen oder die Steinsäge (Shift = so viele wie möglich), fehlende Zutaten werden rot markiert |
| Spieler & Kampf | Toggle Sprint | ja | Dauersprinten per Sprint-Taste |
| Spieler & Kampf | Auto Tool | ja | Bestes Werkzeug beim Abbauen, danach zurück |
| Spieler & Kampf | Auto Sword | ja | Beste Waffe, sobald ein Lebewesen anvisiert wird (also vor dem Schlag) |
| Chat | Chat | – | Zeitstempel, langer Verlauf, Größe/Schrift/Deckkraft, **Chat kopieren** (Rechtsklick im Chat), Chatbox im HUD-Editor an der Ecke ziehen |
| Chat | Namens-Alarm | – | Ton + Hinweis bei deinem Namen (mit/ohne @) und eigenen Stichwörtern |
| Client | Client | – | Menü-Taste, HUD-Editor-Taste, Server-Schutz, Farben, Sortierung |

## Bedienung

- **Rechts-Shift**: Einstellungsmenü. Links die Kategorien (ganz oben „Alle“), Module als Karten mit
  Schalter; ein Klick auf die Karte öffnet Einstellungen, Tastenbelegung, Farben und Aktionen.
  Oben rechts das **Suchfeld** (Strg+F) über alle Kategorien und der Knopf **↕** für die Sortierung:
  Standard, A–Z, Aktive zuerst (wird gespeichert). Unten links die Profile: wechseln, neu, löschen,
  Export/Import über die Zwischenablage.
- **HUD bearbeiten** (Button im Menü oder eigene Taste): Elemente ziehen, Mausrad = Größe,
  Einrasten an Rändern, Mitte und anderen Elementen (Shift = frei), Rechtsklick = Einstellungen,
  R = zurücksetzen. Die Chatbox unten links an der Ecke (⤢) ziehen, Mausrad = Schriftgröße.
- **Linke Alt-Taste** (halten): Freelook. **C** (halten): Zoom. **B**: Wegpunkt setzen, **N**: Wegpunkte verwalten.
- **Im Inventar:** Mausrad über Items verschiebt einzeln (Shift = ganzer Stack), Shift + Ziehen
  verschiebt alle überfahrenen Stacks, Mittelklick sortiert. Rechts die Item-Liste (Suchfeld unten,
  Strg+F); Klick oder R zeigt Rezepte, Rechtsklick oder U die Verwendungen.
- **Im Chat:** Rechtsklick öffnet „Chat kopieren“ (Zeilen markieren, Strg+C).

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
./gradlew build                            # Mod-Jar: fabric-26.3/build/libs/
./gradlew :fabric-26.3:runClient           # Minecraft mit Mod starten
```

Benötigt **Java 25** für den Adapter (der Kern läuft ab Java 21). Versionen stehen in `gradle.properties`:
Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.165.0+26.3, Loom 1.19, Gradle 9.8.2.

## Stand der Verifikation

- **Kern:** kompiliert, 84 Tests grün, darunter Regressionstests zu jedem Fund aus dem Selbst-Audit
  (Start ohne geladene Optionen, Config-Laden ohne kurzes Aktivieren, Wegpunkte sicher vor
  „Zurücksetzen“ und Profilwechsel, Shift-„+“ mit wenigen Paketen, Freelook ohne Kamera-Hook,
  Namens-Alarm bei Server-Chatformaten, Mod-Konflikte).
- **Adapter 26.3:** kompiliert gegen Minecraft 26.3 (CI). Mixin-Ziele prüft erst Minecraft beim
  Laden. Belegt in 26.3-Quellen anderer Mods: Tastatur/Maus, `Entity.turn`, `Camera.calculateFov`,
  Lightmap, Fog, `Hud`, `MultiPlayerGameMode`, `ItemStack`-Tooltip, Container-Felder.
  Unbelegt und darum mit `require = 0` (kosten nur die Funktion): Freelook-Kamera, Chat-Zeitstempel/
  Verlaufslänge, No Hurtcam. Freelook schaltet sich selbst ab, wenn der Kamera-Hook nicht greift.
- **Nicht im Spiel getestet.** Minecraft lässt sich in der Entwicklungsumgebung nicht starten.
- **Rezeptquellen:** Singleplayer → Rezepte des integrierten Servers (inkl. Datenpakete);
  Server mit Fabric-Rezept-Sync → die Rezepte des Servers (auch geänderte);
  sonst → die Standard-Rezepte aus dem Minecraft-Jar (`data/minecraft/recipe`).
- **Daten:** Wegpunkte liegen in `config/aio/waypoints.json` (unabhängig von Profilen und vom Export).
- **Kompatibilität:** Mit JEI, REI oder EMI blendet sich die eigene Item-Liste aus; mit Mouse Tweaks,
  Mouse Wheelie oder Inventory Profiles Next die eigenen Maus-Tweaks.
- **Grenzen:** Die Bienenanzahl kennt nur der Server (steht auch nicht in F3), auf Servern fehlt sie daher.
