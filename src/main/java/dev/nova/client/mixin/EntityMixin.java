package dev.nova.client.mixin;

import dev.nova.client.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leitet die Mausbewegung bei FreeCam/FreeLook an die Kamera statt an den Spieler. */
@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void nova$redirectLook(double dx, double dy, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || (Object) this != mc.player) return;
        ModuleManager mm = ModuleManager.get();
        if (mm == null) return;
        if (mm.freeCam.active()) {
            mm.freeCam.look(dx, dy);
            ci.cancel();
        } else if (mm.freeLook.active()) {
            mm.freeLook.look(dx, dy);
            ci.cancel();
        }
    }
}
