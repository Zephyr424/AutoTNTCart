package com.shadowslice.autotntcart;

public class AutoTntCartConfig {
    // 冷却时间（毫秒），默认 500ms
    public int cooldownMs = 500;
    // 是否自动从背包补充物品
    public boolean autoRestock = true;
    // 是否启用智能瞄准辅助
    public boolean smartAim = true;
    // 默认模式（0=BURST, 1=PLACE, 2=CHAIN, 3=DETONATE）
    public int defaultMode = 0;

    // 获取默认模式枚举
    public CartMode getDefaultMode() {
        return CartMode.fromId(defaultMode);
    }
}