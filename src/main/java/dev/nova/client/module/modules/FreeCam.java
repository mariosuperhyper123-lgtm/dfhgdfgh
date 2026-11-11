package dev.nova.client.module.modules;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.NumberSetting;
import dev.nova.client.util.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.glfw.GLFW;

/**
 * Löst die Kamera vom Spieler. Die Kamera fliegt frei durch Wände, der Spieler bleibt stehen
 * und macht genau das weiter, was er beim Einschalten gemacht hat (laufen, abbauen, benutzen ...).
 */
public class FreeCam extends Module {
    public final NumberSetting speed = add(new NumberSetting("speedBps", "Geschwindigkeit (Blöcke/s)", 1, 60, 15, 1));

    private CameraEntity cam;
    private double x, y, z;
    private float yaw, pitch;
    private long lastFrame;
    private ClientInput savedInput;
    private ClientLevel level;
    private boolean holdAttack, holdUse;

    public FreeCam() {
        super("FreeCam", "freecam", Category.UTILITY, InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, false);
    }

    public boolean active() {
        return isEnabled() && cam != null;
    }

    public Entity camera() {
        return cam;
    }

    /** Mausbewegung (aus dem EntityMixin). */
    public void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Mth.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
        if (cam != null) cam.place(x, y, z, yaw, pitch);
    }

    @Override
    public void onEnable() {
        Minecraft mc = mc();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        ModuleCompat.stopFreeLook();
        level = mc.level;

        x = p.getX();
        y = p.getEyeY();
        z = p.getZ();
        yaw = p.getYRot();
        pitch = p.getXRot();
        cam = new CameraEntity(mc.level, x, y, z);
        cam.place(x, y, z, yaw, pitch);
        lastFrame = System.nanoTime();

        // Aktuelle Eingaben einfrieren, damit der Spieler einfach weitermacht
        savedInput = p.input;
        p.input = new FrozenInput(savedInput.keyPresses, savedInput.getMoveVector());
        holdAttack = mc.options.keyAttack.isDown();
        holdUse = mc.options.keyUse.isDown();

        mc.setCameraEntity(cam);
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc.player != null) {
            if (savedInput != null) mc.player.input = savedInput;
            mc.setCameraEntity(mc.player);
        }
        if (holdAttack) mc.options.keyAttack.setDown(false);
        if (holdUse) mc.options.keyUse.setDown(false);
        cam = null;
        savedInput = null;
        level = null;
    }

    /** Am Anfang jedes Ticks: Maus-Aktionen auf den eingefrorenen Stand setzen. */
    public void preTick() {
        if (!active()) return;
        Options o = mc().options;
        while (o.keyAttack.consumeClick()) { }
        while (o.keyUse.consumeClick()) { }
        while (o.keyPickItem.consumeClick()) { }
        o.keyAttack.setDown(holdAttack);
        o.keyUse.setDown(holdUse);

        // Ziel (Block zum Abbauen/Benutzen) immer vom Spieler aus bestimmen, nicht von der Kamera
        Minecraft mc = mc();
        LocalPlayer p = mc.player;
        if (p != null) {
            mc.hitResult = p.pick(p.blockInteractionRange(), 1f, false);
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (cam == null || mc.player == null || mc.level != level) {
            setEnabled(false);
            return;
        }
        frame();
    }

    /** Jedes Bild: Kamera abhängig von der echten vergangenen Zeit bewegen. */
    public void frame() {
        if (!active()) return;
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        if (Compat.screen() != null) {
            cam.place(x, y, z, yaw, pitch);
            return;
        }

        Options o = mc().options;
        double forward = 0, strafe = 0, up = 0;
        if (o.keyUp.isDown()) forward++;
        if (o.keyDown.isDown()) forward--;
        if (o.keyLeft.isDown()) strafe++;
        if (o.keyRight.isDown()) strafe--;
        if (o.keyJump.isDown()) up++;
        if (o.keyShift.isDown()) up--;

        double blocksPerSecond = speed.get() * (o.keySprint.isDown() ? 2.5 : 1.0);
        double step = blocksPerSecond * dt;
        double rad = Math.toRadians(yaw);
        double dx = -Math.sin(rad) * forward + Math.cos(rad) * strafe;
        double dz = Math.cos(rad) * forward + Math.sin(rad) * strafe;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > 1) {
            dx /= len;
            dz /= len;
        }
        x += dx * step;
        y += up * step;
        z += dz * step;
        cam.place(x, y, z, yaw, pitch);
    }

    /** Eingabe, die immer gleich bleibt. */
    private static final class FrozenInput extends ClientInput {
        private final Input frozen;
        private final Vec2 move;

        FrozenInput(Input frozen, Vec2 move) {
            this.frozen = frozen;
            this.move = move;
            this.keyPresses = frozen;
            this.moveVector = move;
        }

        @Override
        public void tick() {
            this.keyPresses = frozen;
            this.moveVector = move;
        }
    }
}
