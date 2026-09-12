/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.Diagnostic;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.asm.AsmProgram;
import dev.jstech.computers.cannon.asm.AsmReader;
import dev.jstech.computers.cannon.asm.lua.AsmToLua;
import dev.jstech.computers.cannon.run.Loaded;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * What a ComputerCraft computer is given of ours: the agent, and what the agent is made of.
 *
 * <p>The agent is written in our own language and lives in this jar as its source. It becomes Lua here,
 * the same way any program of ours becomes Lua on its way to one of their computers: compiled, turned
 * into the Assembly, and translated. Nothing is written by hand in their language and nothing is kept in
 * this jar in it either, so the agent can never drift from the translator that everything else goes
 * through: if the translator breaks, the agent breaks with it and says so at once.
 *
 * <p>It is made once and kept, because it is the same text for every computer in the world. Nothing here
 * touches the game, so what the agent becomes is proved without one.
 */
public final class JscRom {

    /** The sources, beside this class in the jar. */
    private static final String AGENT_SOURCE = "/jsc/agent/Agent.can";
    private static final String COMMAND_SOURCE = "/jsc/agent/Jsc.can";

    /** Where a ComputerCraft computer looks for what to run when it starts. */
    public static final String AGENT_PATH = "lua/rom/autorun/jsc.lua";
    /** Where it looks for the commands a player can type. */
    public static final String COMMAND_PATH = "lua/rom/programs/jsc.lua";

    private static String agent;
    private static String command;
    private static String failure = "";

    private JscRom() {
    }

    /** The agent as Lua, or nothing at all when it could not be made, which {@link #failure} then says. */
    public static synchronized String agent() {
        if (agent == null) {
            agent = translate(AGENT_SOURCE);
        }
        return agent;
    }

    /** The {@code jsc} command as Lua, or nothing at all when it could not be made. */
    public static synchronized String command() {
        if (command == null) {
            command = translate(COMMAND_SOURCE);
        }
        return command;
    }

    /** What this pack serves, by the path a computer over there looks for it at. */
    public static synchronized String at(final String path) {
        if (AGENT_PATH.equals(path)) {
            return agent();
        }
        return COMMAND_PATH.equals(path) ? command() : "";
    }

    /** Why there is no agent, or an empty text when there is one. */
    public static synchronized String failure() {
        return failure;
    }

    /** Forgets what was made, so a reload makes it again. */
    public static synchronized void forget() {
        agent = null;
        command = null;
        failure = "";
    }

    private static String translate(final String resource) {
        final String source = read(resource);
        if (source.isEmpty()) {
            return "";
        }
        final String named = resource.substring(resource.lastIndexOf('/') + 1);
        final CannonCompiler.Result built = CannonCompiler.compile(List.of(new SourceFile(named, source)));
        if (!built.ok()) {
            return failed("the agent did not compile: " + String.join("; ", built.lines()));
        }
        final DiagnosticBag bag = new DiagnosticBag(named);
        final AsmProgram program = new AsmReader(built.assembly(), bag).read();
        if (bag.hasErrors()) {
            return failed("the agent's assembly could not be read: "
                    + String.join("; ", bag.sorted().stream().map(Diagnostic::format).toList()));
        }
        return AsmToLua.of(Loaded.of(program));
    }

    private static String read(final String resource) {
        try (InputStream held = JscRom.class.getResourceAsStream(resource)) {
            if (held == null) {
                return failed("the agent's source is missing from the jar: " + resource);
            }
            return new String(held.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException unreadable) {
            return failed("the agent's source could not be read: " + unreadable.getMessage());
        }
    }

    private static String failed(final String why) {
        failure = why;
        return "";
    }
}
