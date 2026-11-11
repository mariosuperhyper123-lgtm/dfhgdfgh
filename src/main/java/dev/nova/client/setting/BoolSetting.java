package dev.nova.client.setting;

public class BoolSetting extends Setting {
    private final boolean def;
    private boolean value;

    public BoolSetting(String id, String name, boolean def) {
        super(id, name);
        this.def = def;
        this.value = def;
    }

    public boolean get() { return value; }
    public void set(boolean v) { value = v; }
    public void toggle() { value = !value; }

    @Override public String serialize() { return String.valueOf(value); }
    @Override public void deserialize(String s) { value = Boolean.parseBoolean(s); }
    @Override public void reset() { value = def; }
}
