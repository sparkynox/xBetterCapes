package dev.sparkynox.xbettercapes.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fetches skin textures:
 *  - By player name  → Mojang API: /users/profiles/minecraft/<name> → UUID → session profile → skin URL
 *  - By direct URL   → direct download
 *
 * All fetching is async on a single daemon thread.
 * Results cached in memory.
 */
public class SkinFetcher {

    private static final ExecutorService LOADER =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "xBC-SkinFetcher");
                t.setDaemon(true);
                return t;
            });

    private static final Map<String, Identifier> CACHE   = new ConcurrentHashMap<>();
    private static final Map<String, Boolean>    PENDING = new ConcurrentHashMap<>();
    private static final AtomicInteger           COUNTER = new AtomicInteger(0);

    // Status message for GUI feedback
    public static volatile String statusMsg = "";

    /**
     * Returns cached skin Identifier, or null if still loading.
     * Kicks off async load if needed.
     */
    public static Identifier getSkin(String cfg) {
        if (cfg == null || cfg.equals("NONE")) return null;

        Identifier cached = CACHE.get(cfg);
        if (cached != null) return cached;
        if (PENDING.containsKey(cfg)) return null;

        PENDING.put(cfg, true);
        statusMsg = "Loading skin...";

        if (cfg.startsWith("player:")) {
            String name = cfg.substring("player:".length()).trim();
            LOADER.submit(() -> fetchByName(cfg, name));
        } else if (cfg.startsWith("url:")) {
            String url = cfg.substring("url:".length());
            LOADER.submit(() -> fetchByUrl(cfg, url));
        }
        return null;
    }

    /** Fetch skin by Minecraft player name via Mojang API */
    private static void fetchByName(String cacheKey, String playerName) {
        try {
            // Step 1: Name → UUID
            String profileJson = httpGet(
                    "https://api.mojang.com/users/profiles/minecraft/" + playerName);
            if (profileJson == null) {
                fail(cacheKey, "Player not found: " + playerName); return;
            }
            JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
            String uuid = profile.get("id").getAsString();

            // Step 2: UUID → Session profile (contains skin URL)
            String sessionJson = httpGet(
                    "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            if (sessionJson == null) {
                fail(cacheKey, "Could not fetch profile for: " + playerName); return;
            }

            // Step 3: Decode textures property (base64 JSON)
            JsonObject session = JsonParser.parseString(sessionJson).getAsJsonObject();
            var props = session.getAsJsonArray("properties");
            String skinUrl = null;
            for (var prop : props) {
                JsonObject p = prop.getAsJsonObject();
                if ("textures".equals(p.get("name").getAsString())) {
                    String decoded = new String(Base64.getDecoder()
                            .decode(p.get("value").getAsString()));
                    JsonObject textures = JsonParser.parseString(decoded)
                            .getAsJsonObject()
                            .getAsJsonObject("textures");
                    if (textures.has("SKIN")) {
                        skinUrl = textures.getAsJsonObject("SKIN")
                                .get("url").getAsString();
                        // Detect slim model
                        if (textures.getAsJsonObject("SKIN").has("metadata")) {
                            String model = textures.getAsJsonObject("SKIN")
                                    .getAsJsonObject("metadata")
                                    .get("model").getAsString();
                            if ("slim".equals(model)) {
                                SkinConfig.skinModel = "slim";
                                SkinConfig.save();
                            }
                        }
                    }
                    break;
                }
            }
            if (skinUrl == null) { fail(cacheKey, "No skin found for: " + playerName); return; }

            // Step 4: Download skin PNG
            byte[] bytes = downloadBytes(skinUrl);
            registerTexture(cacheKey, bytes, playerName);

        } catch (Exception e) {
            fail(cacheKey, "Error: " + e.getMessage());
        }
    }

    /** Fetch skin by direct URL */
    private static void fetchByUrl(String cacheKey, String url) {
        try {
            byte[] bytes = downloadBytes(url);
            registerTexture(cacheKey, bytes, "custom");
        } catch (Exception e) {
            fail(cacheKey, "Download failed: " + e.getMessage());
        }
    }

    private static void registerTexture(String cacheKey, byte[] bytes, String label) {
        if (bytes == null || bytes.length < 4) {
            fail(cacheKey, "Empty response for: " + label); return;
        }
        if ((bytes[0]&0xFF) != 0x89 || (bytes[1]&0xFF) != 0x50) {
            fail(cacheKey, "Not a valid PNG: " + label); return;
        }
        MinecraftClient.getInstance().execute(() -> {
            try {
                NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
                NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
                Identifier id = Identifier.of("xbettercapes",
                        "skin_" + COUNTER.incrementAndGet());
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
                CACHE.put(cacheKey, id);
                statusMsg = "Skin loaded! Rejoin to see.";
                System.out.println("[xBetterCapes] Skin loaded: " + id);
            } catch (Exception e) {
                fail(cacheKey, "Register failed: " + e.getMessage());
            } finally {
                PENDING.remove(cacheKey);
            }
        });
    }

    private static void fail(String cacheKey, String msg) {
        System.err.println("[xBetterCapes] " + msg);
        statusMsg = msg;
        PENDING.remove(cacheKey);
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "xBetterCapes/1.0");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        int code = conn.getResponseCode();
        if (code != 200) { conn.disconnect(); return null; }
        try (InputStream is = conn.getInputStream()) {
            return new String(is.readAllBytes());
        } finally { conn.disconnect(); }
    }

    private static byte[] downloadBytes(String urlStr) throws Exception {
        String current = urlStr;
        for (int hop = 0; hop < 6; hop++) {
            HttpURLConnection conn = (HttpURLConnection) new URL(current).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0");
            conn.setRequestProperty("Accept", "image/png,image/*,*/*");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.connect();
            int code = conn.getResponseCode();
            if (code==301||code==302||code==303||code==307||code==308) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc == null) break;
                if (loc.startsWith("/")) {
                    URL base = new URL(current);
                    loc = base.getProtocol()+"://"+base.getHost()+loc;
                }
                current = loc; continue;
            }
            if (code == 200) {
                try (InputStream is = conn.getInputStream()) {
                    return is.readAllBytes();
                } finally { conn.disconnect(); }
            }
            conn.disconnect(); break;
        }
        return null;
    }

    public static void clearCache() {
        MinecraftClient mc = MinecraftClient.getInstance();
        CACHE.forEach((k, id) -> mc.getTextureManager().destroyTexture(id));
        CACHE.clear();
        PENDING.clear();
        statusMsg = "";
    }
}
