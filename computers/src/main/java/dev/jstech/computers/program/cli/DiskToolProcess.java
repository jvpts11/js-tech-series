/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.disk.DiskReport;
import dev.jstech.computers.os.disk.DiskTexts;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.computers.program.tty.TtyQuestion;
import dev.jstech.core.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * A disk tool that takes the terminal over and asks its way through (computers/docs/design/storage-devices.md):
 * diskpart at its DISKPART&gt; prompt, MC-DOS's FDISK and UNIX's fdisk at their menus. It prints its opening the first
 * time somebody watches, asks its question where the prompt would be, echoes what was answered after it, and gives the
 * prompt back when it is left; Ctrl+C leaves it at once.
 */
abstract class DiskToolProcess implements ITtyProcess {

    /** The machine whose disks it changes. */
    protected final ICliComputer computer;
    private boolean opened;
    private boolean over;

    DiskToolProcess(final ICliComputer computer) {
        this.computer = computer;
    }

    @Override
    public void begin(final long now) {
        // A disk tool keeps no clock: it waits on its questions.
    }

    @Override
    public void advance(final long now, @Nullable final ITtySink out) {
        if (!this.opened && out != null) {
            this.opened = true;
            this.open(out);
        }
    }

    @Nullable
    @Override
    public TtyQuestion asking() {
        return this.over ? null : new TtyQuestion(this.question(), false);
    }

    @Override
    public void answer(final String line, final long now, @Nullable final ITtySink out) {
        final TtyQuestion asked = this.asking();
        if (asked == null) {
            return;
        }
        this.advance(now, out);
        if (out != null) {
            out.line(CliLine.build().add(asked.text()).plain(line).done());
        }
        this.handle(line.strip(), out);
    }

    @Override
    public void interrupt(final long now, @Nullable final ITtySink out) {
        say(out, CliLine.plain("^C"));
        this.over = true;
    }

    @Override
    public boolean over() {
        return this.over;
    }

    /** What it prints when it opens. */
    protected abstract void open(ITtySink out);

    /** What it asks now, where the prompt would be. */
    protected abstract CliLine question();

    /** What was answered, already echoed. */
    protected abstract void handle(String line, @Nullable ITtySink out);

    /** Leaves it: the prompt comes back. */
    protected final void finish() {
        this.over = true;
    }

    /** The machine's disks as they are. */
    protected final DiskReport report() {
        return DiskToolLines.report(this.computer);
    }

    /** Asks the machine for a change, and answers why it was refused, or null when it was made. */
    @Nullable
    protected final DiskReport.Refusal change(final String path) {
        return DiskToolLines.ask(this.computer, path).refusal();
    }

    /** Why a change was refused, in the words every disk tool uses. */
    protected static Text reason(final DiskReport.Refusal refusal) {
        return DiskTexts.reason(refusal);
    }

    /** Prints a line to whoever is watching. */
    protected static void say(@Nullable final ITtySink out, final CliLine line) {
        if (out != null) {
            out.line(line);
        }
    }
}
