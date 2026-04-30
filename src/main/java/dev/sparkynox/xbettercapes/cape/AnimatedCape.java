package dev.sparkynox.xbettercapes.cape;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Animated cape system.
 * - Loads a folder of PNGs (frame1.png, frame2.png, ... frameN.png)
 * - Cycles through frames at 16 FPS using System.currentTimeMillis()
 * - Zero tick loop — current frame computed at render time only
 * - All frames cached as separate Identifiers in TextureManager
 */
public class AnimatedCape {

    private static final int FPS = 16;
    private static final long FRAME_MS = 1000L / FPS; // 62ms per frame

    private static final ExecutorService LOADER =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "xBC-AnimLoader");
                t.setDaemon(true);
                return t;
            });

    // cape id → ordered list of frame Identifiers
    private static final Map<String, List<Identifier>> FRAME_CACHE = new LinkedHashMap<>();
    private static final Map<String, Boolean> PENDING = new HashMap<>();

    // Currently registered animated capes (id → display name)
    private static final List<CapeEntry> ANIMATED_ENTRIES = new ArrayList<>();

    /**
     * Called on startup — scans .minecraft/config/xbettercapes/animations/
     * Each subfolder = one animated cape.
     * Subfolder must contain frame1.png, frame2.png, etc.
     */
    public static void discoverAndLoad() {
        File animDir = getAnimDir();
        if (!animDir.exists()) {
            animDir.mkdirs();
            // Create a README so user knows where to put frames
            try {
                File readme = new File(animDir, "README.txt");
                Files.writeString(readme.toPath(),
                        "xBetter Capes — Animated Capes\n" +
                        "================================\n" +
                        "Create a subfolder for each animated cape.\n" +
                        "Put frames inside named: frame1.png, frame2.png, frame3.png ...\n\n" +
                        "Example:\n" +
                        "  animations/\n" +
                        "    rainbow/\n" +
                        "      frame1.png\n" +
                        "      frame2.png\n" +
                        "      frame3.png\n" +
                        "    fire/\n" +
                        "      frame1.png\n" +
                        "      ...\n\n" +
                        "Each PNG must be 64x32 (standard Minecraft cape format).\n" +
                        "Animation runs at 16 FPS.\n");
            } catch (Exception ignored) {}
            return;
        }

        File[] subDirs = animDir.listFiles(File::isDirectory);
        if (subDirs == null) return;

        Arrays.sort(subDirs, Comparator.comparing(File::getName));

        for (File dir : subDirs) {
            String id = "anim_" + dir.getName().toLowerCase()
                    .replaceAll("[^a-z0-9_]", "_");
            String displayName = capitalize(dir.getName());
            ANIMATED_ENTRIES.add(new CapeEntry(
                    CapeEntry.Type.BUILTIN, id, displayName + " (Anim)", null));
            // Load frames async
            loadFramesAsync(id, dir);
        }
    }

    private static void loadFramesAsync(String id, File dir) {
        if (PENDING.containsKey(id)) return;
        PENDING.put(id, true);

        LOADER.submit(() -> {
            // Collect frame files: frame1.png, frame2.png, ...
            List<File> frameFiles = new ArrayList<>();
            int i = 1;
            while (true) {
                File f = new File(dir, "frame" + i + ".png");
                if (!f.exists()) break;
                frameFiles.add(f);
                i++;
            }
            if (frameFiles.isEmpty()) {
                System.err.println("[xBetterCapes] No frames found in: " + dir.getName());
                PENDING.remove(id);
                return;
            }

            // Read all frames as byte arrays on background thread
            List<byte[]> frameBytes = new ArrayList<>();
            for (File f : frameFiles) {
                try {
                    frameBytes.add(Files.readAllBytes(f.toPath()));
                } catch (Exception e) {
                    System.err.println("[xBetterCapes] Failed to read " + f.getName());
                }
            }

            // Register textures on main thread
            MinecraftClient.getInstance().execute(() -> {
                List<Identifier> ids = new ArrayList<>();
                for (int fi = 0; fi < frameBytes.size(); fi++) {
                    try {
                        NativeImage img = NativeImage.read(
                                new java.io.ByteArrayInputStream(frameBytes.get(fi)));
                        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
                        Identifier texId = Identifier.of("xbettercapes",
                                id + "_frame_" + fi);
                        MinecraftClient.getInstance()
                                .getTextureManager().registerTexture(texId, tex);
                        ids.add(texId);
                    } catch (Exception e) {
                        System.err.println("[xBetterCapes] Frame " + fi + " load failed: " + e.getMessage());
                    }
                }
                FRAME_CACHE.put(id, ids);
                PENDING.remove(id);
                System.out.println("[xBetterCapes] Animated cape loaded: " +
                        dir.getName() + " (" + ids.size() + " frames)");
            });
        });
    }

    /**
     * Get the current frame Identifier for an animated cape.
     * Called every render frame — uses System.currentTimeMillis() for timing.
     * Zero overhead — just an array index lookup.
     */
    public static Identifier getCurrentFrame(String capeId) {
        List<Identifier> frames = FRAME_CACHE.get(capeId);
        if (frames == null || frames.isEmpty()) return null;
        int frame = (int)((System.currentTimeMillis() / FRAME_MS) % frames.size());
        return frames.get(frame);
    }

    public static boolean isAnimated(String capeId) {
        return capeId != null && capeId.startsWith("anim_");
    }

    public static boolean isLoaded(String capeId) {
        return FRAME_CACHE.containsKey(capeId) && !FRAME_CACHE.get(capeId).isEmpty();
    }

    public static List<CapeEntry> getAnimatedEntries() {
        return ANIMATED_ENTRIES;
    }

    public static File getAnimDir() {
        return net.fabricmc.loader.api.FabricLoader.getInstance()
                .getConfigDir()
                .resolve("xbettercapes")
                .resolve("animations")
                .toFile();
    }

    public static void releaseAll() {
        MinecraftClient mc = MinecraftClient.getInstance();
        FRAME_CACHE.forEach((id, frames) ->
                frames.forEach(f -> mc.getTextureManager().destroyTexture(f)));
        FRAME_CACHE.clear();
        ANIMATED_ENTRIES.clear();
        PENDING.clear();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
