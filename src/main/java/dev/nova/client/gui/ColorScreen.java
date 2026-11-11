package dev.nova.client.gui;

import dev.nova.client.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Farbwähler mit Reglern für Rot, Grün, Blau und Deckkraft. */
public class ColorScreen extends NovaScreen {
    private static final int[] PRESETS = {
            0xFFFF4040, 0xFFFF9F1A, 0xFFFFFF40, 0xFF40FF60, 0xFF40FFFF,
            0xFF4D8CFF, 0xFFB050FF, 0xFFFF50D0, 0xFFFFFFFF, 0xFF808080
    };

    private final Screen parent;
    private final IntSupplier getter;
    private final IntConsumer setter;

    public ColorScreen(Screen parent, Component title, IntSupplier getter, IntConsumer setter) {
        super(title);
        this.parent = parent;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = 70;
        addRenderableWidget(new ChannelSlider(x, y, "Rot", 16));
        addRenderableWidget(new ChannelSlider(x, y + 22, "Grün", 8));
        addRenderableWidget(new ChannelSlider(x, y + 44, "Blau", 0));
        addRenderableWidget(new ChannelSlider(x, y + 66, "Deckkraft", 24));

        int px = width / 2 - (PRESETS.length * 20) / 2;
        for (int i = 0; i < PRESETS.length; i++) {
            int color = PRESETS[i];
            addRenderableWidget(Button.builder(Component.literal("■").withStyle(s -> s.withColor(color & 0xFFFFFF)), b -> {
                setter.accept(color);
                rebuildWidgets();
            }).bounds(px + i * 20, y + 92, 20, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose()).bounds(x, y + 120, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.centeredText(font, title, width / 2, 18, 0xFF7FD4FF);
        int c = getter.getAsInt();
        int x = width / 2 - 100;
        ctx.fill(x - 1, 35, x + 201, 61, 0xFF000000);
        ctx.fill(x, 36, x + 200, 60, c);
        ctx.centeredText(font, Component.literal(String.format("#%08X", c)), width / 2, 44, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        ModuleManager.get().save();
        open(parent);
    }

    private class ChannelSlider extends AbstractSliderButton {
        private final String label;
        private final int shift;

        ChannelSlider(int x, int y, String label, int shift) {
            super(x, y, 200, 20, Component.empty(), ((getter.getAsInt() >>> shift) & 0xFF) / 255.0);
            this.label = label;
            this.shift = shift;
            updateMessage();
        }

        private int channel() {
            return (int) Math.round(value * 255);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + channel()));
        }

        @Override
        protected void applyValue() {
            int c = getter.getAsInt();
            setter.accept((c & ~(0xFF << shift)) | (channel() << shift));
        }
    }
}
