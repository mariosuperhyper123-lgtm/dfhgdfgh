package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import org.lwjgl.glfw.GLFW;

/** Zoomt, solange die Taste gehalten wird (siehe OptionInstanceMixin / GameRendererMixin). */
public class Zoom extends Module {
    public static final double FACTOR = 4.0;
    private boolean prevSmooth;

    public Zoom() {
        super("Zoom", "zoom", Category.VISUALS, GLFW.GLFW_KEY_C, true);
    }

    @Override
    public void onEnable() {
        prevSmooth = mc().options.smoothCamera;
        mc().options.smoothCamera = true;
    }

    @Override
    public void onDisable() {
        mc().options.smoothCamera = prevSmooth;
    }
}
