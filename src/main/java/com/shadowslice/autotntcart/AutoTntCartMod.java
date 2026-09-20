package com.shadowslice.autotntcart;

import com.shadowslice.autotntcart.network.AutoTntCartPayload;
import com.shadowslice.autotntcart.network.AutoTntCartReceiver;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoTntCartMod implements ModInitializer {
    public static final String MOD_ID = "autotntcart";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(AutoTntCartPayload.ID, AutoTntCartPayload.CODEC);
        AutoTntCartReceiver.register();
        LOGGER.info("AutoTntCart initialized");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}