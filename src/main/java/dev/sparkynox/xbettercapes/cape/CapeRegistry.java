package dev.sparkynox.xbettercapes.cape;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registry of all available capes.
 * To add a new cape:
 *   1. Drop PNG into resources/assets/xbettercapes/textures/capes/
 *   2. Add one register() line below
 */
public class CapeRegistry {

    private static final List<CapeEntry> BUILTIN_CAPES = new ArrayList<>();

    static {
        // ── No Cape (always first) ──────────────────────────────────────────
        register("none",  "No Cape",  null);

        // ── Built-in capes (your 6 PNGs) ───────────────────────────────────
        register("cape1", "Cape 1",   "xbettercapes:textures/capes/1.png");
        register("cape2", "Cape 2",   "xbettercapes:textures/capes/2.png");
        register("cape3", "Cape 3",   "xbettercapes:textures/capes/3.png");
        register("cape4", "Cape 4",   "xbettercapes:textures/capes/4.png");
        register("cape5", "Cape 5",   "xbettercapes:textures/capes/5.png");
        register("cape6", "Cape 6",   "xbettercapes:textures/capes/6.png");
        // ── Add more above this line ↑ ──────────────────────────────────────
    }

    private static void register(String id, String name, String path) {
        BUILTIN_CAPES.add(new CapeEntry(CapeEntry.Type.BUILTIN, id, name, path));
    }

    public static List<CapeEntry> getBuiltinCapes() {
        return BUILTIN_CAPES;
    }

    /** Parse a URL into a CapeEntry for custom URL capes. */
    public static CapeEntry fromUrl(String url) {
        return new CapeEntry(CapeEntry.Type.URL, url, "Custom URL", url);
    }
}
