/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What a settings screen shows of a setting beyond its value: when a change takes hold, the setting it is active
 * under, a picture beside its description, the manual's page that tells more, and the control it is changed with.
 *
 * @param applies   when a change takes hold
 * @param activeIf  the setting it is active under, and the values that one must have; none when always active
 * @param picture   a texture shown beside its description; a tall one is a strip of frames shown in turn
 * @param frames    how many frames a picture's strip holds, one under the other
 * @param manual    the manual's page that tells more of it
 * @param control   the control it is changed with
 */
public record ConfigTraits(ConfigApply applies, Optional<Condition> activeIf, Optional<String> picture, int frames,
                           Optional<ManualPage> manual, ConfigControl control) {

    /** What every setting has unless it says otherwise: taking hold at once, always active, a plain control. */
    public static final ConfigTraits NONE = new ConfigTraits(ConfigApply.NOW, Optional.empty(), Optional.empty(), 1,
            Optional.empty(), ConfigControl.AUTO);

    public ConfigTraits {
        Objects.requireNonNull(applies, "applies");
        Objects.requireNonNull(activeIf, "activeIf");
        Objects.requireNonNull(picture, "picture");
        Objects.requireNonNull(manual, "manual");
        Objects.requireNonNull(control, "control");
        frames = Math.max(1, frames);
    }

    ConfigTraits withApplies(final ConfigApply value) {
        return new ConfigTraits(value, activeIf, picture, frames, manual, control);
    }

    ConfigTraits withActiveIf(final Condition value) {
        return new ConfigTraits(applies, Optional.of(value), picture, frames, manual, control);
    }

    ConfigTraits withPicture(final String texture, final int frameCount) {
        return new ConfigTraits(applies, activeIf, Optional.of(texture), frameCount, manual, control);
    }

    ConfigTraits withManual(final ManualPage page) {
        return new ConfigTraits(applies, activeIf, picture, frames, Optional.of(page), control);
    }

    ConfigTraits withControl(final ConfigControl value) {
        return new ConfigTraits(applies, activeIf, picture, frames, manual, value);
    }

    /**
     * A setting another is active under, and the values it must have for that: {@code "true"} for a switch, the
     * words for a word.
     *
     * @param path   the other setting's dotted path, in the same file
     * @param values what it must hold, written as a file writes them
     * @param hides  whether a settings screen leaves the setting out while it is not active, rather than greying it
     */
    public record Condition(String path, List<String> values, boolean hides) {

        public Condition {
            Objects.requireNonNull(path, "path");
            values = List.copyOf(values);
        }

        /** Whether the setting at {@link #path} of {@code file} holds one of the values, as {@code value} reads it. */
        public boolean holds(final Object value) {
            return values.contains(String.valueOf(value));
        }
    }

    /**
     * A page of a manual.
     *
     * @param manual the manual's id ({@code "jscore:technical_reference"})
     * @param entry  the entry's id inside it
     */
    public record ManualPage(String manual, String entry) {

        public ManualPage {
            Objects.requireNonNull(manual, "manual");
            Objects.requireNonNull(entry, "entry");
        }
    }
}
