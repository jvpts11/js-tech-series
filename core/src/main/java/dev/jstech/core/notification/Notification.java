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
 * One notification: of which kind, how much it matters, its title and what it says, what a click on it does, if
 * anything, and where it came from, when the mod that sent it is not all there is to say about that. Made from its
 * kind, and sent with {@link Notifications}:
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
 * @param origin where it came from, said after the mod's name (a device, a machine); empty for nothing more
 */
public record Notification(ResourceLocation kind, NotificationLevel level, Text title, Text body,
                           Optional<NotificationAction> action, Text origin) {

    /** How a notification travels. */
    public static final StreamCodec<ByteBuf, Notification> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Notification::kind,
            ByteBufCodecs.STRING_UTF8.map(name -> NotificationLevel.byName(name).orElse(NotificationLevel.INFO),
                    NotificationLevel::serializedName), Notification::level,
            TextCodecs.STREAM_CODEC, Notification::title,
            TextCodecs.STREAM_CODEC, Notification::body,
            ByteBufCodecs.optional(NotificationAction.STREAM_CODEC), Notification::action,
            TextCodecs.STREAM_CODEC, Notification::origin,
            Notification::new);

    public Notification {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(origin, "origin");
    }

    /** A notification that says nothing of where it came from beyond its mod. */
    public Notification(final ResourceLocation kind, final NotificationLevel level, final Text title, final Text body,
                        final Optional<NotificationAction> action) {
        this(kind, level, title, body, action, Text.EMPTY);
    }

    /** The same notification, which {@code clicked} answers when it is clicked. */
    public Notification withAction(final NotificationAction clicked) {
        return new Notification(this.kind, this.level, this.title, this.body, Optional.of(clicked), this.origin);
    }

    /** The same notification, saying it came from {@code from}: a device, a machine, after the mod's name. */
    public Notification withOrigin(final Text from) {
        return new Notification(this.kind, this.level, this.title, this.body, this.action, from);
    }
}
