package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class HudModule extends Module {
    public HudModule() {
        super("HUD", "hud", Category.VISUALS, GLFW.GLFW_KEY_H, false);
    }
}
