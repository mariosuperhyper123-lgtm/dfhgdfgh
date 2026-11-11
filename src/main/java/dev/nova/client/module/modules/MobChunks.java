package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.ModuleManager;
import dev.nova.client.render.Projection;
import dev.nova.client.setting.BoolSetting;
import dev.nova.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Markiert Chunks, in denen sich Mobs befinden (Spieler zählen nicht).
 * Gelb = gerade gespawnt, Rot = feindliche Mobs, Grün = nur friedliche Mobs.
 */
public class MobChunks extends Module {
    public final NumberSetting freshSeconds = add(new NumberSetting("fresh", "Spawn-Markierung (s)", 1, 60, 10, 1));
    public final BoolSetting showPassive = add(new BoolSetting("passive", "Friedliche zeigen", true));

    private record Fresh(long expires, double y) {}

    private final Map<Long, Fresh> fresh = new HashMap<>();
    private ClientLevel lastLevel;
    private long levelJoinTime;

    public MobChunks() {
        super("MobChunks", "mobchunks", Category.RENDER, GLFW.GLFW_KEY_J, false);
    }

    /** Aktiv, wenn MobChunks selbst oder das Anzeige-Modul SusChunk eingeschaltet ist. */
    private boolean shown() {
        ModuleManager mm = ModuleManager.get();
        return isEnabled() || (mm != null && mm.susChunk.isEnabled());
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static long chunkKey(Entity e) {
        return key(e.getBlockX() >> 4, e.getBlockZ() >> 4);
    }

    @Override
    public void onDisable() {
        fresh.clear();
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();
        fresh.values().removeIf(f -> f.expires() < now);
    }

    /** Taucht ein Mob mitten im geladenen Bereich auf, wurde er sehr wahrscheinlich gerade gespawnt. */
    public void onEntityLoad(Entity entity, ClientLevel level) {
        long now = System.currentTimeMillis();
        if (level != lastLevel) {
            lastLevel = level;
            levelJoinTime = now;
            fresh.clear();
        }
        if (!shown() || !(entity instanceof Mob)) return;
        if (now - levelJoinTime < 3000) return;

        Minecraft mc = mc();
        if (mc.player == null) return;

        int rangeChunks = Math.min(entity.getType().clientTrackingRange(), mc.options.getEffectiveRenderDistance());
        double limit = rangeChunks * 16 - 24;
        double dx = entity.getX() - mc.player.getX();
        double dz = entity.getZ() - mc.player.getZ();
        if (dx * dx + dz * dz > limit * limit) return;

        fresh.put(chunkKey(entity), new Fresh(now + (long) (freshSeconds.get() * 1000), entity.getY()));
    }

    private static final class ChunkInfo {
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean hostile;
    }

    public void render(GuiGraphicsExtractor ctx, Projection p) {
        if (!shown()) return;
        Minecraft mc = mc();
        if (mc.level == null) return;

        Map<Long, ChunkInfo> chunks = new HashMap<>();
        boolean passive = showPassive.get();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof Mob) || !e.isAlive()) continue;
            boolean hostile = e instanceof Enemy;
            if (!hostile && !passive) continue;
            ChunkInfo info = chunks.computeIfAbsent(chunkKey(e), k -> new ChunkInfo());
            info.hostile |= hostile;
            info.minY = Math.min(info.minY, e.getY());
            info.maxY = Math.max(info.maxY, e.getY() + e.getBbHeight());
        }
        for (Map.Entry<Long, Fresh> f : fresh.entrySet()) {
            ChunkInfo info = chunks.computeIfAbsent(f.getKey(), k -> new ChunkInfo());
            info.minY = Math.min(info.minY, f.getValue().y());
            info.maxY = Math.max(info.maxY, f.getValue().y() + 2);
        }

        for (Map.Entry<Long, ChunkInfo> entry : chunks.entrySet()) {
            long k = entry.getKey();
            int x = (int) (k >> 32) << 4, z = (int) k << 4;
            ChunkInfo info = entry.getValue();
            int color = fresh.containsKey(k) ? 0xFFFFE619 : info.hostile ? 0xFFFF3333 : 0xFF33FF4D;
            p.box3d(ctx, new AABB(x, info.minY - 0.5, z, x + 16, info.maxY + 0.5, z + 16), color);
        }
    }
}
