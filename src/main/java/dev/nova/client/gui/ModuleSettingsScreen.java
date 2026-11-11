package dev.nova.client.gui;

import dev.nova.client.module.Module;
import dev.nova.client.module.ModuleManager;
import dev.nova.client.setting.BoolSetting;
import dev.nova.client.setting.ColorSetting;
import dev.nova.client.setting.ListSetting;
import dev.nova.client.setting.NumberSetting;
import dev.nova.client.setting.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ModuleSettingsScreen extends NovaScreen {
    private final Screen parent;
    private final Module module;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getName() + " – Einstellungen"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        List<Setting> settings = module.getSettings();
        boolean twoCols = settings.size() > 7;
        int colW = twoCols ? 150 : 200;
        int perCol = twoCols ? (settings.size() + 1) / 2 : settings.size();
        int left = twoCols ? width / 2 - colW - 4 : width / 2 - colW / 2;
        for (int i = 0; i < settings.size(); i++) {
            addRenderableWidget(widgetFor(settings.get(i), left + (i / perCol) * (colW + 8), 36 + (i % perCol) * 22, colW));
        }
        int y = 36 + perCol * 22 + 6;
        addRenderableWidget(Button.builder(Component.literal("Zurücksetzen"), b -> {
            settings.forEach(Setting::reset);
            ModuleManager.get().save();
            rebuildWidgets();
        }).bounds(width / 2 - 100, y, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
                .bounds(width / 2 + 2, y, 98, 20).build());
    }

    private AbstractWidget widgetFor(Setting s, int x, int y, int w) {
        if (s instanceof NumberSetting n) return new NumberSlider(x, y, w, n);
        if (s instanceof BoolSetting b) {
            return Button.builder(boolLabel(b), btn -> {
                b.toggle();
                btn.setMessage(boolLabel(b));
            }).bounds(x, y, w, 20).build();
        }
        if (s instanceof ColorSetting c) {
            return Button.builder(
                    Component.literal(c.getName() + ": ").append(Component.literal("■■").withStyle(st -> st.withColor(c.get() & 0xFFFFFF))),
                    btn -> open(new ColorScreen(this, Component.literal(c.getName()), c::get, c::set)))
                    .bounds(x, y, w, 20).build();
        }
        if (s instanceof ListSetting<?> l) {
            return Button.builder(Component.literal(l.getName() + " (" + l.size() + ") ..."),
                    btn -> open(new ListScreen<>(this, l))).bounds(x, y, w, 20).build();
        }
        return Button.builder(Component.literal(s.getName()), btn -> {}).bounds(x, y, w, 20).build();
    }

    private static Component boolLabel(BoolSetting b) {
        return Component.literal(b.getName() + ": ").append(b.get()
                ? Component.literal("AN").withStyle(ChatFormatting.GREEN)
                : Component.literal("AUS").withStyle(ChatFormatting.RED));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.centeredText(font, title, width / 2, 18, 0xFF7FD4FF);
    }

    @Override
    public void onClose() {
        ModuleManager.get().save();
        open(parent);
    }

    private static class NumberSlider extends AbstractSliderButton {
        private final NumberSetting setting;

        NumberSlider(int x, int y, int w, NumberSetting setting) {
            super(x, y, w, 20, Component.empty(),
                    (setting.get() - setting.getMin()) / (setting.getMax() - setting.getMin()));
            this.setting = setting;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(setting.getName() + ": " + setting.format()));
        }

        @Override
        protected void applyValue() {
            setting.set(setting.getMin() + value * (setting.getMax() - setting.getMin()));
        }
    }
}
