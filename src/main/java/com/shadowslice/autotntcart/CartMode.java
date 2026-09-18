package com.shadowslice.autotntcart;

public enum CartMode {
    BURST("key.autotntcart.mode.burst"),
    PLACE("key.autotntcart.mode.place"),
    CHAIN("key.autotntcart.mode.chain"),
    DETONATE("key.autotntcart.mode.detonate");

    public final String translationKey;

    CartMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public CartMode next() {
        CartMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static CartMode fromId(int id) {
        CartMode[] values = values();
        if (id < 0 || id >= values.length) return BURST;
        return values[id];
    }
}