package dev.nova.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.nova.client.module.ModuleManager;
import dev.nova.client.module.modules.ViewModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Unique
    private static ViewModel nova$active() {
        ModuleManager mm = ModuleManager.get();
        return mm != null && mm.viewModel.isEnabled() ? mm.viewModel : null;
    }

    @Unique
    private static float nova$mirror(AbstractClientPlayer player, InteractionHand hand) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        return arm == HumanoidArm.LEFT ? -1f : 1f;
    }

    @Inject(method = {"renderArmWithItem", "submitArmWithItem"}, at = @At("HEAD"), require = 0)
    private void nova$pushPosition(CallbackInfo ci,
                                   @Local(argsOnly = true) AbstractClientPlayer player,
                                   @Local(argsOnly = true) InteractionHand hand,
                                   @Local(argsOnly = true) PoseStack poseStack) {
        poseStack.pushPose();
        ViewModel vm = nova$active();
        if (vm == null) return;
        float mirror = nova$mirror(player, hand);
        poseStack.translate(vm.posX.getFloat() * mirror, vm.posY.getFloat(), vm.posZ.getFloat());
    }

    @Inject(method = {"renderArmWithItem", "submitArmWithItem"}, at = @At("RETURN"), require = 0)
    private void nova$popPosition(CallbackInfo ci, @Local(argsOnly = true) PoseStack poseStack) {
        poseStack.popPose();
    }

    @Inject(method = {"renderArmWithItem", "submitArmWithItem"}, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem"))
    private void nova$rotateScale(CallbackInfo ci,
                                  @Local(argsOnly = true) AbstractClientPlayer player,
                                  @Local(argsOnly = true) InteractionHand hand,
                                  @Local(argsOnly = true) PoseStack poseStack) {
        ViewModel vm = nova$active();
        if (vm == null) return;
        float mirror = nova$mirror(player, hand);
        poseStack.mulPose(Axis.XP.rotationDegrees(vm.rotX.getFloat()));
        poseStack.mulPose(Axis.YP.rotationDegrees(vm.rotY.getFloat() * mirror));
        poseStack.mulPose(Axis.ZP.rotationDegrees(vm.rotZ.getFloat() * mirror));
        float s = vm.scale.getFloat();
        poseStack.scale(s, s, s);
    }
}
