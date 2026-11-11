package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.render.Projection;
import dev.nova.client.setting.BoolSetting;
import dev.nova.client.setting.ColorSetting;
import dev.nova.client.setting.ListSetting;
import dev.nova.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockESP extends Module {
    public final ListSetting<Block> blocks = add(new ListSetting<Block>(
            "blocks", "Blöcke", BuiltInRegistries.BLOCK, Block::getName, BlockESP::defaultColor,
            b -> b != Blocks.AIR && b != Blocks.CAVE_AIR && b != Blocks.VOID_AIR,
            List.of(mc("diamond_ore"), mc("deepslate_diamond_ore"), mc("ancient_debris"), mc("spawner"), mc("chest"))));
    public final BoolSetting box = add(new BoolSetting("box", "Box", true));
    public final NumberSetting fillAlpha = add(new NumberSetting("fill", "Füllung (Deckkraft)", 0, 255, 50, 5));
    public final BoolSetting tracers = add(new BoolSetting("tracers", "Tracer", false));
    public final BoolSetting tracerSame = add(new BoolSetting("tracerSame", "Tracer = Box-Farbe", true));
    public final ColorSetting tracerColor = add(new ColorSetting("tracerColor", "Tracer-Farbe", 0xFFFFFFFF));
    public final NumberSetting radius = add(new NumberSetting("radius", "Radius (Chunks)", 1, 16, 6, 1));
    public final NumberSetting maxBlocks = add(new NumberSetting("max", "Max. Blöcke", 50, 5000, 1000, 50));

    private static final int CHUNKS_PER_TICK = 6;

    private final Map<Long, List<BlockPos>> found = new HashMap<>();
    private final ArrayDeque<int[]> queue = new ArrayDeque<>();
    private final Map<Block, Integer> colors = new HashMap<>();
    private int seenVersion = -1;
    private ClientLevel lastLevel;

    public BlockESP() {
        super("BlockESP", "blockesp", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN, false);
    }

    private static Identifier mc(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int defaultColor(Block block) {
        int rgb = block.defaultMapColor().col;
        return 0xFF000000 | (rgb == 0 ? 0xFFFFFF : rgb);
    }

    @Override
    public void onDisable() {
        found.clear();
        queue.clear();
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc.level == null || mc.player == null) return;

        if (mc.level != lastLevel || blocks.version() != seenVersion) {
            lastLevel = mc.level;
            seenVersion = blocks.version();
            colors.clear();
            blocks.getSelected().forEach((id, c) -> colors.put(blocks.byId(id), c));
            found.clear();
            queue.clear();
        }
        if (colors.isEmpty()) return;

        int cx = mc.player.getBlockX() >> 4;
        int cz = mc.player.getBlockZ() >> 4;
        int r = radius.getInt();
        if (queue.isEmpty()) {
            found.keySet().removeIf(k -> {
                int x = (int) (k >> 32), z = (int) (long) k;
                return Math.abs(x - cx) > r || Math.abs(z - cz) > r;
            });
            List<int[]> list = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++)
                    list.add(new int[]{cx + dx, cz + dz});
            list.sort(Comparator.comparingInt(c -> Math.max(Math.abs(c[0] - cx), Math.abs(c[1] - cz))));
            queue.addAll(list);
        }
        for (int i = 0; i < CHUNKS_PER_TICK && !queue.isEmpty(); i++) {
            int[] c = queue.poll();
            scanChunk(mc.level, c[0], c[1]);
        }
    }

    private void scanChunk(ClientLevel level, int x, int z) {
        long key = key(x, z);
        if (!level.getChunkSource().hasChunk(x, z)) {
            found.remove(key);
            return;
        }
        LevelChunk chunk = level.getChunk(x, z);
        LevelChunkSection[] sections = chunk.getSections();
        List<BlockPos> result = new ArrayList<>();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;
            if (!section.maybeHas(s -> colors.containsKey(s.getBlock()))) continue;
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int y = 0; y < 16; y++)
                for (int zz = 0; zz < 16; zz++)
                    for (int xx = 0; xx < 16; xx++) {
                        if (colors.containsKey(section.getBlockState(xx, y, zz).getBlock())) {
                            result.add(new BlockPos((x << 4) + xx, baseY + y, (z << 4) + zz));
                        }
                    }
        }
        if (result.isEmpty()) found.remove(key);
        else found.put(key, result);
    }

    public void render(GuiGraphicsExtractor ctx, Projection p) {
        if (!isEnabled() || found.isEmpty()) return;
        ClientLevel level = mc().level;
        if (level == null) return;

        int alpha = fillAlpha.getInt();
        int limit = maxBlocks.getInt();
        int count = 0;
        outer:
        for (List<BlockPos> list : found.values()) {
            for (BlockPos pos : list) {
                if (count++ >= limit) break outer;
                BlockState state = level.getBlockState(pos);
                Integer color = colors.get(state.getBlock());
                if (color == null) continue;

                VoxelShape shape = state.getShape(level, pos);
                AABB bb = shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
                if (box.get()) {
                    if (alpha > 0) p.fillBox(ctx, bb, (color & 0x00FFFFFF) | (alpha << 24));
                    p.box3d(ctx, bb, color);
                }
                if (tracers.get()) {
                    p.tracer(ctx, Vec3.atCenterOf(pos), tracerSame.get() ? color : tracerColor.get());
                }
            }
        }
    }
}
