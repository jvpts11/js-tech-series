/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.listing;

/**
 * Every problem a listing can have when it is read back, with the code a player quotes when asking for help.
 *
 * <p>These belong to the listing format, not to a language: a listing is what the machine runs, whichever compiler
 * wrote it, so reading one reports its own problems.
 */
public enum ListingError {

    MISSING_VERSION_LINE("C4001", "the listing has to begin with a version line"),
    VERSION_TOO_NEW("C4002", "this runtime reads assembly version %s, and this listing is version %s"),
    UNKNOWN_INSTRUCTION("C4003", "'%s' is not an instruction"),
    MISSING_OPERAND("C4004", "'%s' needs something after it"),
    UNEXPECTED_OPERAND("C4005", "'%s' takes nothing after it"),
    MALFORMED_OPERAND("C4006", "'%s' is not what '%s' takes"),
    UNKNOWN_DIRECTIVE("C4007", "'%s' is not a line this format has"),
    INSTRUCTION_OUTSIDE_METHOD("C4008", "an instruction has to be inside a method"),
    DIRECTIVE_OUTSIDE_TYPE("C4009", "'%s' has to be inside a type"),
    UNKNOWN_LABEL("C4010", "nothing in this method is labelled '%s'"),
    VERSION_TOO_OLD("C4012", "this listing is assembly version %s and this runtime reads version %s: compile its "
            + "source again");

    private final String code;
    private final String template;

    ListingError(final String code, final String template) {
        this.code = code;
        this.template = template;
    }

    /** The code as it appears in a message, for example {@code C4003}. */
    public String code() {
        return this.code;
    }

    /** The message with its placeholders filled in. */
    public String message(final Object... arguments) {
        return arguments.length == 0 ? this.template : String.format(this.template, arguments);
    }
}
