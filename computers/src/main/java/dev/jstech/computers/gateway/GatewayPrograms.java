/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.asm.AsmProgram;
import dev.jstech.computers.cannon.asm.AsmReader;
import dev.jstech.computers.cannon.asm.lua.AsmToLua;
import dev.jstech.computers.cannon.run.Loaded;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One of our programs, turned into what a computer on the other side can run.
 *
 * <p>A program crosses already translated: there is no switch to throw and no second form of the file
 * kept anywhere, because translating is simply what going to one of their computers means. A program
 * written in their own language crosses unchanged; one of ours is compiled and translated on the way.
 *
 * <p>The work is remembered by what the file SAYS rather than by what it is called, so a program asked
 * for a hundred times is compiled once, a program edited is compiled again, and two machines asking for
 * the same program share the answer. That matters: compiling happens on the thread the world runs on,
 * and a program asked for in a loop would otherwise be compiled in a loop.
 */
public final class GatewayPrograms {

    /** How many translated programs are remembered at once. */
    private static final int KEPT = 32;

    /** By what the source says, what it becomes. */
    private static final Map<String, String> MADE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<String, String> eldest) {
            return this.size() > KEPT;
        }
    };

    private GatewayPrograms() {
    }

    /** Whether a file of that name is a program that can cross at all. */
    public static boolean carries(final String name) {
        final String lower = name.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".lua") || lower.endsWith(".can") || lower.endsWith(".asm");
    }

    /**
     * That source, as the other side runs it.
     *
     * @param name   what the file is called, which says what language it is in
     * @param source what it says
     * @throws GatewayRefusedException when it is not a program, or does not compile
     */
    public static synchronized String translated(final String name, final String source)
            throws GatewayRefusedException {
        if (!carries(name)) {
            throw new GatewayRefusedException(name + " is not a program");
        }
        final String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".lua")) {
            // Already in their language: it crosses as it is, and nothing here has to understand it.
            return source;
        }
        final String fingerprint = lower.endsWith(".can") ? "can:" : "asm:";
        final String key = fingerprint + source.hashCode() + ":" + source.length();
        final String known = MADE.get(key);
        if (known != null) {
            return known;
        }
        final String made = translate(name, source, lower.endsWith(".can"));
        MADE.put(key, made);
        return made;
    }

    private static String translate(final String name, final String source, final boolean ours)
            throws GatewayRefusedException {
        String assembly = source;
        if (ours) {
            final CannonCompiler.Result built =
                    CannonCompiler.compile(java.util.List.of(new SourceFile(name, source)));
            if (!built.ok()) {
                throw new GatewayRefusedException(name + " does not compile: "
                        + String.join("; ", built.lines()));
            }
            assembly = built.assembly();
        }
        final DiagnosticBag bag = new DiagnosticBag(name);
        final AsmProgram program = new AsmReader(assembly, bag).read();
        if (bag.hasErrors()) {
            throw new GatewayRefusedException(name + " cannot be read as a program");
        }
        return AsmToLua.of(Loaded.of(program));
    }

    /** Forgets what has been translated, for a world being left. */
    public static synchronized void forget() {
        MADE.clear();
    }
}
