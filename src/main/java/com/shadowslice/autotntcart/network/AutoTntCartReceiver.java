package com.shadowslice.autotntcart.network;

import com.shadowslice.autotntcart.EnchantHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class AutoTntCartReceiver {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(AutoTntCartPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            ServerLevel world = (ServerLevel) player.level();

            context.server().execute(() -> {
                BlockPos placePos = payload.pos().relative(payload.direction());
                if (!world.getBlockState(placePos).isAir()
                        && !world.getBlockState(placePos).canBeReplaced()) {
                    placePos = placePos.above();
                }
                if (!world.getBlockState(placePos).isAir()
                        && !world.getBlockState(placePos).canBeReplaced()) {
                    player.sendSystemMessage(Component.translatable("autotntcart.message.no_place"));
                    return;
                }

                // 检查：火矢弓（快捷栏）、铁轨（快捷栏）、TNT矿车（快捷栏）
                ItemStack flameBow = findFlameBow(player, world);
                if (flameBow == null) {
                    player.sendSystemMessage(Component.translatable("autotntcart.message.no_bow"));
                    return;
                }
                if (!hasRail(player)) {
                    player.sendSystemMessage(Component.translatable("autotntcart.message.no_rail"));
                    return;
                }
                if (!hasCart(player)) {
                    player.sendSystemMessage(Component.translatable("autotntcart.message.no_cart"));
                    return;
                }

                boolean creative = player.isCreative();
                boolean infinite = EnchantHelper.hasInfinity(world, flameBow);
                if (!creative && !infinite && !hasArrow(player)) {
                    player.sendSystemMessage(Component.translatable("autotntcart.message.no_arrow"));
                    return;
                }

                // 消耗物品（创造模式全免）
                if (!creative) {
                    consumeRail(player);
                    consumeCart(player);
                    if (!infinite) consumeArrow(player);
                }

                // 执行：先射箭，再放铁轨，再放TNT矿车
                shootFlameArrow(player, new Vec3(
                        placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5));
                world.setBlockAndUpdate(placePos, Blocks.RAIL.defaultBlockState());

                MinecartTNT cart = new MinecartTNT(EntityType.TNT_MINECART, world);
                cart.setPos(placePos.getX() + 0.5, placePos.getY() + 0.25, placePos.getZ() + 0.5);
                world.addFreshEntity(cart);
            });
        });
    }

    // ---- 物品检查 ----

    private static ItemStack findFlameBow(ServerPlayer player, Level world) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.BOW) && EnchantHelper.hasFlame(world, s)) return s;
        }
        return null;
    }

    private static boolean isRail(ItemStack s) {
        return s.is(Items.RAIL) || s.is(Items.POWERED_RAIL)
                || s.is(Items.DETECTOR_RAIL) || s.is(Items.ACTIVATOR_RAIL);
    }

    private static boolean hasRail(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) if (isRail(inv.getItem(i))) return true;
        return false;
    }

    private static boolean hasCart(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) if (inv.getItem(i).is(Items.TNT_MINECART)) return true;
        return false;
    }

    private static boolean hasArrow(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.ARROW) || s.is(Items.TIPPED_ARROW)
                    || s.is(Items.SPECTRAL_ARROW)) return true;
        }
        return false;
    }

    // ---- 消耗物品 ----

    private static void consumeRail(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (isRail(inv.getItem(i))) { inv.getItem(i).shrink(1); return; }
        }
    }

    private static void consumeCart(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(Items.TNT_MINECART)) { inv.getItem(i).shrink(1); return; }
        }
    }

    private static void consumeArrow(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.ARROW) || s.is(Items.TIPPED_ARROW)
                    || s.is(Items.SPECTRAL_ARROW)) { s.shrink(1); return; }
        }
    }

    // ---- 射箭 ----

    private static void shootFlameArrow(ServerPlayer player, Vec3 target) {
        Level world = player.level();
        Vec3 from = player.getEyePosition();
        Vec3 dir = target.subtract(from).normalize();

        Arrow arrow = new Arrow(EntityType.ARROW, world);
        arrow.setPos(from.x, from.y, from.z);
        arrow.shoot(dir.x, dir.y, dir.z, 1.2f, 0.0f);
        arrow.setRemainingFireTicks(100);
        arrow.pickup = Arrow.Pickup.DISALLOWED;
        arrow.setOwner(player);
        world.addFreshEntity(arrow);
    }
}