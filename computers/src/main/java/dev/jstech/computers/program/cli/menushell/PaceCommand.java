/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.menushell;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.CliContext;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.CommandGroup;
import dev.jstech.computers.program.cli.CommandScope;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * {@code pace}: PACE, the Panelled Access Command Environment of Bellwether Labs' UNIX System V, a menu interface that
 * takes the whole terminal and opens one numbered frame over another: the player's office, its filecabinet with each
 * file's type, the programs, the machine's administration, and the UNIX System itself, a shell that gives the
 * terminal back to PACE when it exits.
 */
@TextHolder
public final class PaceCommand implements ICliCommand, CliShell.IHandOver {

    private static final TextKey SUMMARY = TextKey.of("jsc.cli.pace.summary",
            "PACE: the system's menus in frames over one another");
    private static final TextKey ABOUT = TextKey.of("jsc.cli.pace.about", "Takes the whole terminal and opens the"
            + " PACE frame, from which each choice opens a frame of its own over the last: the Office of the player"
            + " with its Filecabinet, the Programs, the System Administration and the UNIX System, a shell of its"
            + " own that comes back to PACE with exit.");
    private static final TextKey ABOUT_KEYS = TextKey.of("jsc.cli.pace.about.keys", "The arrows move to an item and"
            + " Enter opens it. The function keys are the labels on the bottom line: F1 HELP, F2 ENTER, F3 PREV-FRM,"
            + " F4 NEXT-FRM, F5 CANCEL closes the frame on top, F6 CMD-MENU. Exit PACE, or CANCEL on the last frame,"
            + " gives the terminal back.");
    private static final TextKey EXAMPLE = TextKey.of("jsc.cli.pace.example", "PACE on the whole glass");

    @Override
    public CommandScope scope() {
        return CommandScope.on(Platform.UNIX);
    }

    @Override
    public String name() {
        return "pace";
    }

    @Override
    public CommandGroup group() {
        return CommandGroup.FILES;
    }

    @Override
    public Text summary() {
        return SUMMARY.text();
    }

    @Override
    public List<Text> description() {
        return List.of(ABOUT.text(), ABOUT_KEYS.text());
    }

    @Override
    public List<Example> examples() {
        return List.of(new Example("pace", EXAMPLE));
    }

    /** The terminal is always given to PACE, which knows the machine through the player's own home. */
    @Override
    public String fileOf(final ICliComputer computer, final List<String> args) {
        return MenuShellListing.view("/usr/player");
    }

    @Override
    public void run(final CliContext ctx) {
        // The terminal has been given away; there is nothing to print behind it.
    }
}
