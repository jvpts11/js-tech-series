/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crash;

import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.crash.CrashStyle;
import dev.jstech.core.text.TextBounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the page a machine's system crashed onto, how much of its time is left, whether a key has stopped
 * its reboot, and whether the monitor goes dark after it rather than the machine starting over.
 *
 * <p>Sent as the system goes down, again to a monitor opened while the page stands, and again to whoever is on it when
 * a key stops or lets go of the reboot. Every word was worked out on the server from the machine itself.
 */
public record OpenCrashPayload(BlockPos hostPos, BlockPos monitorPos, CrashPage page, int remainingTicks,
                               boolean held, boolean endsDark) implements CustomPacketPayload {

    /** The longest a stop code, its technical line or a part's name may be. */
    public static final int MAX_FIELD = 96;
    /** The longest one line of a kernel's console may be: wider than any the console shows unwrapped. */
    public static final int MAX_LINE = 256;

    public static final CustomPacketPayload.Type<OpenCrashPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "open_crash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCrashPayload> STREAM_CODEC =
            StreamCodec.of(OpenCrashPayload::encode, OpenCrashPayload::decode);

    @Override
    public CustomPacketPayload.Type<OpenCrashPayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final OpenCrashPayload p) {
        buf.writeBlockPos(p.hostPos());
        buf.writeBlockPos(p.monitorPos());
        final CrashPage page = p.page();
        buf.writeVarInt(page.style().id());
        buf.writeVarInt(page.cause().id());
        buf.writeUtf(TextBounds.clip(page.stopCode(), MAX_FIELD), MAX_FIELD);
        buf.writeUtf(TextBounds.clip(page.stopLine(), MAX_FIELD), MAX_FIELD);
        buf.writeUtf(TextBounds.clip(page.failedPart(), MAX_FIELD), MAX_FIELD);
        buf.writeVarInt(page.lines().size());
        for (final String line : page.lines()) {
            buf.writeUtf(TextBounds.clip(line, MAX_LINE), MAX_LINE);
        }
        buf.writeVarInt(page.totalTicks());
        buf.writeBoolean(page.holdsOnKey());
        buf.writeVarInt(p.remainingTicks());
        buf.writeBoolean(p.held());
        buf.writeBoolean(p.endsDark());
    }

    private static OpenCrashPayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = buf.readBlockPos();
        final BlockPos monitor = buf.readBlockPos();
        final CrashStyle style = CrashStyle.byId(buf.readVarInt());
        final CrashCause cause = CrashCause.byId(buf.readVarInt());
        final String stopCode = buf.readUtf(MAX_FIELD);
        final String stopLine = buf.readUtf(MAX_FIELD);
        final String part = buf.readUtf(MAX_FIELD);
        final int count = Math.min(buf.readVarInt(), CrashPage.MOST_LINES);
        final List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(buf.readUtf(MAX_LINE));
        }
        final int total = Math.max(1, buf.readVarInt());
        final boolean holds = buf.readBoolean();
        final CrashPage page = new CrashPage(style, cause, stopCode, stopLine, part, lines, total, holds);
        return new OpenCrashPayload(host, monitor, page, buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }
}
