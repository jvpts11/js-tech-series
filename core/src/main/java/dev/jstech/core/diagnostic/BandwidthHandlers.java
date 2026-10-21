/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/*
 * The two handlers that count what goes out of one player's connection, either side of the game's encoder: the first,
 * after it in the pipeline and so before it on the way out, sees each packet and notes its kind; the second, before
 * it and so after it on the way out, sees the bytes the packet was written as and counts them under that kind. Both
 * run on the connection's own thread, where a packet is encoded in the same call it is written in, so the bytes the
 * second sees are those of the packet the first saw last.
 */
final class BandwidthHandlers {

    /** The name of the handler before the encoder in the pipeline. */
    static final String BYTES = "jscore_bandwidth_bytes";
    /** The name of the handler after it. */
    static final String PACKETS = "jscore_bandwidth_packets";
    /** The game's encoder, which writes a packet as bytes. */
    static final String ENCODER = "encoder";

    private static final String UNKNOWN = "unknown";

    private BandwidthHandlers() {
    }

    /* Puts the handlers either side of the encoder, counting into those ledgers; false when there is no encoder. */
    static boolean attach(final ChannelPipeline pipeline, final Ledgers ledgers,
                          final Supplier<ResourceKey<Level>> dimension) {
        if (pipeline.get(ENCODER) == null || pipeline.get(PACKETS) != null) {
            return false;
        }
        final Meter meter = new Meter(ledgers, dimension);
        pipeline.addAfter(ENCODER, PACKETS, new Packets(meter));
        pipeline.addBefore(ENCODER, BYTES, new Bytes(meter));
        return true;
    }

    /* What a packet is counted under: a mod's payload by its id, any other packet by the game's id for its kind. */
    static String kindOf(final Packet<?> packet) {
        if (packet instanceof ClientboundCustomPayloadPacket custom) {
            return custom.payload().type().id().toString();
        }
        return packet.type().id().toString();
    }

    /*
     * Where one player's connection is counted: their ledger and everyone's over the minute, and the same tick by
     * tick.
     */
    record Ledgers(BandwidthLedger player, BandwidthLedger everyone, TickLedger playerTicks,
                   TickLedger everyoneTicks) {
    }

    /* The packet the first handler saw last, and where the second counts its bytes. */
    private static final class Meter {

        private final Ledgers ledgers;
        private final Supplier<ResourceKey<Level>> dimension;
        private @Nullable String kind;
        private BandwidthLedger.@Nullable Sender sender;

        private Meter(final Ledgers ledgers, final Supplier<ResourceKey<Level>> dimension) {
            this.ledgers = ledgers;
            this.dimension = dimension;
        }

        private void saw(final Packet<?> packet) {
            this.kind = kindOf(packet);
            this.sender = null;
            if (packet instanceof ClientboundBlockEntityDataPacket update) {
                final ResourceLocation type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(update.getType());
                if (type != null) {
                    this.sender = new BandwidthLedger.Sender(this.dimension.get(), update.getPos(), type);
                }
            }
        }

        private void wrote(final int bytes) {
            final String counted = this.kind == null ? UNKNOWN : this.kind;
            this.ledgers.player().count(counted, this.sender, bytes);
            this.ledgers.everyone().count(counted, this.sender, bytes);
            this.ledgers.playerTicks().count(counted, bytes);
            this.ledgers.everyoneTicks().count(counted, bytes);
            this.kind = null;
            this.sender = null;
        }
    }

    private static final class Packets extends ChannelOutboundHandlerAdapter {

        private final Meter meter;

        private Packets(final Meter meter) {
            this.meter = meter;
        }

        @Override
        public void write(final ChannelHandlerContext context, final Object message, final ChannelPromise promise)
                throws Exception {
            if (message instanceof Packet<?> packet && DiagnosticBalance.countNetwork()) {
                this.meter.saw(packet);
            }
            super.write(context, message, promise);
        }
    }

    private static final class Bytes extends ChannelOutboundHandlerAdapter {

        private final Meter meter;

        private Bytes(final Meter meter) {
            this.meter = meter;
        }

        @Override
        public void write(final ChannelHandlerContext context, final Object message, final ChannelPromise promise)
                throws Exception {
            if (message instanceof ByteBuf buffer && DiagnosticBalance.countNetwork()) {
                this.meter.wrote(buffer.readableBytes());
            }
            super.write(context, message, promise);
        }
    }
}
