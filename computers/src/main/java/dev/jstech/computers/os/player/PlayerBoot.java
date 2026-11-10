/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.player;

import dev.jstech.core.text.TextBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the picture of a system a player wrote coming up (computers/docs/design/player-systems.md),
 * as its calls to {@code Boot} and {@code Panic.Colours} set it: its logo in the middle, the bar of its progress under
 * it, its last line under the bar and its name in a corner, in its own two colours. Sent whenever it changes.
 *
 * @param host       the machine coming up
 * @param system     the system, with its version
 * @param logo       the path of its logo on its disk, or empty
 * @param line       the last line it wrote
 * @param progress   how far it says it is, in hundredths
 * @param background the colour behind the page, or empty for the mod's
 * @param text       the colour of its bar and its words, or empty for the mod's
 */
public record PlayerBoot(BlockPos host, String system, String logo, String line, int progress, String background,
                         String text) implements CustomPacketPayload {

    private static final int MOST_TEXT = 256;

    public static final CustomPacketPayload.Type<PlayerBoot> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "player_boot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerBoot> STREAM_CODEC = StreamCodec.of(
            (buf, boot) -> {
                buf.writeBlockPos(boot.host());
                buf.writeUtf(TextBounds.clip(boot.system(), MOST_TEXT), MOST_TEXT);
                buf.writeUtf(TextBounds.clip(boot.logo(), MOST_TEXT), MOST_TEXT);
                buf.writeUtf(TextBounds.clip(boot.line(), MOST_TEXT), MOST_TEXT);
                buf.writeVarInt(boot.progress());
                buf.writeUtf(TextBounds.clip(boot.background(), MOST_TEXT), MOST_TEXT);
                buf.writeUtf(TextBounds.clip(boot.text(), MOST_TEXT), MOST_TEXT);
            },
            buf -> new PlayerBoot(buf.readBlockPos(), buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT),
                    buf.readUtf(MOST_TEXT), buf.readVarInt(), buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT)));

    /** Word that the machine is coming up with one of the mod's own systems, which forgets any picture it had. */
    public static PlayerBoot none(final BlockPos host) {
        return new PlayerBoot(host, "", "", "", 0, "", "");
    }

    /** Whether this is the word that no player's system is coming up. */
    public boolean nobody() {
        return this.system.isEmpty();
    }

    @Override
    public CustomPacketPayload.Type<PlayerBoot> type() {
        return TYPE;
    }
}
