/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * {@code LITHO}: Midsoft Lithography as a text screen (computers/docs/design/survival.md, "Masks"), which takes the
 * whole terminal and shows the Lithography Station linked to the computer and the masks it writes, and writes the one
 * picked on the Mask Blank in its slot. On a desktop the same program opens as a window.
 */
@TextHolder
public final class LithoCommand implements ICliCommand, CliShell.IHandOver {

    /* The lithography program's id, by its text: the shell decides where the verb exists with no game loaded. */
    private static final String PACKAGE = "jsc:lithography";
    /* What the terminal is handed over as: the program's screen, which the computer is asked for as it runs. */
    private static final String SCREEN = "litho:screen";
    private static final TextKey SUMMARY = TextKey.of("jsc.cli.litho.summary",
            "show the Lithography Station linked to this computer and write its masks");
    private static final TextKey ABOUT = TextKey.of("jsc.cli.litho.about", "Takes the whole terminal and shows the"
            + " Lithography Station linked to this computer: what it is doing, the wafer and the layer it exposes, its"
            + " liquids and gases, what the computer holds for it and what stands in its mask slot. Beside it are the"
            + " masks it writes for this computer, a list a kind of chip.");
    private static final TextKey ABOUT_KEYS = TextKey.of("jsc.cli.litho.about.keys", "The arrows pick a mask and Tab"
            + " shows the next kind; W writes the mask picked on the Mask Blank in the station, in one layer's time,"
            + " never while the station exposes a wafer. S shows the next station when several are linked. F3 gives"
            + " the terminal back.");
    private static final TextKey EXAMPLE = TextKey.of("jsc.cli.litho.example",
            "the station and its masks on the whole glass");

    /** On a computer that has the lithography program, on every system it runs on; a desktop also opens a window. */
    @Override
    public CommandScope scope() {
        return CommandScope.everywhere().fromPackage(PACKAGE);
    }

    @Override
    public String name() {
        return "litho";
    }

    @Override
    public CommandGroup group() {
        return CommandGroup.MACHINE;
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
        return List.of(new Example("litho", EXAMPLE));
    }

    /** The terminal is always given to the screen. */
    @Override
    public String fileOf(final ICliComputer computer, final List<String> args) {
        return SCREEN;
    }

    @Override
    public void run(final CliContext ctx) {
        // The terminal has been given away; there is nothing to print behind it.
    }
}
