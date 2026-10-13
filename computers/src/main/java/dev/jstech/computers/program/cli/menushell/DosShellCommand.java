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
 * {@code DOSSHELL}: the MC-DOS Shell, Midsoft's menu shell for MC-DOS, which takes the whole terminal and shows the
 * drives, the tree of folders, the files of the one picked, the programs and the tasks the shell is running. Every
 * action in its menus is one of MC-DOS's own commands, and its task switcher keeps the prompts it opened beside it.
 */
@TextHolder
public final class DosShellCommand implements ICliCommand, CliShell.IHandOver {

    private static final TextKey SUMMARY = TextKey.of("jsc.cli.dosshell.summary",
            "the MC-DOS Shell: files, programs and tasks in menus");
    private static final TextKey ABOUT = TextKey.of("jsc.cli.dosshell.about", "Takes the whole terminal and shows"
            + " the drives, the tree of folders and the files of the one picked, the Main group of programs and the"
            + " Active Task List. Every action of the File menu is one of MC-DOS's commands: TYPE to view a file,"
            + " COPY, MOVE, DEL, REN and MKDIR.");
    private static final TextKey ABOUT_KEYS = TextKey.of("jsc.cli.dosshell.about.keys", "Tab moves between the"
            + " areas and the arrows within one; Enter opens what is picked. F10 or Alt with a menu's letter opens the"
            + " menus. Shift+F9 opens a command prompt beside the shell as a task, and Alt+Tab switches between the"
            + " shell and its tasks; EXIT at a task's prompt ends it. F3 or Alt+F4 leaves the shell.");
    private static final TextKey EXAMPLE = TextKey.of("jsc.cli.dosshell.example",
            "the shell on the whole glass, at the root of C:");

    @Override
    public CommandScope scope() {
        return CommandScope.on(Platform.MC_DOS);
    }

    @Override
    public String name() {
        return "dosshell";
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
        return List.of(new Example("dosshell", EXAMPLE));
    }

    /** The terminal is always given to the shell, which opens on the root of the system disk. */
    @Override
    public String fileOf(final ICliComputer computer, final List<String> args) {
        return MenuShellListing.view("C:\\");
    }

    @Override
    public void run(final CliContext ctx) {
        // The terminal has been given away; there is nothing to print behind it.
    }
}
