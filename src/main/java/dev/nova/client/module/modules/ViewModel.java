package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/** Verschiebt, dreht und skaliert Hand und Item in der Ego-Ansicht (siehe ItemInHandRendererMixin). */
public class ViewModel extends Module {
    public final NumberSetting posX = add(new NumberSetting("posX", "Position X", -1, 1, 0, 0.05));
    public final NumberSetting posY = add(new NumberSetting("posY", "Position Y", -1, 1, 0, 0.05));
    public final NumberSetting posZ = add(new NumberSetting("posZ", "Position Z", -1, 1, 0, 0.05));
    public final NumberSetting rotX = add(new NumberSetting("rotX", "Rotation X", -180, 180, 0, 5));
    public final NumberSetting rotY = add(new NumberSetting("rotY", "Rotation Y", -180, 180, 0, 5));
    public final NumberSetting rotZ = add(new NumberSetting("rotZ", "Rotation Z", -180, 180, 0, 5));
    public final NumberSetting scale = add(new NumberSetting("scale", "Größe", 0.1, 2, 1, 0.05));

    public ViewModel() {
        super("ViewModel", "viewmodel", Category.VISUALS, GLFW.GLFW_KEY_UNKNOWN, false);
    }
}
