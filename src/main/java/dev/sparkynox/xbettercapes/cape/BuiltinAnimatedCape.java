package dev.sparkynox.xbettercapes.cape;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles built-in animated capes embedded in the mod jar.
 * Frames are at: assets/xbettercapes/textures/capes/animated/<name>/frame1.png ...
 *
 * 1x speed = 24 FPS (matches PixelLab default export)
 * 75 frames / 24 FPS = ~3.1s loop
 */
public class BuiltinAnimatedCape {

    // 1x speed = 24 FPS → each frame shown for ~41ms
    private static final int FPS      = 24;
    private static final long FRAME_MS = 1000L / FPS;

    private static final ExecutorService LOADER =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "xBC-BuiltinAnimLoader");
                t.setDaemon(true);
                return t;
            });

    // cape id → ordered frame Identifiers
    private static final Map<String, List<Identifier>> FRAME_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> PENDING = new ConcurrentHashMap<>();

    /**
     * Register a built-in animated cape.
     * Call from CapeRegistry static block.
     * @param id        unique id e.g. "anim_builtin_pixellab"
     * @param name      display name e.g. "PixelLab"
     * @param folder    resource folder name e.g. "pixellab"
     * @param frameCount total number of frames
     */
    public static void register(String id, String name, String folder, int frameCount) {
        if (PENDING.containsKey(id) || FRAME_CACHE.containsKey(id)) return;
        PENDING.put(id, true);

        LOADER.submit(() -> {
            // Read all frames from jar resources
            List<byte[]> frameBytes = new ArrayList<>();
            ResourceManager rm = MinecraftClient.getInstance().getResourceManager();

            for (int i = 1; i <= frameCount; i++) {
                Identifier resId = Identifier.of("xbettercapes",
                        "textures/capes/animated/" + folder + "/frame" + i + ".png");
                try {
                    try (InputStream is = rm.getResource(resId).get().getInputStream()) {
                        frameBytes.add(is.readAllBytes());
                    }
                } catch (Exception e) {
                    System.err.println("[xBetterCapes] Missing frame " + i + " in " + folder);
                    break;
                }
            }

            if (frameBytes.isEmpty()) {
                System.err.println("[xBetterCapes] No frames loaded for: " + folder);
                PENDING.remove(id);
                return;
            }

            final List<byte[]> finalBytes = frameBytes;
            MinecraftClient.getInstance().execute(() -> {
                List<Identifier> ids = new ArrayList<>();
                for (int i = 0; i < finalBytes.size(); i++) {
                    try {
                        NativeImage img = NativeImage.read(
                                new java.io.ByteArrayInputStream(finalBytes.get(i)));
                        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
                        Identifier texId = Identifier.of("xbettercapes",
                                id + "_frame_" + i);
                        MinecraftClient.getInstance()
                                .getTextureManager().registerTexture(texId, tex);
                        ids.add(texId);
                    } catch (Exception e) {
                        System.err.println("[xBetterCapes] Frame " + i + " register fail: " + e.getMessage());
                    }
                }
                FRAME_CACHE.put(id, ids);
                PENDING.remove(id);
                System.out.println("[xBetterCapes] Builtin anim loaded: " +
                        name + " (" + ids.size() + " frames @ " + FPS + "FPS)");
            });
        });
    }

    /** Get current frame — called every render, zero overhead */
    public static Identifier getCurrentFrame(String id) {
        List<Identifier> frames = FRAME_CACHE.get(id);
        if (frames == null || frames.isEmpty()) return null;
        int frame = (int)((System.currentTimeMillis() / FRAME_MS) % frames.size());
        return frames.get(frame);
    }

    public static boolean isBuiltinAnim(String id) {
        return id != null && id.startsWith("anim_builtin_");
    }

    public static boolean isLoading(String id) {
        return PENDING.containsKey(id);
    }
}
