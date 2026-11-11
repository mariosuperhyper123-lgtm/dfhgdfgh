package dev.nova.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Alle Stellen, an denen sich die Minecraft-API zwischen Versionen gern ändert, an einem Ort.
 * Seit 26.2 liegen Bildschirme in {@code Minecraft.gui}.
 */
public final class Compat {
    private Compat() {}

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static Screen screen() {
        return mc().gui.screen();
    }

    public static void setScreen(Screen screen) {
        mc().gui.setScreen(screen);
    }

    /** Lokale Chat-Nachricht nur für dich (seit 26.1: sendSystemMessage statt displayClientMessage(…, false)). */
    public static void chat(Component text) {
        if (mc().player != null) mc().player.sendSystemMessage(text);
    }

    /** Chunk-Koordinaten. Seit 26.1 ist ChunkPos ein Record (x(), z()). */
    public static int[] chunkXZ(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        return new int[]{pos.x(), pos.z()};
    }

    public static float guiScale() {
        return (float) mc().getWindow().getGuiScale();
    }
}
