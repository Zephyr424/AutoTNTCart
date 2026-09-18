package com.shadowslice.autotntcart;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AutoTntCartConfigScreen {
    public static Screen create(Screen parent) {
        AutoTntCartConfig config = AutoTntCartMod.getConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("autotntcart.config.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("autotntcart.config.category.general"));

        // 冷却时间（毫秒）
        general.addEntry(entryBuilder
                .startIntField(Component.translatable("autotntcart.config.cooldown"), config.cooldownMs)
                .setDefaultValue(500)
                .setMin(0)
                .setMax(10000)
                .setTooltip(Component.translatable("autotntcart.config.cooldown.tooltip"))
                .setSaveConsumer(v -> config.cooldownMs = v)
                .build());

        // 自动补充
        general.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("autotntcart.config.auto_restock"), config.autoRestock)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("autotntcart.config.auto_restock.tooltip"))
                .setSaveConsumer(v -> config.autoRestock = v)
                .build());

        // 智能瞄准
        general.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("autotntcart.config.smart_aim"), config.smartAim)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("autotntcart.config.smart_aim.tooltip"))
                .setSaveConsumer(v -> config.smartAim = v)
                .build());

        // 默认模式（下拉菜单）
        general.addEntry(entryBuilder
                .startEnumSelector(Component.translatable("autotntcart.config.default_mode"), CartMode.class, config.getDefaultMode())
                .setDefaultValue(CartMode.BURST)
                .setTooltip(Component.translatable("autotntcart.config.default_mode.tooltip"))
                .setSaveConsumer(v -> config.defaultMode = v.ordinal())
                .build());

        // 保存时回调
        builder.setSavingRunnable(AutoTntCartMod::saveConfig);

        return builder.build();
    }
}