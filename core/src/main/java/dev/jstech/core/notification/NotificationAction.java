/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.notification;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * What a click on a notification does: opens the right place. A notification with no action only tells.
 */
public sealed interface NotificationAction permits NotificationAction.OpenManual, NotificationAction.ShowPlace {

    /** How an action travels. */
    StreamCodec<ByteBuf, NotificationAction> STREAM_CODEC = StreamCodec.of(NotificationAction::write,
            NotificationAction::read);

    /**
     * Opens a manual at an entry, such as the page of what the notification is about.
     *
     * @param manual the manual's id, as {@code jscore:technical_reference}
     * @param entry  the entry's id in it; empty opens the manual at its cover
     */
    record OpenManual(String manual, String entry) implements NotificationAction {
    }

    /**
     * Shows a place on a map: the machine the notification is about.
     *
     * @param dimension the dimension it is in
     * @param pos       where it is
     */
    record ShowPlace(ResourceKey<Level> dimension, BlockPos pos) implements NotificationAction {
    }

    private static void write(final ByteBuf buffer, final NotificationAction action) {
        switch (action) {
            case OpenManual open -> {
                buffer.writeByte(0);
                ByteBufCodecs.STRING_UTF8.encode(buffer, open.manual());
                ByteBufCodecs.STRING_UTF8.encode(buffer, open.entry());
            }
            case ShowPlace place -> {
                buffer.writeByte(1);
                ResourceKey.streamCodec(Registries.DIMENSION).encode(buffer, place.dimension());
                BlockPos.STREAM_CODEC.encode(buffer, place.pos());
            }
        }
    }

    private static NotificationAction read(final ByteBuf buffer) {
        final byte kind = buffer.readByte();
        if (kind == 0) {
            return new OpenManual(ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer));
        }
        return new ShowPlace(ResourceKey.streamCodec(Registries.DIMENSION).decode(buffer),
                BlockPos.STREAM_CODEC.decode(buffer));
    }
}
