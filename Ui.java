package dev.fishclient.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Drobne narzędzia rysowania: zaokrąglone prostokąty, obramowania, mieszanie kolorów.
 */
public final class Ui {
    private Ui() {
    }

    /** Wypełniony prostokąt z zaokrąglonymi rogami (promień w pikselach). */
    public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            int inset = cornerInset(r, i);
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);                 // góra
            ctx.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);         // dół
        }
        if (h - 2 * r > 0) {
            ctx.fill(x, y + r, x + w, y + h - r, color);
        }
    }

    /** Jednopikselowa ramka pasująca do {@link #roundedRect}. */
    public static void roundedBorder(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        ctx.fill(x + r, y, x + w - r, y + 1, color);
        ctx.fill(x + r, y + h - 1, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + 1, y + h - r, color);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int inset = cornerInset(r, i);
            ctx.fill(x + inset, y + i, x + inset + 1, y + i + 1, color);
            ctx.fill(x + w - inset - 1, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - 1 - i, x + inset + 1, y + h - i, color);
            ctx.fill(x + w - inset - 1, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    private static int cornerInset(int r, int row) {
        double dy = r - row - 0.5;
        return (int) Math.round(r - Math.sqrt(r * (double) r - dy * dy));
    }

    public static boolean inside(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    public static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, t);
        int r = lerp((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, t);
        int g = lerp((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, t);
        int b = lerp(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    /** Zmienia kanał alfa koloru (0-255). */
    public static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0xFFFFFF);
    }
}
