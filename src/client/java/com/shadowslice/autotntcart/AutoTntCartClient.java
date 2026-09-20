package com.shadowslice.autotntcart;

import com.mojang.blaze3d.platform.InputConstants;
import com.shadowslice.autotntcart.network.AutoTntCartPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class AutoTntCartClient implements ClientModInitializer {

    private static KeyMapping triggerKey;
    private static boolean running = false;
    private static long lastUseTime = 0;

    @Override
    public void onInitializeClient() {
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.autotntcart.trigger",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.autotntcart"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientActionScheduler.tick();
            if (client.player == null || client.level == null) return;
            while (triggerKey.consumeClick()) {
                onKeyPress(client);
            }
        });
    }

    private static void onKeyPress(Minecraft client) {
        if (running) return;

        AutoTntCartConfig cfg = AutoTntCartConfig.get();
        long now = System.currentTimeMillis();
        if (now - lastUseTime < cfg.cooldownMs) return;

        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) {
            sendMsg(client, "autotntcart.message.aim_block");
            return;
        }

        // 判断服务端是否支持我们的自定义包
        boolean serverAvailable = ClientPlayNetworking.canSend(AutoTntCartPayload.ID);

        boolean useServer;
        switch (cfg.mode) {
            case SERVER -> {
                if (!serverAvailable) {
                    sendMsg(client, "autotntcart.message.server_unavailable");
                    return;
                }
                useServer = true;
            }
            case CLIENT -> useServer = false;
            case AUTO -> useServer = serverAvailable;
            default -> useServer = false;
        }

        lastUseTime = now;

        if (useServer) {
            BlockHitResult hit = (BlockHitResult) client.hitResult;
            ClientPlayNetworking.send(new AutoTntCartPayload(hit.getBlockPos(), hit.getDirection()));
        } else {
            startClientSimulation(client);
        }
    }

    // ============================================================
    // 客户端模拟：切弓 → 拉弓 → 射箭 → 切铁轨 → 放 → 切矿车 → 放
    // ============================================================
    private static void startClientSimulation(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;

        Inventory inv = player.getInventory();

        int bowSlot = -1, railSlot = -1, cartSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (bowSlot < 0 && s.is(Items.BOW) && EnchantHelper.hasFlame(player.level(), s)) {
                bowSlot = i;
            } else if (railSlot < 0 && isRail(s)) {
                railSlot = i;
            } else if (cartSlot < 0 && s.is(Items.TNT_MINECART)) {
                cartSlot = i;
            }
        }

        if (bowSlot < 0) { sendMsg(client, "autotntcart.message.no_bow"); return; }
        if (railSlot < 0) { sendMsg(client, "autotntcart.message.no_rail"); return; }
        if (cartSlot < 0) { sendMsg(client, "autotntcart.message.no_cart"); return; }

        boolean creative = player.isCreative();
        boolean infinite = EnchantHelper.hasInfinity(player.level(), inv.getItem(bowSlot));
        if (!creative && !infinite && !hasArrowInInventory(inv)) {
            sendMsg(client, "autotntcart.message.no_arrow");
            return;
        }

        int originalSlot = inv.selected;
        running = true;
        ClientActionScheduler.clear();

        // Tick 0：切到弓，按下右键（开始拉弓）
        ClientActionScheduler.schedule(0, () -> {
            inv.selected = bowSlot;
            client.options.keyUse.setDown(true);
        });

        // Tick 4：松开右键（射箭）
        ClientActionScheduler.schedule(4, () -> client.options.keyUse.setDown(false));

        // Tick 5：切到铁轨
        ClientActionScheduler.schedule(5, () -> inv.selected = railSlot);

        // Tick 6：放置铁轨
        ClientActionScheduler.schedule(6, () -> placeBlock(client));

        // Tick 7：切到 TNT 矿车
        ClientActionScheduler.schedule(7, () -> inv.selected = cartSlot);

        // Tick 8：放置 TNT 矿车
        ClientActionScheduler.schedule(8, () -> placeBlock(client));

        // Tick 10：恢复原槽位
        ClientActionScheduler.schedule(10, () -> {
            inv.selected = originalSlot;
            running = false;
        });
    }

    private static void placeBlock(Minecraft client) {
        if (client.gameMode == null || client.player == null) return;
        if (client.hitResult instanceof BlockHitResult hit) {
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        }
    }

    private static boolean isRail(ItemStack s) {
        return s.is(Items.RAIL) || s.is(Items.POWERED_RAIL)
                || s.is(Items.DETECTOR_RAIL) || s.is(Items.ACTIVATOR_RAIL);
    }

    private static boolean hasArrowInInventory(Inventory inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.ARROW) || s.is(Items.TIPPED_ARROW)
                    || s.is(Items.SPECTRAL_ARROW)) return true;
        }
        return false;
    }

    private static void sendMsg(Minecraft client, String key) {
        if (!AutoTntCartConfig.get().showMessages) return;
        if (client.player != null) {
            client.player.displayClientMessage(Component.translatable(key), true);
        }
    }
}