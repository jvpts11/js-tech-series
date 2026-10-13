/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.msd;

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
 * {@code MSD}: the diagnostics of the Vintage systems, which take the whole terminal and show the machine's parts and
 * its LPT and COM ports, with what is attached to each, and enable or disable a device on one of them.
 *
 * <p>MC-DOS and MC-NET have no Device Manager; this is where a device on their ports is disabled, the way the
 * diagnostics of that age were where a person looked at a machine's hardware.
 */
@TextHolder
public final class MsdCommand implements ICliCommand, CliShell.IHandOver {

    private static final TextKey SUMMARY = TextKey.of("jsc.cli.msd.summary",
            "show the machine's parts and its LPT and COM ports");
    private static final TextKey ABOUT = TextKey.of("jsc.cli.msd.about", "Takes the whole terminal and shows what"
            + " the machine is made of, a button for each part with what it found beside it. LPT Ports and COM Ports"
            + " open the list of the ports with what is attached to each and whether it is on, where a device is"
            + " enabled or disabled. A disabled device keeps its port, and the computer neither reads nor writes it.");
    private static final TextKey ABOUT_KEYS = TextKey.of("jsc.cli.msd.about.keys", "Arrows move between the"
            + " buttons and Enter opens one; L opens the LPT ports and C the COM ports. In the list, E enables the"
            + " picked port's device, D disables it, and Enter puts the list away. F3 gives the terminal back.");
    private static final TextKey EXAMPLE = TextKey.of("jsc.cli.msd.example",
            "the machine's parts and its ports on the whole glass");

    @Override
    public CommandScope scope() {
        return CommandScope.on(Platform.MC_DOS, Platform.MC_NET);
    }

    @Override
    public String name() {
        return "msd";
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
        return List.of(new Example("msd", EXAMPLE));
    }

    /** The terminal is always given to the screen; what is handed over is the state it opens in. */
    @Override
    public String fileOf(final ICliComputer computer, final List<String> args) {
        return MsdState.OPENING.path();
    }

    @Override
    public void run(final CliContext ctx) {
        // The terminal has been given away; there is nothing to print behind it.
    }
}
