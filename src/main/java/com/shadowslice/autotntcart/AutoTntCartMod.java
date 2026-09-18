package com.shadowslice.autotntcart;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shadowslice.autotntcart.network.AutoTntCartPayload;
import com.shadowslice.autotntcart.network.AutoTntCartReceiver;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoTntCartMod implements ModInitializer {
    public static final String MOD_ID = "autotntcart";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Map<UUID, Long> COOLDOWN = new HashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(), "autotntcart.json");
    private static AutoTntCartConfig config;

    @Override
    public void onInitialize() {
        // 加载配置
        loadConfig();

        PayloadTypeRegistry.playC2S().register(AutoTntCartPayload.ID, AutoTntCartPayload.CODEC);
        AutoTntCartReceiver.register();
        LOGGER.info("AutoTntCart initialized");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    // ---- 配置管理 ----
    public static AutoTntCartConfig getConfig() {
        if (config == null) loadConfig();
        return config;
    }

    public static void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, AutoTntCartConfig.class);
            } catch (IOException e) {
                LOGGER.error("Failed to load config", e);
                config = new AutoTntCartConfig();
            }
        } else {
            config = new AutoTntCartConfig();
            saveConfig();
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    // ---- 冷却 ----
    public static boolean isOnCooldown(UUID uuid) {
        Long last = COOLDOWN.get(uuid);
        if (last == null) return false;
        return System.currentTimeMillis() - last < getConfig().cooldownMs;
    }

    public static void markCooldown(UUID uuid) {
        COOLDOWN.put(uuid, System.currentTimeMillis());
    }
}