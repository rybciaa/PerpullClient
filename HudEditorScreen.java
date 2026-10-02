package dev.fishclient.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.fishclient.FishClient;
import dev.fishclient.config.FishConfig;
import dev.fishclient.hud.FishHud;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Edytor HUD: nowoczesny panel z przełącznikami, selektorami wyglądu i przeciąganiem HUD-a myszką.
 * Cały interfejs jest rysowany ręcznie (bez standardowych przycisków Minecrafta).
 */
public class HudEditorScreen extends Screen {
    // --- Wymiary panelu (przestrzeń panelu, przed skalowaniem do ekranu) ---
    private static final int PW = 330;
    private static final int PH = 284;
    private static final int PAD = 12;

    // --- Kolory ---
    private static final int PANEL_BG = 0xF2121218;
    private static final int PANEL_BORDER = 0x30FFFFFF;
    private static final int CELL = 0x18FFFFFF;
    private static final int CELL_HOVER = 0x30FFFFFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_MUTED = 0xFF9A9AA8;
    private static final int TEXT_DIM = 0xFF6E6E80;
    private static final int TRACK_OFF = 0xFF3A3A46;

    /** Pikselowa rybka (X = ciało, E = oko). */
    private static final String[] FISH = {
            "..XXXXX...X",
            ".XXXXXXX.XX",
            "XXXEXXXXXXX",
            ".XXXXXXX.XX",
            "..XXXXX...X"
    };

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }

    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Float> anim = new HashMap<>();
    private long lastNanos = System.nanoTime();
    private float openT = 0f;

    // Ostatnia transformacja panel -> ekran (do obsługi myszy)
    private float lastOx;
    private float lastOy;
    private float lastS = 1f;

    private boolean dragging = false;
    private int dragOffX;
    private int dragOffY;

    public HudEditorScreen() {
        super(Text.translatable("screen.fishclient.title"));
    }

    // Tło rysujemy samodzielnie w render(), więc domyślne tło jest wyłączone.
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min((now - lastNanos) / 1_000_000_000f, 0.1f);
        lastNanos = now;
        openT = Math.min(1f, openT + dt * 7f);
        float ease = 1f - (1f - openT) * (1f - openT) * (1f - openT);

        FishConfig cfg = FishConfig.get();
        int accent = cfg.getAccent();

        // --- Tło: gradientowe przyciemnienie ---
        int a1 = (int) (0x48 * ease);
        int a2 = (int) (0x9A * ease);
        ctx.fillGradient(0, 0, this.width, this.height, a1 << 24, a2 << 24);

        // --- Podgląd HUD-a na żywo + ramka ---
        FishHud.draw(ctx, this.client);
        if (FishHud.getBoundsW() > 0) {
            int color = dragging ? Ui.withAlpha(accent, 0xFF) : 0x66FFFFFF;
            Ui.roundedBorder(ctx, FishHud.getBoundsX() - 3, FishHud.getBoundsY() - 3,
                    FishHud.getBoundsW() + 6, FishHud.getBoundsH() + 6, 4, color);
        }

        // --- Transformacja panelu (dopasowanie do małych okien + animacja otwarcia) ---
        float fit = Math.min(1f, Math.min((this.height - 8f) / PH, (this.width - 8f) / PW));
        float s = fit * (0.94f + 0.06f * ease);
        float ox = Math.round((this.width - PW * s) / 2f);
        float oy = Math.round((this.height - PH * s) / 2f);
        lastOx = ox;
        lastOy = oy;
        lastS = s;
        int pmx = (int) Math.floor((mouseX - ox) / s);
        int pmy = (int) Math.floor((mouseY - oy) / s);

        hits.clear();
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(ox, oy, 0f);
        m.scale(s, s, 1f);
        drawPanel(ctx, cfg, accent, pmx, pmy, dt);
        m.pop();
    }

    private void drawPanel(DrawContext ctx, FishConfig cfg, int accent, int mx, int my, float dt) {
        TextRenderer font = this.textRenderer;

        // --- Cień, tło, ramka, pasek akcentu ---
        for (int i = 4; i >= 1; i--) {
            Ui.roundedRect(ctx, -i, -i + 2, PW + i * 2, PH + i * 2, 6 + i, 0x12000000);
        }
        Ui.roundedRect(ctx, 0, 0, PW, PH, 6, PANEL_BG);
        Ui.roundedBorder(ctx, 0, 0, PW, PH, 6, PANEL_BORDER);
        ctx.fill(6, 0, PW - 6, 2, accent);

        // --- Nagłówek: ikonka, tytuł, podpowiedź, badge klawisza ---
        drawFish(ctx, PAD, 12, accent);
        Text fish = Text.literal("Fish").formatted(Formatting.BOLD);
        Text client = Text.literal(" Client").formatted(Formatting.BOLD);
        int titleX = PAD + 28;
        ctx.drawText(font, fish, titleX, 9, accent, false);
        ctx.drawText(font, client, titleX + font.getWidth(fish), 9, TEXT, false);
        ctx.drawText(font, Text.translatable("screen.fishclient.hint"), titleX, 21, TEXT_DIM, false);

        if (FishClient.openMenuKey != null) {
            String key = FishClient.openMenuKey.getBoundKeyLocalizedText().getString().toUpperCase(Locale.ROOT);
            int bw = font.getWidth(key) + 12;
            int bx = PW - PAD - bw;
            Ui.roundedRect(ctx, bx, 12, bw, 14, 4, 0x26FFFFFF);
            Ui.roundedBorder(ctx, bx, 12, bw, 14, 4, 0x33FFFFFF);
            ctx.drawText(font, key, bx + 6, 12 + (14 - font.fontHeight) / 2 + 1, TEXT_MUTED, false);
        }
        ctx.fill(PAD, 38, PW - PAD, 39, 0x22FFFFFF);

        // --- Sekcja: moduły ---
        drawSection(ctx, font, "screen.fishclient.modules", 46);
        int colW = (PW - PAD * 2 - 6) / 2;
        int col1 = PAD;
        int col2 = PAD + colW + 6;

        drawToggle(ctx, font, "hud", Text.translatable("option.fishclient.hud"), cfg.hudEnabled,
                () -> cfg.hudEnabled = !cfg.hudEnabled, PAD, 60, PW - PAD * 2, 20, accent, mx, my, dt);
        drawToggle(ctx, font, "fps", Text.translatable("option.fishclient.fps"), cfg.showFps,
                () -> cfg.showFps = !cfg.showFps, col1, 84, colW, 20, accent, mx, my, dt);
        drawToggle(ctx, font, "cps", Text.translatable("option.fishclient.cps"), cfg.showCps,
                () -> cfg.showCps = !cfg.showCps, col2, 84, colW, 20, accent, mx, my, dt);
        drawToggle(ctx, font, "ping", Text.translatable("option.fishclient.ping"), cfg.showPing,
                () -> cfg.showPing = !cfg.showPing, col1, 108, colW, 20, accent, mx, my, dt);
        drawToggle(ctx, font, "coords", Text.translatable("option.fishclient.coords"), cfg.showCoords,
                () -> cfg.showCoords = !cfg.showCoords, col2, 108, colW, 20, accent, mx, my, dt);
        drawToggle(ctx, font, "keys", Text.translatable("option.fishclient.keystrokes"), cfg.showKeystrokes,
                () -> cfg.showKeystrokes = !cfg.showKeystrokes, col1, 132, colW, 20, accent, mx, my, dt);
        drawToggle(ctx, font, "space", Text.translatable("option.fishclient.space"), cfg.showSpace,
                () -> cfg.showSpace = !cfg.showSpace, col2, 132, colW, 20, accent, mx, my, dt);

        // --- Sekcja: wygląd ---
        drawSection(ctx, font, "screen.fishclient.appearance", 160);
        int cx = 96;

        // Skala
        drawRowLabel(ctx, font, "option.fishclient.scale", 174);
        int segW = 41;
        for (int i = 0; i < FishConfig.SCALES.length; i++) {
            final int idx = i;
            String label = String.format(Locale.ROOT, "%.2f", FishConfig.SCALES[i]).replaceAll("0+$", "")
                    .replaceAll("\\.$", "") + "x";
            drawSegment(ctx, font, label, cfg.scaleIndex == i, () -> cfg.scaleIndex = idx,
                    cx + i * (segW + 4), 174, segW, 20, accent, mx, my);
        }

        // Kolor akcentu
        drawRowLabel(ctx, font, "option.fishclient.accent", 198);
        for (int i = 0; i < FishConfig.ACCENTS.length; i++) {
            final int idx = i;
            drawSwatch(ctx, FishConfig.ACCENTS[i], cfg.accentIndex == i, () -> cfg.accentIndex = idx,
                    cx + i * 30, 198, 20, mx, my);
        }

        // Tło
        drawRowLabel(ctx, font, "option.fishclient.background", 222);
        int bgW = 71;
        for (int i = 0; i < FishConfig.BG_KEYS.length; i++) {
            final int idx = i;
            drawSegment(ctx, font, Text.translatable(FishConfig.BG_KEYS[i]).getString(), cfg.bgIndex == i,
                    () -> cfg.bgIndex = idx, cx + i * (bgW + 4), 222, bgW, 20, accent, mx, my);
        }

        // --- Stopka ---
        ctx.fill(PAD, 247, PW - PAD, 248, 0x22FFFFFF);
        drawButton(ctx, font, Text.translatable("option.fishclient.reset_pos").getString(), false,
                () -> cfg.resetPosition(), PAD, 254, 104, 18, accent, mx, my);
        drawButton(ctx, font, Text.translatable("option.fishclient.reset_all").getString(), false,
                FishConfig::resetAll, PAD + 110, 254, 104, 18, accent, mx, my);
        drawButton(ctx, font, Text.translatable("gui.done").getString(), true,
                this::close, PAD + 220, 254, PW - PAD * 2 - 220, 18, accent, mx, my);
    }

    // ------------------------------------------------------------------ elementy UI

    private void drawFish(DrawContext ctx, int x, int y, int color) {
        for (int row = 0; row < FISH.length; row++) {
            for (int col = 0; col < FISH[row].length(); col++) {
                char c = FISH[row].charAt(col);
                if (c == '.') {
                    continue;
                }
                int px = x + col * 2;
                int py = y + row * 2;
                ctx.fill(px, py, px + 2, py + 2, c == 'E' ? 0xFF101018 : color);
            }
        }
    }

    private void drawSection(DrawContext ctx, TextRenderer font, String key, int y) {
        String title = Text.translatable(key).getString().toUpperCase(Locale.ROOT);
        ctx.drawText(font, title, PAD, y, TEXT_DIM, false);
        int tw = font.getWidth(title);
        ctx.fill(PAD + tw + 6, y + font.fontHeight / 2, PW - PAD, y + font.fontHeight / 2 + 1, 0x18FFFFFF);
    }

    private void drawRowLabel(DrawContext ctx, TextRenderer font, String key, int y) {
        ctx.drawText(font, Text.translatable(key), PAD, y + (20 - font.fontHeight) / 2 + 1, TEXT_MUTED, false);
    }

    /** Karta z etykietą i animowanym przełącznikiem typu "pigułka". */
    private void drawToggle(DrawContext ctx, TextRenderer font, String id, Text label, boolean value,
                            Runnable toggle, int x, int y, int w, int h, int accent, int mx, int my, float dt) {
        boolean hover = Ui.inside(mx, my, x, y, w, h);
        Ui.roundedRect(ctx, x, y, w, h, 4, hover ? CELL_HOVER : CELL);

        float t = animate(id, value ? 1f : 0f, dt);
        ctx.drawText(font, label, x + 8, y + (h - font.fontHeight) / 2 + 1,
                Ui.lerpColor(TEXT_MUTED, TEXT, t), false);

        int sw = 24;
        int sh = 12;
        int sx = x + w - 8 - sw;
        int sy = y + (h - sh) / 2;
        Ui.roundedRect(ctx, sx, sy, sw, sh, 6, Ui.lerpColor(TRACK_OFF, Ui.withAlpha(accent, 0xFF), t));
        int kx = sx + 2 + Math.round(t * (sw - sh));
        Ui.roundedRect(ctx, kx, sy + 2, 8, 8, 4, 0xFFFFFFFF);

        hits.add(new Hit(x, y, w, h, toggle));
    }

    /** Pojedynczy segment selektora (np. skala, tło). */
    private void drawSegment(DrawContext ctx, TextRenderer font, String label, boolean selected, Runnable onClick,
                             int x, int y, int w, int h, int accent, int mx, int my) {
        boolean hover = Ui.inside(mx, my, x, y, w, h);
        if (selected) {
            Ui.roundedRect(ctx, x, y, w, h, 4, Ui.withAlpha(accent, 0x44));
            Ui.roundedBorder(ctx, x, y, w, h, 4, Ui.withAlpha(accent, 0xFF));
        } else {
            Ui.roundedRect(ctx, x, y, w, h, 4, hover ? CELL_HOVER : CELL);
        }
        int tx = x + (w - font.getWidth(label)) / 2;
        ctx.drawText(font, label, tx, y + (h - font.fontHeight) / 2 + 1, selected ? TEXT : TEXT_MUTED, false);
        hits.add(new Hit(x, y, w, h, onClick));
    }

    /** Kolorowa próbka z obwódką dla wybranego koloru. */
    private void drawSwatch(DrawContext ctx, int color, boolean selected, Runnable onClick,
                            int x, int y, int size, int mx, int my) {
        boolean hover = Ui.inside(mx, my, x, y, size, size);
        Ui.roundedRect(ctx, x, y, size, size, 4, Ui.withAlpha(color, 0xFF));
        if (selected) {
            Ui.roundedBorder(ctx, x - 2, y - 2, size + 4, size + 4, 5, 0xFFFFFFFF);
        } else if (hover) {
            Ui.roundedBorder(ctx, x - 2, y - 2, size + 4, size + 4, 5, 0x88FFFFFF);
        }
        hits.add(new Hit(x, y, size, size, onClick));
    }

    /** Przycisk akcji; wersja "primary" jest wypełniona kolorem akcentu. */
    private void drawButton(DrawContext ctx, TextRenderer font, String label, boolean primary, Runnable onClick,
                            int x, int y, int w, int h, int accent, int mx, int my) {
        boolean hover = Ui.inside(mx, my, x, y, w, h);
        int textColor;
        if (primary) {
            int base = Ui.withAlpha(accent, 0xFF);
            Ui.roundedRect(ctx, x, y, w, h, 4, hover ? Ui.lerpColor(base, 0xFFFFFFFF, 0.2f) : base);
            textColor = 0xFF101018;
        } else {
            Ui.roundedRect(ctx, x, y, w, h, 4, hover ? CELL_HOVER : CELL);
            Ui.roundedBorder(ctx, x, y, w, h, 4, 0x22FFFFFF);
            textColor = TEXT;
        }
        ctx.drawText(font, label, x + (w - font.getWidth(label)) / 2, y + (h - font.fontHeight) / 2 + 1, textColor, false);
        hits.add(new Hit(x, y, w, h, onClick));
    }

    private float animate(String id, float target, float dt) {
        float current = anim.getOrDefault(id, target);
        current += (target - current) * Math.min(1f, dt * 16f);
        anim.put(id, current);
        return current;
    }

    // ------------------------------------------------------------------ mysz / klawiatura

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int px = (int) Math.floor((mouseX - lastOx) / lastS);
            int py = (int) Math.floor((mouseY - lastOy) / lastS);

            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit hit = hits.get(i);
                if (Ui.inside(px, py, hit.x(), hit.y(), hit.w(), hit.h())) {
                    hit.action().run();
                    FishConfig.save();
                    return true;
                }
            }
            if (px >= 0 && px < PW && py >= 0 && py < PH) {
                return true; // kliknięcie w tło panelu - nic nie rób
            }
            if (FishHud.isInsideBounds(mouseX, mouseY)) {
                dragging = true;
                dragOffX = (int) mouseX - FishHud.getBoundsX();
                dragOffY = (int) mouseY - FishHud.getBoundsY();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            FishConfig cfg = FishConfig.get();
            int maxX = Math.max(0, this.width - FishHud.getBoundsW());
            int maxY = Math.max(0, this.height - FishHud.getBoundsH());
            cfg.x = Math.max(0, Math.min((int) mouseX - dragOffX, maxX));
            cfg.y = Math.max(0, Math.min((int) mouseY - dragOffY, maxY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            FishConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Ten sam klawisz, który otwiera edytor, również go zamyka.
        if (FishClient.openMenuKey != null && FishClient.openMenuKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        FishConfig.save();
        super.close();
    }
}
