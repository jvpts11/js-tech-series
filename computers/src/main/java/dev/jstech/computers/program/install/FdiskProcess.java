/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.install.voice.Ext4Figures;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.computers.program.tty.TtyQuestion;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * The partition editor: a tool that is talked to rather than watched.
 *
 * <p>It takes the terminal over the way the real one does and asks its way through everything. A new
 * partition is three questions, its number, where it starts and where it ends, each with a default that Enter
 * takes, and the last answered the way it always is, with a plus and a size. It makes the real one's promise
 * too: nothing reaches the disk until it is told to write, and leaving without writing throws away everything
 * typed since it opened, as does Ctrl+C.
 *
 * <p>A size can also be given on the command's own line, {@code n 512M}, and a type the same way,
 * {@code t 1 uefi}. The real one takes neither, but somebody who has done this before types faster than the
 * questions come, and being made to wait for them teaches nothing.
 */
final class FdiskProcess implements ITtyProcess {

    private final List<LiveDisks.Partition> draft = new ArrayList<>();
    private final LiveDisks disks;
    private final String disk;
    private final long sizeMb;
    private boolean gpt;
    private boolean opened;
    private boolean over;
    private Stage stage = Stage.COMMAND;

    /** The partition a run of questions is about, once the first of them has been answered. */
    private int chosen;

    private static final String VERSION = "fdisk (util-linux 2.40.2)";

    /** How many sectors a megabyte is, at the five hundred and twelve bytes a sector has always been. */
    private static final long SECTORS_PER_MB = 2048L;

    /** Where the first partition may start, which leaves the room a partition table wants before it. */
    private static final long FIRST_SECTOR = 2048L;

    /** The most partitions a table of this kind holds. */
    private static final int MOST = 128;

    private static final CliLine COMMAND = new CliLine("Command (m for help): ", CliStyle.OK);

    /** Where in a run of questions the editor is standing. */
    private enum Stage {
        COMMAND, NEW_NUMBER, NEW_FIRST, NEW_LAST, TYPE_NUMBER, TYPE_KIND, DELETE_NUMBER
    }

    /**
     * @param disks  the machine's disks, which are written to only when the editor is told to write
     * @param disk   the disk it is opened on
     * @param sizeMb how big that disk is
     */
    FdiskProcess(final LiveDisks disks, final String disk, final long sizeMb) {
        this.disks = disks;
        this.disk = disk;
        this.sizeMb = sizeMb;
        this.draft.addAll(disks.table(disk));
        this.gpt = !this.draft.isEmpty();
    }

    @Override
    public void begin(final long now) {
        // Nothing here runs on the clock: the editor waits for whoever is typing.
    }

    @Override
    public void advance(final long now, @Nullable final ITtySink out) {
        if (this.opened) {
            return;
        }
        this.opened = true;
        say(out, "", "Welcome to " + VERSION + ".",
                "Changes will remain in memory only, until you decide to write them.",
                "Be careful before using the write command.", "");
        if (this.draft.isEmpty()) {
            say(out, "Device does not contain a recognized partition table.", "");
        }
    }

    @Override
    @Nullable
    public TtyQuestion asking() {
        if (this.over) {
            return null;
        }
        return new TtyQuestion(switch (this.stage) {
            case COMMAND -> COMMAND;
            case NEW_NUMBER -> CliLine.plain("Partition number (" + this.nextNumber() + "-" + MOST + ", default "
                    + this.nextNumber() + "): ");
            case NEW_FIRST -> CliLine.plain("First sector (" + this.nextStart() + "-" + this.lastUsable()
                    + ", default " + this.nextStart() + "): ");
            case NEW_LAST -> CliLine.plain("Last sector, +/-sectors or +/-size{K,M,G,T,P} (" + this.nextStart()
                    + "-" + this.lastUsable() + ", default " + this.defaultLast() + "): ");
            case TYPE_NUMBER, DELETE_NUMBER -> CliLine.plain("Partition number (1-" + this.draft.size()
                    + ", default " + this.draft.size() + "): ");
            case TYPE_KIND -> CliLine.plain("Partition type or alias (type L to list all): ");
        }, false);
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
        final String typed = line.trim();
        switch (this.stage) {
            case COMMAND -> this.command(typed, out);
            case NEW_NUMBER -> this.stage = Stage.NEW_FIRST;
            case NEW_FIRST -> this.stage = Stage.NEW_LAST;
            case NEW_LAST -> this.made(megabytesOf(typed), out);
            case TYPE_NUMBER -> this.picked(typed, Stage.TYPE_KIND, out);
            case TYPE_KIND -> this.typed(typed, out);
            case DELETE_NUMBER -> this.picked(typed, Stage.COMMAND, out);
        }
    }

    @Override
    public void interrupt(final long now, @Nullable final ITtySink out) {
        // Left without writing, so everything typed since it opened goes with it, as the opening lines said.
        this.over = true;
        say(out, "^C");
    }

    @Override
    public boolean over() {
        return this.over;
    }

    /** One of the editor's one-letter commands, with whatever was typed after it on the same line. */
    private void command(final String typed, @Nullable final ITtySink out) {
        final String[] parts = typed.split("\\s+");
        final String letter = parts[0].toLowerCase(Locale.ROOT);
        switch (letter) {
            case "" -> {
            }
            case "m", "help" -> say(out, "", "Help:", "", "  Generic", "   d   delete a partition",
                    "   n   add a new partition", "   p   print the partition table",
                    "   t   change a partition type", "", "  Save & Exit",
                    "   w   write table to disk and exit", "   q   quit without saving changes", "",
                    "  Create a new label", "   g   create a new empty GPT partition table", "");
            case "g" -> {
                this.gpt = true;
                this.draft.clear();
                say(out, "Created a new GPT disklabel (GUID: " + this.guid() + ").", "");
            }
            case "p" -> this.print(out);
            case "n" -> {
                if (this.draft.size() >= MOST) {
                    say(out, "No free sectors available.", "");
                } else if (parts.length > 1) {
                    this.made(megabytesOf(parts[1]), out);
                } else {
                    this.stage = Stage.NEW_NUMBER;
                }
            }
            case "t" -> this.ask(parts, Stage.TYPE_NUMBER, Stage.TYPE_KIND, out);
            case "d" -> this.ask(parts, Stage.DELETE_NUMBER, Stage.COMMAND, out);
            case "w" -> {
                this.disks.writeTable(this.disk, this.draft);
                this.over = true;
                say(out, "The partition table has been altered.", "Calling ioctl() to re-read partition table.",
                        "Syncing disks.", "");
            }
            case "q" -> {
                this.over = true;
                say(out, "");
            }
            default -> say(out, letter + ": unknown command", "");
        }
    }

    /**
     * Starts a command that is about one partition: with one on the disk there is nothing to ask, with several
     * it asks which, and with the number already on the line it goes straight on.
     */
    private void ask(final String[] parts, final Stage which, final Stage then, @Nullable final ITtySink out) {
        if (this.draft.isEmpty()) {
            say(out, "No partition is defined yet!", "");
            return;
        }
        if (parts.length > 1) {
            this.picked(parts[1], then, out);
            if (this.stage == Stage.TYPE_KIND && parts.length > 2) {
                this.typed(parts[2], out);
            }
            return;
        }
        if (this.draft.size() == 1) {
            say(out, "Selected partition 1");
            this.picked("1", then, out);
            return;
        }
        this.stage = which;
    }

    /** A partition named by its number, for a change of type or for deleting. */
    private void picked(final String typed, final Stage then, @Nullable final ITtySink out) {
        final int number = typed.isEmpty() ? this.draft.size() : numberOf(typed);
        if (number < 1 || number > this.draft.size()) {
            say(out, "Value out of range.", "");
            this.stage = Stage.COMMAND;
            return;
        }
        this.chosen = number;
        if (then == Stage.TYPE_KIND) {
            this.stage = Stage.TYPE_KIND;
            return;
        }
        this.draft.remove(number - 1);
        for (int i = 0; i < this.draft.size(); i++) {
            final LiveDisks.Partition was = this.draft.get(i);
            this.draft.set(i, new LiveDisks.Partition(i + 1, was.sizeMb(), was.esp()));
        }
        this.stage = Stage.COMMAND;
        say(out, "", "Partition " + number + " has been deleted.", "");
    }

    /** The kind a partition is changed to, by the real one's number for it or by a word. */
    private void typed(final String typed, @Nullable final ITtySink out) {
        final String kind = typed.toLowerCase(Locale.ROOT);
        this.stage = Stage.COMMAND;
        final boolean esp = kind.equals("1") || kind.equals("uefi") || kind.equals("efi") || kind.equals("ef");
        if (!esp && !kind.equals("20") && !kind.equals("linux") && !kind.equals("83")) {
            if (kind.equals("l")) {
                say(out, "  1 EFI System                     C12A7328-F81F-11D2-BA4B-00A0C93EC93B",
                        " 20 Linux filesystem               0FC63DAF-8483-4772-8E79-3D69D8477DE4", "");
                this.stage = Stage.TYPE_KIND;
                return;
            }
            say(out, "Failed to parse partition type '" + typed + "'.", "");
            return;
        }
        final LiveDisks.Partition was = this.draft.get(this.chosen - 1);
        this.draft.set(this.chosen - 1, new LiveDisks.Partition(was.number(), was.sizeMb(), esp));
        say(out, "Changed type of partition '" + was.type() + "' to '" + this.draft.get(this.chosen - 1).type()
                + "'.", "");
    }

    /** A new partition of that size, or of whatever is left of the disk when no size was given. */
    private void made(final int sizeMb, @Nullable final ITtySink out) {
        this.stage = Stage.COMMAND;
        final int number = this.draft.size() + 1;
        final long room = this.sizeMb - this.taken();
        if (room <= 0 || sizeMb > room) {
            say(out, "Value out of range.", "");
            return;
        }
        this.draft.add(new LiveDisks.Partition(number, sizeMb, false));
        say(out, "", "Created a new partition " + number + " of type 'Linux filesystem' and of size "
                + size(sizeMb > 0 ? sizeMb : room) + ".", "");
    }

    /** The table as the editor prints it: the disk, its label, and every partition by its sectors. */
    private void print(@Nullable final ITtySink out) {
        final long sectors = this.sizeMb * SECTORS_PER_MB;
        say(out, "Disk /dev/" + this.disk + ": " + size(this.sizeMb) + ", " + this.sizeMb * 1024L * 1024L
                        + " bytes, " + sectors + " sectors",
                "Units: sectors of 1 * 512 = 512 bytes",
                "Sector size (logical/physical): 512 bytes / 512 bytes",
                "I/O size (minimum/optimal): 512 bytes / 512 bytes",
                "Disklabel type: " + (this.gpt ? "gpt" : "dos"));
        if (this.gpt) {
            say(out, "Disk identifier: " + this.guid());
        }
        if (!this.draft.isEmpty()) {
            say(out, "", String.format(Locale.ROOT, "%-10s %10s %10s %10s %6s %s", "Device", "Start", "End",
                    "Sectors", "Size", "Type"));
            long start = FIRST_SECTOR;
            for (final LiveDisks.Partition part : this.draft) {
                final long megabytes = part.sizeMb() > 0 ? part.sizeMb() : this.sizeMb - this.taken();
                final long count = part.sizeMb() > 0 ? megabytes * SECTORS_PER_MB : this.defaultLast() - start + 1;
                say(out, String.format(Locale.ROOT, "%-10s %10d %10d %10d %6s %s", "/dev/" + part.on(this.disk),
                        start, start + count - 1, count, size(megabytes).replace(" ", "").replace("iB", ""),
                        part.type()));
                start += count;
            }
        }
        say(out, "");
    }

    private int nextNumber() {
        return this.draft.size() + 1;
    }

    private long nextStart() {
        return FIRST_SECTOR + this.taken() * SECTORS_PER_MB;
    }

    private long lastUsable() {
        return this.sizeMb * SECTORS_PER_MB - 34L;
    }

    private long defaultLast() {
        return this.sizeMb * SECTORS_PER_MB - SECTORS_PER_MB - 1L;
    }

    /** How much of the disk the partitions that asked for a size have had between them. */
    private long taken() {
        long sum = 0;
        for (final LiveDisks.Partition part : this.draft) {
            sum += Math.max(0, part.sizeMb());
        }
        return sum;
    }

    /** An identifier shaped like the real thing, the same for the same disk every time it is asked. */
    private String guid() {
        return Ext4Figures.uuid(this.disk, this.sizeMb).toUpperCase(Locale.ROOT);
    }

    /** A size as a person writes it at the last question: {@code +512M}, {@code +1G}, or nothing for the rest. */
    private static int megabytesOf(final String written) {
        final String digits = written.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        final int value = Integer.parseInt(digits);
        final char unit = Character.toUpperCase(written.charAt(written.length() - 1));
        return unit == 'G' ? value * 1024 : unit == 'T' ? value * 1024 * 1024
                : unit == 'K' ? Math.max(1, value / 1024) : value;
    }

    private static int numberOf(final String written) {
        try {
            return Integer.parseInt(written.trim());
        } catch (final NumberFormatException wrong) {
            return -1;
        }
    }

    /** A size as the editor writes one: the largest unit that fits, a decimal only when it says something. */
    private static String size(final long megabytes) {
        if (megabytes >= 1024L * 1024L) {
            return trimmed(megabytes / (1024.0 * 1024.0)) + " TiB";
        }
        if (megabytes >= 1024L) {
            return trimmed(megabytes / 1024.0) + " GiB";
        }
        return megabytes + " MiB";
    }

    private static String trimmed(final double value) {
        final double rounded = Math.round(value * 10.0) / 10.0;
        return rounded == Math.floor(rounded) ? String.valueOf((long) rounded)
                : String.format(Locale.ROOT, "%.1f", rounded);
    }

    private static void say(@Nullable final ITtySink out, final String... lines) {
        if (out != null) {
            for (final String line : lines) {
                out.line(CliLine.plain(line));
            }
        }
    }
}
