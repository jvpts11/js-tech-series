/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.notification;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * One notification: of which kind, how much it matters, its title and what it says, and what a click on it does, if
 * anything. Made from its kind, and sent with {@link Notifications}:
 *
 * <pre>{@code
 * Notifications.send(player, OVERVOLTAGE.of(TEXTS.OVERVOLTAGE.text(), TEXTS.BURNED.with(where))
 *         .withAction(new NotificationAction.ShowPlace(level.dimension(), pos)));
 * }</pre>
 *
 * @param kind   the kind's id
 * @param level  how much it matters
 * @param title  its title, a few words
 * @param body   what it says, a line
 * @param action what a click on it does, or nothing
 */
public record Notification(ResourceLocation kind, NotificationLevel level, Text title, Text body,
                           Optional<NotificationAction> action) {

    /** How a notification travels. */
    public static final StreamCodec<ByteBuf, Notification> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Notification::kind,
            ByteBufCodecs.STRING_UTF8.map(name -> NotificationLevel.byName(name).orElse(NotificationLevel.INFO),
                    NotificationLevel::serializedName), Notification::level,
            TextCodecs.STREAM_CODEC, Notification::title,
            TextCodecs.STREAM_CODEC, Notification::body,
            ByteBufCodecs.optional(NotificationAction.STREAM_CODEC), Notification::action,
            Notification::new);

    public Notification {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(action, "action");
    }

    /** The same notification, which {@code clicked} answers when it is clicked. */
    public Notification withAction(final NotificationAction clicked) {
        return new Notification(this.kind, this.level, this.title, this.body, Optional.of(clicked));
    }
}
