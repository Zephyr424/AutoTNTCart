package com.shadowslice.autotntcart;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AutoTntCartConfigScreen {
    public static Screen create(Screen parent) {
        AutoTntCartConfig config = AutoTntCartConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("autotntcart.config.title"));

        ConfigEntryBuilder entry = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("autotntcart.config.category.general"));

        // 触发模式
        general.addEntry(entry
                .startEnumSelector(Component.translatable("autotntcart.config.mode"),
                        AutoTntCartConfig.TriggerMode.class, config.mode)
                .setDefaultValue(AutoTntCartConfig.TriggerMode.AUTO)
                .setTooltip(Component.translatable("autotntcart.config.mode.tooltip"))
                .setSaveConsumer(v -> config.mode = v)
                .build());

        // 冷却
        general.addEntry(entry
                .startIntField(Component.translatable("autotntcart.config.cooldown"), config.cooldownMs)
                .setDefaultValue(1000)
                .setMin(0).setMax(10000)
                .setTooltip(Component.translatable("autotntcart.config.cooldown.tooltip"))
                .setSaveConsumer(v -> config.cooldownMs = v)
                .build());

        // 显示消息
        general.addEntry(entry
                .startBooleanToggle(Component.translatable("autotntcart.config.show_messages"), config.showMessages)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("autotntcart.config.show_messages.tooltip"))
                .setSaveConsumer(v -> config.showMessages = v)
                .build());

        builder.setSavingRunnable(AutoTntCartConfig::save);
        return builder.build();
    }
}