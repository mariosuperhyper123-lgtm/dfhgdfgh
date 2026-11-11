package dev.nova.client.setting;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/** Auswahl aus einer Registry (Mobs oder Blöcke). Jeder ausgewählte Eintrag hat eine eigene Farbe. */
public class ListSetting<T> extends Setting {
    private final Registry<T> registry;
    private final Function<T, Component> nameFn;
    private final Function<T, Integer> defaultColor;
    private final Predicate<T> filter;
    private final List<Identifier> defaults;
    private final Map<Identifier, Integer> selected = new LinkedHashMap<>();
    private int version;

    public ListSetting(String id, String name, Registry<T> registry, Function<T, Component> nameFn,
                       Function<T, Integer> defaultColor, Predicate<T> filter, List<Identifier> defaults) {
        super(id, name);
        this.registry = registry;
        this.nameFn = nameFn;
        this.defaultColor = defaultColor;
        this.filter = filter;
        this.defaults = defaults;
        reset();
    }

    public List<T> entries() {
        return registry.stream().filter(filter)
                .sorted(Comparator.comparing(t -> nameFn.apply(t).getString()))
                .toList();
    }

    public Identifier idOf(T t) { return registry.getKey(t); }
    public Component nameOf(T t) { return nameFn.apply(t); }
    public Integer getColor(Identifier id) { return selected.get(id); }
    public boolean isSelected(T t) { return selected.containsKey(idOf(t)); }
    public Map<Identifier, Integer> getSelected() { return selected; }
    public int size() { return selected.size(); }
    public int version() { return version; }
    public T byId(Identifier id) { return registry.getValue(id); }

    public void setSelected(T t, boolean on) {
        Identifier id = idOf(t);
        if (on) selected.putIfAbsent(id, defaultColor.apply(t));
        else selected.remove(id);
        version++;
    }

    public void toggle(T t) { setSelected(t, !isSelected(t)); }

    public int colorOf(T t) {
        Integer c = selected.get(idOf(t));
        return c != null ? c : defaultColor.apply(t);
    }

    public void setColor(T t, int argb) {
        Identifier id = idOf(t);
        if (selected.containsKey(id)) { selected.put(id, argb); version++; }
    }

    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        selected.forEach((id, c) -> {
            if (!sb.isEmpty()) sb.append(';');
            sb.append(id).append('=').append(String.format("%08X", c));
        });
        return sb.toString();
    }

    @Override
    public void deserialize(String s) {
        selected.clear();
        for (String part : s.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            Identifier id = Identifier.tryParse(part.substring(0, eq));
            if (id == null || !registry.containsKey(id)) continue;
            try {
                selected.put(id, (int) Long.parseLong(part.substring(eq + 1), 16));
            } catch (NumberFormatException ignored) {}
        }
        version++;
    }

    @Override
    public void reset() {
        selected.clear();
        for (Identifier id : defaults) {
            if (registry.containsKey(id)) selected.put(id, defaultColor.apply(registry.getValue(id)));
        }
        version++;
    }
}
