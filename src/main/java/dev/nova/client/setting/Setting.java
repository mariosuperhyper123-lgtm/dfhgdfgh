package dev.nova.client.setting;

public abstract class Setting {
    private final String id;
    private final String name;

    protected Setting(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public abstract String serialize();
    public abstract void deserialize(String value);
    public abstract void reset();
}
