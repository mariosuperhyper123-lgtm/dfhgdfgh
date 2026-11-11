package dev.nova.client.module;

import dev.nova.client.NovaClient;
import dev.nova.client.module.modules.*;
import dev.nova.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class ModuleManager {
    private static ModuleManager instance;

    private final List<Module> modules = new ArrayList<>();
    private final Path configFile = FabricLoader.getInstance().getConfigDir().resolve("novaclient.properties");
    private boolean loading;

    public final Fullbright fullbright = add(new Fullbright());
    public final Zoom zoom = add(new Zoom());
    public final HudModule hud = add(new HudModule());
    public final AutoTool autoTool = add(new AutoTool());
    public final MobChunks mobChunks = add(new MobChunks());
    public final ViewModel viewModel = add(new ViewModel());
    public final EntityESP entityESP = add(new EntityESP());
    public final BlockESP blockESP = add(new BlockESP());
    public final FreeCam freeCam = add(new FreeCam());
    public final FreeLook freeLook = add(new FreeLook());
    public final CustomSky customSky = add(new CustomSky());
    public final GalaxyPickaxe galaxyPickaxe = add(new GalaxyPickaxe());

    public final HeavyChunks heavyChunks = add(new HeavyChunks());
    public final SusChunk susChunk = add(new SusChunk());
    public final StaffList staffList = add(new StaffList());
    public final RegionMap regionMap = add(new RegionMap());

    // Anzeige-Module (ohne Funktion, nur für Menü und Modul-Liste)
    {
        for (String[] d : new String[][]{
                {"AutoCrystal", "autocrystal"}, {"AnchorMacro", "anchormacro"}, {"AutoHitCrystal", "autohitcrystal"},
                {"AutoTotem", "autototem"}, {"InvTotem", "invtotem"}, {"AimAssist", "aimassist"},
                {"TriggerBot", "triggerbot"}, {"Hitboxes", "hitboxes"}, {"ShieldBreaker", "shieldbreaker"},
                {"AutoDoubleHand", "autodoublehand"}, {"FakeStats", "fakestats"}}) {
            add(new DisplayModule(d[0], d[1]));
        }
    }

    private ModuleManager() {}

    public static void init() {
        instance = new ModuleManager();
        instance.load();
    }

    /** Kann vor init() null sein. */
    public static ModuleManager get() {
        return instance;
    }

    private <T extends Module> T add(T module) {
        modules.add(module);
        return module;
    }

    public List<Module> all() {
        return Collections.unmodifiableList(modules);
    }

    private void load() {
        loading = true;
        Properties p = new Properties();
        if (Files.exists(configFile)) {
            try (Reader r = Files.newBufferedReader(configFile)) {
                p.load(r);
            } catch (IOException e) {
                NovaClient.LOGGER.warn("Konfiguration konnte nicht geladen werden", e);
            }
        }
        for (Module m : modules) {
            for (Setting st : m.getSettings()) {
                String v = p.getProperty(m.getName() + "." + st.getId());
                if (v != null) st.deserialize(v);
            }
            if (m.isHold() || m == freeCam) continue;
            boolean def = m == hud;
            m.setEnabled(Boolean.parseBoolean(p.getProperty(m.getName(), String.valueOf(def))));
        }
        loading = false;
    }

    public void save() {
        if (loading) return;
        Properties p = new Properties();
        for (Module m : modules) {
            if (!m.isHold()) p.setProperty(m.getName(), String.valueOf(m.isEnabled()));
            for (Setting st : m.getSettings()) {
                p.setProperty(m.getName() + "." + st.getId(), st.serialize());
            }
        }
        try (Writer w = Files.newBufferedWriter(configFile)) {
            p.store(w, "Nova Client");
        } catch (IOException e) {
            NovaClient.LOGGER.warn("Konfiguration konnte nicht gespeichert werden", e);
        }
    }
}
