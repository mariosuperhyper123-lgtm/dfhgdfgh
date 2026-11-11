package dev.nova.client.mixin;

import dev.nova.client.module.ModuleManager;
import dev.nova.client.module.modules.Fullbright;
import dev.nova.client.module.modules.Zoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fullbright (Gamma hoch) und Zoom (Sichtfeld runter) über die Optionswerte. */
@Mixin(OptionInstance.class)
public class OptionInstanceMixin {
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void nova$override(CallbackInfoReturnable<Object> cir) {
        ModuleManager mm = ModuleManager.get();
        if (mm == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return;
        Object self = this;
        if (mm.fullbright.isEnabled() && self == mc.options.gamma()) {
            cir.setReturnValue(Fullbright.GAMMA);
        } else if (mm.zoom.isEnabled() && self == mc.options.fov() && cir.getReturnValue() instanceof Integer fov) {
            cir.setReturnValue(Math.max(1, (int) Math.round(fov / Zoom.FACTOR)));
        }
    }
}
