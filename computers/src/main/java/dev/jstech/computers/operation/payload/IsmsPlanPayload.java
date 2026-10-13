/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the estimated plan of a CRAFT, for the studio window {@code window}'s Plan pane: a line for
 * each thing the craft needs, with how much the network has and how it would be made, indented under what needs it.
 * Nothing is made; it is what the engine would do now.
 *
 * @param ok    whether the network could plan it at all
 * @param lines the plan, top down, or why there is none
 * @param depth how far in each line sits, in steps
 */
public record IsmsPlanPayload(int window, boolean ok, List<Text> lines, List<Integer> depth)
        implements CustomPacketPayload {

    public static final int MAX_LINES = 64;

    public static final CustomPacketPayload.Type<IsmsPlanPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "isms_plan"));

    public static final StreamCodec<RegistryFriendlyByteBuf, IsmsPlanPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, IsmsPlanPayload::window,
                    ByteBufCodecs.BOOL, IsmsPlanPayload::ok,
                    TextCodecs.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES)), IsmsPlanPayload::lines,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_LINES)), IsmsPlanPayload::depth,
                    IsmsPlanPayload::new);

    public IsmsPlanPayload {
        lines = List.copyOf(lines.subList(0, Math.min(MAX_LINES, lines.size())));
        depth = List.copyOf(depth.subList(0, Math.min(lines.size(), depth.size())));
    }

    @Override
    public CustomPacketPayload.Type<IsmsPlanPayload> type() {
        return TYPE;
    }
}
