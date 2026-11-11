package dev.nova.client.gui;

import dev.nova.client.util.Compat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Random;

/**
 * Basis für alle Nova-Menüs: animierter Hintergrund mit Farbverlauf, wandernden Lichtflecken
 * und einem Netz aus schwebenden Partikeln.
 */
public abstract class NovaScreen extends Screen {
    private static final int PARTICLES = 60;
    private static final float[] PX = new float[PARTICLES], PY = new float[PARTICLES];
    private static final float[] VX = new float[PARTICLES], VY = new float[PARTICLES], SIZE = new float[PARTICLES];
    private static boolean particlesReady;
    private static long lastFrame;

    protected final long openedAt = System.currentTimeMillis();

    protected NovaScreen(Component title) {
        super(title);
    }

    protected float openProgress() {
        return Draw2D.easeOutCubic((System.currentTimeMillis() - openedAt) / 350f);
    }

    protected static float time() {
        return (System.currentTimeMillis() % 1_000_000L) / 1000f;
    }

    public static int accent() {
        return Draw2D.hsv(0.72f + 0.07f * (float) Math.sin(time() * 0.6f), 0.65f, 1f);
    }

    /** Zurück zu einem anderen Bildschirm (oder schließen bei null). */
    protected void open(Screen screen) {
        Compat.setScreen(screen);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        float a = openProgress();
        float t = time();
        ctx.fillGradient(0, 0, width, height,
                Draw2D.argb((int) (0xC8 * a), 6, 8, 22),
                Draw2D.argb((int) (0xE6 * a), 16, 6, 34));

        Draw2D.glow(ctx, width * (0.25f + 0.12f * (float) Math.sin(t * 0.21f)),
                height * (0.30f + 0.15f * (float) Math.cos(t * 0.17f)), 110, 0x7B3FE4, 0.35f * a);
        Draw2D.glow(ctx, width * (0.78f + 0.10f * (float) Math.cos(t * 0.13f)),
                height * (0.65f + 0.12f * (float) Math.sin(t * 0.19f)), 130, 0x2F6BFF, 0.30f * a);
        Draw2D.glow(ctx, width * (0.55f + 0.20f * (float) Math.sin(t * 0.09f)),
                height * (0.95f + 0.05f * (float) Math.cos(t * 0.23f)), 90, 0xE04FB0, 0.22f * a);

        drawParticles(ctx, a);

        ctx.fillGradient(0, 0, width, 40, Draw2D.argb((int) (0x70 * a), 0, 0, 0), 0x00000000);
        ctx.fillGradient(0, height - 40, width, height, 0x00000000, Draw2D.argb((int) (0x70 * a), 0, 0, 0));
    }

    private void drawParticles(GuiGraphicsExtractor ctx, float alpha) {
        if (!particlesReady) {
            Random rnd = new Random();
            for (int i = 0; i < PARTICLES; i++) {
                PX[i] = rnd.nextFloat();
                PY[i] = rnd.nextFloat();
                double ang = rnd.nextDouble() * Math.PI * 2;
                float speed = 0.004f + rnd.nextFloat() * 0.012f;
                VX[i] = (float) Math.cos(ang) * speed;
                VY[i] = (float) Math.sin(ang) * speed;
                SIZE[i] = 0.6f + rnd.nextFloat() * 1.2f;
            }
            particlesReady = true;
            lastFrame = System.currentTimeMillis();
        }
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        for (int i = 0; i < PARTICLES; i++) {
            PX[i] = (PX[i] + VX[i] * dt + 1f) % 1f;
            PY[i] = (PY[i] + VY[i] * dt + 1f) % 1f;
        }

        float maxDist = Math.min(width, height) * 0.18f;
        for (int i = 0; i < PARTICLES; i++) {
            float xi = PX[i] * width, yi = PY[i] * height;
            for (int j = i + 1; j < PARTICLES; j++) {
                float dx = PX[j] * width - xi, dy = PY[j] * height - yi;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < maxDist) {
                    Draw2D.line(ctx, xi, yi, xi + dx, yi + dy,
                            Draw2D.withAlpha(0xFF9FA8FF, (1 - d / maxDist) * 0.35f * alpha), 1);
                }
            }
        }
        float t = time();
        for (int i = 0; i < PARTICLES; i++) {
            float tw = 0.6f + 0.4f * (float) Math.sin(t * 2 + i);
            Draw2D.circle(ctx, PX[i] * width, PY[i] * height, SIZE[i], Draw2D.withAlpha(0xFFE6E8FF, 0.8f * tw * alpha));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
