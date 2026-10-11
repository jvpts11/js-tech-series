/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.text.TextKey;
import java.util.Objects;

/**
 * A cell of a table as a mod declares it, in a row of {@link ModGuide.EntryBuilder#row}: words in English, translated
 * like any sentence; words many entries share; code, the same in every language, such as a command or an error's
 * code; or an amount with its unit, written the reader's way.
 *
 * <pre>{@code
 * .table("Commands for files", "Command", "What it does")
 * .row(GuideCell.code("dir"), GuideCell.words("Lists what is in the folder you are in."))
 * }</pre>
 */
public sealed interface GuideCell {

    /** Words in English, translated like any sentence of the entry. */
    static GuideCell words(final String english) {
        return new Words(english);
    }

    /** Words many entries share, declared once as a {@link TextKey}. */
    static GuideCell words(final TextKey shared) {
        return new Shared(shared);
    }

    /** Code: the same in every language, written in the code font. */
    static GuideCell code(final String text) {
        return new Code(text);
    }

    /** An amount with the symbol of its unit, written the reader's way: "12,000 FE". */
    static GuideCell amount(final long value, final String unit) {
        return new Amount(value, unit);
    }

    /** Words in English. */
    record Words(String english) implements GuideCell {

        public Words {
            Objects.requireNonNull(english, "english");
        }
    }

    /** Words many entries share. */
    record Shared(TextKey key) implements GuideCell {

        public Shared {
            Objects.requireNonNull(key, "key");
        }
    }

    /** Code, the same in every language. */
    record Code(String text) implements GuideCell {

        public Code {
            Objects.requireNonNull(text, "text");
        }
    }

    /** An amount with its unit. */
    record Amount(long value, String unit) implements GuideCell {

        public Amount {
            unit = unit == null ? "" : unit;
        }
    }
}
