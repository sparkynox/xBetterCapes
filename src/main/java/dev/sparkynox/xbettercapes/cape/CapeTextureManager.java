package dev.sparkynox.xbettercapes.cape;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CapeTextureManager {

    private static final ExecutorService LOADER =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "xBetterCapes-Loader");
                t.setDaemon(true);
                return t;
            });

    private static final Map<String, Identifier> CACHE   = new ConcurrentHashMap<>();
    private static final Map<String, Boolean>    PENDING = new ConcurrentHashMap<>();
    private static final AtomicInteger           COUNTER = new AtomicInteger(0);

    public static Identifier getTexture(CapeEntry entry) {
        if (entry == null || entry.resourcePath == null) return null;
        String key = entry.toConfigString();

        Identifier cached = CACHE.get(key);
        if (cached != null) return cached;
        if (PENDING.containsKey(key)) return null;

        if (entry.type == CapeEntry.Type.BUILTIN) {
            String path = entry.resourcePath;
            int colon = path.indexOf(':');
            Identifier id = (colon >= 0)
                    ? Identifier.of(path.substring(0, colon), path.substring(colon + 1))
                    : Identifier.of(path);
            CACHE.put(key, id);
            return id;
        }

        // URL type — load async
        PENDING.put(key, true);
        LOADER.submit(() -> fetchUrl(key, entry.resourcePath));
        return null;
    }

    /** Call this from Load URL button to kick off fetch immediately */
    public static void prefetch(CapeEntry entry) {
        if (entry == null || entry.type != CapeEntry.Type.URL) return;
        getTexture(entry); // triggers async load if not already cached
    }

    private static void fetchUrl(String key, String urlStr) {
        try {
            byte[] bytes = downloadWithRedirects(urlStr, 5);
            if (bytes == null || bytes.length == 0) {
                PENDING.remove(key);
                return;
            }
            MinecraftClient.getInstance().execute(() -> {
                try {
                    NativeImage img = NativeImage.read(
                            new java.io.ByteArrayInputStream(bytes));
                    NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
                    Identifier id = Identifier.of("xbettercapes",
                            "url_cape_" + COUNTER.incrementAndGet());
                    MinecraftClient.getInstance()
                            .getTextureManager().registerTexture(id, tex);
                    CACHE.put(key, id);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    PENDING.remove(key);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            PENDING.remove(key);
        }
    }

    /**
     * Simple HTTP download that follows redirects manually.
     * Java's HttpURLConnection follows same-protocol redirects automatically,
     * but NOT http→https redirects. We handle up to maxRedirects hops.
     */
    private static byte[] downloadWithRedirects(String urlStr, int maxRedirects)
            throws Exception {
        String currentUrl = urlStr;
        for (int i = 0; i <= maxRedirects; i++) {
            HttpURLConnection conn = (HttpURLConnection) new URL(currentUrl).openConnection();
            conn.setInstanceFollowRedirects(false); // handle manually
            conn.setRequestProperty("User-Agent", "xBetterCapes/1.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.connect();

            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc == null) break;
                // Handle relative redirects
                if (loc.startsWith("/")) {
                    URL base = new URL(currentUrl);
                    loc = base.getProtocol() + "://" + base.getHost() + loc;
                }
                currentUrl = loc;
                continue;
            }

            if (code == 200) {
                try (InputStream is = conn.getInputStream()) {
                    java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                    byte[] tmp = new byte[8192];
                    int n;
                    while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
                    conn.disconnect();
                    return buf.toByteArray();
                }
            }
            conn.disconnect();
            break;
        }
        return null;
    }

    public static void releaseUrlTextures() {
        MinecraftClient client = MinecraftClient.getInstance();
        CACHE.entrySet().removeIf(e -> {
            if (e.getKey().startsWith("url:")) {
                client.getTextureManager().destroyTexture(e.getValue());
                return true;
            }
            return false;
        });
    }
}
