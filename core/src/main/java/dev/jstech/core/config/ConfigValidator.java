/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import com.mojang.serialization.DataResult;
import java.util.Objects;
import java.util.Optional;

/**
 * Takes what a file holds for a setting and gives the value the setting will have: what was written, when it reads as
 * the setting's kind of value and keeps to what the setting is held to; the nearer end of its range, for a number
 * outside it; the default, for anything else. Each value it does not take as written is said in the log.
 */
public final class ConfigValidator {

    private final IConfigLogger logger;

    public ConfigValidator(final IConfigLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** What {@code raw}, a plain value read from a file, gives {@code key}; null is a value the file does not have. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> IConfigValidationResult<T> validate(final ConfigKey<T> key, final Object raw) {
        Objects.requireNonNull(key, "key");
        if (raw == null) {
            return rejected(key, "'" + key.dottedPath() + "' has no value; the default " + key.plain(key.defaultValue())
                    + " is used");
        }
        final DataResult<T> read = key.read(raw);
        final Optional<T> value = read.resultOrPartial();
        final String why = read.error().map(DataResult.Error::message).orElse("no value");
        if (value.isEmpty()) {
            return rejected(key, "'" + key.dottedPath() + "' cannot be read from " + raw + " (" + why
                    + "); the default " + key.plain(key.defaultValue()) + " is used");
        }
        final T typed = value.get();
        // Part of a map or a list could not be read: the rest is kept, as one who wrote one entry wrong meant.
        final boolean partial = read.error().isPresent();
        if (key.allowed().isPresent() && !key.allowed().get().contains(typed)) {
            return rejected(key, "'" + key.dottedPath() + "' is " + typed + ", which is not one of "
                    + key.allowed().get() + "; the default " + key.defaultValue() + " is used");
        }
        if (key.range().isPresent()) {
            final ConfigKeyRange range = key.range().get();
            final Comparable number = (Comparable) typed;
            if (!range.contains((Number & Comparable) number)) {
                final T clamped = (T) range.clamp((Number & Comparable) number);
                this.logger.warn("'" + key.dottedPath() + "' is " + typed + ", outside its range [" + range.min()
                        + ", " + range.max() + "]; clamped to " + clamped);
                return new IConfigValidationResult.Clamped<>(clamped, typed);
            }
        }
        if (partial) {
            final String reason = "'" + key.dottedPath() + "' has a part that cannot be read (" + why
                    + "); the rest is kept";
            this.logger.warn(reason);
            return new IConfigValidationResult.Repaired<>(typed, reason);
        }
        return new IConfigValidationResult.Valid<>(typed);
    }

    private <T> IConfigValidationResult<T> rejected(final ConfigKey<T> key, final String reason) {
        this.logger.warn(reason);
        return new IConfigValidationResult.Rejected<>(key.defaultValue(), reason);
    }
}
