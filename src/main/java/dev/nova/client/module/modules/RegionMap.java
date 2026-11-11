package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

/** Kleine Karte oben rechts mit Gelände, Chunk- und Region-Gitter (Regionen = 512×512 Blöcke). */
public class RegionMap extends Module {
    public final NumberSetting zoom = add(new NumberSetting("zoom", "Blöcke pro Pixel", 1, 4, 2, 1));

    private static final int CELLS = 48; // Kartenauflösung (48×48 Felder)
    private final int[] colors = new int[CELLS * CELLS];
    private int centerX, centerZ, step = 2;

    public RegionMap() {
        super("RegionMap", "regionmap", Category.UTILITY, GLFW.GLFW_KEY_UNKNOWN, false);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        ClientLevel level = mc.level;
        LocalPlayer p = mc.player;
        if (level == null || p == null) return;
        step = zoom.getInt();
        centerX = p.getBlockX();
        centerZ = p.getBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < CELLS; i++) {
            for (int j = 0; j < CELLS; j++) {
                int bx = centerX + (i - CELLS / 2) * step;
                int bz = centerZ + (j - CELLS / 2) * step;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz) - 1;
                pos.set(bx, y, bz);
                BlockState state = level.getBlockState(pos);
                int col = state.getMapColor(level, pos).col;
                if (col == 0) col = 0x101018;
                // etwas Höhen-Schattierung
                float shade = 0.75f + Math.max(-0.25f, Math.min(0.25f, (y - p.getBlockY()) / 64f));
                int r = Math.min(255, (int) (((col >> 16) & 255) * shade));
                int g = Math.min(255, (int) (((col >> 8) & 255) * shade));
                int b = Math.min(255, (int) ((col & 255) * shade));
                colors[j * CELLS + i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
    }

    public void render(GuiGraphicsExtractor ctx) {
        if (!isEnabled()) return;
        Minecraft mc = mc();
        LocalPlayer p = mc.player;
        if (p == null) return;
        Font font = mc.font;

        int px = 2; // GUI-Pixel pro Feld
        int size = CELLS * px;
        int x0 = ctx.guiWidth() - size - 6;
        int y0 = ctx.guiHeight() - size - 22;

        ctx.fill(x0 - 2, y0 - 2, x0 + size + 2, y0 + size + 2, 0xC0101018);
        for (int j = 0; j < CELLS; j++) {
            for (int i = 0; i < CELLS; i++) {
                int c = colors[j * CELLS + i];
                if (c != 0) ctx.fill(x0 + i * px, y0 + j * px, x0 + i * px + px, y0 + j * px + px, c);
            }
        }

        // Chunk- (16) und Region-Linien (512)
        for (int i = 0; i < CELLS; i++) {
            int bx = centerX + (i - CELLS / 2) * step;
            int bz = centerZ + (i - CELLS / 2) * step;
            if (Math.floorMod(bx, 512) < step) ctx.fill(x0 + i * px, y0, x0 + i * px + 1, y0 + size, 0xFFFF5070);
            else if (Math.floorMod(bx, 16) < step) ctx.fill(x0 + i * px, y0, x0 + i * px + 1, y0 + size, 0x30FFFFFF);
            if (Math.floorMod(bz, 512) < step) ctx.fill(x0, y0 + i * px, x0 + size, y0 + i * px + 1, 0xFFFF5070);
            else if (Math.floorMod(bz, 16) < step) ctx.fill(x0, y0 + i * px, x0 + size, y0 + i * px + 1, 0x30FFFFFF);
        }

        // Spieler-Pfeil in der Mitte
        int cx = x0 + size / 2, cy = y0 + size / 2;
        Matrix3x2fStack m = ctx.pose();
        m.pushMatrix();
        m.translate(cx, cy);
        m.rotate((float) Math.toRadians(p.getYRot() + 180));
        ctx.fill(-1, -4, 1, 3, 0xFFFFFFFF);
        ctx.fill(-3, -2, 3, -1, 0xFFFFFFFF);
        m.popMatrix();

        int rx = Math.floorDiv(p.getBlockX(), 512), rz = Math.floorDiv(p.getBlockZ(), 512);
        String label = "Region r." + rx + "." + rz + "   Chunk " + (p.getBlockX() >> 4) + ", " + (p.getBlockZ() >> 4);
        ctx.text(font, label, x0 + size - font.width(label), y0 + size + 6, 0xFFE0E0F0, true);
    }
}
