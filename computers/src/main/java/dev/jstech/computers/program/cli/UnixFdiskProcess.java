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
import dev.jstech.computers.os.disk.PartitionEntry;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * UNIX System V's fdisk (computers/docs/design/storage-devices.md): the disk's partitions in cylinders, a megabyte
 * each, and its menu. What it is told is kept in a draft and shown in the table; Exit writes it to the disk, Cancel
 * leaves the disk as it was. Changing the active partition is how the machine is made to start another system.
 */
@TextHolder
final class UnixFdiskProcess extends DiskToolProcess {

    private final List<String> pending = new ArrayList<>();
    private DiskLayout draft;
    private Stage stage = Stage.MENU;

    private static final int DISK = 0;
    private static final TextKey TOTAL = TextKey.of("jsc.cli.unix_fdisk.total",
            "Total disk size is %s MB (%s cylinders)");
    private static final TextKey HEADER = TextKey.of("jsc.cli.unix_fdisk.header",
            "Partition   Status    Type      Start   End   Length    %");
    private static final TextKey RULE = TextKey.of("jsc.cli.unix_fdisk.rule",
            "=========   ======    ====      =====   ===   ======   ===");
    private static final TextKey SELECT = TextKey.of("jsc.cli.unix_fdisk.select", "SELECT ONE OF THE FOLLOWING:");
    private static final TextKey CREATE = TextKey.of("jsc.cli.unix_fdisk.create", "1.   Create a partition");
    private static final TextKey CHANGE = TextKey.of("jsc.cli.unix_fdisk.change",
            "2.   Change Active (Boot from) partition");
    private static final TextKey DELETE = TextKey.of("jsc.cli.unix_fdisk.delete", "3.   Delete a partition");
    private static final TextKey EXIT = TextKey.of("jsc.cli.unix_fdisk.exit",
            "4.   Exit (Update disk configuration and exit)");
    private static final TextKey CANCEL = TextKey.of("jsc.cli.unix_fdisk.cancel",
            "5.   Cancel (Exit without updating disk configuration)");
    private static final TextKey ENTER = TextKey.of("jsc.cli.unix_fdisk.enter", "Enter Selection: ");
    private static final TextKey ASK_PERCENT = TextKey.of("jsc.cli.unix_fdisk.ask_percent",
            "Indicate the percentage of the free space for this UNIX partition: ");
    private static final TextKey ASK_ACTIVE = TextKey.of("jsc.cli.unix_fdisk.ask_active",
            "Enter the number of the partition to make active: ");
    private static final TextKey ASK_DELETE = TextKey.of("jsc.cli.unix_fdisk.ask_delete",
            "Enter the number of the partition to delete: ");
    private static final TextKey ACTIVE = TextKey.of("jsc.cli.unix_fdisk.active", "Active");
    private static final TextKey UPDATED = TextKey.of("jsc.cli.unix_fdisk.updated",
            "The disk configuration is updated.");
    private static final TextKey CANCELLED = TextKey.of("jsc.cli.unix_fdisk.cancelled",
            "The disk configuration is not changed.");
    private static final TextKey PARTLY_UPDATED = TextKey.of("jsc.cli.unix_fdisk.partly_updated",
            "The disk configuration is updated only in part: the changes above were refused.");
    private static final TextKey INVALID = TextKey.of("jsc.cli.unix_fdisk.invalid", "Invalid selection.");

    UnixFdiskProcess(final ICliComputer computer) {
        super(computer);
        final DiskReport.Disk disk = this.report().disk(DISK);
        this.draft = disk == null ? DiskLayout.empty(PartitionScheme.MBR, 0L) : disk.layout();
    }

    @Override
    protected void open(final ITtySink out) {
        this.table(out);
    }

    @Override
    protected CliLine question() {
        return switch (this.stage) {
            case MENU -> CliLine.of(ENTER, CliStyle.OK);
            case PERCENT -> CliLine.of(ASK_PERCENT, CliStyle.OK);
            case ACTIVE -> CliLine.of(ASK_ACTIVE, CliStyle.OK);
            case DELETE -> CliLine.of(ASK_DELETE, CliStyle.OK);
        };
    }

    @Override
    protected void handle(final String line, @Nullable final ITtySink out) {
        switch (this.stage) {
            case MENU -> this.choose(line, out);
            case PERCENT -> this.create(number(line), out);
            case ACTIVE -> this.keep(this.draft.activate(number(line)), DiskCommand.active(DISK, number(line)), out);
            case DELETE -> {
                final DiskReport.Disk disk = this.report().disk(DISK);
                final DiskReport.Part part = disk == null ? null : disk.part(number(line));
                if (part != null && part.mounted()) {
                    say(out, new CliLine(reason(DiskReport.Refusal.MOUNTED), CliStyle.ERROR));
                    this.stage = Stage.MENU;
                    this.table(out);
                    return;
                }
                this.keep(this.draft.delete(number(line)), DiskCommand.delete(DISK, number(line)), out);
            }
        }
    }

    private void choose(final String line, @Nullable final ITtySink out) {
        switch (line) {
            case "1" -> this.stage = Stage.PERCENT;
            case "2" -> this.stage = Stage.ACTIVE;
            case "3" -> this.stage = Stage.DELETE;
            case "4" -> {
                boolean refused = false;
                for (final String path : this.pending) {
                    final DiskReport.Refusal refusal = this.change(path);
                    if (refusal != null) {
                        refused = true;
                        say(out, new CliLine(reason(refusal), CliStyle.ERROR));
                    }
                }
                // Updated is said only of a table written whole; one the disk refused a part of says so.
                say(out, refused ? CliLine.of(PARTLY_UPDATED, CliStyle.ERROR) : CliLine.of(UPDATED, CliStyle.OK));
                this.finish();
            }
            case "5" -> {
                say(out, CliLine.of(CANCELLED, CliStyle.OK));
                this.finish();
            }
            default -> say(out, CliLine.of(INVALID, CliStyle.ERROR));
        }
    }

    /* A UNIX partition of that share of the largest free space, formatted UFS when written. */
    private void create(final int percent, @Nullable final ITtySink out) {
        DiskLayout.FreeSpace largest = null;
        for (final DiskLayout.FreeSpace gap : this.draft.free()) {
            if (largest == null || gap.sizeMb() > largest.sizeMb()) {
                largest = gap;
            }
        }
        final long sizeMb = largest == null ? 0L : largest.sizeMb() * Math.max(0, Math.min(100, percent)) / 100L;
        if (largest == null || sizeMb <= 0L) {
            say(out, new CliLine(reason(DiskReport.Refusal.NO_ROOM), CliStyle.ERROR));
            this.stage = Stage.MENU;
            this.table(out);
            return;
        }
        final String fs = FileSystemType.UFS.serializedName();
        this.keep(this.draft.create(largest.startMb(), sizeMb, FileSystemType.UFS, PartitionRole.DATA),
                DiskCommand.create(DISK, largest.startMb(), sizeMb, fs), out);
    }

    /* Keeps a change in the draft, to be written on Exit, or says why it cannot be made. */
    private void keep(final DiskLayout.Change change, final String path, @Nullable final ITtySink out) {
        if (change.ok() && change.layout() != null) {
            this.draft = change.layout();
            this.pending.add(path);
        } else {
            say(out, new CliLine(reason(change.refusal() == null ? DiskReport.Refusal.NO_ROOM
                    : DiskReport.Refusal.of(change.refusal())), CliStyle.ERROR));
        }
        this.stage = Stage.MENU;
        this.table(out);
    }

    /* The draft's partitions in cylinders, and the menu under them. */
    private void table(@Nullable final ITtySink out) {
        final long size = Math.max(1L, this.draft.diskMb());
        say(out, CliLine.plain(""));
        say(out, new CliLine(TOTAL.with(size, size), CliStyle.OK));
        say(out, CliLine.plain(""));
        say(out, CliLine.of(HEADER, CliStyle.OK));
        say(out, CliLine.of(RULE, CliStyle.OK));
        for (final PartitionEntry partition : this.draft.partitions()) {
            final FileSystemType fs = partition.fs();
            final String type = fs == FileSystemType.UFS ? "UNIX" : fs != null && fs.name().startsWith("FAT") ? "DOS"
                    : "Other";
            say(out, CliLine.build().plain(String.format(Locale.ROOT, "%9d   ", partition.number()))
                    .add(partition.active() ? ACTIVE.text() : Text.literal("      "), CliStyle.OK)
                    .plain(String.format(Locale.ROOT, "    %-6s %6d %5d %8d %5d", type, partition.startMb(),
                            partition.endMb() - 1, partition.sizeMb(), partition.sizeMb() * 100L / size))
                    .done());
        }
        say(out, CliLine.plain(""));
        say(out, CliLine.of(SELECT, CliStyle.OK));
        say(out, CliLine.plain(""));
        for (final TextKey choice : List.of(CREATE, CHANGE, DELETE, EXIT, CANCEL)) {
            say(out, CliLine.of(choice, CliStyle.OK));
        }
        say(out, CliLine.plain(""));
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
        MENU, PERCENT, ACTIVE, DELETE
    }
}
