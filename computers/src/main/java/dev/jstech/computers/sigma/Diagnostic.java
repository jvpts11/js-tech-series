/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Objects;

/**
 * One message from the compiler about one place in the source.
 *
 * <p>Line and column are both one-based, because they are read by a person counting lines in an
 * editor, not by a machine indexing an array.
 *
 * <p>The message is a sentence, read in the player's language; the file, the place and the code stay as they are,
 * since those are what a person looks up and what a tool reads.
 *
 * @param arguments the names and values the message was written around, in the order it names them, so a tool
 *                  that acts on a diagnostic reads them here instead of taking the sentence apart
 */
public record Diagnostic(String file, int line, int column, Severity severity, String code, Text message,
                         List<String> arguments) {

    /** How much a diagnostic matters: a program with an error does not compile, one with a warning does. */
    @TextHolder
    public enum Severity {
        ERROR(TextKey.of("jsc.sigma.diagnostic.error", "%s(%s,%s): error %s: %s")),
        WARNING(TextKey.of("jsc.sigma.diagnostic.warning", "%s(%s,%s): warning %s: %s"));

        private final TextKey line;

        Severity(final TextKey line) {
            this.line = line;
        }

        /** The line a diagnostic of this severity reads as: the file, the line, the column, the code, the message. */
        TextKey line() {
            return this.line;
        }
    }

    public Diagnostic {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("line and column are one-based: " + line + "," + column);
        }
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
    }

    /** Whether this diagnostic stops the compilation. */
    public boolean isError() {
        return this.severity == Severity.ERROR;
    }

    /**
     * The one-line form the compiler prints, for example
     * {@code Monitor.sgs(12,9): error S2001: expected ';' but found '}'}, in the words of whoever reads it.
     */
    public Text text() {
        return this.severity.line().with(this.file, this.line, this.column, this.code, this.message);
    }

    /** The same line in English, the form it takes as data: in a file, down a pipe, in a log. */
    public String format() {
        return this.text().english();
    }

    @Override
    public String toString() {
        return this.format();
    }
}
