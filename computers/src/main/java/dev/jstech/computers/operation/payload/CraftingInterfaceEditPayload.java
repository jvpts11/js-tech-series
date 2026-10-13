/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.TextBounds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: a change made in an open Crafting Interface window, applied to the interface the window is open
 * on.
 *
 * @param action  what changes: {@link #NAME}, {@link #ROUTE}, {@link #MODE}, {@link #STATE} or {@link #JOBS}
 * @param pattern for a route, the pattern's place in the interface
 * @param input   for a route, the input's place in the pattern
 * @param value   the router's place in the routers' list for a route (-1 back to the default), 1 or 0 for the mode
 *                (exclusive) and the state (paused), the most jobs (0 for as many as come)
 * @param text    for the name, the new name
 */
public record CraftingInterfaceEditPayload(int action, int pattern, int input, int value, String text)
        implements CustomPacketPayload {

    public static final int NAME = 0;
    public static final int ROUTE = 1;
    public static final int MODE = 2;
    public static final int STATE = 3;
    public static final int JOBS = 4;
    private static final int MOST_TEXT = 64;

    public static final CustomPacketPayload.Type<CraftingInterfaceEditPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "crafting_interface_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingInterfaceEditPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CraftingInterfaceEditPayload::action,
                    ByteBufCodecs.VAR_INT, CraftingInterfaceEditPayload::pattern,
                    ByteBufCodecs.VAR_INT, CraftingInterfaceEditPayload::input,
                    ByteBufCodecs.VAR_INT, CraftingInterfaceEditPayload::value,
                    ByteBufCodecs.stringUtf8(MOST_TEXT), CraftingInterfaceEditPayload::text,
                    CraftingInterfaceEditPayload::new);

    public CraftingInterfaceEditPayload {
        text = TextBounds.clip(text, MOST_TEXT);
    }

    /** A change that names nothing. */
    public static CraftingInterfaceEditPayload of(final int action, final int value) {
        return new CraftingInterfaceEditPayload(action, 0, 0, value, "");
    }

    @Override
    public CustomPacketPayload.Type<CraftingInterfaceEditPayload> type() {
        return TYPE;
    }
}
