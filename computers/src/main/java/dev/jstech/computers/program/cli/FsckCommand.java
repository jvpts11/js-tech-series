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
import dev.jstech.computers.os.defrag.FsckLines;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.computers.program.tty.TtyQuestion;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * {@code fsck [-y] [device]}: what repairs a disk on Linux, FreeBSD and UNIX, their Defrag
 * (computers/docs/design/storage.md). It holds the terminal while the pass runs, printing its passes or phases as the
 * repair goes, and at the end what it found, what came back and what was lost, and the bad sectors before and after.
 */
@TextHolder
public final class FsckCommand implements ICliCommand {

    private static final TextKey SUMMARY = TextKey.of("jsc.cli.fsck.summary",
            "check a disk and repair the corrupted items on it");
    private static final TextKey ABOUT = TextKey.of("jsc.cli.fsck.about", "Repairs the disk named, the system disk"
            + " when none is: it tries every corrupted item once and gives back what it can, the rest is lost, and it"
            + " mends a quarter of the bad sectors left. It takes as long as a pass takes on that kind of disk; Ctrl+C"
            + " stops it where it stands.");
    private static final TextKey EXAMPLE = TextKey.of("jsc.cli.fsck.example", "repair the second disk");
    private static final TextKey NO_DEVICE = TextKey.of("jsc.cli.fsck.no_device",
            "fsck: %s: No such file or directory");
    private static final TextKey BUSY = TextKey.of("jsc.cli.fsck.busy",
            "fsck: %s is busy: a repair is running on another disk");

    /* How often the tool looks at how the repair goes, in ticks. */
    private static final int LOOK_EVERY = 10;

    @Override
    public CommandScope scope() {
        return CommandScope.on(Platform.LINUX, Platform.FREEBSD, Platform.UNIX);
    }

    @Override
    public String name() {
        return "fsck";
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
        return List.of(new Example("fsck -y /dev/sdb1", EXAMPLE));
    }

    @Override
    public void run(final CliContext ctx) {
        final ICliComputer computer = ctx.computer();
        final DefragState state = DefragState.read(computer.defrag(DefragState.path(DefragState.LOOK, 0)));
        String named = "";
        for (final String arg : ctx.args()) {
            if (!arg.startsWith("-")) {
                named = arg;
            }
        }
        final int disk = named.isEmpty() ? (state.disks().isEmpty() ? -1 : 0)
                : state.look().diskNamed(named, state.disks().size());
        if (disk < 0) {
            ctx.out().error(NO_DEVICE.with(named.isEmpty() ? "/" : named));
            return;
        }
        /*
         * A pass already over that disk is watched rather than started again: the line is typed again to find the
         * tool after a load, and the pass it started is still going.
         */
        final DefragState.Run running = state.running();
        if (running == null || running.disk() != disk) {
            final DefragState after = DefragState.read(computer.defrag(DefragState.path(DefragState.START, disk)));
            if (after.running() == null || after.running().disk() != disk) {
                ctx.out().error(BUSY.with(state.look().device(disk)));
                return;
            }
        }
        ctx.out().start(new Repair(computer, state.look(), disk));
    }

    /** The repair under way, printing as it goes. */
    private static final class Repair implements ITtyProcess {

        private final ICliComputer computer;
        private final DefragLook look;
        private final int disk;
        private final List<String> phases;
        private boolean opened;
        private int printed;
        private long lookedAt = Long.MIN_VALUE;
        private boolean over;

        Repair(final ICliComputer computer, final DefragLook look, final int disk) {
            this.computer = computer;
            this.look = look;
            this.disk = disk;
            this.phases = FsckLines.phases(look);
        }

        @Override
        public void begin(final long now) {
            this.lookedAt = Long.MIN_VALUE;
        }

        @Override
        public void advance(final long now, @Nullable final ITtySink out) {
            if (this.over || now - this.lookedAt < LOOK_EVERY) {
                return;
            }
            this.lookedAt = now;
            if (!this.opened) {
                this.opened = true;
                FsckLines.opening(this.look, this.disk).forEach(line -> print(out, line));
            }
            final DefragState state = DefragState.read(this.computer.defrag(
                    DefragState.path(DefragState.LOOK, this.disk)));
            final DefragState.Run running = state.running();
            if (running != null && running.disk() == this.disk) {
                printPhases(out, FsckLines.phasesBy(running.percent()));
                return;
            }
            printPhases(out, this.phases.size());
            final DefragState.Result last = state.last();
            if (last != null && last.disk() == this.disk) {
                FsckLines.summary(this.look, state.disk(this.disk), last).forEach(line -> print(out, line));
            }
            this.over = true;
        }

        @Nullable
        @Override
        public TtyQuestion asking() {
            return null;
        }

        @Override
        public void answer(final String line, final long now, @Nullable final ITtySink out) {
            // fsck -y asks nothing.
        }

        @Override
        public void interrupt(final long now, @Nullable final ITtySink out) {
            this.computer.defrag(DefragState.path(DefragState.STOP, this.disk));
            print(out, "^C");
            this.over = true;
        }

        @Override
        public boolean over() {
            return this.over;
        }

        private void printPhases(@Nullable final ITtySink out, final int upTo) {
            while (this.printed < Math.min(upTo, this.phases.size())) {
                print(out, this.phases.get(this.printed++));
            }
        }

        private static void print(@Nullable final ITtySink out, final String line) {
            if (out != null) {
                out.line(CliLine.plain(line));
            }
        }
    }
}
