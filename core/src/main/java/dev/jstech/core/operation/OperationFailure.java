/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

import dev.jstech.core.util.Utf8Text;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Why an Operation failed, in a form that can be shown to whoever asked for it.
 *
 * <p>A reason used to be a sentence in English written where the failure happened, which meant it could not be
 * translated and, in practice, was never shown at all: it was built, handed to the dispatcher and dropped there.
 * A player saw the word {@code failed} and nothing else, whether the network had no room, the pattern had gone
 * missing or the machine had been broken while the craft ran.
 *
 * <p>So a reason is a key and the things that fill its holes. The key names a line in the language files, which
 * is what lets it be read in any language; the arguments are the parts that are not words, such as the name of
 * the thing that ran out or the machine that stopped answering.
 *
 * @param key       the translation key naming the reason, empty when there is no reason to give
 * @param arguments what fills the holes in that line, in the order they appear
 */
public record OperationFailure(String key, List<String> arguments) {

    /** No reason: what an Operation that did not fail carries. */
    public static final OperationFailure NONE = new OperationFailure("", List.of());

    /** As much of a key as travels; a longer one is a mistake in the code, not something a player did. */
    public static final int MAX_KEY_BYTES = 128;

    /** As much of one argument as travels, which is long enough for any name a player can give a thing. */
    public static final int MAX_ARGUMENT_BYTES = 256;

    /** How many arguments a reason may carry, which is more than any line of text has holes in it. */
    public static final int MAX_ARGUMENTS = 8;

    public OperationFailure {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");
        /*
         * Clamped rather than refused, because a reason is built at the moment something has already gone
         * wrong. Throwing here would lose the failure itself and replace it with one about its own message.
         */
        key = Utf8Text.clamp(key, MAX_KEY_BYTES);
        arguments = arguments.stream()
                .limit(MAX_ARGUMENTS)
                .map(argument -> Utf8Text.clamp(argument == null ? "" : argument, MAX_ARGUMENT_BYTES))
                .toList();
    }

    /** A reason under that key, with the things that fill its holes. */
    public static OperationFailure of(final String key, final String... arguments) {
        return new OperationFailure(key, Arrays.asList(arguments));
    }

    /** Whether there is a reason to show at all. */
    public boolean isPresent() {
        return !this.key.isEmpty();
    }
}
