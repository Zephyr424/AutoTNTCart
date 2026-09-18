package com.shadowslice.autotntcart.network;

import com.shadowslice.autotntcart.AutoTntCartMod;
import com.shadowslice.autotntcart.CartMode;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

public class AutoTntCartReceiver {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(AutoTntCartPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            ServerLevel world = (ServerLevel) player.level();

            context.server().execute(() -> {
                // 服务端冷却检查
                if (AutoTntCartMod.isOnCooldown(player.getUUID())) return;
                AutoTntCartMod.markCooldown(player.getUUID());

                CartMode mode = CartMode.fromId(payload.mode());
                BlockPos targetPos = payload.pos();
                Direction face = payload.direction();

                switch (mode) {
                    case BURST -> handleBurst(player, world, targetPos, face);
                    case PLACE -> handlePlace(player, world, targetPos, face);
                    case CHAIN -> handleChain(player, world, targetPos, face);
                    case DETONATE -> handleDetonate(player, world, targetPos, face);
                }
            });
        });
    }

    // ============================================================
    // 模式 1：BURST —— 瞬爆矿车
    // ============================================================
    private static void handleBurst(ServerPlayer player, ServerLevel world, BlockPos targetPos, Direction face) {
        BlockPos placePos = findPlacementPos(world, targetPos, face);
        if (placePos == null) {
            sendKey(player, "autotntcart.message.no_place");
            return;
        }

        if (!ensureItem(player, s -> s.is(Items.RAIL), 1)) {
            sendKey(player, "autotntcart.message.missing_rail");
            return;
        }
        if (!ensureItem(player, s -> s.is(Items.TNT_MINECART), 1)) {
            sendKey(player, "autotntcart.message.missing_cart");
            return;
        }

        ItemStack flameBow = findFlameBow(player, world);
        if (flameBow == null) {
            sendKey(player, "autotntcart.message.missing_bow");
            return;
        }

        boolean hasInfinite = hasInfinity(world, flameBow);
        if (!hasInfinite && !ensureItem(player,
                s -> s.is(Items.ARROW) || s.is(Items.TIPPED_ARROW) || s.is(Items.SPECTRAL_ARROW), 1)) {
            sendKey(player, "autotntcart.message.missing_arrow");
            return;
        }

        if (!consumeItems(player, hasInfinite)) return;

        shootFlameArrow(player, new Vec3(placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5));
        world.setBlockAndUpdate(placePos, Blocks.RAIL.defaultBlockState());
        spawnTntCart(world, placePos);
    }

    // ============================================================
    // 模式 2：PLACE —— 普通放置
    // ============================================================
    private static void handlePlace(ServerPlayer player, ServerLevel world, BlockPos targetPos, Direction face) {
        BlockPos placePos = findPlacementPos(world, targetPos, face);
        if (placePos == null) {
            sendKey(player, "autotntcart.message.no_place");
            return;
        }

        if (!ensureItem(player, s -> s.is(Items.RAIL), 1)) {
            sendKey(player, "autotntcart.message.missing_rail");
            return;
        }
        if (!ensureItem(player, s -> s.is(Items.TNT_MINECART), 1)) {
            sendKey(player, "autotntcart.message.missing_cart");
            return;
        }
        if (!consumeItems(player, true)) return;

        world.setBlockAndUpdate(placePos, Blocks.RAIL.defaultBlockState());
        spawnTntCart(world, placePos);
        sendKey(player, "autotntcart.message.placed");
    }

    // ============================================================
    // 模式 3：CHAIN —— 铁轨链
    // ============================================================
    private static void handleChain(ServerPlayer player, ServerLevel world, BlockPos targetPos, Direction face) {
        Direction facing = player.getDirection();
        BlockPos start = findPlacementPos(world, targetPos, face);
        if (start == null) {
            sendKey(player, "autotntcart.message.no_place");
            return;
        }

        int placed = 0;
        for (int i = 0; i < 5; i++) {
            BlockPos pos = start.relative(facing, i);
            if (!world.getBlockState(pos).isAir() && !world.getBlockState(pos).canBeReplaced()) break;
            if (!ensureItem(player, s -> s.is(Items.RAIL), 1)) break;
            if (!consumeItems(player, true, true)) break;
            world.setBlockAndUpdate(pos, Blocks.RAIL.defaultBlockState());
            placed++;
        }

        if (placed == 0) {
            sendKey(player, "autotntcart.message.chain_failed");
        } else {
            player.sendSystemMessage(Component.translatable("autotntcart.message.chain_placed", placed));
        }
    }

    // ============================================================
    // 模式 4：DETONATE —— 远程引爆
    // ============================================================
    private static void handleDetonate(ServerPlayer player, ServerLevel world, BlockPos targetPos, Direction face) {
        Vec3 center = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        var carts = world.getEntitiesOfClass(MinecartTNT.class,
                new AABB(center, center).inflate(1.5));
        if (carts.isEmpty()) {
            sendKey(player, "autotntcart.message.no_cart_nearby");
            return;
        }

        ItemStack flameBow = findFlameBow(player, world);
        if (flameBow == null) {
            sendKey(player, "autotntcart.message.missing_bow");
            return;
        }

        boolean hasInfinite = hasInfinity(world, flameBow);
        if (!hasInfinite && !ensureItem(player,
                s -> s.is(Items.ARROW) || s.is(Items.TIPPED_ARROW) || s.is(Items.SPECTRAL_ARROW), 1)) {
            sendKey(player, "autotntcart.message.missing_arrow");
            return;
        }
        if (!hasInfinite) consumeItems(player, false, false);

        MinecartTNT target = carts.get(0);
        shootFlameArrow(player, target.position().add(0, 0.5, 0));
    }

    // ============================================================
    // 智能瞄准辅助（受配置开关控制）
    // ============================================================
    private static BlockPos findPlacementPos(Level world, BlockPos targetPos, Direction face) {
        BlockPos primary = targetPos.relative(face);
        if (isPlaceable(world, primary)) return primary;

        // 如果配置中关闭了智能瞄准，直接返回 null
        if (!AutoTntCartMod.getConfig().smartAim) return null;

        // 尝试相邻方块顶面
        Direction[] horizontals = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction d : horizontals) {
            BlockPos candidate = targetPos.relative(d).above();
            if (isPlaceable(world, candidate)) return candidate;
        }

        // 尝试上方再上一格
        BlockPos above = primary.above();
        if (isPlaceable(world, above)) return above;

        return null;
    }

    private static boolean isPlaceable(Level world, BlockPos pos) {
        return world.getBlockState(pos).isAir() || world.getBlockState(pos).canBeReplaced();
    }

    // ============================================================
    // 自动从背包补充（受配置开关控制）
    // ============================================================
    private static boolean ensureItem(ServerPlayer player, Predicate<ItemStack> matcher, int required) {
        Inventory inv = player.getInventory();

        // 先统计快捷栏数量
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (matcher.test(s)) count += s.getCount();
        }
        if (count >= required) return true;

        // 如果配置关闭了自动补充，直接返回 false
        if (!AutoTntCartMod.getConfig().autoRestock) return false;

        // 从背包主槽位（9-35）补充
        for (int i = 9; i < 36; i++) {
            ItemStack source = inv.getItem(i);
            if (!matcher.test(source)) continue;

            // 优先合并到已有的快捷栏堆叠
            for (int j = 0; j < 9; j++) {
                ItemStack dest = inv.getItem(j);
                if (!dest.isEmpty() && ItemStack.isSameItemSameComponents(dest, source)
                        && dest.getCount() < dest.getMaxStackSize()) {
                    int move = Math.min(source.getCount(), dest.getMaxStackSize() - dest.getCount());
                    dest.grow(move);
                    source.shrink(move);
                    if (source.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
                    return true;
                }
            }

            // 再放入空的快捷栏槽位
            for (int j = 0; j < 9; j++) {
                if (inv.getItem(j).isEmpty()) {
                    inv.setItem(j, source.copy());
                    inv.setItem(i, ItemStack.EMPTY);
                    return true;
                }
            }
        }

        return false;
    }

    // ============================================================
    // 消耗物品
    // ============================================================
    private static boolean consumeItems(ServerPlayer player, boolean hasInfinite) {
        return consumeItems(player, hasInfinite, true);
    }

    private static boolean consumeItems(ServerPlayer player, boolean hasInfinite, boolean consumeCart) {
        Inventory inv = player.getInventory();
        boolean railConsumed = false, cartConsumed = !consumeCart;
        boolean arrowConsumed = hasInfinite;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (!railConsumed && stack.is(Items.RAIL)) {
                stack.shrink(1);
                railConsumed = true;
            } else if (!cartConsumed && stack.is(Items.TNT_MINECART)) {
                stack.shrink(1);
                cartConsumed = true;
            } else if (!hasInfinite && !arrowConsumed
                    && (stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW)
                    || stack.is(Items.SPECTRAL_ARROW))) {
                stack.shrink(1);
                arrowConsumed = true;
            }
            if (railConsumed && cartConsumed && arrowConsumed) break;
        }
        return railConsumed && cartConsumed && arrowConsumed;
    }

    // ============================================================
    // 生成 TNT 矿车
    // ============================================================
    private static void spawnTntCart(ServerLevel world, BlockPos placePos) {
        MinecartTNT cart = new MinecartTNT(EntityType.TNT_MINECART, world);
        cart.setPos(placePos.getX() + 0.5, placePos.getY() + 0.25, placePos.getZ() + 0.5);
        world.addFreshEntity(cart);
    }

    // ============================================================
    // 射火矢箭
    // ============================================================
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

    // ============================================================
    // 附魔检测
    // ============================================================
    private static ItemStack findFlameBow(ServerPlayer player, Level world) {
        var lookup = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var flameEntry = lookup.getOrThrow(Enchantments.FLAME);

        Inventory inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.BOW) && EnchantmentHelper.getItemEnchantmentLevel(flameEntry, stack) > 0) {
                return stack;
            }
        }
        return null;
    }

    private static boolean hasInfinity(Level world, ItemStack bow) {
        var lookup = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var infEntry = lookup.getOrThrow(Enchantments.INFINITY);
        return EnchantmentHelper.getItemEnchantmentLevel(infEntry, bow) > 0;
    }

    // ============================================================
    // 工具方法
    // ============================================================
    private static void sendKey(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key));
    }
}