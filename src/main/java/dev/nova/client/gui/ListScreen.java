package dev.nova.client.gui;

import dev.nova.client.module.ModuleManager;
import dev.nova.client.setting.ListSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Auswahl-Liste mit Suche: Eintrag anklicken = an/aus, Farbfeld = Farbe ändern. */
public class ListScreen<T> extends NovaScreen {
    private static final int ROW = 22;

    private final Screen parent;
    private final ListSetting<T> setting;
    private final List<T> all;
    private final List<AbstractWidget> rows = new ArrayList<>();
    private List<T> filtered = List.of();
    private String search = "";
    private boolean onlySelected;
    private int scroll;

    public ListScreen(Screen parent, ListSetting<T> setting) {
        super(Component.literal(setting.getName()));
        this.parent = parent;
        this.setting = setting;
        this.all = setting.entries();
    }

    private int listTop() { return 56; }
    private int visibleRows() { return Math.max(1, (height - listTop() - 34) / ROW); }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        EditBox searchField = new EditBox(font, x, 32, 200, 18, Component.literal("Suche"));
        searchField.setHint(Component.literal("Suchen...").withStyle(ChatFormatting.DARK_GRAY));
        searchField.setValue(search);
        searchField.setResponder(s -> {
            search = s;
            scroll = 0;
            refresh();
        });
        addRenderableWidget(searchField);
        setInitialFocus(searchField);

        int by = height - 26;
        addRenderableWidget(Button.builder(Component.literal("Alle an"), b -> {
            filtered.forEach(t -> setting.setSelected(t, true));
            refresh();
        }).bounds(x - 54, by, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Alle aus"), b -> {
            filtered.forEach(t -> setting.setSelected(t, false));
            refresh();
        }).bounds(x + 10, by, 60, 20).build());
        addRenderableWidget(Button.builder(filterLabel(), b -> {
            onlySelected = !onlySelected;
            b.setMessage(filterLabel());
            scroll = 0;
            refresh();
        }).bounds(x + 74, by, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose()).bounds(x + 158, by, 96, 20).build());

        addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
            scroll -= visibleRows();
            refresh();
        }).bounds(x + 206, listTop(), 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
            scroll += visibleRows();
            refresh();
        }).bounds(x + 206, listTop() + 22, 20, 20).build());

        refresh();
    }

    private Component filterLabel() {
        return Component.literal(onlySelected ? "Nur aktive" : "Alle zeigen");
    }

    private void refresh() {
        rows.forEach(this::removeWidget);
        rows.clear();

        String q = search.toLowerCase(Locale.ROOT).trim();
        filtered = all.stream()
                .filter(t -> !onlySelected || setting.isSelected(t))
                .filter(t -> q.isEmpty()
                        || setting.nameOf(t).getString().toLowerCase(Locale.ROOT).contains(q)
                        || setting.idOf(t).toString().contains(q))
                .toList();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, filtered.size() - visibleRows())));

        int x = width / 2 - 100;
        int y = listTop();
        for (int i = scroll; i < filtered.size() && i < scroll + visibleRows(); i++) {
            T t = filtered.get(i);
            rows.add(addRenderableWidget(Button.builder(entryLabel(t), b -> {
                setting.toggle(t);
                refresh();
            }).bounds(x, y, 176, 20).build()));
            if (setting.isSelected(t)) {
                int color = setting.colorOf(t);
                rows.add(addRenderableWidget(Button.builder(
                        Component.literal("■").withStyle(s -> s.withColor(color & 0xFFFFFF)),
                        b -> open(new ColorScreen(this, setting.nameOf(t), () -> setting.colorOf(t), c -> setting.setColor(t, c))))
                        .bounds(x + 180, y, 20, 20).build()));
            }
            y += ROW;
        }
    }

    private Component entryLabel(T t) {
        boolean on = setting.isSelected(t);
        return Component.literal((on ? "✔ " : "") + setting.nameOf(t).getString())
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.centeredText(font, Component.literal(setting.getName() + " – " + setting.size() + " ausgewählt"),
                width / 2, 16, 0xFF7FD4FF);
        if (filtered.size() > visibleRows()) {
            ctx.text(font, (scroll + 1) + "-" + Math.min(filtered.size(), scroll + visibleRows()) + " / " + filtered.size(),
                    width / 2 + 130, listTop() + 50, 0xFFAAAAAA, true);
        }
    }

    @Override
    public void onClose() {
        ModuleManager.get().save();
        open(parent);
    }
}
