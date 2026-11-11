package dev.nova.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.nova.client.render.Projection;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Merkt sich das echte Sichtfeld der Welt (inkl. Sprint-Effekt und Zoom) für ESP und Tracer.
 * Wichtig: Minecraft fragt das Sichtfeld auch für die Hand ab (dann immer 70°, useFovSetting = false);
 * diese Abfrage wird ignoriert.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"), require = 0)
    private float nova$captureFov(float fov, @Local(argsOnly = true) boolean useFovSetting) {
        if (useFovSetting) Projection.captureFov(fov);
        return fov;
    }
}
