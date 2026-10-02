package dev.fishclient.hud;

import java.util.ArrayDeque;

import org.lwjgl.glfw.GLFW;

/**
 * Liczy kliknięcia na sekundę metodą przesuwanego okna (ostatnie 1000 ms).
 * Wszystkie wywołania następują na wątku renderowania, więc synchronizacja nie jest potrzebna.
 */
public final class CpsTracker {
    private static final long WINDOW_NANOS = 1_000_000_000L;

    private final ArrayDeque<Long> left = new ArrayDeque<>();
    private final ArrayDeque<Long> right = new ArrayDeque<>();

    /** Rejestruje naciśnięcie przycisku myszy (GLFW_MOUSE_BUTTON_*). */
    public void registerClick(int button) {
        long now = System.nanoTime();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            left.addLast(now);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            right.addLast(now);
        }
    }

    public int getLeft() {
        return count(left);
    }

    public int getRight() {
        return count(right);
    }

    private static int count(ArrayDeque<Long> clicks) {
        long now = System.nanoTime();
        while (!clicks.isEmpty() && now - clicks.peekFirst() > WINDOW_NANOS) {
            clicks.pollFirst();
        }
        return clicks.size();
    }
}
