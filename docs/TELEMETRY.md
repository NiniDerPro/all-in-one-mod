# Anonyme Nutzungsstatistik

AIO sammelt lokal eine private Statistik (`config/aio/stats.json`, nur auf dem eigenen Rechner).
Zusätzlich kann AIO **anonyme technische Zahlen** an einen eigenen Statistik-Server senden. Das ist
**standardmäßig aus**: beim ersten Start fragt ein Hinweis im Titelbildschirm
„Share anonymous usage data for public statistics?“ – **Okay** schaltet es ein, **No thanks** lässt es aus.
Jederzeit änderbar unter **Client → Send anonymous usage statistics**.

Gesendet wird nur, wenn der Schalter an ist **und** der Build eine Server-Adresse enthält. Die Adresse steht
nicht im Quelltext; offizielle Builds bekommen sie beim Bauen aus einem Secret (`AIO_STATS_URL`), eigene Builds
senden nichts.

## Was gesendet wird

Beim Spielstart (nach ca. 90 s) und danach alle 10 Minuten ein Paket:

| Gruppe | Felder |
|---|---|
| Installation | zufällige Installations-ID (keine Verbindung zu Konto oder Gerät), Schema-Version |
| Versionen | Mod-, Minecraft-, Fabric-Loader-, Java-Version, Betriebssystem (windows/macos/linux/other) |
| Nutzung | Zahl der Starts, Gesamtspielzeit in Gruppen (<1, 1-10, 10-50, 50-200, 200+ h), Länge der Sitzung |
| Module | Liste der aktiven Module, Anzahl, Höchstwert, Nutzungsdauer je Modul in Gruppen |
| Kompatibilität | welche Module wegen einer anderen Mod abgeschaltet sind (und welche Mod), Server-Kompatibilitätsmodus |
| Fehler | Module mit nicht greifenden Hooks, nicht eingebaute Mixins |
| Leistung | Durchschnitts- und Mindest-FPS dieser Sitzung (alle 5 s gemessen) |

Live-Nutzer bestimmt der Server daraus, dass eine Installations-ID in den letzten ca. 15 Minuten
gesendet hat.

## Was nie gesendet wird
Spielername, UUID, Account, IP-Adresse (der Server speichert sie nicht), Servernamen, Koordinaten,
Chat, Dateien, Einstellungswerte, Tastenbelegungen.

## Server
Der Server (Cloudflare Worker + D1, Quelltext in `server/stats-worker/`; die Adresse steht nicht im Repo, sie wird beim Bauen aus einem Secret eingesetzt) nimmt `POST` mit JSON entgegen, speichert die Felder pro
Installations-ID (zuletzt gesehen) und wertet sie aus. Er darf keine IP-Adressen und keine
Header außer den nötigen speichern.
