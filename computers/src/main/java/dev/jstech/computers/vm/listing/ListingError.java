/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.listing;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;

/**
 * Every problem a listing can have when it is read back or made ready to run, with the code a player quotes when asking
 * for help.
 *
 * <p>These belong to the listing format, not to a language: a listing is what the machine runs, whichever compiler
 * wrote it, so reading one reports its own problems, and so does finding that something it names has nothing to
 * answer it.
 *
 * <p>Each message is a sentence read in the player's language; the code is the last word of its key, the same in every
 * language.
 */
@TextHolder
public enum ListingError {

    MISSING_VERSION_LINE(TextKey.of("jsc.vm.listing.a4001", "the listing has to begin with a version line")),
    VERSION_TOO_NEW(TextKey.of("jsc.vm.listing.a4002",
            "this runtime reads assembly version %s, and this listing is version %s")),
    UNKNOWN_INSTRUCTION(TextKey.of("jsc.vm.listing.a4003", "'%s' is not an instruction")),
    MISSING_OPERAND(TextKey.of("jsc.vm.listing.a4004", "'%s' needs something after it")),
    UNEXPECTED_OPERAND(TextKey.of("jsc.vm.listing.a4005", "'%s' takes nothing after it")),
    MALFORMED_OPERAND(TextKey.of("jsc.vm.listing.a4006", "'%s' is not what '%s' takes")),
    UNKNOWN_DIRECTIVE(TextKey.of("jsc.vm.listing.a4007", "'%s' is not a line this format has")),
    INSTRUCTION_OUTSIDE_METHOD(TextKey.of("jsc.vm.listing.a4008", "an instruction has to be inside a method")),
    DIRECTIVE_OUTSIDE_TYPE(TextKey.of("jsc.vm.listing.a4009", "'%s' has to be inside a type")),
    UNKNOWN_LABEL(TextKey.of("jsc.vm.listing.a4010", "nothing in this method is labelled '%s'")),
    VERSION_TOO_OLD(TextKey.of("jsc.vm.listing.a4012",
            "this listing is assembly version %s and this runtime reads version %s and later: compile its source"
                    + " again")),
    UNKNOWN_MEMBER(TextKey.of("jsc.vm.listing.a4013", "nothing answers '%s'")),
    READ_ONLY_VALUE(TextKey.of("jsc.vm.listing.a4014", "'%s' can be read but not written")),
    /* Nothing is wrong with the listing here: it is the machine it was brought to that will not run it. */
    ARCHITECTURE_MISMATCH(TextKey.of("jsc.vm.listing.a4015", "built for %s; this machine is %s"));

    private final TextKey text;

    ListingError(final TextKey text) {
        this.text = text;
    }

    /** The code as it appears in a message, for example {@code A4003}. */
    public String code() {
        final String key = this.text.key();
        return key.substring(key.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
    }

    /** The message with its placeholders filled in. */
    public Text message(final Object... arguments) {
        return this.text.with(arguments);
    }
}
