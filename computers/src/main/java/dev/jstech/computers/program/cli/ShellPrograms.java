/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.SourceFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The little programs a line at the prompt runs on the machine's behalf.
 *
 * <p>Some of what a prompt offers cannot be answered while the line is being typed: carrying a file
 * across a Gateway waits on a computer belonging to another mod, which answers when it gets to it. A
 * prompt has no way to say anything once its line is done, but a program does, so the line starts one.
 *
 * <p>They are written in our own language and live in this jar as source, compiled once on first use.
 * That way they are exactly the programs a player could have written, held to the same budget and the
 * same rules, with nothing hidden behind the prompt that a program could not do itself.
 */
public final class ShellPrograms {

    private static final String TRANSFER_SOURCE = "/jsc/shell/Transfer.can";

    private static String transfer;
    private static String failure = "";

    private ShellPrograms() {
    }

    /** The program that carries one file across a Gateway, as the assembly it runs from. */
    public static synchronized String transfer() {
        if (transfer == null) {
            transfer = compile(TRANSFER_SOURCE);
        }
        return transfer;
    }

    /** Why there is no program, or an empty text when there is one. */
    public static synchronized String failure() {
        return failure;
    }

    private static String compile(final String resource) {
        final String source = read(resource);
        if (source.isEmpty()) {
            failure = "the source is missing from the jar: " + resource;
            return "";
        }
        final String named = resource.substring(resource.lastIndexOf('/') + 1);
        final CannonCompiler.Result built = CannonCompiler.compile(List.of(new SourceFile(named, source)));
        if (!built.ok()) {
            failure = named + " does not compile: " + String.join("; ", built.lines());
            return "";
        }
        return built.assembly();
    }

    private static String read(final String resource) {
        try (InputStream held = ShellPrograms.class.getResourceAsStream(resource)) {
            return held == null ? "" : new String(held.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException unreadable) {
            return "";
        }
    }
}
