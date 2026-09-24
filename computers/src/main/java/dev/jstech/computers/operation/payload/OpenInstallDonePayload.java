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
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: reopen the system installer at its last beat. The system is already on the
 * disk, but the machine has not restarted since, so it is still the installer that is running, and
 * the monitor shows its "reboot" prompt again rather than a system that never booted.
 *
 * @param host         the computer
 * @param monitorPos   the monitor the screen renders on
 * @param firmwareKind the firmware look of the machine's era, as its id
 * @param osName       the system that was installed, for the prompt
 * @param targetLabel  the disk it went onto, for the prompt, read in the player's language
 * @param targetSlot   the disk slot to boot when the player restarts ({@code -1} = the default disk)
 * @param failure      empty when the system is on the disk; otherwise why the write was refused, so the
 *                     installer ends on that instead of a "complete" it never earned
 */
public record OpenInstallDonePayload(BlockPos host, BlockPos monitorPos, int firmwareKind, String osName,
                                     Text targetLabel, int targetSlot, Text failure)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenInstallDonePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "open_install_done"));

    // Seven fields: one past what StreamCodec.composite takes, so the two halves are written by hand.
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenInstallDonePayload> STREAM_CODEC =
            StreamCodec.of(OpenInstallDonePayload::write, OpenInstallDonePayload::read);

    private static void write(final RegistryFriendlyByteBuf buf, final OpenInstallDonePayload payload) {
        buf.writeBlockPos(payload.host);
        buf.writeBlockPos(payload.monitorPos);
        buf.writeVarInt(payload.firmwareKind);
        buf.writeUtf(payload.osName);
        TextCodecs.STREAM_CODEC.encode(buf, payload.targetLabel);
        buf.writeVarInt(payload.targetSlot);
        TextCodecs.STREAM_CODEC.encode(buf, payload.failure);
    }

    private static OpenInstallDonePayload read(final RegistryFriendlyByteBuf buf) {
        return new OpenInstallDonePayload(buf.readBlockPos(), buf.readBlockPos(), buf.readVarInt(), buf.readUtf(),
                TextCodecs.STREAM_CODEC.decode(buf), buf.readVarInt(), TextCodecs.STREAM_CODEC.decode(buf));
    }

    @Override
    public CustomPacketPayload.Type<OpenInstallDonePayload> type() {
        return TYPE;
    }
}
