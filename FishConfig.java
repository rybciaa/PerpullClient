package dev.fishclient.config;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.fishclient.FishClient;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Ustawienia HUD-a zapisywane w pliku {@code .minecraft/config/fishclient.json}.
 */
public final class FishConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("fishclient.json");

    /** Dostępne skale HUD-a. */
    public static final float[] SCALES = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    /** Kolory akcentu (ARGB) i klucze tłumaczeń ich nazw. */
    public static final int[] ACCENTS = {0xFF5CD6FF, 0xFF6BFF8A, 0xFFFF6BD6, 0xFFFFA94D, 0xFFFFFFFF};
    public static final String[] ACCENT_KEYS = {
            "color.fishclient.cyan", "color.fishclient.green", "color.fishclient.pink",
            "color.fishclient.orange", "color.fishclient.white"};
    /** Przezroczystość tła (0-255) i klucze tłumaczeń. */
    public static final int[] BG_ALPHAS = {0x40, 0x78, 0xB0};
    public static final String[] BG_KEYS = {
            "bg.fishclient.light", "bg.fishclient.medium", "bg.fishclient.dark"};

    private static FishConfig instance;

    // --- Moduły ---
    public boolean hudEnabled = true;
    public boolean showFps = true;
    public boolean showCps = true;
    public boolean showPing = false;
    public boolean showCoords = false;
    public boolean showKeystrokes = true;
    public boolean showSpace = true;

    // --- Wygląd ---
    public int scaleIndex = 1;
    public int accentIndex = 0;
    public int bgIndex = 1;

    /** Pozycja lewego górnego rogu HUD-a (piksele GUI). */
    public int x = 6;
    public int y = 6;

    private FishConfig() {
    }

    public static FishConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        FishConfig loaded = null;
        try {
            if (Files.exists(PATH)) {
                loaded = GSON.fromJson(Files.readString(PATH), FishConfig.class);
            }
        } catch (Exception e) {
            FishClient.LOGGER.warn("Nie udało się wczytać konfiguracji, używam domyślnej.", e);
        }
        instance = loaded != null ? loaded : new FishConfig();
    }

    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(get()));
        } catch (Exception e) {
            FishClient.LOGGER.warn("Nie udało się zapisać konfiguracji.", e);
        }
    }

    /** Przywraca wszystkie ustawienia domyślne. */
    public static void resetAll() {
        instance = new FishConfig();
        save();
    }

    public void resetPosition() {
        x = 6;
        y = 6;
    }

    // --- Skala ---
    public float getScale() {
        return SCALES[Math.floorMod(scaleIndex, SCALES.length)];
    }

    public void cycleScale() {
        scaleIndex = Math.floorMod(scaleIndex + 1, SCALES.length);
    }

    // --- Kolor akcentu ---
    public int getAccent() {
        return ACCENTS[Math.floorMod(accentIndex, ACCENTS.length)];
    }

    public String getAccentKey() {
        return ACCENT_KEYS[Math.floorMod(accentIndex, ACCENT_KEYS.length)];
    }

    public void cycleAccent() {
        accentIndex = Math.floorMod(accentIndex + 1, ACCENTS.length);
    }

    // --- Tło ---
    public int getBackgroundAlpha() {
        return BG_ALPHAS[Math.floorMod(bgIndex, BG_ALPHAS.length)];
    }

    public String getBackgroundKey() {
        return BG_KEYS[Math.floorMod(bgIndex, BG_KEYS.length)];
    }

    public void cycleBackground() {
        bgIndex = Math.floorMod(bgIndex + 1, BG_ALPHAS.length);
    }
}
