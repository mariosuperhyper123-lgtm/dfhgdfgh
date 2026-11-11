package dev.nova.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nova.client.gui.ClickGuiScreen;
import dev.nova.client.gui.Hud;
import dev.nova.client.module.KeyCategory;
import dev.nova.client.module.Module;
import dev.nova.client.module.ModuleManager;
import dev.nova.client.render.Projection;
import dev.nova.client.util.Compat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NovaClient implements ClientModInitializer {
    public static final String MOD_ID = "novaclient";
    public static final String NAME = "Nova Client";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static KeyMapping guiKey;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        // Mitgelieferte Ressourcenpakete (geschaltet über CustomSky und GalaxyPickaxe)
        ModContainer mod = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(id("galaxy_sky"), mod, PackActivationType.NORMAL);
        ResourceLoader.registerBuiltinPack(id("galaxy_pickaxe"), mod, PackActivationType.NORMAL);

        guiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.novaclient.clickgui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyCategory.MAIN));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            ModuleManager mm = ModuleManager.get();
            mm.freeCam.preTick();
            mm.freeCam.frame();
            mm.freeLook.frame(1f);
            mm.customSky.syncPack(client);
            mm.galaxyPickaxe.syncPack(client);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.consumeClick()) {
                Compat.setScreen(new ClickGuiScreen());
            }
            boolean noScreen = Compat.screen() == null;
            for (Module m : ModuleManager.get().all()) {
                if (m.isHold()) {
                    m.setEnabled(m.getKey().isDown() && noScreen);
                } else {
                    while (m.getKey().consumeClick()) m.toggle();
                }
                if (m.isEnabled() && client.player != null) m.onTick();
            }
        });

        // ESP-Ebene und Text-HUD
        HudElementRegistry.addLast(id("esp"), (ctx, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            ModuleManager mm = ModuleManager.get();
            float pt = deltaTracker.getGameTimeDeltaPartialTick(true);
            // Kameras jedes Bild nachführen (flüssig, unabhängig von 20 Ticks/Sekunde)
            mm.freeCam.frame();
            mm.freeLook.frame(pt);
            Projection p = Projection.create(mc, ctx, pt);
            if (p == null) return;
            mm.mobChunks.render(ctx, p);
            mm.heavyChunks.render(ctx, p);
            mm.blockESP.render(ctx, p);
            mm.entityESP.render(ctx, p, pt);
        });
        HudElementRegistry.addLast(id("hud"), Hud::render);
        HudElementRegistry.addLast(id("panels"), (ctx, deltaTracker) -> {
            ModuleManager mm = ModuleManager.get();
            if (Minecraft.getInstance().player == null) return;
            mm.staffList.render(ctx);
            mm.regionMap.render(ctx);
        });

        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            ModuleManager.get().mobChunks.onEntityLoad(entity, level);
            ModuleManager.get().heavyChunks.onEntityLoad(entity, level);
        });
        // Asynchroner Chunk-Scanner: geladene/entladene Chunks immer mitverfolgen
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> ModuleManager.get().heavyChunks.onChunkLoad(level, chunk));
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> ModuleManager.get().heavyChunks.onChunkUnload(level, chunk));
        LOGGER.info("{} geladen ({} Module)", NAME, ModuleManager.get().all().size());
    }
}
