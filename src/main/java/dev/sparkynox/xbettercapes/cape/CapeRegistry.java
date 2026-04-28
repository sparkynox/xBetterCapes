package dev.sparkynox.xbettercapes.cape;

import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;

public class CapeRegistry {

    private static final List<CapeEntry> BUILTIN_CAPES = new ArrayList<>();
    // Custom local cape slot — updated at runtime when user picks a file
    private static CapeEntry customLocalEntry =
            new CapeEntry(CapeEntry.Type.BUILTIN, "custom_local", "Custom", null);

    static {
        register("none",   "No Cape", null);
        // Custom slot is always second
        BUILTIN_CAPES.add(customLocalEntry);

        register("cape1",  "Cape 1",  "xbettercapes:textures/capes/1.png");
        register("cape2",  "Cape 2",  "xbettercapes:textures/capes/2.png");
        register("cape3",  "Cape 3",  "xbettercapes:textures/capes/3.png");
        register("cape4",  "Cape 4",  "xbettercapes:textures/capes/4.png");
        register("cape5",  "Cape 5",  "xbettercapes:textures/capes/5.png");
        register("cape6",  "Cape 6",  "xbettercapes:textures/capes/6.png");
        register("cape7",  "Cape 7",  "xbettercapes:textures/capes/7.png");
        register("cape8",  "Cape 8",  "xbettercapes:textures/capes/8.png");
        register("cape9",  "Cape 9",  "xbettercapes:textures/capes/9.png");
        register("cape10", "Cape 10", "xbettercapes:textures/capes/10.png");
        register("cape11", "Cape 11", "xbettercapes:textures/capes/11.png");
        register("cape12", "Cape 12", "xbettercapes:textures/capes/12.png");
        register("cape13", "Cape 13", "xbettercapes:textures/capes/13.png");
        register("cape14", "Cape 14", "xbettercapes:textures/capes/14.png");
        register("cape15", "Cape 15", "xbettercapes:textures/capes/15.png");
        register("cape16", "Cape 16", "xbettercapes:textures/capes/16.png");
        register("cape17", "Cape 17", "xbettercapes:textures/capes/17.png");
        register("cape18", "Cape 18", "xbettercapes:textures/capes/18.png");
        register("cape19", "Cape 19", "xbettercapes:textures/capes/19.png");
        register("cape20", "Cape 20", "xbettercapes:textures/capes/20.png");
        register("cape21", "Cape 21", "xbettercapes:textures/capes/21.png");
        register("cape22", "Cape 22", "xbettercapes:textures/capes/22.png");
        register("cape23", "Cape 23", "xbettercapes:textures/capes/23.png");
        register("cape24", "Cape 24", "xbettercapes:textures/capes/24.png");
        register("cape25", "Cape 25", "xbettercapes:textures/capes/25.png");
        register("cape26", "Cape 26", "xbettercapes:textures/capes/26.png");
        register("cape27", "Cape 27", "xbettercapes:textures/capes/27.png");
        register("cape28", "Cape 28", "xbettercapes:textures/capes/28.png");
        register("cape29", "Cape 29", "xbettercapes:textures/capes/29.png");
        register("cape30", "Cape 30", "xbettercapes:textures/capes/30.png");
        register("cape31", "Cape 31", "xbettercapes:textures/capes/31.png");
        register("cape32", "Cape 32", "xbettercapes:textures/capes/32.png");
        register("cape33", "Cape 33", "xbettercapes:textures/capes/33.png");
        register("cape34", "Cape 34", "xbettercapes:textures/capes/34.png");
        register("cape35", "Cape 35", "xbettercapes:textures/capes/35.png");
        register("cape36", "Cape 36", "xbettercapes:textures/capes/36.png");
        register("cape37", "Cape 37", "xbettercapes:textures/capes/37.png");
        register("cape38", "Cape 38", "xbettercapes:textures/capes/38.png");
        register("cape39", "Cape 39", "xbettercapes:textures/capes/39.png");
        register("cape40", "Cape 40", "xbettercapes:textures/capes/40.png");
        register("cape41", "Cape 41", "xbettercapes:textures/capes/41.png");
    }

    private static void register(String id, String name, String path) {
        BUILTIN_CAPES.add(new CapeEntry(CapeEntry.Type.BUILTIN, id, name, path));
    }

    public static List<CapeEntry> getBuiltinCapes() {
        return BUILTIN_CAPES;
    }

    /**
     * Called when user picks a local file.
     * Updates the Custom slot's resourcePath with the registered texture ID,
     * so CapeTextureManager will find it by the Identifier directly.
     */
    public static void setCustomLocal(Identifier textureId, String fileName) {
        // Replace entry in-place so existing list references stay valid
        String path = textureId.getNamespace() + ":" + textureId.getPath();
        CapeEntry updated = new CapeEntry(
                CapeEntry.Type.BUILTIN, "custom_local",
                "Custom", path);
        int idx = BUILTIN_CAPES.indexOf(customLocalEntry);
        if (idx >= 0) BUILTIN_CAPES.set(idx, updated);
        customLocalEntry = updated;
        // Pre-cache the identifier so getTexture() returns it immediately
        // (texture is already registered in TextureManager by caller)
    }

    public static CapeEntry fromUrl(String url) {
        return new CapeEntry(CapeEntry.Type.URL, url, "Custom URL", url);
    }
}
