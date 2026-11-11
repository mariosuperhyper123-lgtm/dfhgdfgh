package dev.nova.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nova.client.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** FreeCam/FreeLook: Fadenkreuz (Abbauen/Benutzen) bleibt beim Spieler, nicht bei der Kamera. */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @WrapOperation(method = "pick", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;"))
    private Entity nova$pickFromPlayer(Minecraft mc, Operation<Entity> original) {
        ModuleManager mm = ModuleManager.get();
        if (mm != null && (mm.freeCam.active() || mm.freeLook.active()) && mc.player != null) return mc.player;
        return original.call(mc);
    }
}
