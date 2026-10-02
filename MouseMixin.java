package dev.fishclient.mixin;

import dev.fishclient.hud.FishHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Przechwytuje każde kliknięcie myszy w momencie jego wystąpienia (a nie raz na tick),
 * dzięki czemu CPS jest dokładny nawet przy bardzo szybkim klikaniu.
 */
@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void fishclient$countClick(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) {
            return;
        }
        // Liczymy tylko kliknięcia w grze, nie w menu/inwentarzu.
        if (MinecraftClient.getInstance().currentScreen == null) {
            FishHud.CPS.registerClick(button);
        }
    }
}
