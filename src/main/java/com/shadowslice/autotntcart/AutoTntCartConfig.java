package com.shadowslice.autotntcart;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AutoTntCartConfig {

    public enum TriggerMode {
        AUTO,   // 服务端支持则走服务端，否则回退到客户端模拟
        SERVER, // 强制服务端模式（服务端必须安装）
        CLIENT  // 强制客户端模拟（任何服务器都能用）
    }

    public TriggerMode mode = TriggerMode.AUTO;
    public int cooldownMs = 1000;
    public boolean showMessages = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(), "autotntcart.json");
    private static AutoTntCartConfig INSTANCE;

    public static AutoTntCartConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        if (FILE.exists()) {
            try (FileReader r = new FileReader(FILE)) {
                INSTANCE = GSON.fromJson(r, AutoTntCartConfig.class);
                if (INSTANCE == null) INSTANCE = new AutoTntCartConfig();
            } catch (IOException e) {
                INSTANCE = new AutoTntCartConfig();
            }
        } else {
            INSTANCE = new AutoTntCartConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter w = new FileWriter(FILE)) {
            GSON.toJson(INSTANCE, w);
        } catch (IOException ignored) {}
    }
}