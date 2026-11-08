/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.defrag.DefragLook;
import dev.jstech.computers.os.defrag.DefragState;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * {@code DEFRAG [drive:]}: MC-DOS's Defrag, which takes the whole terminal and shows the map of a disk's clusters while
 * it repairs the corrupted items on it (computers/docs/design/storage.md). MC-NET runs the same.
 */
@TextHolder
public final class DefragCommand implements ICliCommand, CliShell.IHandOver {

    private static final TextKey SUMMARY = TextKey.of("jsc.cli.defrag.summary",
            "repair the corrupted items on a disk and mend its bad sectors");
    private static final TextKey ABOUT = TextKey.of("jsc.cli.defrag.about", "Takes the whole terminal and shows the"
            + " disk's clusters: what is used, what is free, what is corrupted and the bad sectors. Enter starts a"
            + " pass, which tries every corrupted item once and gives back what it can; what it cannot is lost. Every"
            + " pass also mends a quarter of the bad sectors left. Esc gives the terminal back.");
    private static final TextKey EXAMPLE = TextKey.of("jsc.cli.defrag.example", "repair the system disk");
    private static final TextKey EXAMPLE_OTHER = TextKey.of("jsc.cli.defrag.example_other", "repair the second disk");
    private static final TextKey INVALID_DRIVE = TextKey.of("jsc.cli.defrag.invalid_drive",
            "Invalid drive specification");

    @Override
    public CommandScope scope() {
        return CommandScope.on(Platform.MC_DOS, Platform.MC_NET);
    }

    @Override
    public String name() {
        return "defrag";
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
        return List.of(ABOUT.text());
    }

    @Override
    public List<Example> examples() {
        return List.of(new Example("defrag", EXAMPLE), new Example("defrag d:", EXAMPLE_OTHER));
    }

    /** The terminal goes to the screen on the disk named, the system disk when none is; kept for a drive not there. */
    @Override
    public String fileOf(final ICliComputer computer, final List<String> args) {
        final int disk = diskOf(computer, args);
        return disk < 0 ? null : DefragState.path(DefragState.LOOK, disk);
    }

    @Override
    public void run(final CliContext ctx) {
        if (diskOf(ctx.computer(), ctx.args()) < 0) {
            ctx.out().error(INVALID_DRIVE);
        }
    }

    /* The disk the arguments name, the system disk when they name none; -1 when they name one that is not there. */
    private static int diskOf(final ICliComputer computer, final List<String> args) {
        final int disks = DefragState.read(computer.defrag(DefragState.path(DefragState.LOOK, 0))).disks().size();
        if (args.isEmpty()) {
            return disks > 0 ? 0 : -1;
        }
        return DefragLook.DOS.diskNamed(args.getFirst(), disks);
    }
}
