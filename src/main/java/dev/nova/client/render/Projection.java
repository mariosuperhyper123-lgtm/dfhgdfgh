package dev.nova.client.render;

import dev.nova.client.util.CameraAccess;
import dev.nova.client.util.Compat;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;

/**
 * Rechnet Weltkoordinaten in Bildschirmkoordinaten um und zeichnet damit 3D-Linien, Boxen und
 * Tracer auf der HUD-Ebene. Dadurch ist alles automatisch durch Wände sichtbar.
 */
public final class Projection {
    private static final double NEAR = 0.05;

    /** Echtes Sichtfeld der Welt in Grad (vom GameRendererMixin), sonst Wert aus den Optionen. */
    private static volatile float capturedFov = -1f;
    private static volatile long capturedAt;

    public static void captureFov(float fov) {
        capturedFov = fov;
        capturedAt = System.currentTimeMillis();
    }

    private static double currentFov(Minecraft mc) {
        if (capturedFov > 0 && System.currentTimeMillis() - capturedAt < 1000) return capturedFov;
        return mc.options.fov().get();
    }

    private final Vec3 cam;
    private final double fx, fy, fz, rx, rz, ux, uy, uz;
    private final double tanHalf, aspect, w, h;
    private final float scale;

    private Projection(Vec3 cam, float yaw, float pitch, double fovDeg, double w, double h, float scale) {
        this.cam = cam;
        Vec3 f = Vec3.directionFromRotation(pitch, yaw);
        double yawRad = Math.toRadians(yaw);
        this.fx = f.x;
        this.fy = f.y;
        this.fz = f.z;
        this.rx = -Math.cos(yawRad);
        this.rz = -Math.sin(yawRad);
        this.ux = -rz * fy;
        this.uy = rz * fx - rx * fz;
        this.uz = rx * fy;
        this.tanHalf = Math.tan(Math.toRadians(fovDeg) / 2.0);
        this.w = w;
        this.h = h;
        this.aspect = w / h;
        this.scale = scale;
    }

    public static Projection create(Minecraft mc, GuiGraphicsExtractor ctx, float partialTick) {
        Vec3 pos;
        float yaw, pitch;
        CameraAccess.State cam = CameraAccess.read(mc);
        if (cam != null) {
            // Echte Kamera: passt auch mit fremden FreeCam-Mods und in jeder F5-Ansicht
            pos = cam.pos();
            yaw = cam.yaw();
            pitch = cam.pitch();
        } else {
            Entity camEntity = mc.getCameraEntity();
            if (camEntity == null) return null;
            pos = camEntity.getPosition(partialTick).add(0, camEntity.getEyeHeight(), 0);
            yaw = camEntity.getViewYRot(partialTick);
            pitch = camEntity.getViewXRot(partialTick);
            if (camEntity == mc.player) {
                CameraType type = mc.options.getCameraType();
                if (type == CameraType.THIRD_PERSON_BACK) {
                    pos = pos.subtract(Vec3.directionFromRotation(pitch, yaw).scale(4.0));
                } else if (type == CameraType.THIRD_PERSON_FRONT) {
                    yaw += 180f;
                    pitch = -pitch;
                    pos = pos.subtract(Vec3.directionFromRotation(pitch, yaw).scale(4.0));
                }
            }
        }

        // Bildgröße exakt aus der echten Fenstergröße (die GUI-Größe ist gerundet und verzerrt sonst das Seitenverhältnis)
        float scale = Compat.guiScale();
        double w = mc.getWindow().getWidth() / (double) scale;
        double h = mc.getWindow().getHeight() / (double) scale;
        return new Projection(pos, yaw, pitch, currentFov(mc), w, h, scale);
    }

    public Vec3 camPos() {
        return cam;
    }

    private double[] toCam(double px, double py, double pz) {
        double dx = px - cam.x, dy = py - cam.y, dz = pz - cam.z;
        return new double[]{dx * rx + dz * rz, dx * ux + dy * uy + dz * uz, dx * fx + dy * fy + dz * fz};
    }

    private float[] screen(double[] c) {
        double sx = w / 2 + (c[0] / (c[2] * tanHalf * aspect)) * w / 2;
        double sy = h / 2 - (c[1] / (c[2] * tanHalf)) * h / 2;
        return new float[]{(float) sx, (float) sy};
    }

    public void line3d(GuiGraphicsExtractor ctx, double ax, double ay, double az, double bx, double by, double bz, int color) {
        double[] a = toCam(ax, ay, az);
        double[] b = toCam(bx, by, bz);
        if (a[2] <= NEAR && b[2] <= NEAR) return;
        if (a[2] <= NEAR) a = clip(b, a);
        else if (b[2] <= NEAR) b = clip(a, b);
        float[] sa = screen(a), sb = screen(b);
        line2d(ctx, sa[0], sa[1], sb[0], sb[1], color);
    }

    private static double[] clip(double[] front, double[] back) {
        double t = (front[2] - NEAR) / (front[2] - back[2]);
        return new double[]{front[0] + (back[0] - front[0]) * t, front[1] + (back[1] - front[1]) * t, NEAR};
    }

    public void box3d(GuiGraphicsExtractor ctx, AABB b, int color) {
        double x1 = b.minX, y1 = b.minY, z1 = b.minZ, x2 = b.maxX, y2 = b.maxY, z2 = b.maxZ;
        line3d(ctx, x1, y1, z1, x2, y1, z1, color);
        line3d(ctx, x2, y1, z1, x2, y1, z2, color);
        line3d(ctx, x2, y1, z2, x1, y1, z2, color);
        line3d(ctx, x1, y1, z2, x1, y1, z1, color);
        line3d(ctx, x1, y2, z1, x2, y2, z1, color);
        line3d(ctx, x2, y2, z1, x2, y2, z2, color);
        line3d(ctx, x2, y2, z2, x1, y2, z2, color);
        line3d(ctx, x1, y2, z2, x1, y2, z1, color);
        line3d(ctx, x1, y1, z1, x1, y2, z1, color);
        line3d(ctx, x2, y1, z1, x2, y2, z1, color);
        line3d(ctx, x2, y1, z2, x2, y2, z2, color);
        line3d(ctx, x1, y1, z2, x1, y2, z2, color);
    }

    public void fillBox(GuiGraphicsExtractor ctx, AABB b, int color) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double[] c = toCam((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
            if (c[2] <= NEAR) return;
            float[] s = screen(c);
            minX = Math.min(minX, s[0]);
            minY = Math.min(minY, s[1]);
            maxX = Math.max(maxX, s[0]);
            maxY = Math.max(maxY, s[1]);
        }
        if (maxX < 0 || maxY < 0 || minX > w || minY > h) return;
        Matrix3x2fStack m = ctx.pose();
        m.pushMatrix();
        m.scale(1f / scale, 1f / scale);
        ctx.fill(Math.round(minX * scale), Math.round(minY * scale), Math.round(maxX * scale), Math.round(maxY * scale), color);
        m.popMatrix();
    }

    public void tracer(GuiGraphicsExtractor ctx, Vec3 target, int color) {
        float cx = (float) (w / 2), cy = (float) (h / 2);
        double[] c = toCam(target.x, target.y, target.z);
        if (c[2] > NEAR) {
            float[] s = screen(c);
            line2d(ctx, cx, cy, s[0], s[1], color);
        } else {
            double dx = c[0], dy = -c[1];
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 1e-6) return;
            double reach = Math.max(w, h);
            line2d(ctx, cx, cy, (float) (cx + dx / len * reach), (float) (cy + dy / len * reach), color);
        }
    }

    public void line2d(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f || !Float.isFinite(len)) return;
        len = Math.min(len, 20000f);
        Matrix3x2fStack m = ctx.pose();
        m.pushMatrix();
        m.translate(x1, y1);
        m.rotate((float) Math.atan2(dy, dx));
        m.scale(1f / scale, 1f / scale);
        ctx.fill(0, 0, Math.round(len * scale), 1, color);
        m.popMatrix();
    }
}
