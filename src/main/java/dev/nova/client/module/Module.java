package dev.nova.client.module;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nova.client.setting.Setting;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String name;
    private final Category category;
    private final KeyMapping key;
    private final boolean hold;
    private final List<Setting> settings = new ArrayList<>();
    private boolean enabled;

    protected Module(String name, String id, Category category, int defaultKey, boolean hold) {
        this(name, id, category, InputConstants.Type.KEYSYM, defaultKey, hold);
    }

    protected Module(String name, String id, Category category, InputConstants.Type keyType, int defaultKey, boolean hold) {
        this.name = name;
        this.category = category;
        this.hold = hold;
        this.key = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.novaclient." + id, keyType, defaultKey, KeyCategory.MAIN));
    }

    protected Minecraft mc() {
        return Minecraft.getInstance();
    }

    protected <S extends Setting> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
        if (!hold) ModuleManager.get().save();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}

    public String getName() { return name; }
    public Category getCategory() { return category; }
    public KeyMapping getKey() { return key; }
    public boolean isHold() { return hold; }
    public boolean isEnabled() { return enabled; }
    public List<Setting> getSettings() { return Collections.unmodifiableList(settings); }
}
