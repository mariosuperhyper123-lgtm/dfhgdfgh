package dev.nova.client.module.modules;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.render.Projection;
import dev.nova.client.setting.BoolSetting;
import dev.nova.client.setting.ColorSetting;
import dev.nova.client.setting.ListSetting;
import dev.nova.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class EntityESP extends Module {
    public final ListSetting<EntityType<?>> entities = add(new ListSetting<EntityType<?>>(
            "entities", "Mobs", BuiltInRegistries.ENTITY_TYPE, EntityType::getDescription, EntityESP::defaultColor,
            t -> true,
            List.of(mc("player"), mc("zombie"), mc("skeleton"), mc("creeper"), mc("spider"), mc("enderman"))));
    public final BoolSetting box = add(new BoolSetting("box", "Box", true));
    public final NumberSetting fillAlpha = add(new NumberSetting("fill", "Füllung (Deckkraft)", 0, 255, 40, 5));
    public final BoolSetting tracers = add(new BoolSetting("tracers", "Tracer", true));
    public final BoolSetting tracerSame = add(new BoolSetting("tracerSame", "Tracer = Box-Farbe", true));
    public final ColorSetting tracerColor = add(new ColorSetting("tracerColor", "Tracer-Farbe", 0xFFFFFFFF));
    public final NumberSetting range = add(new NumberSetting("range", "Reichweite", 16, 512, 128, 8));

    public EntityESP() {
        super("EntityESP", "entityesp", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN, false);
    }

    private static Identifier mc(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    private static int defaultColor(EntityType<?> type) {
        if ("player".equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath())) return 0xFF4D8CFF;
        return switch (type.getCategory()) {
            case MONSTER -> 0xFFFF4040;
            case CREATURE -> 0xFF40FF60;
            case AMBIENT -> 0xFFFFFF60;
            case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> 0xFF40C8FF;
            default -> 0xFFC0C0C0;
        };
    }

    public void render(GuiGraphicsExtractor ctx, Projection p, float partialTick) {
        if (!isEnabled()) return;
        Minecraft mc = mc();
        if (mc.level == null || mc.player == null) return;

        double maxSq = range.get() * range.get();
        int alpha = fillAlpha.getInt();
        Vec3 cam = p.camPos();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || e == mc.getCameraEntity()) continue;
            Integer color = entities.getColor(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()));
            if (color == null) continue;

            Vec3 lerped = e.getPosition(partialTick);
            if (lerped.distanceToSqr(cam) > maxSq) continue;
            AABB bb = e.getBoundingBox().move(lerped.subtract(e.getX(), e.getY(), e.getZ()));

            if (box.get()) {
                if (alpha > 0) p.fillBox(ctx, bb, (color & 0x00FFFFFF) | (alpha << 24));
                p.box3d(ctx, bb, color);
            }
            if (tracers.get()) {
                p.tracer(ctx, bb.getCenter(), tracerSame.get() ? color : tracerColor.get());
            }
        }
    }
}
