package dev.nova.client.setting;

/** Farbe als ARGB (0xAARRGGBB). */
public class ColorSetting extends Setting {
    private final int def;
    private int value;

    public ColorSetting(String id, String name, int def) {
        super(id, name);
        this.def = def;
        this.value = def;
    }

    public int get() { return value; }
    public void set(int argb) { value = argb; }

    @Override public String serialize() { return String.format("%08X", value); }
    @Override public void deserialize(String s) { try { value = (int) Long.parseLong(s, 16); } catch (NumberFormatException ignored) {} }
    @Override public void reset() { value = def; }
}
