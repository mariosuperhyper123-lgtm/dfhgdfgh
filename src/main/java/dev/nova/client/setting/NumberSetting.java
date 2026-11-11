package dev.nova.client.setting;

public class NumberSetting extends Setting {
    private final double min, max, def, step;
    private double value;

    public NumberSetting(String id, String name, double min, double max, double def, double step) {
        super(id, name);
        this.min = min;
        this.max = max;
        this.def = def;
        this.step = step;
        this.value = def;
    }

    public double get() { return value; }
    public float getFloat() { return (float) value; }
    public int getInt() { return (int) Math.round(value); }

    public void set(double v) {
        v = Math.round(v / step) * step;
        value = Math.max(min, Math.min(max, v));
    }

    public String format() {
        return step >= 1 ? String.valueOf((long) Math.round(value)) : String.format("%.2f", value);
    }

    public double getMin() { return min; }
    public double getMax() { return max; }

    @Override public String serialize() { return String.valueOf(value); }
    @Override public void deserialize(String s) { try { set(Double.parseDouble(s)); } catch (NumberFormatException ignored) {} }
    @Override public void reset() { value = def; }
}
