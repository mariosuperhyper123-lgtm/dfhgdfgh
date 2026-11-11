package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.ModuleManager;
import org.lwjgl.glfw.GLFW;

/**
 * Anzeige-Modul "SusChunk": markiert beim Einschalten Chunks mit farbigen Rahmen,
 * benutzt dafür die Mob-Erkennung von MobChunks (keine Basen- oder Stash-Suche).
 */
public class SusChunk extends Module {
    public SusChunk() {
        super("SusChunk", "suschunk", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN, false);
    }

    @Override
    public void onTick() {
        ModuleManager mm = ModuleManager.get();
        if (mm != null && !mm.mobChunks.isEnabled()) mm.mobChunks.onTick();
    }
}
