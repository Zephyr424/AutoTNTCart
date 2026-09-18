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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class AutoTntCartClient implements ClientModInitializer {

    private static KeyMapping triggerKey;
    private static KeyMapping modeKey;
    public static CartMode currentMode = CartMode.BURST;
    private static long lastUseTime = 0;

    @Override
    public void onInitializeClient() {
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.autotntcart.trigger",
                GLFW.GLFW_KEY_C,
                "category.autotntcart"
        ));
        modeKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.autotntcart.mode",
                GLFW.GLFW_KEY_V,
                "category.autotntcart"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;

            // 模式切换
            while (modeKey.consumeClick()) {
                currentMode = currentMode.next();
                client.player.displayClientMessage(
                        Component.translatable("autotntcart.message.mode_switch",
                                Component.translatable(currentMode.translationKey)),
                        true
                );
            }

            // 触发
            while (triggerKey.consumeClick()) {
                onKeyPress(client);
            }
        });
    }

    private void onKeyPress(Minecraft client) {
        long now = System.currentTimeMillis();
        int cooldown = AutoTntCartMod.getConfig().cooldownMs;
        if (now - lastUseTime < cooldown) {
            long remain = cooldown - (now - lastUseTime);
            sendTranslatable(client, "autotntcart.message.cooldown", remain);
            return;
        }

        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) {
            sendTranslatable(client, "autotntcart.message.aim_block");
            return;
        }
        BlockHitResult hit = (BlockHitResult) client.hitResult;

        if (currentMode == CartMode.BURST || currentMode == CartMode.PLACE) {
            if (hit.getDirection() != net.minecraft.core.Direction.UP) {
                sendTranslatable(client, "autotntcart.message.aim_top");
                return;
            }
            String missingKey = getMissingItemsKey(client);
            if (missingKey != null) {
                sendTranslatable(client, missingKey);
                return;
            }
        } else if (currentMode == CartMode.DETONATE) {
            String missingKey = getDetonateMissingKey(client);
            if (missingKey != null) {
                sendTranslatable(client, missingKey);
                return;
            }
        } else if (currentMode == CartMode.CHAIN) {
            if (!hasRail(client)) {
                sendTranslatable(client, "autotntcart.message.missing_rail");
                return;
            }
        }

        lastUseTime = now;
        ClientPlayNetworking.send(new AutoTntCartPayload(
                hit.getBlockPos(), hit.getDirection(), currentMode.ordinal()));
    }

    /** 返回缺失物品的翻译键，如果没有缺失则返回 null */
    private String getMissingItemsKey(Minecraft client) {
        LocalPlayer player = client.player;
        Inventory inventory = player.getInventory();
        boolean hasRail = false, hasCart = false, hasArrow = false;
        boolean hasFlameBow = false, bowHasInfinity = false;

        var lookup = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var flameEntry = lookup.getOrThrow(Enchantments.FLAME);
        var infEntry = lookup.getOrThrow(Enchantments.INFINITY);

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (!hasRail && stack.is(Items.RAIL)) hasRail = true;
            else if (!hasCart && stack.is(Items.TNT_MINECART)) hasCart = true;
            else if (stack.is(Items.BOW)) {
                if (EnchantmentHelper.getItemEnchantmentLevel(flameEntry, stack) > 0) hasFlameBow = true;
                if (EnchantmentHelper.getItemEnchantmentLevel(infEntry, stack) > 0) bowHasInfinity = true;
            } else if (!hasArrow && (stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW)
                    || stack.is(Items.SPECTRAL_ARROW))) hasArrow = true;
        }

        if (!hasRail) return "autotntcart.message.missing_rail";
        if (!hasCart) return "autotntcart.message.missing_cart";
        if (!hasFlameBow) return "autotntcart.message.missing_bow";
        if (!bowHasInfinity && !hasArrow) return "autotntcart.message.missing_arrow";
        return null;
    }

    /** 远程引爆模式的缺失物品检查 */
    private String getDetonateMissingKey(Minecraft client) {
        LocalPlayer player = client.player;
        Inventory inventory = player.getInventory();
        boolean hasArrow = false, hasFlameBow = false, bowHasInfinity = false;

        var lookup = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var flameEntry = lookup.getOrThrow(Enchantments.FLAME);
        var infEntry = lookup.getOrThrow(Enchantments.INFINITY);

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(Items.BOW)) {
                if (EnchantmentHelper.getItemEnchantmentLevel(flameEntry, stack) > 0) hasFlameBow = true;
                if (EnchantmentHelper.getItemEnchantmentLevel(infEntry, stack) > 0) bowHasInfinity = true;
            } else if (!hasArrow && (stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW)
                    || stack.is(Items.SPECTRAL_ARROW))) hasArrow = true;
        }

        if (!hasFlameBow) return "autotntcart.message.missing_bow";
        if (!bowHasInfinity && !hasArrow) return "autotntcart.message.missing_arrow";
        return null;
    }

    private boolean hasRail(Minecraft client) {
        Inventory inv = client.player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i).is(Items.RAIL)) return true;
        }
        return false;
    }

    private void sendTranslatable(Minecraft client, String key) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.translatable(key), true);
        }
    }

    private void sendTranslatable(Minecraft client, String key, Object... args) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.translatable(key, args), true);
        }
    }
}