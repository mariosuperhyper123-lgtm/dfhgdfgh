package dev.nova.client.module;

public enum Category {
    COMBAT("Combat"),
    RENDER("Render"),
    VISUALS("Optik"),
    UTILITY("Kamera & Utility");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
