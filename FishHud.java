package dev.fishclient.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.fishclient.config.FishConfig;
import dev.fishclient.gui.HudEditorScreen;
import dev.fishclient.gui.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

/**
 * Nowoczesny HUD: karta z FPS / CPS / ping / XYZ oraz klawisze W A S D, Spacja, LMB, RMB.
 * Pozycję, skalę, kolory i widoczność modułów ustawia się w {@link HudEditorScreen}.
 */
public final class FishHud {
    /** Globalny licznik CPS zasilany przez MouseMixin. */
    public static final CpsTracker CPS = new CpsTracker();

    // --- Układ (w pikselach GUI, przed skalowaniem) ---
    private static final int GAP = 3;
    private static final int PAD = 5;
    private static final int ROW_EXTRA = 2;
    private static final int KEY_H = 20;
    private static final int SPACE_H = 12;
    private static final int MIN_WIDTH = 78;
    private static final int CARD_RADIUS = 3;
    private static final int KEY_RADIUS = 2;

    // --- Kolory ---
    private static final int BG_BASE = 0x0C0C14;        // lekko granatowa czerń
    private static final int CARD_BORDER = 0x26FFFFFF;
    private static final int KEY_BORDER = 0x2AFFFFFF;
    private static final int TEXT_IDLE = 0xFFFFFFFF;
    private static final int TEXT_PRESSED = 0xFF0E0E14;
    private static final int GOOD = 0xFF6BFF8A;
    private static final int OK = 0xFFFFD84D;
    private static final int BAD = 0xFFFF5C5C;

    private record Row(String label, String value, int valueColor) {
    }

    // --- Stan FPS ---
    private static long fpsWindowStart = System.nanoTime();
    private static int fpsFrames = 0;
    private static int fps = 0;

    // --- Animacja klawiszy: W, A, S, D, LMB, RMB, SPACE ---
    private static final float[] ANIM = new float[7];
    private static long lastFrame = System.nanoTime();
    private static float frameDt = 0f;

    // --- Ostatnio narysowany obszar HUD-a na ekranie (dla edytora) ---
    private static int boundsX, boundsY, boundsW, boundsH;

    private FishHud() {
    }

    /** Wywoływane co klatkę przez HudRenderCallback. */
    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        frameUpdate();

        // W edytorze HUD rysuje sam ekran edytora.
        if (mc.currentScreen instanceof HudEditorScreen) {
            return;
        }

        GameOptions o = mc.options;
        // Ukryj przy F1 oraz przy F3 (żeby nie nachodziło na ekran debug).
        if (o.hudHidden || mc.player == null || mc.inGameHud.getDebugHud().shouldShowDebugHud()) {
            boundsW = 0;
            boundsH = 0;
            return;
        }
        draw(ctx, mc);
    }

    /** Rysuje HUD zgodnie z konfiguracją i zapamiętuje jego obszar. */
    public static void draw(DrawContext ctx, MinecraftClient mc) {
        FishConfig cfg = FishConfig.get();
        boundsW = 0;
        boundsH = 0;
        if (!cfg.hudEnabled) {
            return;
        }

        GameOptions o = mc.options;
        TextRenderer font = mc.textRenderer;

        // --- Wiersze karty informacyjnej ---
        List<Row> rows = new ArrayList<>();
        if (cfg.showFps) {
            rows.add(new Row("FPS", String.valueOf(fps), fps >= 60 ? GOOD : fps >= 30 ? OK : BAD));
        }
        if (cfg.showCps) {
            rows.add(new Row("CPS", CPS.getLeft() + " | " + CPS.getRight(), TEXT_IDLE));
        }
        if (cfg.showPing) {
            int ping = getPing(mc);
            if (ping >= 0) {
                rows.add(new Row("PING", ping + " ms", ping < 80 ? GOOD : ping < 150 ? OK : BAD));
            }
        }
        if (cfg.showCoords && mc.player != null) {
            rows.add(new Row("XYZ",
                    mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ(), TEXT_IDLE));
        }

        if (rows.isEmpty() && !cfg.showKeystrokes) {
            return;
        }

        // --- Wymiary (przed skalowaniem) ---
        int labelW = 0;
        int valueW = 0;
        for (Row row : rows) {
            labelW = Math.max(labelW, font.getWidth(row.label()));
            valueW = Math.max(valueW, font.getWidth(row.value()));
        }
        int rowH = font.fontHeight + ROW_EXTRA;
        int width = Math.max(MIN_WIDTH, labelW + valueW + PAD * 2 + 3 + 12);
        int infoH = rows.isEmpty() ? 0 : rows.size() * rowH + PAD * 2 - ROW_EXTRA;
        int keysH = 0;
        if (cfg.showKeystrokes) {
            keysH = KEY_H * 3 + GAP * 2;
            if (cfg.showSpace) {
                keysH += GAP + SPACE_H;
            }
        }
        int height = infoH + (infoH > 0 && keysH > 0 ? GAP : 0) + keysH;

        // --- Skala i pozycja (przycięta do ekranu) ---
        float scale = cfg.getScale();
        int screenW = Math.round(width * scale);
        int screenH = Math.round(height * scale);
        int maxX = Math.max(0, mc.getWindow().getScaledWidth() - screenW);
        int maxY = Math.max(0, mc.getWindow().getScaledHeight() - screenH);
        int px = Math.max(0, Math.min(cfg.x, maxX));
        int py = Math.max(0, Math.min(cfg.y, maxY));

        boundsX = px;
        boundsY = py;
        boundsW = screenW;
        boundsH = screenH;

        // --- Kolory z konfiguracji ---
        int accent = cfg.getAccent();
        int bgIdle = (cfg.getBackgroundAlpha() << 24) | BG_BASE;

        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate((float) px, (float) py, 0f);
        matrices.scale(scale, scale, 1f);

        int x = 0;
        int y = 0;

        // --- Karta informacyjna ---
        if (!rows.isEmpty()) {
            Ui.roundedRect(ctx, x, y, width, infoH, CARD_RADIUS, bgIdle);
            Ui.roundedBorder(ctx, x, y, width, infoH, CARD_RADIUS, CARD_BORDER);
            ctx.fill(x + 3, y + 4, x + 5, y + infoH - 4, accent); // pionowa "pigułka" akcentu
            int ty = y + PAD;
            for (Row row : rows) {
                ctx.drawText(font, row.label(), x + PAD + 4, ty, Ui.withAlpha(accent, 0xFF), false);
                ctx.drawText(font, row.value(), x + width - PAD - font.getWidth(row.value()), ty,
                        row.valueColor(), false);
                ty += rowH;
            }
            y += infoH + GAP;
        }

        // --- Keystrokes ---
        if (cfg.showKeystrokes) {
            long window = mc.getWindow().getHandle();
            boolean[] down = {
                    o.forwardKey.isPressed(),
                    o.leftKey.isPressed(),
                    o.backKey.isPressed(),
                    o.rightKey.isPressed(),
                    GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS,
                    GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS,
                    o.jumpKey.isPressed()
            };
            float speed = Math.min(1f, frameDt * 18f);
            for (int i = 0; i < ANIM.length; i++) {
                ANIM[i] += ((down[i] ? 1f : 0f) - ANIM[i]) * speed;
            }

            int keyW = (width - GAP * 2) / 3;
            drawKey(ctx, font, "W", x + keyW + GAP, y, keyW, KEY_H, ANIM[0], bgIdle, accent);
            y += KEY_H + GAP;
            drawKey(ctx, font, "A", x, y, keyW, KEY_H, ANIM[1], bgIdle, accent);
            drawKey(ctx, font, "S", x + keyW + GAP, y, keyW, KEY_H, ANIM[2], bgIdle, accent);
            drawKey(ctx, font, "D", x + (keyW + GAP) * 2, y, width - (keyW + GAP) * 2, KEY_H, ANIM[3], bgIdle, accent);
            y += KEY_H + GAP;

            int mouseW = (width - GAP) / 2;
            drawKey(ctx, font, "LMB", x, y, mouseW, KEY_H, ANIM[4], bgIdle, accent);
            drawKey(ctx, font, "RMB", x + mouseW + GAP, y, width - mouseW - GAP, KEY_H, ANIM[5], bgIdle, accent);
            y += KEY_H + GAP;

            if (cfg.showSpace) {
                drawKey(ctx, font, null, x, y, width, SPACE_H, ANIM[6], bgIdle, accent);
            }
        }

        matrices.pop();
    }

    // --- Obszar HUD-a na ekranie (używany przez edytor) ---
    public static int getBoundsX() { return boundsX; }
    public static int getBoundsY() { return boundsY; }
    public static int getBoundsW() { return boundsW; }
    public static int getBoundsH() { return boundsH; }

    public static boolean isInsideBounds(double mx, double my) {
        return boundsW > 0
                && mx >= boundsX && mx <= boundsX + boundsW
                && my >= boundsY && my <= boundsY + boundsH;
    }

    /** Ping gracza w ms albo -1, jeśli niedostępny. */
    private static int getPing(MinecraftClient mc) {
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null || mc.player == null) {
            return -1;
        }
        UUID id = mc.player.getUuid();
        PlayerListEntry entry = handler.getPlayerListEntry(id);
        return entry == null ? -1 : entry.getLatency();
    }

    private static void frameUpdate() {
        long now = System.nanoTime();
        frameDt = Math.min((now - lastFrame) / 1_000_000_000f, 0.1f);
        lastFrame = now;

        fpsFrames++;
        long elapsed = now - fpsWindowStart;
        if (elapsed >= 500_000_000L) { // odświeżaj co 0,5 s
            fps = Math.round(fpsFrames * 1_000_000_000f / elapsed);
            fpsFrames = 0;
            fpsWindowStart = now;
        }
    }

    /** Klawisz: zaokrąglone tło, ramka i płynne wypełnienie kolorem akcentu po wciśnięciu. Label null = pasek spacji. */
    private static void drawKey(DrawContext ctx, TextRenderer font, String label,
                                int x, int y, int w, int h, float t, int bgIdle, int accent) {
        int bg = Ui.lerpColor(bgIdle, Ui.withAlpha(accent, 0xE6), t);
        Ui.roundedRect(ctx, x, y, w, h, KEY_RADIUS, bg);
        Ui.roundedBorder(ctx, x, y, w, h, KEY_RADIUS, Ui.lerpColor(KEY_BORDER, Ui.withAlpha(accent, 0xFF), t));

        int textColor = Ui.lerpColor(TEXT_IDLE, TEXT_PRESSED, t);
        if (label == null) {
            int lineW = Math.min(w - 12, 22);
            int lx = x + (w - lineW) / 2;
            int ly = y + h / 2;
            ctx.fill(lx, ly - 1, lx + lineW, ly + 1, textColor);
        } else {
            int tx = x + (w - font.getWidth(label)) / 2;
            int ty = y + (h - font.fontHeight) / 2 + 1;
            ctx.drawText(font, label, tx, ty, textColor, false);
        }
    }
}
