package dev.nova.client.gui;

import dev.nova.client.util.Compat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

/** Kleine Zeichenhilfen für das GUI: dünne Linien, Kreise, abgerundete Rechtecke, Farben. */
public final class Draw2D {
    private Draw2D() {}

    public static int argb(int a, int r, int g, int b) {
        return (Math.max(0, Math.min(255, a)) << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int rgb, float alpha) {
        int base = (rgb >>> 24) == 0 ? 255 : (rgb >>> 24);
        int a = Math.round(Math.max(0, Math.min(1, alpha)) * base);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int lerpColor(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = (int) (((c1 >>> 24) & 255) + (((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t);
        int r = (int) (((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int g = (int) (((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return argb(a, r, g, b);
    }

    public static int hsv(float h, float s, float v) {
        h = (h % 1f + 1f) % 1f;
        int i = (int) (h * 6);
        float f = h * 6 - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static void line(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, int color, int thickness) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;
        float s = Compat.guiScale();
        Matrix3x2fStack m = ctx.pose();
        m.pushMatrix();
        m.translate(x1, y1);
        m.rotate((float) Math.atan2(dy, dx));
        m.scale(1f / s, 1f / s);
        ctx.fill(0, -thickness / 2, Math.round(len * s), thickness - thickness / 2, color);
        m.popMatrix();
    }

    public static void circle(GuiGraphicsExtractor ctx, float cx, float cy, float radius, int color) {
        float s = Compat.guiScale();
        int r = Math.max(1, Math.round(radius * s));
        Matrix3x2fStack m = ctx.pose();
        m.pushMatrix();
        m.translate(cx, cy);
        m.scale(1f / s, 1f / s);
        int step = Math.max(1, r / 40);
        for (int dy = -r; dy < r; dy += step) {
            int w = (int) Math.sqrt((double) r * r - (double) dy * dy);
            ctx.fill(-w, dy, w, dy + step, color);
        }
        m.popMatrix();
    }

    public static void glow(GuiGraphicsExtractor ctx, float cx, float cy, float radius, int rgb, float alpha) {
        for (int i = 0; i < 5; i++) {
            circle(ctx, cx, cy, radius * (1f - i * 0.18f), withAlpha(rgb | 0xFF000000, alpha * 0.22f));
        }
    }

    public static void roundRect(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1 + 2, y1, x2 - 2, y2, color);
        ctx.fill(x1 + 1, y1 + 1, x1 + 2, y2 - 1, color);
        ctx.fill(x2 - 2, y1 + 1, x2 - 1, y2 - 1, color);
        ctx.fill(x1, y1 + 2, x1 + 1, y2 - 2, color);
        ctx.fill(x2 - 1, y1 + 2, x2, y2 - 2, color);
    }

    public static float easeOutCubic(float t) {
        t = Math.max(0, Math.min(1, t));
        float f = 1 - t;
        return 1 - f * f * f;
    }
}
