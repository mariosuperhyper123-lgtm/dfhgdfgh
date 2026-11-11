package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

/** HUD-Fenster "Staff List". Im Singleplayer gibt es keine Staff, daher immer leer. */
public class StaffList extends Module {
    public StaffList() {
        super("StaffList", "stafflist", Category.UTILITY, GLFW.GLFW_KEY_UNKNOWN, false);
    }

    public void render(GuiGraphicsExtractor ctx) {
        if (!isEnabled()) return;
        Minecraft mc = mc();
        Font font = mc.font;
        int x = 4, y = 60, w = 110;
        ctx.fill(x, y, x + w, y + 30, 0xA0101018);
        ctx.fill(x, y, x + w, y + 1, 0xFF9B6BFF);
        ctx.text(font, "Staff List", x + 5, y + 5, 0xFFFFFFFF, true);
        ctx.text(font, "Keine Staff online", x + 5, y + 17, 0xFF8C8FA8, true);
    }
}
