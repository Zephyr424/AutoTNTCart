package com.shadowslice.autotntcart;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

public class EnchantHelper {

    public static boolean hasFlame(Level world, ItemStack stack) {
//? if >=1.21.2 {
        var lookup = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var entry = lookup.getOrThrow(Enchantments.FLAME);
        return EnchantmentHelper.getItemEnchantmentLevel(entry, stack) > 0;
//?} else {
        /*var reg = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var entry = reg.getHolder(Enchantments.FLAME).orElse(null);
        return entry != null && EnchantmentHelper.getItemEnchantmentLevel(entry, stack) > 0;*/
//?}
    }

    public static boolean hasInfinity(Level world, ItemStack stack) {
//? if >=1.21.2 {
        var lookup = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var entry = lookup.getOrThrow(Enchantments.INFINITY);
        return EnchantmentHelper.getItemEnchantmentLevel(entry, stack) > 0;
//?} else {
        /*var reg = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var entry = reg.getHolder(Enchantments.INFINITY).orElse(null);
        return entry != null && EnchantmentHelper.getItemEnchantmentLevel(entry, stack) > 0;*/
//?}
    }
}