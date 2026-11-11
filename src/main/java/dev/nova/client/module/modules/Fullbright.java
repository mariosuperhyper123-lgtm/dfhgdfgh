package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import org.lwjgl.glfw.GLFW;

/** Setzt den Gamma-Wert per Mixin hoch (siehe OptionInstanceMixin). */
public class Fullbright extends Module {
    public static final double GAMMA = 16.0;

    public Fullbright() {
        super("Fullbright", "fullbright", Category.RENDER, GLFW.GLFW_KEY_G, false);
    }
}
