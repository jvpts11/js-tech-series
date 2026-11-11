/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.disk.DiskCommand;
import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.DiskReport;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * Frames' DiskPart (computers/docs/design/storage-devices.md): its DISKPART&gt; prompt and its commands, a disk and a
 * partition selected and acted on as the real one does. A change it cannot make is a Virtual Disk Service error, with
 * the reason under it: a disk converted only empty, a partition the system runs from never deleted nor formatted.
 */
@TextHolder
final class DiskpartProcess extends DiskToolProcess {

    private int disk = -1;
    private int partition = -1;

    private static final TextKey PROMPT = TextKey.of("jsc.cli.diskpart.prompt", "DISKPART> ");
    private static final TextKey VERSION = TextKey.of("jsc.cli.diskpart.version", "Midsoft DiskPart version %s");
    private static final TextKey DISK_HEADER = TextKey.of("jsc.cli.diskpart.disk_header",
            "  Disk ###  Status         Size     Free     Dyn  Gpt");
    private static final TextKey DISK_RULE = TextKey.of("jsc.cli.diskpart.disk_rule",
            "  --------  -------------  -------  -------  ---  ---");
    private static final TextKey PART_HEADER = TextKey.of("jsc.cli.diskpart.part_header",
            "  Partition ###  Type              Size     Offset");
    private static final TextKey PART_RULE = TextKey.of("jsc.cli.diskpart.part_rule",
            "  -------------  ----------------  -------  -------");
    private static final TextKey ONLINE = TextKey.of("jsc.cli.diskpart.online", "Online");
    private static final TextKey SYSTEM = TextKey.of("jsc.cli.diskpart.system", "System");
    private static final TextKey PRIMARY = TextKey.of("jsc.cli.diskpart.primary", "Primary");
    private static final TextKey UNKNOWN = TextKey.of("jsc.cli.diskpart.unknown", "Unknown");
    private static final TextKey DISK_SELECTED = TextKey.of("jsc.cli.diskpart.disk_selected",
            "Disk %s is now the selected disk.");
    private static final TextKey PART_SELECTED = TextKey.of("jsc.cli.diskpart.part_selected",
            "Partition %s is now the selected partition.");
    private static final TextKey NO_DISK = TextKey.of("jsc.cli.diskpart.no_disk",
            "There is no disk selected. Select a disk and try again.");
    private static final TextKey NO_PARTITION = TextKey.of("jsc.cli.diskpart.no_partition",
            "There is no partition selected. Select a partition and try again.");
    private static final TextKey NOT_VALID = TextKey.of("jsc.cli.diskpart.not_valid",
            "The arguments specified for this command are not valid.");
    private static final TextKey VDS_ERROR = TextKey.of("jsc.cli.diskpart.vds_error", "Virtual Disk Service error:");
    private static final TextKey CREATED = TextKey.of("jsc.cli.diskpart.created",
            "DiskPart succeeded in creating the specified partition.");
    private static final TextKey DELETED = TextKey.of("jsc.cli.diskpart.deleted",
            "DiskPart successfully deleted the selected partition.");
    private static final TextKey FORMATTED = TextKey.of("jsc.cli.diskpart.formatted",
            "DiskPart successfully formatted the volume.");
    private static final TextKey EXTENDED = TextKey.of("jsc.cli.diskpart.extended",
            "DiskPart successfully extended the volume.");
    private static final TextKey SHRUNK = TextKey.of("jsc.cli.diskpart.shrunk",
            "DiskPart successfully shrunk the volume by: %s MB");
    private static final TextKey ACTIVE = TextKey.of("jsc.cli.diskpart.active",
            "DiskPart marked the current partition as active.");
    private static final TextKey CONVERTED = TextKey.of("jsc.cli.diskpart.converted",
            "DiskPart successfully converted the selected disk to %s format.");
    private static final TextKey LEAVING = TextKey.of("jsc.cli.diskpart.leaving", "Leaving DiskPart...");
    private static final TextKey HELP = TextKey.of("jsc.cli.diskpart.help", "list disk, select disk N, list"
            + " partition, select partition N, create partition primary [size=N], delete partition, format fs=ntfs"
            + " [quick] [label=NAME], extend [size=N], shrink desired=N, active, convert gpt|mbr, exit");
    /* The DiskPart each Frames shipped, by its place in the family: XP, 7, 10 and 11. */
    private static final String[] VERSIONS = {"5.1.2600", "5.1.2600", "6.1.7601", "10.0.10240", "10.0.22000"};

    DiskpartProcess(final ICliComputer computer) {
        super(computer);
    }

    @Override
    protected void open(final ITtySink out) {
        final int edition = Math.max(1, Math.min(VERSIONS.length, this.computer.osEdition()));
        say(out, new CliLine(VERSION.with(VERSIONS[edition - 1]), CliStyle.ACCENT));
        say(out, CliLine.plain(""));
    }

    @Override
    protected CliLine question() {
        return CliLine.of(PROMPT, CliStyle.ACCENT);
    }

    @Override
    protected void handle(final String line, @Nullable final ITtySink out) {
        final String typed = line.toLowerCase(Locale.ROOT);
        final String[] words = typed.split("\\s+");
        if (typed.isEmpty()) {
            return;
        }
        switch (words[0]) {
            case "list" -> this.list(words, out);
            case "select", "sel" -> this.select(words, out);
            case "create" -> this.create(words, out);
            case "delete" -> {
                if (this.done(DiskCommand.delete(this.disk, this.partition), DELETED.text(), out, true)) {
                    this.partition = -1;
                }
            }
            case "format" -> this.format(words, line, out);
            case "extend" -> this.extend(words, out);
            case "shrink" -> this.shrink(words, out);
            case "active" -> this.done(DiskCommand.active(this.disk, this.partition), ACTIVE.text(), out, true);
            case "convert" -> this.convert(words, out);
            case "help", "?" -> say(out, CliLine.plain(HELP.text()));
            case "exit" -> {
                say(out, CliLine.plain(LEAVING.text()));
                this.finish();
            }
            default -> say(out, new CliLine(NOT_VALID.text(), CliStyle.ERROR));
        }
        say(out, CliLine.plain(""));
    }

    private void list(final String[] words, @Nullable final ITtySink out) {
        final DiskReport report = this.report();
        if (words.length > 1 && words[1].startsWith("disk")) {
            say(out, CliLine.of(DISK_HEADER, CliStyle.PLAIN));
            say(out, CliLine.of(DISK_RULE, CliStyle.PLAIN));
            for (final DiskReport.Disk each : report.disks()) {
                say(out, CliLine.build().plain(String.format(Locale.ROOT, "%s Disk %-4d ", each.index() == this.disk
                                ? "*" : " ", each.index())).add(ONLINE, CliStyle.PLAIN)
                        .plain(String.format(Locale.ROOT, "%14s %8s  %3s  %3s", size(each.sizeMb()),
                                size(each.layout().freeMb()), "", each.scheme() == PartitionScheme.GPT ? "*" : ""))
                        .done());
            }
            return;
        }
        final DiskReport.Disk chosen = report.disk(this.disk);
        if (chosen == null) {
            say(out, new CliLine(NO_DISK.text(), CliStyle.ERROR));
            return;
        }
        say(out, CliLine.of(PART_HEADER, CliStyle.PLAIN));
        say(out, CliLine.of(PART_RULE, CliStyle.PLAIN));
        for (final DiskReport.Part part : chosen.parts()) {
            say(out, CliLine.build().plain(String.format(Locale.ROOT, "%s Partition %-4d ",
                            part.number() == this.partition ? "*" : " ", part.number()))
                    .add(typeOf(part), CliStyle.PLAIN)
                    .plain(String.format(Locale.ROOT, "%17s %8s", size(part.sizeMb()), size(part.startMb() + 1L)))
                    .done());
        }
    }

    private void select(final String[] words, @Nullable final ITtySink out) {
        final int number = words.length > 2 ? parse(words[2]) : -1;
        if (words.length > 1 && words[1].startsWith("disk")) {
            if (this.report().disk(number) == null) {
                say(out, new CliLine(NOT_VALID.text(), CliStyle.ERROR));
                return;
            }
            this.disk = number;
            this.partition = -1;
            say(out, CliLine.plain(DISK_SELECTED.with(number)));
            return;
        }
        final DiskReport.Disk chosen = this.report().disk(this.disk);
        if (chosen == null) {
            say(out, new CliLine(NO_DISK.text(), CliStyle.ERROR));
            return;
        }
        if (chosen.part(number) == null) {
            say(out, new CliLine(NOT_VALID.text(), CliStyle.ERROR));
            return;
        }
        this.partition = number;
        say(out, CliLine.plain(PART_SELECTED.with(number)));
    }

    /* create partition primary [size=N]: a raw partition in the largest free space, selected once made. */
    private void create(final String[] words, @Nullable final ITtySink out) {
        final DiskReport.Disk chosen = this.report().disk(this.disk);
        if (chosen == null) {
            say(out, new CliLine(NO_DISK.text(), CliStyle.ERROR));
            return;
        }
        DiskLayout.FreeSpace largest = null;
        for (final DiskLayout.FreeSpace gap : chosen.layout().free()) {
            if (largest == null || gap.sizeMb() > largest.sizeMb()) {
                largest = gap;
            }
        }
        final long asked = option(words, "size=");
        final long sizeMb = largest == null ? 0L : asked > 0L ? Math.min(asked, largest.sizeMb()) : largest.sizeMb();
        if (largest == null || this.refused(DiskCommand.create(this.disk, largest.startMb(), sizeMb, ""), out)) {
            if (largest == null) {
                this.vds(DiskReport.Refusal.NO_ROOM, out);
            }
            return;
        }
        final DiskReport.Disk after = this.report().disk(this.disk);
        if (after != null) {
            for (final DiskReport.Part part : after.parts()) {
                if (part.startMb() == largest.startMb()) {
                    this.partition = part.number();
                }
            }
        }
        say(out, new CliLine(CREATED.text(), CliStyle.PLAIN));
    }

    /* format fs=ntfs [quick] [label=NAME]: the selected partition, and its label after. */
    private void format(final String[] words, final String line, @Nullable final ITtySink out) {
        String fs = "ntfs";
        String label = "";
        for (final String word : words) {
            if (word.startsWith("fs=")) {
                fs = word.substring(3);
            }
        }
        final int at = line.toLowerCase(Locale.ROOT).indexOf("label=");
        if (at >= 0) {
            label = line.substring(at + 6).replace("\"", "").strip();
        }
        final FileSystemType type = switch (fs) {
            case "fat" -> FileSystemType.FAT16;
            case "fat32" -> FileSystemType.FAT32;
            case "exfat" -> FileSystemType.EXFAT;
            default -> FileSystemType.NTFS;
        };
        if (this.refused(DiskCommand.format(this.disk, this.partition, type.serializedName()), out)) {
            return;
        }
        if (!label.isEmpty()) {
            this.change(DiskCommand.label(this.disk, this.partition, label));
        }
        say(out, CliLine.plain("  100 percent completed"));
        say(out, CliLine.plain(""));
        say(out, CliLine.plain(FORMATTED.text()));
    }

    private void extend(final String[] words, @Nullable final ITtySink out) {
        final DiskReport.Part part = this.selected(out);
        if (part == null) {
            return;
        }
        final DiskReport.Disk chosen = this.report().disk(this.disk);
        final long room = chosen == null ? 0L : chosen.layout().roomAfter(part.entry());
        final long asked = option(words, "size=");
        this.done(DiskCommand.resize(this.disk, part.number(), part.sizeMb() + (asked > 0L ? asked : room)),
                EXTENDED.text(), out, false);
    }

    private void shrink(final String[] words, @Nullable final ITtySink out) {
        final DiskReport.Part part = this.selected(out);
        final long by = option(words, "desired=");
        if (part == null) {
            return;
        }
        if (by <= 0L) {
            say(out, new CliLine(NOT_VALID.text(), CliStyle.ERROR));
            return;
        }
        this.done(DiskCommand.resize(this.disk, part.number(), part.sizeMb() - by), SHRUNK.with(by), out, false);
    }

    private void convert(final String[] words, @Nullable final ITtySink out) {
        if (this.report().disk(this.disk) == null) {
            say(out, new CliLine(NO_DISK.text(), CliStyle.ERROR));
            return;
        }
        final PartitionScheme scheme = words.length > 1 && "mbr".equals(words[1]) ? PartitionScheme.MBR
                : PartitionScheme.GPT;
        this.done(DiskCommand.convert(this.disk, scheme), CONVERTED.with(scheme.name()), out, false);
    }

    /* Asks for a change and says it was made, or why not; answers whether it was made. */
    private boolean done(final String path, final Text said, @Nullable final ITtySink out, final boolean needsPart) {
        if (needsPart && this.selected(out) == null || this.refused(path, out)) {
            return false;
        }
        say(out, CliLine.plain(said));
        return true;
    }

    /* Asks for a change; on a refusal prints the Virtual Disk Service error and answers true. */
    private boolean refused(final String path, @Nullable final ITtySink out) {
        final DiskReport.Refusal refusal = this.change(path);
        if (refusal == null) {
            return false;
        }
        this.vds(refusal, out);
        return true;
    }

    private void vds(final DiskReport.Refusal refusal, @Nullable final ITtySink out) {
        say(out, new CliLine(VDS_ERROR.text(), CliStyle.ERROR));
        say(out, new CliLine(reason(refusal), CliStyle.ERROR));
    }

    @Nullable
    private DiskReport.Part selected(@Nullable final ITtySink out) {
        final DiskReport.Disk chosen = this.report().disk(this.disk);
        final DiskReport.Part part = chosen == null ? null : chosen.part(this.partition);
        if (part == null) {
            say(out, new CliLine((chosen == null ? NO_DISK : NO_PARTITION).text(), CliStyle.ERROR));
        }
        return part;
    }

    /* A partition's type as DiskPart lists it: System for the EFI one, Primary for what Frames reads. */
    private static Text typeOf(final DiskReport.Part part) {
        if (part.role() == PartitionRole.EFI) {
            return SYSTEM.text();
        }
        final FileSystemType fs = part.fileSystem();
        return fs != null && fs.readBy(Platform.FRAMES, HardwareEra.ADVANCED) ? PRIMARY.text() : UNKNOWN.text();
    }

    /* A size as DiskPart prints it: in GB from a gigabyte, in MB under it. */
    private static String size(final long mb) {
        return mb >= 1_024L ? mb / 1_024L + " GB" : mb + " MB";
    }

    private static long option(final String[] words, final String name) {
        for (final String word : words) {
            if (word.startsWith(name)) {
                return Math.max(0L, parse(word.substring(name.length())));
            }
        }
        return 0L;
    }

    private static int parse(final String text) {
        try {
            return Integer.parseInt(text);
        } catch (final NumberFormatException e) {
            return -1;
        }
    }
}
