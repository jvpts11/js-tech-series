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
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
 *
 * <p>Its questions, its menu and what it says back are read in the player's language, as the real one's are. The
 * table it prints is data: device names, sectors, sizes, and the names of rows of the table of partition types.
 */
@TextHolder
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

    /** The tool's name and version, which it welcomes you to and which are the same in every language. */
    private static final Text VERSION = Text.literal("fdisk (util-linux 2.40.2)");

    /** How many sectors a megabyte is, at the five hundred and twelve bytes a sector has always been. */
    private static final long SECTORS_PER_MB = 2048L;

    /** Where the first partition may start, which leaves the room a partition table wants before it. */
    private static final long FIRST_SECTOR = 2048L;

    /** The most partitions a table of this kind holds. */
    private static final int MOST = 128;

    /* Its questions, each followed by a space the answer is typed after. */
    private static final TextKey ASK_COMMAND = TextKey.of("jsc.install.fdisk_process.ask_command",
            "Command (m for help):");
    private static final TextKey ASK_NUMBER = TextKey.of("jsc.install.fdisk_process.ask_number",
            "Partition number (%s-%s, default %s):");
    private static final TextKey ASK_FIRST = TextKey.of("jsc.install.fdisk_process.ask_first",
            "First sector (%s-%s, default %s):");
    private static final TextKey ASK_LAST = TextKey.of("jsc.install.fdisk_process.ask_last",
            "Last sector, +/-sectors or +/-size{K,M,G,T,P} (%s-%s, default %s):");
    private static final TextKey ASK_TYPE = TextKey.of("jsc.install.fdisk_process.ask_type",
            "Partition type or alias (type L to list all):");

    private static final TextKey WELCOME = TextKey.of("jsc.install.fdisk_process.welcome", "Welcome to %s.");
    private static final TextKey IN_MEMORY = TextKey.of("jsc.install.fdisk_process.in_memory",
            "Changes will remain in memory only, until you decide to write them.");
    private static final TextKey BE_CAREFUL = TextKey.of("jsc.install.fdisk_process.be_careful",
            "Be careful before using the write command.");
    private static final TextKey NO_TABLE = TextKey.of("jsc.install.fdisk_process.no_table",
            "Device does not contain a recognized partition table.");

    /* The menu m prints: the groups, and what each command's letter does. */
    private static final TextKey HELP = TextKey.of("jsc.install.fdisk_process.help", "Help:");
    private static final TextKey GROUP_GENERIC = TextKey.of("jsc.install.fdisk_process.group_generic", "Generic");
    private static final TextKey GROUP_SAVE = TextKey.of("jsc.install.fdisk_process.group_save", "Save & Exit");
    private static final TextKey GROUP_LABEL = TextKey.of("jsc.install.fdisk_process.group_label",
            "Create a new label");
    private static final TextKey MENU_DELETE = TextKey.of("jsc.install.fdisk_process.menu_delete",
            "delete a partition");
    private static final TextKey MENU_NEW = TextKey.of("jsc.install.fdisk_process.menu_new", "add a new partition");
    private static final TextKey MENU_PRINT = TextKey.of("jsc.install.fdisk_process.menu_print",
            "print the partition table");
    private static final TextKey MENU_TYPE = TextKey.of("jsc.install.fdisk_process.menu_type",
            "change a partition type");
    private static final TextKey MENU_WRITE = TextKey.of("jsc.install.fdisk_process.menu_write",
            "write table to disk and exit");
    private static final TextKey MENU_QUIT = TextKey.of("jsc.install.fdisk_process.menu_quit",
            "quit without saving changes");
    private static final TextKey MENU_GPT = TextKey.of("jsc.install.fdisk_process.menu_gpt",
            "create a new empty GPT partition table");

    private static final TextKey CREATED_LABEL = TextKey.of("jsc.install.fdisk_process.created_label",
            "Created a new GPT disklabel (GUID: %s).");
    private static final TextKey NO_FREE_SECTORS = TextKey.of("jsc.install.fdisk_process.no_free_sectors",
            "No free sectors available.");
    private static final TextKey ALTERED = TextKey.of("jsc.install.fdisk_process.altered",
            "The partition table has been altered.");
    private static final TextKey REREAD = TextKey.of("jsc.install.fdisk_process.reread",
            "Calling ioctl() to re-read partition table.");
    private static final TextKey SYNCING = TextKey.of("jsc.install.fdisk_process.syncing", "Syncing disks.");
    private static final TextKey UNKNOWN_COMMAND = TextKey.of("jsc.install.fdisk_process.unknown_command",
            "%s: unknown command");
    private static final TextKey NO_PARTITION = TextKey.of("jsc.install.fdisk_process.no_partition",
            "No partition is defined yet!");
    private static final TextKey SELECTED = TextKey.of("jsc.install.fdisk_process.selected",
            "Selected partition %s");
    private static final TextKey OUT_OF_RANGE = TextKey.of("jsc.install.fdisk_process.out_of_range",
            "Value out of range.");
    private static final TextKey DELETED = TextKey.of("jsc.install.fdisk_process.deleted",
            "Partition %s has been deleted.");
    private static final TextKey PARSE_FAILED = TextKey.of("jsc.install.fdisk_process.parse_failed",
            "Failed to parse partition type '%s'.");
    private static final TextKey CHANGED_TYPE = TextKey.of("jsc.install.fdisk_process.changed_type",
            "Changed type of partition '%s' to '%s'.");
    private static final TextKey CREATED = TextKey.of("jsc.install.fdisk_process.created",
            "Created a new partition %s of type '%s' and of size %s.");

    /* What p prints above the table: the disk, how it is measured, and its label. */
    private static final TextKey DISK_LINE = TextKey.of("jsc.install.fdisk_process.disk_line",
            "Disk %s: %s, %s bytes, %s sectors");
    private static final TextKey UNITS = TextKey.of("jsc.install.fdisk_process.units",
            "Units: sectors of 1 * 512 = 512 bytes");
    private static final TextKey SECTOR_SIZE = TextKey.of("jsc.install.fdisk_process.sector_size",
            "Sector size (logical/physical): 512 bytes / 512 bytes");
    private static final TextKey IO_SIZE = TextKey.of("jsc.install.fdisk_process.io_size",
            "I/O size (minimum/optimal): 512 bytes / 512 bytes");
    private static final TextKey LABEL_TYPE = TextKey.of("jsc.install.fdisk_process.label_type",
            "Disklabel type: %s");
    private static final TextKey DISK_ID = TextKey.of("jsc.install.fdisk_process.disk_id", "Disk identifier: %s");

    private static final CliLine COMMAND =
            CliLine.build().add(ASK_COMMAND, CliStyle.OK).add(" ", CliStyle.OK).done();

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
        say(out, Text.EMPTY, WELCOME.with(VERSION), IN_MEMORY.text(), BE_CAREFUL.text(), Text.EMPTY);
        if (this.draft.isEmpty()) {
            say(out, NO_TABLE.text(), Text.EMPTY);
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
            case NEW_NUMBER -> question(ASK_NUMBER.with(this.nextNumber(), MOST, this.nextNumber()));
            case NEW_FIRST -> question(ASK_FIRST.with(this.nextStart(), this.lastUsable(), this.nextStart()));
            case NEW_LAST -> question(ASK_LAST.with(this.nextStart(), this.lastUsable(), this.defaultLast()));
            case TYPE_NUMBER, DELETE_NUMBER -> question(ASK_NUMBER.with(1, this.draft.size(), this.draft.size()));
            case TYPE_KIND -> question(ASK_TYPE.text());
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
        say(out, Text.literal("^C"));
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
            case "m", "help" -> help(out);
            case "g" -> {
                this.gpt = true;
                this.draft.clear();
                say(out, CREATED_LABEL.with(this.guid()), Text.EMPTY);
            }
            case "p" -> this.print(out);
            case "n" -> {
                if (this.draft.size() >= MOST) {
                    say(out, NO_FREE_SECTORS.text(), Text.EMPTY);
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
                say(out, ALTERED.text(), REREAD.text(), SYNCING.text(), Text.EMPTY);
            }
            case "q" -> {
                this.over = true;
                say(out, Text.EMPTY);
            }
            default -> say(out, UNKNOWN_COMMAND.with(letter), Text.EMPTY);
        }
    }

    /**
     * Starts a command that is about one partition: with one on the disk there is nothing to ask, with several
     * it asks which, and with the number already on the line it goes straight on.
     */
    private void ask(final String[] parts, final Stage which, final Stage then, @Nullable final ITtySink out) {
        if (this.draft.isEmpty()) {
            say(out, NO_PARTITION.text(), Text.EMPTY);
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
            say(out, SELECTED.with(1));
            this.picked("1", then, out);
            return;
        }
        this.stage = which;
    }

    /** A partition named by its number, for a change of type or for deleting. */
    private void picked(final String typed, final Stage then, @Nullable final ITtySink out) {
        final int number = typed.isEmpty() ? this.draft.size() : numberOf(typed);
        if (number < 1 || number > this.draft.size()) {
            say(out, OUT_OF_RANGE.text(), Text.EMPTY);
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
        say(out, Text.EMPTY, DELETED.with(number), Text.EMPTY);
    }

    /** The kind a partition is changed to, by the real one's number for it or by a word. */
    private void typed(final String typed, @Nullable final ITtySink out) {
        final String kind = typed.toLowerCase(Locale.ROOT);
        this.stage = Stage.COMMAND;
        final boolean esp = kind.equals("1") || kind.equals("uefi") || kind.equals("efi") || kind.equals("ef");
        if (!esp && !kind.equals("20") && !kind.equals("linux") && !kind.equals("83")) {
            if (kind.equals("l")) {
                // Rows of the table of partition types: a number, a name and an identifier, all data.
                say(out, Text.literal("  1 EFI System                     C12A7328-F81F-11D2-BA4B-00A0C93EC93B"),
                        Text.literal(" 20 Linux filesystem               0FC63DAF-8483-4772-8E79-3D69D8477DE4"),
                        Text.EMPTY);
                this.stage = Stage.TYPE_KIND;
                return;
            }
            say(out, PARSE_FAILED.with(typed), Text.EMPTY);
            return;
        }
        final LiveDisks.Partition was = this.draft.get(this.chosen - 1);
        this.draft.set(this.chosen - 1, new LiveDisks.Partition(was.number(), was.sizeMb(), esp));
        say(out, CHANGED_TYPE.with(was.type(), this.draft.get(this.chosen - 1).type()), Text.EMPTY);
    }

    /** A new partition of that size, or of whatever is left of the disk when no size was given. */
    private void made(final int sizeMb, @Nullable final ITtySink out) {
        this.stage = Stage.COMMAND;
        final int number = this.draft.size() + 1;
        final long room = this.sizeMb - this.taken();
        if (room <= 0 || sizeMb > room) {
            say(out, OUT_OF_RANGE.text(), Text.EMPTY);
            return;
        }
        final LiveDisks.Partition added = new LiveDisks.Partition(number, sizeMb, false);
        this.draft.add(added);
        say(out, Text.EMPTY, CREATED.with(number, added.type(), size(sizeMb > 0 ? sizeMb : room)), Text.EMPTY);
    }

    /** The table as the editor prints it: the disk, its label, and every partition by its sectors. */
    private void print(@Nullable final ITtySink out) {
        final long sectors = this.sizeMb * SECTORS_PER_MB;
        say(out, DISK_LINE.with(Text.literal("/dev/" + this.disk), size(this.sizeMb), this.sizeMb * 1024L * 1024L,
                        sectors),
                UNITS.text(), SECTOR_SIZE.text(), IO_SIZE.text(),
                LABEL_TYPE.with(Text.literal(this.gpt ? "gpt" : "dos")));
        if (this.gpt) {
            say(out, DISK_ID.with(this.guid()));
        }
        if (!this.draft.isEmpty()) {
            /*
             * The table is data, its column names too: they are laid out in fixed widths the rows below line up
             * under, which words of another length would break.
             */
            say(out, Text.EMPTY, Text.literal(String.format(Locale.ROOT, "%-10s %10s %10s %10s %6s %s", "Device",
                    "Start", "End", "Sectors", "Size", "Type")));
            long start = FIRST_SECTOR;
            for (final LiveDisks.Partition part : this.draft) {
                final long megabytes = part.sizeMb() > 0 ? part.sizeMb() : this.sizeMb - this.taken();
                final long count = part.sizeMb() > 0 ? megabytes * SECTORS_PER_MB : this.defaultLast() - start + 1;
                line(out, CliLine.build().plain(Text.literal(String.format(Locale.ROOT, "%-10s %10d %10d %10d %6s ",
                        "/dev/" + part.on(this.disk), start, start + count - 1, count,
                        size(megabytes).replace(" ", "").replace("iB", "")))).plain(part.type()).done());
                start += count;
            }
        }
        say(out, Text.EMPTY);
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

    /** The menu {@code m} prints: each group's name, and under it each command's letter and what it does. */
    private static void help(@Nullable final ITtySink out) {
        say(out, Text.EMPTY, HELP.text(), Text.EMPTY);
        menuGroup(out, GROUP_GENERIC);
        menuItem(out, 'd', MENU_DELETE);
        menuItem(out, 'n', MENU_NEW);
        menuItem(out, 'p', MENU_PRINT);
        menuItem(out, 't', MENU_TYPE);
        say(out, Text.EMPTY);
        menuGroup(out, GROUP_SAVE);
        menuItem(out, 'w', MENU_WRITE);
        menuItem(out, 'q', MENU_QUIT);
        say(out, Text.EMPTY);
        menuGroup(out, GROUP_LABEL);
        menuItem(out, 'g', MENU_GPT);
        say(out, Text.EMPTY);
    }

    /** A group of the menu, by its name, indented as the real one indents it. */
    private static void menuGroup(@Nullable final ITtySink out, final TextKey name) {
        line(out, CliLine.build().plain("  ").plain(name.text()).done());
    }

    /** A command of the menu: its letter, which is what is typed, and what it does. */
    private static void menuItem(@Nullable final ITtySink out, final char letter, final TextKey does) {
        line(out, CliLine.build().plain("   " + letter + "   ").plain(does.text()).done());
    }

    /** A question as the editor asks it, with the space the answer is typed after. */
    private static CliLine question(final Text asked) {
        return CliLine.build().plain(asked).plain(" ").done();
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

    private static void say(@Nullable final ITtySink out, final Text... lines) {
        if (out != null) {
            for (final Text line : lines) {
                out.line(CliLine.plain(line));
            }
        }
    }

    private static void line(@Nullable final ITtySink out, final CliLine line) {
        if (out != null) {
            out.line(line);
        }
    }
}
