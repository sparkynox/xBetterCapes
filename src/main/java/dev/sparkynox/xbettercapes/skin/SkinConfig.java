package dev.sparkynox.xbettercapes.skin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class SkinConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("xbettercapes_skin.json");

    // "NONE" | "player:<name>" | "url:<https://...>"
    public static String selectedSkin = "NONE";
    // "classic" | "slim"
    public static String skinModel = "classic";

    public static void load() {
        try {
            File f = PATH.toFile();
            if (!f.exists()) return;
            try (Reader r = new FileReader(f)) {
                Data d = GSON.fromJson(r, Data.class);
                if (d != null) {
                    if (d.selectedSkin != null) selectedSkin = d.selectedSkin;
                    if (d.skinModel    != null) skinModel    = d.skinModel;
                }
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Data d = new Data();
            d.selectedSkin = selectedSkin;
            d.skinModel    = skinModel;
            try (Writer w = new FileWriter(PATH.toFile())) {
                GSON.toJson(d, w);
            }
        } catch (Exception ignored) {}
    }

    private static class Data {
        String selectedSkin;
        String skinModel;
    }
}
