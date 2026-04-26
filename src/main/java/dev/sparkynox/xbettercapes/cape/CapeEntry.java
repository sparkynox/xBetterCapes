package dev.sparkynox.xbettercapes.cape;

/**
 * Represents one cape entry in the GUI.
 * type = BUILTIN → resourcePath is the mod resource path (e.g. "xbettercapes:textures/capes/mystic.png")
 * type = URL     → resourcePath is a full HTTPS URL
 */
public class CapeEntry {

    public enum Type { BUILTIN, URL }

    public final Type type;
    public final String id;        // unique key stored in config
    public final String displayName;
    public final String resourcePath; // mod namespace:path  OR  https://...

    public CapeEntry(Type type, String id, String displayName, String resourcePath) {
        this.type = type;
        this.id = id;
        this.displayName = displayName;
        this.resourcePath = resourcePath;
    }

    /** Returns the config string that gets saved */
    public String toConfigString() {
        return (type == Type.BUILTIN ? "builtin:" : "url:") + id;
    }
}
