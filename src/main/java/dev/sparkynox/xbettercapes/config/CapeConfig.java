package dev.sparkynox.xbettercapes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class CapeConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("xbettercapes.json");

    // "NONE" | "builtin:<id>" | "url:<https://...>"
    public static String selectedCape = "NONE";

    public static void load() {
        try {
            File f = CONFIG_PATH.toFile();
            if (!f.exists()) return;
            try (Reader r = new FileReader(f)) {
                Data d = GSON.fromJson(r, Data.class);
                if (d != null && d.selectedCape != null) selectedCape = d.selectedCape;
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Data d = new Data();
            d.selectedCape = selectedCape;
            try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(d, w);
            }
        } catch (Exception ignored) {}
    }

    private static class Data {
        String selectedCape;
    }
}
