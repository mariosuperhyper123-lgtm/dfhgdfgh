package dev.nova.client.module.modules;

import dev.nova.client.NovaClient;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.render.Projection;
import dev.nova.client.setting.BoolSetting;
import dev.nova.client.setting.ColorSetting;
import dev.nova.client.setting.NumberSetting;
import dev.nova.client.util.Compat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asynchroner Chunk-Scanner ("Heavy Chunks").
 * <p>
 * Ablauf:
 * <ol>
 *   <li>{@code ClientChunkEvents.CHUNK_LOAD} meldet neue Chunks → sie kommen in eine Warteschlange.
 *       Entities werden vom Server erst kurz nach dem Chunk geschickt, deshalb wartet der Scan
 *       {@link #SCAN_DELAY_MS} ms. Neu geladene Entities lösen einen erneuten Scan ihres Chunks aus.</li>
 *   <li>Im Client-Tick wird pro Chunk nur ein kleiner Schnappschuss gemacht (Liste der Block-Entities,
 *       Entity-Typen). Das ist billig und muss auf dem Haupt-Thread passieren, weil die Weltdaten nicht
 *       thread-sicher sind.</li>
 *   <li>Die eigentliche Analyse (Zählen, Schwellenwerte, Höhenbereich) läuft auf einem eigenen
 *       Worker-Thread über {@link CompletableFuture} – die FPS bleiben beim Laden der Welt stabil.</li>
 *   <li>Treffer landen in einer {@link ConcurrentHashMap}; {@code CHUNK_UNLOAD} entfernt sie wieder.</li>
 * </ol>
 */
public class HeavyChunks extends Module {

    // ---- Standard-Schwellenwerte (im ClickGUI unter "..." änderbar, 0 = Kriterium aus) ----
    public static final int MIN_BLOCK_ENTITIES = 15;
    public static final int MIN_CHESTS = 12;
    public static final int MIN_HOPPERS = 5;
    public static final int MIN_SHULKERS = 1;
    public static final int MIN_FURNACES = 8;
    public static final int MIN_SPAWNERS = 2;
    public static final int MIN_BARRELS = 8;
    public static final int MIN_ENTITIES = 25;
    /** Ab so vielen relevanten Objekten ist die Box ganz rot. */
    public static final int EXTREME_COUNT = 100;

    /** Wartezeit nach dem Laden, damit die Entities des Chunks schon da sind. */
    private static final long SCAN_DELAY_MS = 1500;
    /** Wie viele Schnappschüsse pro Tick höchstens (verteilt die Arbeit beim Welt-Laden). */
    private static final int SNAPSHOTS_PER_TICK = 12;
    /** Höchstens so viele Chat-Meldungen pro {@link #CHAT_WINDOW_MS}, Rest wird zusammengefasst. */
    private static final int CHAT_BURST = 5;
    private static final long CHAT_WINDOW_MS = 3000;

    public final NumberSetting minBlockEntities = add(new NumberSetting("minBE", "Min. Block-Entities gesamt", 0, 200, MIN_BLOCK_ENTITIES, 1));
    public final NumberSetting minChests = add(new NumberSetting("minChests", "Min. Kisten", 0, 200, MIN_CHESTS, 1));
    public final NumberSetting minHoppers = add(new NumberSetting("minHoppers", "Min. Trichter", 0, 200, MIN_HOPPERS, 1));
    public final NumberSetting minShulkers = add(new NumberSetting("minShulkers", "Min. Shulker-Kisten", 0, 100, MIN_SHULKERS, 1));
    public final NumberSetting minFurnaces = add(new NumberSetting("minFurnaces", "Min. Öfen", 0, 200, MIN_FURNACES, 1));
    public final NumberSetting minSpawners = add(new NumberSetting("minSpawners", "Min. Spawner", 0, 20, MIN_SPAWNERS, 1));
    public final NumberSetting minBarrels = add(new NumberSetting("minBarrels", "Min. Fässer", 0, 200, MIN_BARRELS, 1));
    public final NumberSetting minEntities = add(new NumberSetting("minEntities", "Min. Entities", 0, 500, MIN_ENTITIES, 1));

    public final BoolSetting severityColor = add(new BoolSetting("severity", "Farbe nach Schweregrad", true));
    public final ColorSetting lowColor = add(new ColorSetting("lowColor", "Farbe: wenig", 0xFFFFE619));
    public final ColorSetting highColor = add(new ColorSetting("highColor", "Farbe: extrem viel", 0xFFFF2222));
    public final NumberSetting extreme = add(new NumberSetting("extreme", "Ganz rot ab (Objekte)", 10, 1000, EXTREME_COUNT, 5));
    public final NumberSetting alpha = add(new NumberSetting("alpha", "Deckkraft Rahmen", 40, 255, 190, 5));
    public final BoolSetting fullHeight = add(new BoolSetting("fullHeight", "Ganze Welthöhe", false));
    public final BoolSetting chat = add(new BoolSetting("chat", "Chat-Meldung", true));

    /** Ergebnis eines Scans. Unveränderlich, darf zwischen Threads geteilt werden. */
    public record Result(int chunkX, int chunkZ, int blockEntities, int chests, int hoppers, int shulkers,
                         int furnaces, int spawners, int barrels, int items, int armorStands, int itemFrames,
                         double minY, double maxY, int worldMinY, int worldMaxY) {
        public int entities() { return items + armorStands + itemFrames; }
        public int score() { return blockEntities + entities(); }
    }

    /** Schnappschuss auf dem Haupt-Thread – nur Typen und Höhen, keine Referenzen auf Weltobjekte. */
    private record Snapshot(int gen, int chunkX, int chunkZ, List<String> beTypes, int[] beYs,
                            List<String> entityTypes, double[] entityYs, int worldMinY, int worldMaxY) {}

    private record Thresholds(int be, int chests, int hoppers, int shulkers, int furnaces, int spawners,
                              int barrels, int entities) {}

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Nova-ChunkScanner");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    /** Markierte Chunks (thread-sicher, vom Worker befüllt, vom Render-Code gelesen). */
    private final Map<Long, Result> heavy = new ConcurrentHashMap<>();
    /** Alle aktuell geladenen Chunks (nur Haupt-Thread). */
    private final Set<Long> loaded = new HashSet<>();
    /** Chunk → frühester Scan-Zeitpunkt (nur Haupt-Thread). */
    private final Map<Long, Long> pending = new LinkedHashMap<>();
    /** Chunks, zu denen schon eine Chat-Meldung kam (bis zum Entladen). */
    private final Set<Long> notified = new HashSet<>();
    /** Wird bei Weltwechsel / Ausschalten erhöht; alte Worker-Ergebnisse werden dann verworfen. */
    private final AtomicInteger generation = new AtomicInteger();

    private ClientLevel lastLevel;
    private long chatWindowStart;
    private int chatInWindow;
    private int chatSuppressed;

    public HeavyChunks() {
        super("HeavyChunks", "heavychunks", Category.RENDER, GLFW.GLFW_KEY_K, false);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public int count() {
        return heavy.size();
    }

    // ------------------------------------------------------------------ Events (Haupt-Thread)

    private void checkLevel(ClientLevel level) {
        if (level == lastLevel) return;
        lastLevel = level;
        generation.incrementAndGet();
        heavy.clear();
        loaded.clear();
        pending.clear();
        notified.clear();
    }

    public void onChunkLoad(ClientLevel level, LevelChunk chunk) {
        checkLevel(level);
        int[] pos = Compat.chunkXZ(chunk);
        long k = key(pos[0], pos[1]);
        loaded.add(k);
        if (isEnabled()) pending.put(k, System.currentTimeMillis() + SCAN_DELAY_MS);
    }

    public void onChunkUnload(ClientLevel level, LevelChunk chunk) {
        if (level != lastLevel) return;
        int[] pos = Compat.chunkXZ(chunk);
        long k = key(pos[0], pos[1]);
        loaded.remove(k);
        pending.remove(k);
        heavy.remove(k);
        notified.remove(k);
    }

    /** Neue Entity (z. B. Item-Rahmen kommen nach dem Chunk) → Chunk kurz danach neu scannen. */
    public void onEntityLoad(Entity entity, ClientLevel level) {
        if (!isEnabled() || level != lastLevel || !isCounted(entity.getType())) return;
        long k = key(entity.getBlockX() >> 4, entity.getBlockZ() >> 4);
        if (loaded.contains(k)) pending.putIfAbsent(k, System.currentTimeMillis() + 1000);
    }

    @Override
    public void onEnable() {
        // Alles, was schon geladen ist, einmal scannen
        long now = System.currentTimeMillis();
        for (long k : loaded) pending.put(k, now);
    }

    @Override
    public void onDisable() {
        generation.incrementAndGet();
        heavy.clear();
        pending.clear();
        notified.clear();
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        ClientLevel level = mc.level;
        if (level == null) return;
        checkLevel(level);
        if (pending.isEmpty()) return;

        long now = System.currentTimeMillis();
        List<Long> due = new ArrayList<>();
        for (Iterator<Map.Entry<Long, Long>> it = pending.entrySet().iterator(); it.hasNext() && due.size() < SNAPSHOTS_PER_TICK; ) {
            Map.Entry<Long, Long> e = it.next();
            if (e.getValue() > now) continue;
            due.add(e.getKey());
            it.remove();
        }
        if (due.isEmpty()) return;

        // Entities einmal durchgehen und nach Chunk sortieren (nur die fälligen Chunks)
        Map<Long, List<Entity>> byChunk = new HashMap<>();
        for (Long k : due) byChunk.put(k, new ArrayList<>());
        for (Entity e : level.entitiesForRendering()) {
            if (!isCounted(e.getType())) continue;
            List<Entity> list = byChunk.get(key(e.getBlockX() >> 4, e.getBlockZ() >> 4));
            if (list != null) list.add(e);
        }

        Thresholds th = thresholds();
        int gen = generation.get();
        for (Long k : due) {
            int cx = (int) (k >> 32), cz = (int) (long) k;
            if (!level.getChunkSource().hasChunk(cx, cz)) continue;
            Snapshot snap = snapshot(gen, level.getChunk(cx, cz), cx, cz, byChunk.get(k));
            CompletableFuture.supplyAsync(() -> analyze(snap, th), WORKER)
                    .thenAccept(result -> mc.execute(() -> apply(k, gen, result)))
                    .exceptionally(t -> {
                        NovaClient.LOGGER.warn("HeavyChunks: Scan von Chunk [{}, {}] fehlgeschlagen", cx, cz, t);
                        return null;
                    });
        }
    }

    private Thresholds thresholds() {
        return new Thresholds(minBlockEntities.getInt(), minChests.getInt(), minHoppers.getInt(), minShulkers.getInt(),
                minFurnaces.getInt(), minSpawners.getInt(), minBarrels.getInt(), minEntities.getInt());
    }

    /** Registry-ID ohne Namespace, z. B. "item" oder "chest". Seit 26.x gibt es die Konstanten wie EntityType.ITEM nicht mehr. */
    private static String entityId(EntityType<?> t) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath();
    }

    private static boolean isCounted(EntityType<?> t) {
        return switch (entityId(t)) {
            case "item", "armor_stand", "item_frame", "glow_item_frame" -> true;
            default -> false;
        };
    }

    private static Snapshot snapshot(int gen, LevelChunk chunk, int cx, int cz, List<Entity> entities) {
        List<BlockEntity> bes = new ArrayList<>(chunk.getBlockEntities().values());
        List<String> types = new ArrayList<>(bes.size());
        int[] ys = new int[bes.size()];
        for (int i = 0; i < bes.size(); i++) {
            BlockEntity be = bes.get(i);
            var id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
            types.add(id == null ? "" : id.getPath());
            ys[i] = be.getBlockPos().getY();
        }
        List<String> eTypes = new ArrayList<>(entities.size());
        double[] eYs = new double[entities.size()];
        for (int i = 0; i < entities.size(); i++) {
            eTypes.add(entityId(entities.get(i).getType()));
            eYs[i] = entities.get(i).getY();
        }
        int sections = chunk.getSections().length;
        int minY = chunk.getSectionYFromSectionIndex(0) << 4;
        int maxY = (chunk.getSectionYFromSectionIndex(sections - 1) + 1) << 4;
        return new Snapshot(gen, cx, cz, types, ys, eTypes, eYs, minY, maxY);
    }

    // ------------------------------------------------------------------ Analyse (Worker-Thread)

    private static Result analyze(Snapshot s, Thresholds th) {
        int total = 0, chests = 0, hoppers = 0, shulkers = 0, furnaces = 0, spawners = 0, barrels = 0;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (int i = 0; i < s.beTypes().size(); i++) {
            // entspricht ChestBlockEntity, ShulkerBoxBlockEntity, HopperBlockEntity,
            // (Abstract)FurnaceBlockEntity, SpawnerBlockEntity, BarrelBlockEntity
            switch (s.beTypes().get(i)) {
                case "chest", "trapped_chest" -> chests++;
                case "shulker_box" -> shulkers++;
                case "hopper" -> hoppers++;
                case "furnace", "blast_furnace", "smoker" -> furnaces++;
                case "mob_spawner" -> spawners++;
                case "barrel" -> barrels++;
                default -> { continue; }
            }
            total++;
            minY = Math.min(minY, s.beYs()[i]);
            maxY = Math.max(maxY, s.beYs()[i] + 1);
        }

        int items = 0, stands = 0, frames = 0;
        for (int i = 0; i < s.entityTypes().size(); i++) {
            switch (s.entityTypes().get(i)) {
                case "item" -> items++;
                case "armor_stand" -> stands++;
                default -> frames++;
            }
            minY = Math.min(minY, s.entityYs()[i]);
            maxY = Math.max(maxY, s.entityYs()[i] + 1);
        }
        int entities = items + stands + frames;

        boolean hit = reached(total, th.be()) || reached(chests, th.chests()) || reached(hoppers, th.hoppers())
                || reached(shulkers, th.shulkers()) || reached(furnaces, th.furnaces())
                || reached(spawners, th.spawners()) || reached(barrels, th.barrels())
                || reached(entities, th.entities());
        if (!hit) return null;

        if (minY > maxY) { minY = s.worldMinY(); maxY = s.worldMaxY(); }
        return new Result(s.chunkX(), s.chunkZ(), total, chests, hoppers, shulkers, furnaces, spawners, barrels,
                items, stands, frames, Math.max(minY, s.worldMinY()), Math.min(maxY, s.worldMaxY()),
                s.worldMinY(), s.worldMaxY());
    }

    /** Schwellenwert 0 = Kriterium ausgeschaltet. */
    private static boolean reached(int value, int threshold) {
        return threshold > 0 && value >= threshold;
    }

    // ------------------------------------------------------------------ Ergebnis (Haupt-Thread)

    private void apply(long k, int gen, Result r) {
        if (gen != generation.get() || !isEnabled() || !loaded.contains(k)) return;
        if (r == null) {
            heavy.remove(k);
            return;
        }
        heavy.put(k, r);
        if (chat.get() && notified.add(k)) notifyChat(r);
    }

    private void notifyChat(Result r) {
        long now = System.currentTimeMillis();
        if (now - chatWindowStart > CHAT_WINDOW_MS) {
            if (chatSuppressed > 0) {
                Compat.chat(prefix().append(Component.literal("… und " + chatSuppressed + " weitere Chunks markiert")
                        .withStyle(ChatFormatting.GRAY)));
            }
            chatWindowStart = now;
            chatInWindow = 0;
            chatSuppressed = 0;
        }
        if (chatInWindow++ >= CHAT_BURST) {
            chatSuppressed++;
            return;
        }

        List<String> parts = new ArrayList<>();
        part(parts, r.chests(), "Kiste", "Kisten");
        part(parts, r.shulkers(), "Shulker", "Shulker");
        part(parts, r.hoppers(), "Trichter", "Trichter");
        part(parts, r.furnaces(), "Ofen", "Öfen");
        part(parts, r.barrels(), "Fass", "Fässer");
        part(parts, r.spawners(), "Spawner", "Spawner");
        part(parts, r.items(), "Item", "Items");
        part(parts, r.itemFrames(), "Rahmen", "Rahmen");
        part(parts, r.armorStands(), "Rüstungsständer", "Rüstungsständer");

        Compat.chat(prefix()
                .append(Component.literal("Chunk [" + r.chunkX() + ", " + r.chunkZ() + "]").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" (Block " + ((r.chunkX() << 4) + 8) + ", " + ((r.chunkZ() << 4) + 8) + ") markiert: ")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.join(", ", parts)).withStyle(ChatFormatting.WHITE)));
    }

    private static MutableComponent prefix() {
        return Component.literal("[Nova] ").withStyle(ChatFormatting.AQUA);
    }

    private static void part(List<String> parts, int n, String one, String many) {
        if (n > 0) parts.add(n + " " + (n == 1 ? one : many));
    }

    // ------------------------------------------------------------------ Rendering

    public void render(GuiGraphicsExtractor ctx, Projection p) {
        if (!isEnabled() || heavy.isEmpty()) return;
        int a = alpha.getInt() << 24;
        for (Result r : heavy.values()) {
            double x = r.chunkX() << 4, z = r.chunkZ() << 4;
            double y1, y2;
            if (fullHeight.get()) {
                // Ganze Welthöhe, in der Oberwelt Y=-64 bis Y=320
                y1 = r.worldMinY();
                y2 = r.worldMaxY();
            } else {
                y1 = r.minY() - 0.5;
                y2 = r.maxY() + 0.5;
            }
            int color = (colorFor(r) & 0x00FFFFFF) | a;
            p.box3d(ctx, new AABB(x, y1, z, x + 16, y2, z + 16), color);
            // Innenrahmen etwas eingerückt → wirkt kräftiger und ist besser zu sehen
            p.box3d(ctx, new AABB(x + 0.15, y1 + 0.15, z + 0.15, x + 15.85, y2 - 0.15, z + 15.85),
                    (color & 0x00FFFFFF) | ((alpha.getInt() / 2) << 24));
        }
    }

    private int colorFor(Result r) {
        if (!severityColor.get()) return lowColor.get();
        int min = Math.max(1, minBlockEntities.getInt());
        double t = (r.score() - min) / Math.max(1.0, extreme.get() - min);
        t = Math.max(0, Math.min(1, t));
        return lerp(lowColor.get(), highColor.get(), t);
    }

    private static int lerp(int c1, int c2, double t) {
        int r = (int) Math.round(((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int) Math.round((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
