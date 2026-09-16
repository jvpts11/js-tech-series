/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.boot.BootMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the machine has stopped at its boot manager, with these entries and this much of the wait
 * left.
 *
 * <p>Sent when the self-test hands over to a system that brings a boot manager, and again whenever a monitor is
 * opened on a machine still standing there. The entries were worked out on the server from the disks that really
 * carry a system, so the menu is an account of the machine rather than a list written in advance.
 */
public record OpenBootMenuPayload(BlockPos hostPos, BlockPos monitorPos, BootMenu menu,
                                  int remainingTicks) implements CustomPacketPayload {

    /** The longest an entry's name may be, which is wider than any disk's system and its device together. */
    public static final int MAX_LABEL = 64;

    public static final CustomPacketPayload.Type<OpenBootMenuPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "open_boot_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBootMenuPayload> STREAM_CODEC =
            StreamCodec.of(OpenBootMenuPayload::encode, OpenBootMenuPayload::decode);

    @Override
    public CustomPacketPayload.Type<OpenBootMenuPayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final OpenBootMenuPayload p) {
        buf.writeBlockPos(p.hostPos());
        buf.writeBlockPos(p.monitorPos());
        buf.writeVarInt(p.remainingTicks());
        buf.writeVarInt(p.menu().defaultIndex());
        buf.writeVarInt(p.menu().countdownTicks());
        final List<BootMenu.Entry> entries = p.menu().entries();
        buf.writeVarInt(entries.size());
        for (final BootMenu.Entry entry : entries) {
            buf.writeUtf(entry.label().length() <= MAX_LABEL ? entry.label()
                    : entry.label().substring(0, MAX_LABEL), MAX_LABEL);
            buf.writeVarInt(entry.slot());
        }
    }

    private static OpenBootMenuPayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = buf.readBlockPos();
        final BlockPos monitor = buf.readBlockPos();
        final int remaining = buf.readVarInt();
        final int chosen = buf.readVarInt();
        final int countdown = buf.readVarInt();
        final int count = Math.min(buf.readVarInt(), BootMenu.MOST_ENTRIES);
        final List<BootMenu.Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new BootMenu.Entry(buf.readUtf(MAX_LABEL), buf.readVarInt()));
        }
        return new OpenBootMenuPayload(host, monitor, new BootMenu(entries, chosen, countdown), remaining);
    }
}
