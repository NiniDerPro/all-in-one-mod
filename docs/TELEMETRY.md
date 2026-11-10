# Anonyme Nutzungsstatistik (bStats)

AIO sammelt lokal eine private Statistik (`config/aio/stats.json`, nur auf dem eigenen Rechner,
keine Anzeige, wird nie verschickt). Zusätzlich kann AIO eine **anonyme** Online-Statistik über
[bStats](https://bstats.org) senden – so wie viele Plugins. Das ist standardmäßig vorbereitet, aber
erst aktiv, wenn du die zwei folgenden Schritte machst. Bis dahin wird nichts gesendet.

## Was gesendet wird (und was nicht)

Gesendet werden nur anonyme, aggregierte Zahlen:

- die von bStats standardmäßig erfassten Werte (Anzahl aktiver Installationen, Peak, Länder grob,
  Minecraft-/Java-Version) – alles ohne Namen, ohne Accounts;
- `active_modules` – wie viele Module gerade an sind;
- `peak_active_modules` – höchster gleichzeitiger Modulstand;
- `most_used_module` – das am längsten genutzte Modul;
- `modules_in_use` – welche Module gerade an sind (damit du siehst, was beliebt ist).

**Nicht** gesendet werden: Spielername, UUID, IP, Server, Koordinaten, Chat – nichts
Persönliches. bStats verwirft die IP sofort beim Empfang.

Standardmäßig ist das Senden **aus** (Opt-in). Beim ersten Start fragt ein Hinweis im Titelbildschirm
„Share anonymous usage data for public statistics?“ – **Okay** schaltet es ein, **No thanks** lässt es aus.
Jederzeit änderbar unter **Client → Send anonymous usage statistics**.

## Aktivieren (zwei Schritte)

### 1. Mod bei bStats anlegen und Service-ID eintragen

1. Auf <https://bstats.org> einloggen und unter *Add plugin* die Mod als **Fabric** eintragen.
2. Du bekommst eine **Service-ID** (eine Zahl).
3. Diese Zahl in `fabric-26.3/src/main/java/dev/aio/fabric/Telemetry.java` bei
   `SERVICE_ID` eintragen (statt der `0`).

Solange dort `0` steht, wird nichts gesendet – auch wenn der Schalter an ist.

### 2. bStats ins Jar einbinden (Gradle)

bStats verlangt, dass seine Klassen „relocated“ mitgeliefert werden (damit sich mehrere Mods nicht
in die Quere kommen). Das geht mit dem Shadow-Plugin. In `fabric-26.3/build.gradle`:

```gradle
plugins {
    id 'net.fabricmc.fabric-loom'
    id 'com.gradleup.shadow' version '8.3.5'   // Relocation für bStats
}

repositories {
    maven { url = 'https://repo.bstats.org/releases/' }
}

dependencies {
    // ... bestehende ...
    implementation 'org.bstats:bstats-fabric:3.1.0'
}

shadowJar {
    archiveClassifier = 'shadow'
    relocate 'org.bstats', 'dev.aio.shadow.bstats'
    dependencies { exclude(dependency('net.fabricmc:.*')) }
}

// Loom nimmt das relocatete Jar als Eingang fürs Remapping
remapJar {
    dependsOn shadowJar
    inputFile = shadowJar.archiveFile
}
```

Danach normal bauen (`./gradlew build`). Die relocatete bStats-Bibliothek liegt dann im Jar, AIO
findet sie zur Laufzeit automatisch und beginnt – wenn der Schalter an ist und die Service-ID stimmt
– mit dem anonymen Senden (erste Übertragung einige Minuten nach dem Start, danach alle 30 Min).

> Wird einer der beiden Schritte weggelassen, bleibt alles beim rein Lokalen: AIO merkt beim Start
> „bStats not bundled; only local statistics are kept“ im Log und sendet nichts.
