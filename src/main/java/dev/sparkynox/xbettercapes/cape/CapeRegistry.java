package dev.sparkynox.xbettercapes.cape;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registry of all available capes.
 * Add new built-in capes here — just drop the PNG into
 * resources/assets/xbettercapes/textures/capes/ and add an entry below.
 */
public class CapeRegistry {

    private static final List<CapeEntry> BUILTIN_CAPES = new ArrayList<>();

    static {
        // ── Built-in capes ──────────────────────────────────────────────────
        // Format: register("id", "Display Name", "namespace:textures/capes/file.png")
        register("none",       "No Cape",       null);
        register("mystic",     "Mystic",        "xbettercapes:textures/capes/mystic.png");
        register("fire",       "Fire",          "xbettercapes:textures/capes/fire.png");
        register("galaxy",     "Galaxy",        "xbettercapes:textures/capes/galaxy.png");
        register("yori",       "Yori",          "xbettercapes:textures/capes/yori.png");
        register("sparkynox",  "SparkyNox",     "xbettercapes:textures/capes/sparkynox.png");
        // ────────────────────────────────────────────────────────────────────
        // Add more capes above this line ↑
    }

    private static void register(String id, String name, String path) {
        BUILTIN_CAPES.add(new CapeEntry(CapeEntry.Type.BUILTIN, id, name, path));
    }

    public static List<CapeEntry> getBuiltinCapes() {
        return BUILTIN_CAPES;
    }

    /** Parse a URL string into a CapeEntry (for custom URL capes). */
    public static CapeEntry fromUrl(String url) {
        return new CapeEntry(CapeEntry.Type.URL, url, "Custom URL", url);
    }
}
