package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.util.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

/** Wechselt beim Abbauen automatisch auf das schnellste Werkzeug in der Hotbar. */
public class AutoTool extends Module {
    public AutoTool() {
        super("AutoTool", "autotool", Category.UTILITY, GLFW.GLFW_KEY_UNKNOWN, false);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc.player == null || mc.level == null || Compat.screen() != null) return;
        if (!mc.options.keyAttack.isDown()) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockState state = mc.level.getBlockState(hit.getBlockPos());
        if (state.isAir()) return;

        Inventory inv = mc.player.getInventory();
        int best = inv.getSelectedSlot();
        float bestSpeed = inv.getItem(best).getDestroySpeed(state);
        for (int i = 0; i < 9; i++) {
            float speed = inv.getItem(i).getDestroySpeed(state);
            if (speed > bestSpeed) {
                best = i;
                bestSpeed = speed;
            }
        }
        if (best != inv.getSelectedSlot()) inv.setSelectedSlot(best);
    }
}
