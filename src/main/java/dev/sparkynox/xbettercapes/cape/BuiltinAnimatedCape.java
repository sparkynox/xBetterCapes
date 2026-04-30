package dev.sparkynox.xbettercapes.cape;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads built-in animated capes from jar resources.
 * Uses getResourceAsStream instead of ResourceManager —
 * works before world join, no timing issues.
 */
public class BuiltinAnimatedCape {

    private static final int  FPS      = 24;
    private static final long FRAME_MS = 1000L / FPS; // 41ms per frame

    private static final ExecutorService LOADER =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "xBC-BuiltinAnimLoader");
                t.setDaemon(true);
                return t;
            });

    private static final Map<String, List<Identifier>> FRAME_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean>          PENDING     = new ConcurrentHashMap<>();

    /**
     * Register + load a built-in animated cape.
     * Can be called anytime — uses class loader, not ResourceManager.
     */
    public static void register(String id, String name, String folder, int frameCount) {
        if (PENDING.containsKey(id) || FRAME_CACHE.containsKey(id)) return;
        PENDING.put(id, true);

        LOADER.submit(() -> {
            List<byte[]> frameBytes = new ArrayList<>();

            for (int i = 1; i <= frameCount; i++) {
                // Load directly from jar via ClassLoader — works at any time
                String path = "/assets/xbettercapes/textures/capes/animated/"
                        + folder + "/frame" + i + ".png";
                try (InputStream is = BuiltinAnimatedCape.class.getResourceAsStream(path)) {
                    if (is == null) {
                        System.err.println("[xBetterCapes] Missing: " + path);
                        break;
                    }
                    frameBytes.add(is.readAllBytes());
                } catch (Exception e) {
                    System.err.println("[xBetterCapes] Read error frame " + i + ": " + e.getMessage());
                    break;
                }
            }

            if (frameBytes.isEmpty()) {
                System.err.println("[xBetterCapes] No frames for: " + folder);
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
                        Identifier texId = Identifier.of("xbettercapes", id + "_frame_" + i);
                        MinecraftClient.getInstance()
                                .getTextureManager().registerTexture(texId, tex);
                        ids.add(texId);
                    } catch (Exception e) {
                        System.err.println("[xBetterCapes] Frame reg fail " + i + ": " + e.getMessage());
                    }
                }
                FRAME_CACHE.put(id, ids);
                PENDING.remove(id);
                System.out.println("[xBetterCapes] Loaded: " + name +
                        " (" + ids.size() + " frames @ " + FPS + "FPS)");
            });
        });
    }

    /** Called every render frame — just an array lookup, zero overhead */
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
