package dev.nova.client.module.modules;

import dev.nova.client.NovaClient;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.lwjgl.glfw.GLFW;

/** Modul, das ein mitgeliefertes Ressourcenpaket an- und ausschaltet. */
public abstract class PackModule extends Module {
    private final String packPath;
    private Boolean applied;
    private long lastApply;

    protected PackModule(String name, String id, String packPath) {
        super(name, id, Category.VISUALS, GLFW.GLFW_KEY_UNKNOWN, false);
        this.packPath = packPath;
    }

    /** Jeden Tick aufrufen: schaltet das Paket um, sobald sich der Modul-Zustand ändert. */
    public void syncPack(Minecraft mc) {
        if (applied != null && applied == isEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastApply < 3000) return; // nicht während eines laufenden Neuladens erneut auslösen

        PackRepository repo = mc.getResourcePackRepository();
        String id = null;
        for (Pack pack : repo.getAvailablePacks()) {
            if (pack.getId().contains(packPath)) {
                id = pack.getId();
                break;
            }
        }
        applied = isEnabled();
        if (id == null) {
            NovaClient.LOGGER.warn("Ressourcenpaket {} nicht gefunden", packPath);
            return;
        }
        boolean on = repo.getSelectedIds().contains(id);
        if (on == isEnabled()) return;
        if (isEnabled()) repo.addPack(id);
        else repo.removePack(id);
        lastApply = now;
        mc.options.updateResourcePacks(repo);
    }
}
