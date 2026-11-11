/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.teracoin;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.MiningComputerTexts;
import dev.jstech.computers.menu.AsicMenu;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.teracoin.TeracoinAddress;
import dev.jstech.core.text.GameText;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The address an ASIC pays, as its window's SET sends it (computers/docs/design/teracoin.md): taken only from a player
 * with that ASIC's window open, and only a well-formed address.
 *
 * @param pos     where the ASIC stands
 * @param address the address typed
 */
public record AsicAddressPayload(BlockPos pos, String address) implements CustomPacketPayload {

    public static final Type<AsicAddressPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "asic_address"));
    public static final StreamCodec<ByteBuf, AsicAddressPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AsicAddressPayload::pos,
            ByteBufCodecs.stringUtf8(TeracoinAddress.LENGTH + 8), AsicAddressPayload::address,
            AsicAddressPayload::new);

    @Override
    public Type<AsicAddressPayload> type() {
        return TYPE;
    }

    /** Hands the game the payload and what the server does with it, from the mod's payload registration. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.onMenu(registrar, TYPE, STREAM_CODEC, AsicMenu.class, AsicMenu::asicPos, AsicAddressPayload::pos,
                (payload, menu, player, level) -> {
                    if (!menu.asic().setAddress(payload.address())) {
                        player.displayClientMessage(GameText.component(MiningComputerTexts.BAD_ADDRESS), true);
                    }
                });
    }
}
