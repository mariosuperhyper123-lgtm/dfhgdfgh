package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import org.lwjgl.glfw.GLFW;

/**
 * Anzeige-Modul: lässt sich schalten und erscheint in Menü und Modul-Liste,
 * hat aber bewusst keine Funktion im Spiel (nur zum Herzeigen).
 */
public class DisplayModule extends Module {
    public DisplayModule(String name, String id) {
        super(name, id, Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN, false);
    }
}
