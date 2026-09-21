/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.boot.BootIdentity;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.computers.os.boot.BootSplash;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the system is coming up on this machine, what it has to say while it does, and how much of it
 * is left.
 *
 * <p>Sent when the self-test hands over and again whenever a monitor is opened on a machine still coming up, so a
 * player who walked away and came back joins it where it has got to. Every line was worked out on the server from
 * the machine itself; the screen only decides when each one has been reached.
 *
 * <p>The same sequence carries a machine on its way down, where nothing follows it: {@code endsDark} says that
 * what comes after this is a dark monitor rather than a system, so the screen sees itself out.
 */
public record OpenSystemBootPayload(BlockPos hostPos, BlockPos monitorPos, int remainingTicks, int totalTicks,
                                    BootSequence sequence, boolean endsDark, BootSplash splash,
                                    BootIdentity who) implements CustomPacketPayload {

    /** The longest a step's words may be; anything past it is a sentence, not a step. */
    public static final int MAX_TEXT = 64;

    public static final CustomPacketPayload.Type<OpenSystemBootPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "open_system_boot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSystemBootPayload> STREAM_CODEC =
            StreamCodec.of(OpenSystemBootPayload::encode, OpenSystemBootPayload::decode);

    @Override
    public CustomPacketPayload.Type<OpenSystemBootPayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final OpenSystemBootPayload p) {
        buf.writeBlockPos(p.hostPos());
        buf.writeBlockPos(p.monitorPos());
        buf.writeVarInt(p.remainingTicks());
        buf.writeVarInt(p.totalTicks());
        buf.writeUtf(p.splash().serializedName(), MAX_TEXT);
        buf.writeUtf(clip(p.sequence().title()), MAX_TEXT);
        buf.writeUtf(clip(p.sequence().subtitle()), MAX_TEXT);
        final List<BootSequence.Line> lines = p.sequence().lines();
        buf.writeVarInt(lines.size());
        for (final BootSequence.Line line : lines) {
            buf.writeUtf(clip(line.label()), MAX_TEXT);
            buf.writeUtf(clip(line.value()), MAX_TEXT);
            buf.writeUtf(clip(line.mark()), MAX_TEXT);
            buf.writeBoolean(line.good());
        }
        buf.writeBoolean(p.endsDark());
        buf.writeUtf(clip(p.who().desktopId()), MAX_TEXT);
        buf.writeUtf(clip(p.who().systemName()), MAX_TEXT);
        buf.writeUtf(clip(p.who().hostName()), MAX_TEXT);
        buf.writeUtf(clip(p.who().look()), MAX_TEXT);
    }

    private static OpenSystemBootPayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = buf.readBlockPos();
        final BlockPos monitor = buf.readBlockPos();
        final int remaining = buf.readVarInt();
        final int total = buf.readVarInt();
        final BootSplash splash = BootSplash.byName(buf.readUtf(MAX_TEXT));
        final String title = buf.readUtf(MAX_TEXT);
        final String subtitle = buf.readUtf(MAX_TEXT);
        final int count = Math.min(buf.readVarInt(), BootSequence.MOST_LINES);
        final List<BootSequence.Line> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(new BootSequence.Line(buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT),
                    buf.readUtf(MAX_TEXT), buf.readBoolean()));
        }
        final boolean endsDark = buf.readBoolean();
        return new OpenSystemBootPayload(host, monitor, remaining, total,
                new BootSequence(title, subtitle, lines), endsDark, splash,
                new BootIdentity(buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT),
                        buf.readUtf(MAX_TEXT)));
    }

    /*
     * Cut rather than refused: a machine that names a drive something enormous should still come up, and the cap
     * is wider than anything a step of a starting system has to say.
     */
    private static String clip(final String text) {
        return text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT);
    }
}
