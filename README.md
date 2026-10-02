# Fish Client (Fabric 1.21.4)

Minimalistyczny HUD w lewym górnym rogu ekranu:

- **FPS** – liczony z faktycznie wyrenderowanych klatek, odświeżany co 0,5 s,
- **CPS** – lewy | prawy przycisk myszy, okno przesuwne 1 s, kliknięcia łapane mixinem w chwili wystąpienia,
- **Ping** – opóźnienie do serwera w ms (opcjonalnie),
- **Współrzędne** – XYZ gracza (opcjonalnie),
- **Keystrokes** – W, A, S, D, Spacja, LMB, RMB z płynną animacją wciśnięcia.

Wygląd: karta z zaokrąglonymi rogami, kolorowane wartości (FPS/ping zielone-żółte-czerwone), klawisze z ramką i płynnym podświetleniem w kolorze akcentu.

HUD znika przy F1 (ukryty interfejs) i F3 (ekran debug), a CPS liczy tylko kliknięcia w grze (nie w menu).

## Edytor HUD i klawisz skrótu

Domyślnie **Prawy Shift** (w świecie gry) otwiera ekran dostosowania HUD-a. Ten sam klawisz go zamyka (Esc / „Gotowe" też).

W edytorze możesz:
- włączyć/wyłączyć cały HUD oraz osobno FPS, CPS, ping, współrzędne, Keystrokes i klawisz Spacji,
- zmienić **skalę** HUD-a (0,75x – 2x), **kolor akcentu** (błękitny, zielony, różowy, pomarańczowy, biały) i **przezroczystość tła**,
- **przeciągnąć HUD myszką** w dowolne miejsce ekranu (lewy przycisk, podgląd na żywo),
- zresetować samą pozycję albo wszystkie ustawienia.

Klawisz zmienisz w **Opcje → Sterowanie → Klawisze → Fish Client → Otwórz edytor HUD**.
Ustawienia zapisują się automatycznie w `.minecraft/config/fishclient.json`.

## Struktura projektu

```
fish-client/
├── build.gradle
├── gradle.properties            <- wersje MC / Yarn / Loader / Fabric API
├── settings.gradle
├── gradle/wrapper/gradle-wrapper.properties
└── src/main
    ├── java/dev/fishclient
    │   ├── FishClient.java      <- ClientModInitializer + keybind
    │   ├── config/FishConfig.java  <- ustawienia (JSON)
    │   ├── gui/HudEditorScreen.java <- edytor HUD (własny, ręcznie rysowany interfejs)
    │   ├── gui/Ui.java          <- zaokrąglone prostokąty, ramki, kolory
    │   ├── hud/FishHud.java     <- rendering HUD
    │   ├── hud/CpsTracker.java  <- licznik CPS
    │   └── mixin/MouseMixin.java
    └── resources
        ├── assets/fishclient/lang/  <- tłumaczenia (en_us, pl_pl)
        ├── fabric.mod.json
        └── fishclient.mixins.json
```

## Konfiguracja w IntelliJ IDEA

1. **JDK 21** – zainstaluj np. Temurin 21. (MC 1.21.4 wymaga Javy 21+.)
2. **IntelliJ IDEA** 2023.1 lub nowszy (Community wystarczy).
3. Rozpakuj projekt, a w IntelliJ wybierz **File → Open…**, wskaż plik `build.gradle` (lub cały folder) i **Open as Project** → **Trust Project**.
4. **File → Project Structure → Project → SDK** ustaw na JDK 21.
5. **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM** ustaw na JDK 21. Wybierz *Use Gradle from: 'gradle-wrapper.properties' file* (Gradle 8.11.1).
6. Poczekaj na synchronizację Gradle. Pierwszy raz trwa kilka minut (pobieranie Minecrafta i remapowanie).
7. *(Opcjonalnie)* Gradle → **Tasks → fabric → genSources**, aby mieć czytelne źródła Minecrafta do podglądu. Po zakończeniu: klik w Gradle „Reload".
8. Uruchom grę: na liście konfiguracji u góry wybierz **Minecraft Client** i kliknij ▶ (albo terminal: `gradlew runClient`). Wejdź do świata, HUD pojawi się w lewym górnym rogu.

> Jeśli nie ma plików `gradlew` / `gradlew.bat`, wygeneruj je poleceniem `gradle wrapper --gradle-version 8.11.1`
> (wymaga zainstalowanego Gradle), albo skopiuj `gradlew`, `gradlew.bat` i `gradle/wrapper/gradle-wrapper.jar`
> z oficjalnego szablonu: https://github.com/FabricMC/fabric-example-mod (gałąź `1.21.4`).

## Budowanie JAR-a

Gradle → **Tasks → build → build** (albo `gradlew build`). Gotowy plik:
`build/libs/fishclient-1.21.4-1.3.0.jar` (nie używaj wersji `-sources`).

## Zbudowanie JAR-a online (bez instalowania czegokolwiek)

W projekcie jest gotowy workflow GitHub Actions (`.github/workflows/build.yml`):

1. Załóż darmowe repozytorium na GitHubie i wgraj do niego zawartość tego folderu (przycisk *Add file → Upload files* wystarczy, razem z ukrytym folderem `.github`).
2. Wejdź w zakładkę **Actions** i poczekaj kilka minut, aż build się zakończy (albo uruchom ręcznie: *Build Fish Client → Run workflow*).
3. Otwórz zakończony przebieg i pobierz z sekcji **Artifacts** plik `fishclient-jar`. W środku jest gotowy `fishclient-1.21.4-1.3.0.jar`.
4. Jeśli build się nie powiedzie, otwórz log kroku „Build" i wklej mi błąd.

## Instalacja w grze

1. Zainstaluj **Fabric Loader** (>= 0.16) dla Minecrafta 1.21.4.
2. Wrzuć do `.minecraft/mods`: `fishclient-1.21.4-1.3.0.jar` oraz **Fabric API** dla 1.21.4.

## Rozwiązywanie problemów

- **Nie można pobrać `fabric-api:0.119.2+1.21.4` / `yarn:1.21.4+build.8`** – wejdź na https://fabricmc.net/develop/, wybierz 1.21.4 i podmień wartości w `gradle.properties`.
- **`Unsupported class file major version`** – Gradle JVM nie jest ustawione na JDK 21 (krok 5).
- **Brak konfiguracji „Minecraft Client"** – wykonaj *Reload All Gradle Projects*; ewentualnie uruchom `gradlew runClient`.
- **Inna wersja Minecrafta** – od 1.21.9 zmieniły się m.in. sygnatury zdarzeń myszy w `Mouse` i część API renderowania, więc mixin i `FishHud` wymagają poprawek.

## Dostosowanie

Stałe układu i kolorów (marginesy, rozmiar klawiszy, barwy ARGB) znajdziesz na górze `FishHud.java`.
Etykiety W/A/S/D są stałe, ale ich stan pochodzi z przypisanych klawiszy gry, więc po zmianie bindów podświetlają się właściwe przyciski.
