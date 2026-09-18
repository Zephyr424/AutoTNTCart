package com.shadowslice.autotntcart.network;

import com.shadowslice.autotntcart.AutoTntCartMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AutoTntCartPayload(BlockPos pos, Direction direction, int mode) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AutoTntCartPayload> ID =
            new CustomPacketPayload.Type<>(AutoTntCartMod.id("trigger"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AutoTntCartPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, AutoTntCartPayload::pos,
                    Direction.STREAM_CODEC, AutoTntCartPayload::direction,
                    ByteBufCodecs.VAR_INT, AutoTntCartPayload::mode,
                    AutoTntCartPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}