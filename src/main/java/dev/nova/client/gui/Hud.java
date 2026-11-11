package dev.nova.client.gui;

import dev.nova.client.NovaClient;
import dev.nova.client.module.Module;
import dev.nova.client.module.ModuleManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

import java.util.Comparator;
import java.util.List;

public final class Hud {
    private static final int ACCENT = 0xFF7FD4FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;

    private Hud() {}

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        ModuleManager mm = ModuleManager.get();
        if (mm == null || !mm.hud.isEnabled()) return;
        LocalPlayer p = mc.player;
        if (p == null) return;

        Font font = mc.font;
        int y = 4;
        ctx.text(font, NovaClient.NAME, 4, y, ACCENT, true);
        y += 11;
        ctx.text(font, "FPS: " + mc.getFps(), 4, y, WHITE, true);
        y += 10;
        ctx.text(font, String.format("XYZ: %.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ()), 4, y, WHITE, true);
        y += 10;
        ctx.text(font, "Richtung: " + p.getDirection().getSerializedName(), 4, y, GRAY, true);
        if (mm.heavyChunks.isEnabled()) {
            y += 10;
            int n = mm.heavyChunks.count();
            ctx.text(font, "Heavy Chunks: " + n, 4, y, n > 0 ? 0xFFFF8833 : GRAY, true);
        }

        List<Module> active = mm.all().stream()
                .filter(Module::isEnabled)
                .filter(m -> m != mm.hud)
                .sorted(Comparator.comparingInt((Module m) -> font.width(m.getName())).reversed())
                .toList();
        int screenW = ctx.guiWidth();
        int ry = 4;
        for (Module m : active) {
            int w = font.width(m.getName());
            ctx.fill(screenW - w - 6, ry - 1, screenW, ry + 9, 0x80000000);
            ctx.fill(screenW - 2, ry - 1, screenW, ry + 9, ACCENT);
            ctx.text(font, m.getName(), screenW - w - 4, ry, WHITE, true);
            ry += 10;
        }
    }
}
