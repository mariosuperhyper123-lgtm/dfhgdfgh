# Nova Client

Fabric-Utility-Client für Minecraft **26.2** (Java 25), gedacht für Singleplayer.

## Module
| Modul | Standard-Taste | Funktion |
|---|---|---|
| ClickGUI | Rechte Umschalttaste | Menü zum An- und Ausschalten der Module |
| Fullbright | G | Alles hell, auch in Höhlen und nachts |
| Zoom | C (halten) | 4-facher Zoom mit weicher Kamera |
| HUD | H | FPS, Koordinaten, Richtung, Liste der aktiven Module |
| AutoTool | keine | Wechselt beim Abbauen aufs schnellste Werkzeug |
| MobChunks | J | Rahmen um Chunks mit Mobs: **gelb** = gerade gespawnt, **rot** = feindlich, **grün** = friedlich |
| EntityESP | keine | Box und Tracer um ausgewählte Mobs, Farbe pro Mob, durch Wände sichtbar |
| BlockESP | keine | Box und Tracer um ausgewählte Blöcke (z. B. Erze, Spawner, Kisten), Farbe pro Block |
| FreeCam | Mausrad-Klick | Kamera fliegt frei und durch Wände (WASD, Leertaste, Shift, Sprint = schneller). Der Spieler bleibt stehen und macht weiter, was er beim Einschalten gemacht hat |
| FreeLook | Linke Alt (halten) | Um den Spieler herumschauen, ohne den Kopf zu drehen; Kamera geht durch Wände |
| CustomSky | keine | Sonne wird zu einem animierten Schwarzen Loch, dazu ein großer, detaillierter Mond mit Mondphasen |
| GalaxyPickaxe | keine | Alle Spitzhacken bekommen eine animierte Galaxie-Textur |
| HeavyChunks | K | Asynchroner Chunk-Scanner: markiert Chunks mit vielen Kisten, Shulkern, Trichtern, Öfen, Fässern, Spawnern oder Items/Rahmen/Rüstungsständern. Box **gelb → rot** je nach Menge, Chat-Meldung mit Chunk-Koordinaten, Zähler im HUD. Schwellenwerte über „...“ (0 = Kriterium aus) |
| SusChunk | keine | Anzeige-Modul: markiert Chunks mit farbigen Rahmen (nutzt die Mob-Erkennung von MobChunks) |
| StaffList | keine | HUD-Fenster mit Staff-Liste (im Singleplayer immer leer) |
| RegionMap | keine | Kleine Karte unten rechts mit Gelände, Chunk- und Region-Gitter |
| Combat-Panel | keine | AutoCrystal, AnchorMacro, AimAssist, TriggerBot … reine Anzeige-Module ohne Funktion |
| ViewModel | keine | Position, Rotation und Größe von Hand/Item verschieben (Einstellungen über „...“ im ClickGUI) |

Alle Tasten kannst du unter *Optionen → Steuerung → Nova Client* ändern.
Einstellungen öffnest du im ClickGUI über den „...“-Knopf neben dem Modul. In den Listen kannst du suchen, Einträge per Klick an- und ausschalten und über das Farbfeld rechts die Farbe ändern.
Die Modul-Zustände und Einstellungen werden in `config/novaclient.properties` gespeichert.

## Bauen (GitHub Actions)
1. Repository auf GitHub anlegen und den ganzen Ordner hochladen (inklusive `.github`).
2. Unter **Actions** läuft der Workflow „Build“ automatisch (oder manuell über „Run workflow“).
3. Wenn er fertig ist, unten unter **Artifacts** `nova-client` herunterladen und die `.jar` entpacken.

## Installieren
1. [Fabric Loader](https://fabricmc.net/use/) für 26.2 installieren.
2. Die `nova-client-2.0.0.jar` und die passende **Fabric API** in den Ordner `.minecraft/mods` legen.

**Tipp:** ESP und Tracer werden auf dem Bildschirm berechnet. Am genauesten sitzen sie, wenn unter *Optionen → Videoeinstellungen* „Ansicht wackeln“ (View Bobbing) aus ist.
