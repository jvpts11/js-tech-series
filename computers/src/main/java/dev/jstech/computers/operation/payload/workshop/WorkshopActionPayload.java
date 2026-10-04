/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.workshop;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: one thing done in the Workshop window of the Personal Computer at {@code host}, from the monitor
 * at {@code monitorPos}. The server answers every one, {@link #REFRESH} included, with a fresh
 * {@link WorkshopStatePayload}. What {@code index}, {@code button} and {@code text} mean is listed on each action.
 */
public record WorkshopActionPayload(BlockPos host, BlockPos monitorPos, int action, int index, int button,
                                    String text) implements CustomPacketPayload {

    /** The longest text an action carries: the anvil's name. */
    public static final int MAX_TEXT = 64;

    /** Asks for the state and changes nothing. */
    public static final int REFRESH = 0;
    /** A click on Workshop slot {@code index} with the cursor, mouse button {@code button}. */
    public static final int CLICK = 1;
    /** A shift-click on the player's inventory slot {@code index}: its stack goes to tab {@code button}'s slot. */
    public static final int SHIFT_INSERT = 2;
    /** Crafts once, or as many times as can be made with {@code index} 1. */
    public static final int CRAFT = 3;
    /** Empties the crafting grid into the player's inventory. */
    public static final int CLEAR_GRID = 4;
    /** Enchants with offer {@code index}. */
    public static final int ENCHANT = 5;
    /** Names the item on the anvil card {@code text}. */
    public static final int ANVIL_NAME = 6;
    /** Takes what the anvil card makes. */
    public static final int ANVIL_TAKE = 7;
    /** The window closed: the grid, the enchanting item and the anvil's two go back to the player. */
    public static final int CLOSED = 8;

    public static final CustomPacketPayload.Type<WorkshopActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "workshop_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorkshopActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, WorkshopActionPayload::host,
                    BlockPos.STREAM_CODEC, WorkshopActionPayload::monitorPos,
                    ByteBufCodecs.VAR_INT, WorkshopActionPayload::action,
                    ByteBufCodecs.VAR_INT, WorkshopActionPayload::index,
                    ByteBufCodecs.VAR_INT, WorkshopActionPayload::button,
                    ByteBufCodecs.stringUtf8(MAX_TEXT), WorkshopActionPayload::text,
                    WorkshopActionPayload::new);

    public WorkshopActionPayload {
        text = text == null ? "" : text;
    }

    /** An action that names nothing but itself and perhaps one number. */
    public static WorkshopActionPayload of(final BlockPos host, final BlockPos monitorPos, final int action,
                                           final int index) {
        return new WorkshopActionPayload(host, monitorPos, action, index, 0, "");
    }

    @Override
    public CustomPacketPayload.Type<WorkshopActionPayload> type() {
        return TYPE;
    }
}
