package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.NumberSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Linke Alt-Taste halten: eine eigene Kamera kreist um den Spieler, gesteuert mit der Maus.
 * Der Kopf des Spielers bewegt sich dabei nicht, und die Kamera geht durch Wände.
 */
public class FreeLook extends Module {
    public final NumberSetting distance = add(new NumberSetting("distance", "Abstand", 2, 12, 4, 0.5));

    private CameraEntity cam;
    private float yaw, pitch;
    private CameraType previous;

    public FreeLook() {
        super("FreeLook", "freelook", Category.UTILITY, GLFW.GLFW_KEY_LEFT_ALT, true);
    }

    public boolean active() {
        return isEnabled() && cam != null;
    }

    public void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Mth.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
    }

    @Override
    public void onEnable() {
        Minecraft mc = mc();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || ModuleCompat.freeCamActive()) return;
        yaw = p.getYRot();
        pitch = p.getXRot();
        previous = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        cam = new CameraEntity(mc.level, p.getX(), p.getEyeY(), p.getZ());
        frame(1f);
        mc.setCameraEntity(cam);
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (cam != null && mc.player != null) mc.setCameraEntity(mc.player);
        if (previous != null) mc.options.setCameraType(previous);
        previous = null;
        cam = null;
    }

    @Override
    public void onTick() {
        if (cam != null && (mc().player == null || mc().level != cam.level())) {
            setEnabled(false);
        }
    }

    /** Jedes Bild: Kamera auf die Kreisbahn hinter dem Spieler setzen. */
    public void frame(float partialTick) {
        if (cam == null) return;
        LocalPlayer p = mc().player;
        if (p == null) return;
        Vec3 eye = p.getPosition(partialTick).add(0, p.getEyeHeight(), 0);
        Vec3 pos = eye.subtract(Vec3.directionFromRotation(pitch, yaw).scale(distance.get()));
        cam.place(pos.x, pos.y, pos.z, yaw, pitch);
    }
}
