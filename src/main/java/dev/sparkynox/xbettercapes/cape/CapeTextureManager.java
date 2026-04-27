package dev.sparkynox.xbettercapes.cape;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

        PENDING.put(key, true);
        LOADER.submit(() -> fetchUrl(key, entry.resourcePath));
        return null;
    }

    public static void prefetch(CapeEntry entry) {
        if (entry == null || entry.type != CapeEntry.Type.URL) return;
        getTexture(entry);
    }

    private static void fetchUrl(String key, String urlStr) {
        try {
            byte[] bytes = downloadWithRedirects(urlStr, 8);
            if (bytes == null || bytes.length == 0) {
                PENDING.remove(key);
                return;
            }

            // Verify PNG magic bytes before passing to NativeImage
            // PNG starts with: 0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
            if (bytes.length < 8 ||
                (bytes[0] & 0xFF) != 0x89 ||
                (bytes[1] & 0xFF) != 0x50 ||
                (bytes[2] & 0xFF) != 0x4E ||
                (bytes[3] & 0xFF) != 0x47) {
                System.err.println("[xBetterCapes] URL did not return a valid PNG. " +
                        "Got " + bytes.length + " bytes. First bytes: " +
                        String.format("%02X %02X %02X %02X",
                                bytes[0] & 0xFF, bytes[1] & 0xFF,
                                bytes[2] & 0xFF, bytes[3] & 0xFF));
                PENDING.remove(key);
                return;
            }

            final byte[] finalBytes = bytes;
            MinecraftClient.getInstance().execute(() -> {
                try {
                    NativeImage img = NativeImage.read(new ByteArrayInputStream(finalBytes));
                    NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
                    Identifier id = Identifier.of("xbettercapes",
                            "url_cape_" + COUNTER.incrementAndGet());
                    MinecraftClient.getInstance()
                            .getTextureManager().registerTexture(id, tex);
                    CACHE.put(key, id);
                    System.out.println("[xBetterCapes] URL cape loaded: " + id);
                } catch (Exception e) {
                    System.err.println("[xBetterCapes] Failed to register texture: " + e.getMessage());
                } finally {
                    PENDING.remove(key);
                }
            });
        } catch (Exception e) {
            System.err.println("[xBetterCapes] Fetch error: " + e.getMessage());
            PENDING.remove(key);
        }
    }

    private static byte[] downloadWithRedirects(String urlStr, int maxRedirects)
            throws Exception {
        String currentUrl = urlStr;
        for (int hop = 0; hop <= maxRedirects; hop++) {
            HttpURLConnection conn = (HttpURLConnection)
                    new URL(currentUrl).openConnection();
            conn.setInstanceFollowRedirects(false);
            // Spoof a real browser — postimg.cc blocks bots without these headers
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "image/png,image/webp,image/*,*/*");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setRequestProperty("Referer", "https://postimg.cc/");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.connect();

            int code = conn.getResponseCode();

            // Follow redirects manually (handles http→https too)
            if (code == 301 || code == 302 || code == 303 ||
                code == 307 || code == 308) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc == null || loc.isEmpty()) break;
                if (loc.startsWith("/")) {
                    URL base = new URL(currentUrl);
                    loc = base.getProtocol() + "://" + base.getHost() + loc;
                }
                System.out.println("[xBetterCapes] Redirect " + hop +
                        ": " + currentUrl + " -> " + loc);
                currentUrl = loc;
                continue;
            }

            if (code == 200) {
                String contentType = conn.getContentType();
                // Make sure we actually got an image
                if (contentType != null && contentType.contains("text/html")) {
                    System.err.println("[xBetterCapes] Got HTML instead of image! " +
                            "Content-Type: " + contentType);
                    conn.disconnect();
                    return null;
                }
                try (InputStream is = conn.getInputStream()) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    byte[] tmp = new byte[8192];
                    int n;
                    while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
                    conn.disconnect();
                    return buf.toByteArray();
                }
            }

            System.err.println("[xBetterCapes] HTTP error: " + code + " for " + currentUrl);
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
