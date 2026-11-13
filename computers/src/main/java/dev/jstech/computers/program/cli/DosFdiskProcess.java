/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.disk.DiskCommand;
import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.DiskReport;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * MC-DOS's Fixed Disk Setup Program (computers/docs/design/storage-devices.md): its menu, a DOS partition made in the
 * free space, the active partition set, which is the one the machine starts and so how a second system on the disk is
 * reached, a partition deleted, and the partitions displayed. Each change is made at once, as the real one made them.
 */
@TextHolder
final class DosFdiskProcess extends DiskToolProcess {

    private Stage stage = Stage.MENU;
    private int disk;

    private static final TextKey TITLE_1 = TextKey.of("jsc.cli.dos_fdisk.title_1", "MC-DOS Version 1.0");
    private static final TextKey TITLE_2 = TextKey.of("jsc.cli.dos_fdisk.title_2", "Fixed Disk Setup Program");
    private static final TextKey TITLE_3 = TextKey.of("jsc.cli.dos_fdisk.title_3",
            "(C) Copyright Midsoft Corp. 1987");
    private static final TextKey OPTIONS = TextKey.of("jsc.cli.dos_fdisk.options", "FDISK Options");
    private static final TextKey CURRENT = TextKey.of("jsc.cli.dos_fdisk.current", "Current fixed disk drive: %s");
    private static final TextKey CHOOSE = TextKey.of("jsc.cli.dos_fdisk.choose", "Choose one of the following:");
    private static final TextKey CREATE = TextKey.of("jsc.cli.dos_fdisk.create", "1. Create DOS partition");
    private static final TextKey ACTIVE = TextKey.of("jsc.cli.dos_fdisk.active", "2. Set active partition");
    private static final TextKey DELETE = TextKey.of("jsc.cli.dos_fdisk.delete", "3. Delete partition");
    private static final TextKey DISPLAY = TextKey.of("jsc.cli.dos_fdisk.display",
            "4. Display partition information");
    private static final TextKey NEXT_DISK = TextKey.of("jsc.cli.dos_fdisk.next_disk",
            "5. Select next fixed disk drive");
    private static final TextKey ENTER_CHOICE = TextKey.of("jsc.cli.dos_fdisk.enter_choice", "Enter choice: ");
    private static final TextKey TO_EXIT = TextKey.of("jsc.cli.dos_fdisk.to_exit",
            "Press Enter on an empty choice to exit FDISK");
    private static final TextKey ASK_SIZE = TextKey.of("jsc.cli.dos_fdisk.ask_size",
            "Enter partition size in Mbytes (%s Mbytes free): ");
    private static final TextKey ASK_ACTIVE = TextKey.of("jsc.cli.dos_fdisk.ask_active",
            "Enter the number of the partition you want to make active: ");
    private static final TextKey ASK_DELETE = TextKey.of("jsc.cli.dos_fdisk.ask_delete",
            "Which partition do you want to delete? ");
    private static final TextKey TO_CONTINUE = TextKey.of("jsc.cli.dos_fdisk.to_continue",
            "Press Enter to continue ");
    private static final TextKey CREATED = TextKey.of("jsc.cli.dos_fdisk.created", "Primary DOS Partition created");
    private static final TextKey MADE_ACTIVE = TextKey.of("jsc.cli.dos_fdisk.made_active",
            "Partition %s made active");
    private static final TextKey DELETED = TextKey.of("jsc.cli.dos_fdisk.deleted", "Partition %s deleted");
    private static final TextKey DISPLAY_TITLE = TextKey.of("jsc.cli.dos_fdisk.display_title",
            "Display Partition Information");
    private static final TextKey HEADER = TextKey.of("jsc.cli.dos_fdisk.header",
            "Partition  Status  Type     Volume Label  Mbytes  System  Usage");
    private static final TextKey TOTAL = TextKey.of("jsc.cli.dos_fdisk.total",
            "Total disk space is %s Mbytes (1 Mbyte = 1048576 bytes)");
    private static final TextKey FREE = TextKey.of("jsc.cli.dos_fdisk.free", "Free space:          %s Mbytes");
    private static final TextKey ACTIVE_NOTE = TextKey.of("jsc.cli.dos_fdisk.active_note",
            "The active partition (A) is the one the machine starts.");
    private static final TextKey NOT_A_CHOICE = TextKey.of("jsc.cli.dos_fdisk.not_a_choice",
            "Invalid entry, please enter 1-%s.");
    private static final TextKey BAD_SIZE = TextKey.of("jsc.cli.dos_fdisk.bad_size",
            "Invalid entry, please enter a size in Mbytes.");

    DosFdiskProcess(final ICliComputer computer) {
        super(computer);
    }

    @Override
    protected void open(final ITtySink out) {
        this.menu(out);
    }

    @Override
    protected CliLine question() {
        final DiskReport.Disk disk = this.report().disk(this.disk);
        final long free = disk == null ? 0L : largestFree(disk);
        return switch (this.stage) {
            case MENU -> CliLine.of(ENTER_CHOICE, CliStyle.PLAIN);
            case SIZE -> new CliLine(ASK_SIZE.with(Math.min(free, FileSystemType.FAT16.mostPartitionMb())),
                    CliStyle.PLAIN);
            case ACTIVE -> CliLine.of(ASK_ACTIVE, CliStyle.PLAIN);
            case DELETE -> CliLine.of(ASK_DELETE, CliStyle.PLAIN);
            case SHOWN -> CliLine.of(TO_CONTINUE, CliStyle.DIM);
        };
    }

    @Override
    protected void handle(final String line, @Nullable final ITtySink out) {
        switch (this.stage) {
            case MENU -> this.choose(line, out);
            case SIZE -> this.create(line, out);
            case ACTIVE -> this.answered(DiskCommand.active(this.disk, number(line)), MADE_ACTIVE.with(line), out);
            case DELETE -> this.answered(DiskCommand.delete(this.disk, number(line)), DELETED.with(line), out);
            case SHOWN -> {
                this.stage = Stage.MENU;
                this.menu(out);
            }
        }
    }

    private void choose(final String line, @Nullable final ITtySink out) {
        final int disks = this.report().disks().size();
        switch (line) {
            case "" -> this.finish();
            case "1" -> this.stage = Stage.SIZE;
            case "2" -> {
                this.display(out);
                this.stage = Stage.ACTIVE;
            }
            case "3" -> {
                this.display(out);
                this.stage = Stage.DELETE;
            }
            case "4" -> {
                this.display(out);
                say(out, CliLine.plain(""));
                say(out, CliLine.of(ACTIVE_NOTE, CliStyle.DIM));
                this.stage = Stage.SHOWN;
            }
            case "5" -> {
                if (disks > 1) {
                    this.disk = (this.disk + 1) % disks;
                }
                this.menu(out);
            }
            default -> say(out, new CliLine(NOT_A_CHOICE.with(disks > 1 ? 5 : 4), CliStyle.ERROR));
        }
    }

    /* A primary DOS partition of that many megabytes in the largest free space, FAT16 as MC-DOS formats. */
    private void create(final String line, @Nullable final ITtySink out) {
        final DiskReport.Disk disk = this.report().disk(this.disk);
        DiskLayout.FreeSpace largest = null;
        if (disk != null) {
            for (final DiskLayout.FreeSpace gap : disk.layout().free()) {
                if (largest == null || gap.sizeMb() > largest.sizeMb()) {
                    largest = gap;
                }
            }
        }
        final long room = largest == null ? 0L : Math.min(largest.sizeMb(), FileSystemType.FAT16.mostPartitionMb());
        // A size that is no number, or none, is a wrong entry, not a disk with no room.
        if (!line.isEmpty() && number(line) <= 0L) {
            say(out, new CliLine(BAD_SIZE.text(), CliStyle.ERROR));
            this.stage = Stage.MENU;
            this.menu(out);
            return;
        }
        final long asked = line.isEmpty() ? room : Math.min(room, number(line));
        if (largest == null || asked <= 0L) {
            this.back(DiskReport.Refusal.NO_ROOM, out);
            return;
        }
        this.answered(DiskCommand.create(this.disk, largest.startMb(), asked,
                FileSystemType.FAT16.serializedName()), CREATED.text(), out);
    }

    /* Asks for a change, says it was made or why not, and goes back to the menu. */
    private void answered(final String path, final Text said, @Nullable final ITtySink out) {
        final DiskReport.Refusal refusal = this.change(path);
        if (refusal != null) {
            this.back(refusal, out);
            return;
        }
        say(out, CliLine.plain(said));
        this.stage = Stage.MENU;
        this.menu(out);
    }

    private void back(final DiskReport.Refusal refusal, @Nullable final ITtySink out) {
        say(out, new CliLine(reason(refusal), CliStyle.ERROR));
        this.stage = Stage.MENU;
        this.menu(out);
    }

    private void menu(@Nullable final ITtySink out) {
        say(out, CliLine.plain(""));
        say(out, CliLine.of(TITLE_1, CliStyle.OK));
        say(out, CliLine.of(TITLE_2, CliStyle.OK));
        say(out, CliLine.of(TITLE_3, CliStyle.OK));
        say(out, CliLine.plain(""));
        say(out, CliLine.of(OPTIONS, CliStyle.OK));
        say(out, CliLine.plain(""));
        say(out, new CliLine(CURRENT.with(this.disk + 1), CliStyle.OK));
        say(out, CliLine.plain(""));
        say(out, CliLine.of(CHOOSE, CliStyle.OK));
        say(out, CliLine.plain(""));
        say(out, CliLine.of(CREATE, CliStyle.OK));
        say(out, CliLine.of(ACTIVE, CliStyle.OK));
        say(out, CliLine.of(DELETE, CliStyle.OK));
        say(out, CliLine.of(DISPLAY, CliStyle.OK));
        if (this.report().disks().size() > 1) {
            say(out, CliLine.of(NEXT_DISK, CliStyle.OK));
        }
        say(out, CliLine.plain(""));
        say(out, CliLine.of(TO_EXIT, CliStyle.DIM));
    }

    /* The partitions of the current disk, as Display Partition Information shows them. */
    private void display(@Nullable final ITtySink out) {
        final DiskReport.Disk disk = this.report().disk(this.disk);
        say(out, CliLine.plain(""));
        say(out, CliLine.of(DISPLAY_TITLE, CliStyle.OK));
        say(out, CliLine.plain(""));
        say(out, new CliLine(CURRENT.with(this.disk + 1), CliStyle.OK));
        say(out, CliLine.plain(""));
        say(out, CliLine.of(HEADER, CliStyle.OK));
        if (disk == null) {
            return;
        }
        for (final DiskReport.Part part : disk.parts()) {
            final FileSystemType fs = part.fileSystem();
            final boolean dos = fs == FileSystemType.FAT12 || fs == FileSystemType.FAT16 || fs == FileSystemType.FAT32;
            say(out, new CliLine(String.format(Locale.ROOT, " %-2s %-6d %-6s %-8s %-12s %7d  %-7s %4d%%",
                    dos && part.number() == firstDos(disk) ? "C:" : "", part.number(), part.active() ? "A" : "",
                    dos ? "PRI DOS" : "Non-DOS", part.label(), part.sizeMb(),
                    fs == null ? "" : fs.displayName(), part.sizeMb() * 100L / Math.max(1L, disk.sizeMb())),
                    CliStyle.OK));
        }
        say(out, CliLine.plain(""));
        say(out, new CliLine(TOTAL.with(disk.sizeMb()), CliStyle.OK));
        say(out, new CliLine(FREE.with(disk.layout().freeMb()), CliStyle.OK));
    }

    private static int firstDos(final DiskReport.Disk disk) {
        for (final DiskReport.Part part : disk.parts()) {
            final FileSystemType fs = part.fileSystem();
            if (fs == FileSystemType.FAT12 || fs == FileSystemType.FAT16 || fs == FileSystemType.FAT32) {
                return part.number();
            }
        }
        return -1;
    }

    private static long largestFree(final DiskReport.Disk disk) {
        long largest = 0L;
        for (final DiskLayout.FreeSpace gap : disk.layout().free()) {
            largest = Math.max(largest, gap.sizeMb());
        }
        return largest;
    }

    private static int number(final String text) {
        try {
            return Integer.parseInt(text.strip());
        } catch (final NumberFormatException e) {
            return -1;
        }
    }

    /* Where in its menu the program is. */
    private enum Stage {
        MENU, SIZE, ACTIVE, DELETE, SHOWN
    }
}
