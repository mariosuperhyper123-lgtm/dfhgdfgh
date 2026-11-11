package dev.nova.client.gui;

import dev.nova.client.NovaClient;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.ModuleManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Hauptmenü: ein Panel pro Kategorie, Module zum An-/Ausschalten. */
public class ClickGuiScreen extends NovaScreen {
    private static final int PANEL_W = 122;
    private static final int GAP = 10;
    private static final int HEADER = 20;
    private static final int ROW = 16;
    private static final int TOP = 54;

    private record Row(Module module, int x, int y, boolean hasSettings) {}
    private record Panel(Category category, int x, List<Row> rows) {}

    private final List<Panel> panels = new ArrayList<>();
    private final Map<Module, Float> anim = new HashMap<>();
    private long lastFrame = System.currentTimeMillis();

    public ClickGuiScreen() {
        super(Component.literal(NovaClient.NAME));
    }

    @Override
    protected void init() {
        panels.clear();
        Category[] cats = Category.values();
        int total = cats.length * PANEL_W + (cats.length - 1) * GAP;
        int x = (width - total) / 2;
        for (Category cat : cats) {
            List<Row> rows = new ArrayList<>();
            int y = TOP + HEADER;
            for (Module m : ModuleManager.get().all()) {
                if (m.getCategory() != cat) continue;
                boolean hasSettings = !m.getSettings().isEmpty();
                rows.add(new Row(m, x, y, hasSettings));
                int mainW = hasSettings ? PANEL_W - 18 : PANEL_W;
                if (!m.isHold()) {
                    Button toggle = Button.builder(Component.empty(), b -> m.toggle()).bounds(x, y, mainW, ROW).build();
                    toggle.setAlpha(0f);
                    addRenderableWidget(toggle);
                }
                if (hasSettings) {
                    Button gear = Button.builder(Component.empty(), b -> open(new ModuleSettingsScreen(this, m)))
                            .bounds(x + PANEL_W - 18, y, 18, ROW).build();
                    gear.setAlpha(0f);
                    addRenderableWidget(gear);
                }
                anim.putIfAbsent(m, m.isEnabled() ? 1f : 0f);
                y += ROW;
            }
            panels.add(new Panel(cat, x, rows));
            x += PANEL_W + GAP;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float open = openProgress();
        int accent = accent();

        drawTitle(ctx, open, accent);
        for (int p = 0; p < panels.size(); p++) {
            float local = Draw2D.easeOutCubic(((now - openedAt) - p * 70) / 380f);
            drawPanel(ctx, panels.get(p), Math.round((1 - local) * 18), local, accent, mouseX, mouseY, dt);
        }
        ctx.centeredText(font, Component.literal("Klick = an/aus   ·   ⚙ = Einstellungen   ·   ESC = schließen"),
                width / 2, height - 16, Draw2D.withAlpha(0xFF8C8FA8, open));
    }

    private void drawTitle(GuiGraphicsExtractor ctx, float open, int accent) {
        Matrix3x2fStack m = ctx.pose();
        String title = "NOVA";
        float pulse = 0.5f + 0.5f * (float) Math.sin(time() * 2.2f);
        m.pushMatrix();
        m.translate(width / 2f, 14);
        m.scale(2.4f, 2.4f);
        int w = font.width(title);
        for (int i = 1; i <= 2; i++) {
            int glow = Draw2D.withAlpha(accent | 0xFF000000, (0.18f + 0.12f * pulse) * open / i);
            ctx.text(font, title, -w / 2 - i, 0, glow, false);
            ctx.text(font, title, -w / 2 + i, 0, glow, false);
            ctx.text(font, title, -w / 2, -i, glow, false);
            ctx.text(font, title, -w / 2, i, glow, false);
        }
        ctx.text(font, title, -w / 2, 0, Draw2D.withAlpha(0xFFFFFFFF, open), false);
        m.popMatrix();
        ctx.centeredText(font, Component.literal("CLIENT").withStyle(ChatFormatting.BOLD), width / 2, 37,
                Draw2D.withAlpha(accent | 0xFF000000, open));
    }

    private void drawPanel(GuiGraphicsExtractor ctx, Panel panel, int offset, float alpha, int accent,
                           int mouseX, int mouseY, float dt) {
        if (alpha <= 0.01f) return;
        int x = panel.x();
        int top = TOP + offset;
        int bottom = TOP + HEADER + panel.rows().size() * ROW + 4 + offset;

        Draw2D.roundRect(ctx, x - 1, top - 1, x + PANEL_W + 1, bottom + 2, Draw2D.argb((int) (0x50 * alpha), 0, 0, 0));
        Draw2D.roundRect(ctx, x, top, x + PANEL_W, bottom, Draw2D.argb((int) (0xD8 * alpha), 14, 14, 24));
        ctx.fillGradient(x + 1, top + 1, x + PANEL_W - 1, top + HEADER,
                Draw2D.withAlpha(accent | 0xFF000000, 0.32f * alpha), Draw2D.argb(0, 14, 14, 24));
        ctx.fill(x + 2, top, x + PANEL_W - 2, top + 1, Draw2D.withAlpha(accent | 0xFF000000, alpha));
        ctx.text(font, Component.literal(panel.category().displayName()).withStyle(ChatFormatting.BOLD),
                x + 7, top + 6, Draw2D.withAlpha(0xFFFFFFFF, alpha), true);
        String count = String.valueOf(panel.rows().stream().filter(r -> r.module().isEnabled()).count());
        ctx.text(font, count, x + PANEL_W - 7 - font.width(count), top + 6, Draw2D.withAlpha(0xFF8C8FA8, alpha), true);

        for (Row row : panel.rows()) {
            Module m = row.module();
            int ry = row.y() + offset;
            float target = m.isEnabled() ? 1f : 0f;
            float a = anim.getOrDefault(m, target);
            a += (target - a) * Math.min(1f, dt * 10f);
            anim.put(m, a);

            boolean hover = mouseX >= x && mouseX < x + PANEL_W && mouseY >= row.y() && mouseY < row.y() + ROW;
            if (hover) ctx.fill(x + 1, ry, x + PANEL_W - 1, ry + ROW, Draw2D.argb((int) (0x18 * alpha), 255, 255, 255));
            if (a > 0.01f) {
                ctx.fill(x + 1, ry, x + PANEL_W - 1, ry + ROW, Draw2D.withAlpha(accent | 0xFF000000, 0.16f * a * alpha));
                ctx.fill(x + 1, ry + 2, x + 3, ry + ROW - 2, Draw2D.withAlpha(accent | 0xFF000000, a * alpha));
            }
            int textColor = Draw2D.lerpColor(0xFF9A9CB0, 0xFFFFFFFF, Math.max(a, hover ? 0.6f : 0f));
            ctx.text(font, m.getName(), x + 8 + Math.round(a * 2), ry + 4, Draw2D.withAlpha(textColor, alpha), true);

            int right = x + PANEL_W - 6;
            if (row.hasSettings()) {
                boolean gearHover = hover && mouseX >= x + PANEL_W - 18;
                ctx.text(font, "⚙", right - font.width("⚙"), ry + 4,
                        Draw2D.withAlpha(gearHover ? 0xFFFFFFFF : 0xFF6E7190, alpha), true);
                right -= 14;
            }
            if (m.isHold()) {
                Component key = Component.literal("halten: ").append(m.getKey().getTranslatedKeyMessage());
                ctx.text(font, key, right - font.width(key), ry + 4, Draw2D.withAlpha(0xFF6E7190, alpha), true);
            }
        }
    }
}
