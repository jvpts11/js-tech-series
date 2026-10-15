/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.notification;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A notification the server sends a player, shown on their game's HUD.
 *
 * @param notification the notification
 */
public record NotificationPayload(Notification notification) implements CustomPacketPayload {

    public static final Type<NotificationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "notification"));
    public static final StreamCodec<ByteBuf, NotificationPayload> STREAM_CODEC = Notification.STREAM_CODEC.map(
            NotificationPayload::new, NotificationPayload::notification);

    @Override
    public Type<NotificationPayload> type() {
        return TYPE;
    }
}
