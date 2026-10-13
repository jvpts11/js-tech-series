/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliContext;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CommandScope;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.tty.TtyScript;
import dev.jstech.computers.program.tty.TtyScriptProcess;
import dev.jstech.core.text.Text;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A tool that fetches nothing, for a while, and says it has: enough of one to hold a terminal. It is registered once
 * at start-up, the way another mod adds a command, so it is in every shell's command list from the first tick and
 * not from whenever a test class happens to load.
 */
public final class TestFetchCommand implements ICliCommand {

    public static final String NAME = "jstests-fetch";

    /** How long the tool works for, in ticks. */
    public static final int WORK = 20;

    /** The machines whose tool ran to its end, by the name each answers to, since a tool has nowhere to say. */
    private static final Set<String> FETCHED = ConcurrentHashMap.newKeySet();

    private TestFetchCommand() {
    }

    public static void register() {
        CliCommands.register(new TestFetchCommand());
    }

    public static boolean fetched(final String nodeId) {
        return FETCHED.contains(nodeId);
    }

    public static void forget(final String nodeId) {
        FETCHED.remove(nodeId);
    }

    @Override
    public CommandScope scope() {
        return CommandScope.everywhere();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Text summary() {
        return Text.literal("A tool that holds the terminal for a while, for the tests.");
    }

    @Override
    public void run(final CliContext context) {
        final String at = context.computer().nodeId();
        context.out().start(new TtyScriptProcess(TtyScript.script()
                .say("fetching")
                .flood(WORK, WORK * 2, index -> CliLine.plain("part " + index))
                .effect(() -> FETCHED.add(at))
                .say("fetched")
                .done()));
    }
}
