package dev.sparkynox.xbettercapes.cape;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages cape texture loading.
 * - Built-in capes: loaded from mod resources (zero network).
 * - URL capes: fetched async on a single background thread, registered on main thread.
 * - All textures are cached; never loaded twice.
 * - Call releaseAll() when the GUI closes to free VRAM.
 */
public class CapeTextureManager {

    // Single daemon thread — no extra RAM, no frame stutter
    private static final ExecutorService LOADER =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "xBetterCapes-TextureLoader");
                t.setDaemon(true);
                return t;
            });

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    // id → Identifier (Minecraft texture)
    private static final Map<String, Identifier> CACHE = new ConcurrentHashMap<>();
    // ids currently being fetched — prevents duplicate requests
    private static final Map<String, Boolean> PENDING = new ConcurrentHashMap<>();

    private static int urlTextureCounter = 0;

    /**
     * Returns the Identifier for a CapeEntry, or null if not yet ready.
     * Kicks off async loading if needed.
     */
    public static Identifier getTexture(CapeEntry entry) {
        if (entry == null || entry.resourcePath == null) return null;

        String cacheKey = entry.toConfigString();

        Identifier cached = CACHE.get(cacheKey);
        if (cached != null) return cached;

        if (PENDING.containsKey(cacheKey)) return null; // already loading

        if (entry.type == CapeEntry.Type.BUILTIN) {
            // Parse "namespace:path" into Identifier
            String path = entry.resourcePath;
            int colon = path.indexOf(':');
            Identifier id = (colon >= 0)
                    ? Identifier.of(path.substring(0, colon), path.substring(colon + 1))
                    : Identifier.of(path);
            CACHE.put(cacheKey, id);
            return id;
        }

        // URL cape — fetch async
        PENDING.put(cacheKey, true);
        LOADER.submit(() -> loadUrlTexture(cacheKey, entry.resourcePath));
        return null;
    }

    private static void loadUrlTexture(String cacheKey, String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "xBetterCapes/1.0")
                    .GET()
                    .build();

            HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() != 200) throw new RuntimeException("HTTP " + res.statusCode());

            byte[] bytes = res.body();

            // Convert to NativeImage on main thread to avoid GL context issues
            MinecraftClient.getInstance().execute(() -> {
                try (InputStream is = new ByteArrayInputStream(bytes)) {
                    NativeImage img = NativeImage.read(is);
                    NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
                    String texName = "xbettercapes:url_cape_" + (urlTextureCounter++);
                    Identifier id = Identifier.of("xbettercapes", "url_cape_" + urlTextureCounter);
                    MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
                    CACHE.put(cacheKey, id);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    PENDING.remove(cacheKey);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            PENDING.remove(cacheKey);
        }
    }

    /** Call this to free dynamically registered textures (URL capes) from VRAM. */
    public static void releaseUrlTextures() {
        MinecraftClient client = MinecraftClient.getInstance();
        CACHE.forEach((key, id) -> {
            if (key.startsWith("url:")) {
                client.getTextureManager().destroyTexture(id);
            }
        });
        CACHE.entrySet().removeIf(e -> e.getKey().startsWith("url:"));
    }
}
