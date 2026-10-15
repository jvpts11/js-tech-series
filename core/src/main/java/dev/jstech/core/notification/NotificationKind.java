/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.notification;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A kind of notification a mod sends, declared once: its id, how much it matters, what the player calls it when they
 * silence it, and how long it stays on the screen.
 *
 * <pre>{@code
 * public static final NotificationKind OVERVOLTAGE = Notifications.kind(rl("overvoltage"),
 *         NotificationLevel.ALERT, TEXTS.OVERVOLTAGE).declare();
 * }</pre>
 *
 * @param id     the kind's id
 * @param level  how much its notifications matter
 * @param name   what the player calls it, in the history and when silencing it
 * @param lasts  how long its notifications stay on the screen, in milliseconds
 */
public record NotificationKind(ResourceLocation id, NotificationLevel level, TextKey name, long lasts) {

    public NotificationKind {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(name, "name");
    }

    /** A notification of this kind, with that title and what it says. */
    public Notification of(final Text title, final Text body) {
        return new Notification(this.id, this.level, title, body, Optional.empty());
    }

    /** How a kind is declared: how long it stays on the screen, its level's time unless it says. */
    public static final class Builder {

        private final ResourceLocation id;
        private final NotificationLevel level;
        private final TextKey name;
        private long lasts;

        Builder(final ResourceLocation id, final NotificationLevel level, final TextKey name) {
            this.id = Objects.requireNonNull(id, "id");
            this.level = Objects.requireNonNull(level, "level");
            this.name = Objects.requireNonNull(name, "name");
            this.lasts = level.defaultMillis();
        }

        /** How long its notifications stay on the screen. */
        public Builder lasts(final Duration time) {
            this.lasts = Math.max(1L, time.toMillis());
            return this;
        }

        /**
         * Declares the kind.
         *
         * @throws DeclarationError when a kind of that id is declared already
         */
        public NotificationKind declare() {
            return Notifications.declare(new NotificationKind(this.id, this.level, this.name, this.lasts));
        }
    }
}
