/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import static dev.jstech.computers.client.FirmwareScreenTexts.of;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.PartitionEntry;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.computers.os.install.DiskChoices;
import dev.jstech.computers.os.install.DiskSpot;
import dev.jstech.computers.os.install.InstallPlan;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.Placement;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The disk step of the installers that list partitions, and the pages that come with it (computers/docs/design/
 * storage-devices.md): Frames XP's text phase and its format page, the table of Frames 7 to 11 with its New, Delete,
 * Format and Extend, Ubuntu's guided storage, bsdinstall's ZFS Configuration, and the partitions under each disk of
 * the installers that list whole disks. It draws; the screen holds the cursor and tells the machine what was chosen.
 */
final class InstallerDiskPages {

    /* Where the columns of the table of Frames 7 to 11 start, in hundredths of its width. */
    private static final int TABLE_SIZE_AT = 45;
    private static final int TABLE_FREE_AT = 59;
    private static final int TABLE_HOLDS_AT = 73;
    /* Frames XP's list: the letter, the partition and where its size ends, in hundredths of the box's width. */
    private static final int XP_LETTER_AT = 6;
    private static final int XP_NAME_AT = 13;
    private static final int XP_SIZE_ENDS = 66;
    /* The gap between the links under the table, and between a link and its neighbour's start. */
    private static final int LINK_GAP = 14;
    /* bsdinstall's ZFS Configuration: where its label and its value start, in hundredths of the box's width. */
    private static final int POOL_LABEL_AT = 12;
    private static final int POOL_VALUE_AT = 55;
    /* Ubuntu's sub-lines start this many characters in, under the label of their choice. */
    private static final String UBUNTU_INDENT = "      ";
    /* The letter Frames gives the first partition it reads; D: is the CD drive the installer runs from. */
    private static final char FIRST_LETTER = 'C';
    private static final char CD_LETTER = 'D';
    private static final int MB_PER_GB = 1_024;

    private InstallerDiskPages() {
    }

    /** Frames XP's text phase: the partitions and the unpartitioned space of every disk, each disk under its line. */
    static Drawn xpDisks(final GuiGraphics g, final IInstallerPen pen, final InstallerFrames.Frame f,
                         final InstallerFlow flow, final int selection) {
        final InstallerFrames.Paint p = f.paint();
        final int step = pen.row();
        int ty = f.y() + step;
        pen.say(g, of(InstallerScreenTexts.XP_INTRO_KEYS), f.x(), ty, p.text());
        ty += step * 2;
        pen.say(g, of(InstallerScreenTexts.XP_INTRO_SET_UP.with(flow.systemName())), f.x() + 8, ty, p.text());
        ty += step;
        pen.say(g, of(InstallerScreenTexts.XP_INTRO_DELETE), f.x() + 8, ty, p.text());
        ty += step * 2;
        final List<DiskChoices.Pick> picks = DiskChoices.picks(flow);
        final Map<Integer, String> letters = framesLetters(picks);
        final InstallerFrames.Paint box = InstallerFrames.boxedList(flow.style());
        final List<Integer> rowMap = new ArrayList<>();
        int lines = 0;
        int lastDisk = -1;
        for (final DiskChoices.Pick pick : picks) {
            if (pick.diskIndex() != lastDisk) {
                lines++;
                lastDisk = pick.diskIndex();
            }
            lines++;
        }
        final int bx = f.x() + 4;
        final int bw = f.w() - 8;
        g.fill(bx, ty - 2, bx + bw, ty + lines * step, InstallerFrames.listBox(flow.style()));
        final int top = ty;
        lastDisk = -1;
        for (int i = 0; i < picks.size(); i++) {
            final DiskChoices.Pick pick = picks.get(i);
            if (pick.diskIndex() != lastDisk) {
                lastDisk = pick.diskIndex();
                pen.say(g, of(InstallerScreenTexts.XP_DISK_LINE.with(pick.disk().sizeMb(), pick.diskIndex(),
                        schemeWord(pick.disk().scheme()))), bx + 4, ty, box.text());
                rowMap.add(-1);
                ty += step;
            }
            final boolean here = i == selection;
            if (here) {
                g.fill(bx, ty - 1, bx + bw, ty + step - 1, box.select());
            }
            final int ink = here ? box.selectText() : box.text();
            xpRow(g, pen, bx, bw, ty, pick, letters.getOrDefault(i, ""), ink);
            rowMap.add(i);
            ty += step;
        }
        ty += step;
        final InstallPlan.Plan plan = flow.plan();
        final String note = plan.ok()
                ? of(InstallerScreenTexts.XP_LEAVES_OTHERS.with(flow.systemName(), whereXp(flow, picks, letters)))
                : of(InstallPlan.reason(plan.problem(), flow.systemName()));
        paragraph(g, pen, note, f.x(), ty, f.w(), plan.ok() ? p.text() : p.accent());
        return new Drawn(top, rowMap, List.of());
    }

    /** Frames XP's format page: the partition chosen, then how it is formatted, or left as it is. */
    static Drawn xpFormat(final GuiGraphics g, final IInstallerPen pen, final InstallerFrames.Frame f,
                          final InstallerFlow flow, final int selection) {
        final InstallerFrames.Paint p = f.paint();
        final int step = pen.row();
        final List<DiskChoices.Pick> picks = DiskChoices.picks(flow);
        final Map<Integer, String> letters = framesLetters(picks);
        final int chosen = DiskChoices.indexOfChosen(flow, picks);
        int ty = f.y();
        if (!picks.isEmpty()) {
            final DiskChoices.Pick pick = picks.get(chosen);
            xpRow(g, pen, f.x() + 8, f.w() - 16, ty, pick, letters.getOrDefault(chosen, ""), p.bright());
            ty += step * 2;
            pen.say(g, of(InstallerScreenTexts.XP_ON_DISK.with(pick.disk().sizeMb(), pick.diskIndex(),
                    schemeWord(pick.disk().scheme()))), f.x(), ty, p.text());
            ty += step * 2;
        }
        ty = paragraph(g, pen, of(InstallerScreenTexts.XP_FORMAT_HELP.with(flow.systemName())), f.x(), ty, f.w(),
                p.text()) + step;
        final List<DiskChoices.Format> formats = DiskChoices.xpFormats(flow);
        final InstallerFrames.Paint box = InstallerFrames.boxedList(flow.style());
        final int bx = f.x() + 4;
        final int bw = f.w() - 8;
        g.fill(bx, ty - 2, bx + bw, ty + formats.size() * step, InstallerFrames.listBox(flow.style()));
        final int top = ty;
        final List<Integer> rowMap = new ArrayList<>();
        boolean fat32 = false;
        for (int i = 0; i < formats.size(); i++) {
            final DiskChoices.Format format = formats.get(i);
            final boolean here = i == selection;
            if (here) {
                g.fill(bx, ty - 1, bx + bw, ty + step - 1, box.select());
            }
            fat32 |= FileSystemType.FAT32.serializedName().equals(format.fileSystem());
            pen.say(g, xpFormatLabel(format), bx + 4, ty, here ? box.selectText() : box.text());
            rowMap.add(i);
            ty += step;
        }
        ty += step;
        final String where = picks.isEmpty() ? "" : picks.get(chosen).spot().isFree()
                ? of(InstallerScreenTexts.XP_THE_UNPARTITIONED) : letters.getOrDefault(chosen,
                "Partition" + picks.get(chosen).spot().number());
        String note = of(InstallerScreenTexts.XP_ONLY_FORMATTED.with(where));
        if (fat32) {
            note = note + " " + of(InstallerScreenTexts.XP_FAT32_NOTE);
        }
        paragraph(g, pen, note, f.x(), ty, f.w(), p.text());
        return new Drawn(top, rowMap, List.of());
    }

    /**
     * The table of Frames 7 to 11: every partition and free space of every disk with its size, what is free in it and
     * what it holds, the four links under it, and what Next does with the row chosen.
     */
    static Drawn table(final GuiGraphics g, final IInstallerPen pen, final InstallerFrames.Frame f,
                       final InstallerFlow flow, final int selection, final int columnRule) {
        final InstallerFrames.Paint p = f.paint();
        final int step = pen.row();
        final List<DiskChoices.Pick> picks = DiskChoices.picks(flow);
        final Map<Integer, String> letters = framesLetters(picks);
        final int sizeAt = f.x() + f.w() * TABLE_SIZE_AT / 100;
        final int freeAt = f.x() + f.w() * TABLE_FREE_AT / 100;
        final int holdsAt = f.x() + f.w() * TABLE_HOLDS_AT / 100;
        int ty = f.y();
        pen.say(g, of(InstallerScreenTexts.COLUMN_DISK), f.x() + 4, ty, p.dim());
        pen.say(g, of(InstallerScreenTexts.COLUMN_SIZE), sizeAt, ty, p.dim());
        pen.say(g, of(InstallerScreenTexts.COLUMN_FREE), freeAt, ty, p.dim());
        pen.say(g, of(InstallerScreenTexts.COLUMN_HOLDS), holdsAt, ty, p.dim());
        g.fill(f.x(), ty + step - 2, f.x() + f.w(), ty + step - 1, columnRule);
        ty += step + 2;
        // What is left under the rows: the links and three lines of what Next does.
        final int fits = Math.max(1, (f.h() - (ty - f.y()) - step * 5) / step);
        final int first = Math.max(0, Math.min(selection - fits + 1, picks.size() - fits));
        final int top = ty;
        final List<Integer> rowMap = new ArrayList<>();
        for (int i = first; i < Math.min(picks.size(), first + fits); i++) {
            final DiskChoices.Pick pick = picks.get(i);
            final boolean here = i == selection;
            if (here) {
                g.fill(f.x(), ty - 1, f.x() + f.w(), ty + step - 1, p.select());
                g.fill(f.x(), ty - 1, f.x() + 2, ty + step - 1, p.accent());
            }
            final int ink = here ? p.selectText() : p.text();
            final DiskSpot spot = pick.spot();
            pen.say(g, pen.fit(tableName(pick, letters.getOrDefault(i, "")), sizeAt - f.x() - 8), f.x() + 4, ty, ink);
            // Each cell is fitted to its column, so a big disk's sizes never run into the next.
            pen.say(g, pen.fit(tableSize(spot.sizeMb()), freeAt - sizeAt - 4), sizeAt, ty, ink);
            if (spot.isFree() || spot.holdsData() || spot.role() == PartitionRole.EFI) {
                pen.say(g, pen.fit(tableSize(spot.freeMb()), holdsAt - freeAt - 4), freeAt, ty, ink);
            }
            pen.say(g, pen.fit(tableHolds(spot), f.x() + f.w() - holdsAt), holdsAt, ty, ink);
            rowMap.add(i);
            ty += step;
        }
        ty += step / 2;
        final DiskChoices.Pick chosen = picks.isEmpty() ? null : picks.get(Math.min(selection, picks.size() - 1));
        final List<int[]> links = new ArrayList<>();
        int lx = f.x();
        final String[] words = {of(InstallerScreenTexts.LINK_NEW), of(InstallerScreenTexts.LINK_DELETE),
                of(InstallerScreenTexts.LINK_FORMAT), of(InstallerScreenTexts.LINK_EXTEND)};
        for (int link = 0; link < words.length; link++) {
            final boolean on = chosen != null && linkWorks(link, chosen);
            pen.say(g, words[link], lx, ty, on ? p.accent() : p.dim());
            links.add(on ? new int[]{lx, ty - 1, pen.width(words[link]), step} : null);
            lx += pen.width(words[link]) + LINK_GAP;
        }
        ty += step + step / 2;
        paragraph(g, pen, nextSays(flow), f.x(), ty, f.w(), flow.plan().ok() ? p.dim() : p.accent());
        return new Drawn(top, rowMap, links);
    }

    /** Which of the table's links does something for the row chosen: New, Delete, Format and Extend, in that order. */
    static boolean linkWorks(final int link, final DiskChoices.Pick pick) {
        final DiskSpot spot = pick.spot();
        return switch (link) {
            case Link.NEW -> spot.isFree() && !pick.disk().layout().full();
            case Link.DELETE, Link.FORMAT -> !spot.isFree();
            case Link.EXTEND -> !spot.isFree() && spot.fileSystem() != null && spot.fileSystem().resizes(true, false)
                    && roomAfter(pick) > 0L;
            default -> false;
        };
    }

    /** Ubuntu's guided storage: its three ways, the one beside another system with the sizes it makes. */
    static Drawn ubuntu(final GuiGraphics g, final IInstallerPen pen, final InstallerFrames.Frame f,
                        final InstallerFlow flow, final int selection) {
        final InstallerFrames.Paint p = f.paint();
        final int step = pen.row();
        int ty = f.y();
        pen.say(g, of(InstallerScreenTexts.UBUNTU_CHOOSE.with(flow.systemName())), f.x(), ty, p.text());
        ty += step * 2;
        final List<DiskChoices.Option> options = DiskChoices.ubuntuOptions(flow);
        final DiskChoices.Option.Kind chosen = DiskChoices.ubuntuChosen(flow);
        final InstallerFlow.Disk target = flow.target();
        final List<Integer> rowMap = new ArrayList<>();
        final int top = ty;
        final InstallPlan.Plan plan = flow.plan();
        for (int i = 0; i < options.size(); i++) {
            final DiskChoices.Option option = options.get(i);
            final boolean on = option.kind() == chosen && (option.kind() == DiskChoices.Option.Kind.CUSTOM
                    || option.disk() != null && target != null && option.disk().slot() == target.slot());
            final boolean here = i == selection;
            pen.say(g, on ? "(X)" : "( )", f.x() + 8, ty, on ? p.accent() : p.text());
            pen.say(g, pen.fit(ubuntuLabel(flow, option), f.w() - 40), f.x() + 30, ty, here ? p.bright() : p.text());
            rowMap.add(i);
            ty += step;
            if (on && option.kind() == DiskChoices.Option.Kind.ALONGSIDE && plan.ok()) {
                ty = ubuntuBeside(g, pen, f, flow, plan, ty, rowMap);
            }
        }
        ty += step;
        String says = of(InstallerScreenTexts.NEEDS.with(flow.systemName(), tableSize(flow.footprintMb())));
        final DiskSpot other = flow.besideSpot();
        if (plan.ok() && chosen == DiskChoices.Option.Kind.ALONGSIDE && other != null) {
            says += " " + of(InstallerScreenTexts.GRUB_LISTS.with(other.holds(), flow.systemName()));
        }
        if (plan.ok() && plan.layout() != null && plan.layout().scheme() == PartitionScheme.MBR) {
            says += " " + of(InstallerScreenTexts.MBR_PARTITIONS.with(plan.layout().partitions().size()));
        }
        if (!plan.ok()) {
            says += " " + of(InstallPlan.reason(plan.problem(), flow.systemName()));
        }
        paragraph(g, pen, says, f.x(), ty, f.w(), plan.ok() ? p.text() : p.accent());
        return new Drawn(top, rowMap, List.of());
    }

    /** bsdinstall's ZFS Configuration: Install and the pool's disks to choose, the rest as it sets them. */
    static Drawn pool(final GuiGraphics g, final IInstallerPen pen, final InstallerFrames.Frame f,
                      final InstallerFlow flow, final int selection) {
        final InstallerFrames.Paint p = f.paint();
        final int step = pen.row();
        int ty = f.y();
        pen.say(g, of(InstallerScreenTexts.POOL_CONFIGURE), f.x(), ty, p.text());
        ty += step + step / 2;
        final boolean mirror = flow.diskAt(flow.placement().mirrorSlot()) != null;
        final String no = of(InstallerScreenTexts.POOL_NO);
        final String[][] rows = {
                {">>>", of(InstallerScreenTexts.POOL_INSTALL), of(InstallerScreenTexts.POOL_PROCEED)},
                {"T", of(InstallerScreenTexts.POOL_TYPE), of(mirror ? InstallerScreenTexts.POOL_MIRROR
                        : InstallerScreenTexts.POOL_STRIPE)},
                {"-", of(InstallerScreenTexts.POOL_RESCAN), "*"},
                {"-", of(InstallerScreenTexts.POOL_DISK_INFO), "*"},
                {"N", of(InstallerScreenTexts.POOL_NAME), "zroot"},
                {"4", of(InstallerScreenTexts.POOL_FORCE_4K), of(InstallerScreenTexts.POOL_YES)},
                {"E", of(InstallerScreenTexts.POOL_ENCRYPT), no},
                {"P", of(InstallerScreenTexts.POOL_SCHEME), of(flow.facts().scheme() == PartitionScheme.GPT
                        ? InstallerScreenTexts.POOL_GPT : InstallerScreenTexts.POOL_MBR)},
                {"S", of(InstallerScreenTexts.POOL_SWAP), swapSize(flow.facts().swapMb())},
                {"M", of(InstallerScreenTexts.POOL_MIRROR_SWAP), no},
                {"W", of(InstallerScreenTexts.POOL_ENCRYPT_SWAP), no},
        };
        final int top = ty;
        final List<Integer> rowMap = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            final boolean chooses = i < Pool.CHOICES;
            final boolean here = chooses && i == selection;
            if (here) {
                g.fill(f.x() + f.w() * POOL_LABEL_AT / 100 - 2, ty - 1, f.x() + f.w(), ty + step - 1, p.select());
            }
            pen.say(g, rows[i][0], f.x() + 4, ty, p.accent());
            pen.say(g, rows[i][1], f.x() + f.w() * POOL_LABEL_AT / 100, ty, here ? p.selectText() : p.text());
            pen.say(g, rows[i][2], f.x() + f.w() * POOL_VALUE_AT / 100, ty, here ? p.selectText() : p.text());
            rowMap.add(chooses ? i : -1);
            ty += step;
        }
        return new Drawn(top, rowMap, List.of());
    }

    /**
     * Under each disk of an installer that lists whole disks, its partitions and its free space, dim, as Debian's
     * partitioner and Fedora's destination list them.
     *
     * @return where the next line goes
     */
    static int partitionsUnder(final GuiGraphics g, final IInstallerPen pen, final InstallerFrames.Frame f,
                               final InstallerFlow flow, final InstallerFlow.Disk disk, final int y,
                               final List<Integer> rowMap) {
        final InstallerFrames.Paint p = f.paint();
        int ty = y;
        for (final DiskSpot spot : flow.spotsOn(disk)) {
            final String line = spot.isFree() ? of(InstallerScreenTexts.FREE_LINE.with(tableSize(spot.sizeMb())))
                    : of(InstallerScreenTexts.PART_LINE.with(spot.number(), tableSize(spot.sizeMb()),
                    spot.fileSystem() == null ? "" : spot.fileSystem().displayName(), tableHolds(spot)));
            pen.say(g, pen.fit(line, f.w() - 16), f.x() + 16, ty, p.dim());
            rowMap.add(-1);
            ty += pen.row();
        }
        return ty;
    }

    /** What the installer does with the disk chosen, in a sentence, or why it cannot. */
    static String planSays(final InstallerFlow flow) {
        final InstallPlan.Plan plan = flow.plan();
        final PartitionEntry system = plan.system();
        if (!plan.ok() || system == null || system.fs() == null) {
            return of(plan.problem() == null ? InstallPlan.reason(InstallPlan.Problem.NO_ROOM, flow.systemName())
                    : InstallPlan.reason(plan.problem(), flow.systemName()));
        }
        final int disk = diskIndexOf(flow, plan.slot());
        final String size = tableSize(system.sizeMb());
        final String fs = system.fs().displayName();
        final DiskSpot other = flow.besideSpot();
        final String says;
        if (plan.wholeDisk()) {
            says = of(InstallerScreenTexts.PLAN_WHOLE.with(disk, size, fs));
        } else if (plan.shrunk() > 0 && other != null && plan.layout() != null) {
            final PartitionEntry shrunk = plan.layout().find(plan.shrunk());
            says = of(InstallerScreenTexts.PLAN_BESIDE.with(other.holds(),
                    tableSize(shrunk == null ? 0L : shrunk.sizeMb()), size, fs));
        } else if (plan.made().contains(plan.systemPartition()) && freeBefore(flow, system)) {
            says = of(InstallerScreenTexts.PLAN_FREE.with(disk, size, fs));
        } else {
            says = of(InstallerScreenTexts.PLAN_PARTITION.with(system.number(), disk, size, fs));
        }
        final PartitionEntry swap = plan.swap();
        return swap == null ? says : says + " " + of(InstallerScreenTexts.PLAN_SWAP.with(tableSize(swap.sizeMb())));
    }

    /** A size as the table of Frames 7 to 11 writes it: megabytes, whole gigabytes, or gigabytes to a tenth. */
    static String tableSize(final long mb) {
        if (mb < MB_PER_GB) {
            return mb + " MB";
        }
        if (mb % MB_PER_GB == 0) {
            return mb / MB_PER_GB + " GB";
        }
        return String.format(Locale.ROOT, "%.1f GB", mb / (double) MB_PER_GB);
    }

    /** A size as Ubuntu's server installer writes it: gigabytes to a tenth, with a G. */
    static String ubuntuSize(final long mb) {
        return String.format(Locale.ROOT, "%.1fG", mb / (double) MB_PER_GB);
    }

    /*
     * The letter Frames gives each partition it reads, C: first and on past the D: of the CD drive, in the order the
     * disks and their partitions lie; keyed by the pick's place.
     */
    private static Map<Integer, String> framesLetters(final List<DiskChoices.Pick> picks) {
        final Map<Integer, String> letters = new HashMap<>();
        char next = FIRST_LETTER;
        for (int i = 0; i < picks.size(); i++) {
            final DiskSpot spot = picks.get(i).spot();
            if (spot.holdsData() && spot.fileSystem() != null
                    && spot.fileSystem().readBy(Platform.FRAMES, HardwareEra.ADVANCED)) {
                letters.put(i, next + ":");
                next = next + 1 == CD_LETTER ? (char) (CD_LETTER + 1) : (char) (next + 1);
            }
        }
        return letters;
    }

    /* One row of Frames XP's list: a partition with its letter, its file system, its size and what is free in it. */
    private static void xpRow(final GuiGraphics g, final IInstallerPen pen, final int x, final int w, final int y,
                              final DiskChoices.Pick pick, final String letter, final int ink) {
        final DiskSpot spot = pick.spot();
        final int sizeEnds = x + w * XP_SIZE_ENDS / 100;
        if (spot.isFree()) {
            pen.say(g, of(InstallerScreenTexts.XP_UNPARTITIONED), x + w * XP_NAME_AT / 100, y, ink);
            pen.sayRight(g, spot.sizeMb() + " MB", sizeEnds, y, ink);
            return;
        }
        if (!letter.isEmpty()) {
            pen.say(g, letter, x + w * XP_LETTER_AT / 100, y, ink);
        }
        pen.say(g, of(InstallerScreenTexts.XP_PARTITION.with(spot.number(), xpFileSystem(spot))),
                x + w * XP_NAME_AT / 100, y, ink);
        pen.sayRight(g, spot.sizeMb() + " MB", sizeEnds, y, ink);
        if (!letter.isEmpty()) {
            pen.say(g, of(InstallerScreenTexts.XP_PARTITION_FREE.with(spot.freeMb())), sizeEnds + 8, y, ink);
        }
    }

    /* A file system as Frames XP's setup names it: FAT for FAT16, Unknown for what it does not read. */
    private static String xpFileSystem(final DiskSpot spot) {
        final FileSystemType fs = spot.fileSystem();
        if (fs == null || !spot.holdsData() || !fs.readBy(Platform.FRAMES, HardwareEra.LEGACY)) {
            return of(InstallerScreenTexts.XP_UNKNOWN);
        }
        return fs == FileSystemType.FAT16 ? "FAT" : fs.displayName();
    }

    private static String xpFormatLabel(final DiskChoices.Format format) {
        if (Placement.KEEP.equals(format.fileSystem())) {
            return of(InstallerScreenTexts.XP_FORMAT_KEEP);
        }
        final String full = of(FileSystemType.NTFS.serializedName().equals(format.fileSystem())
                ? InstallerScreenTexts.XP_FORMAT_NTFS : InstallerScreenTexts.XP_FORMAT_FAT);
        return format.quick() ? of(InstallerScreenTexts.XP_QUICK.with(full)) : full;
    }

    /* Where Frames XP is going, as its note names it: the partition's letter, or the unpartitioned space. */
    private static String whereXp(final InstallerFlow flow, final List<DiskChoices.Pick> picks,
                                  final Map<Integer, String> letters) {
        if (picks.isEmpty()) {
            return "";
        }
        final int chosen = DiskChoices.indexOfChosen(flow, picks);
        final DiskSpot spot = picks.get(chosen).spot();
        return spot.isFree() ? of(InstallerScreenTexts.XP_THE_UNPARTITIONED)
                : letters.getOrDefault(chosen, "Partition" + spot.number());
    }

    private static String schemeWord(final PartitionScheme scheme) {
        return scheme.serializedName().toUpperCase(Locale.ROOT);
    }

    /* A row's name in the table: its disk and number, and its label, its letter or EFI after them. */
    private static String tableName(final DiskChoices.Pick pick, final String letter) {
        final DiskSpot spot = pick.spot();
        if (spot.isFree()) {
            return of(InstallerScreenTexts.TABLE_FREE.with(pick.diskIndex()));
        }
        final String named = spot.role() == PartitionRole.EFI ? "EFI" : !spot.label().isEmpty() ? spot.label()
                : letter;
        return named.isEmpty() ? of(InstallerScreenTexts.TABLE_PARTITION.with(pick.diskIndex(), spot.number()))
                : of(InstallerScreenTexts.TABLE_PARTITION_NAMED.with(pick.diskIndex(), spot.number(), named));
    }

    /* What a row holds: its system, the boot entries, swap, files, or nothing. */
    private static String tableHolds(final DiskSpot spot) {
        if (spot.hasSystem()) {
            return spot.holds();
        }
        if (spot.role() == PartitionRole.EFI) {
            return of(InstallerScreenTexts.HOLDS_BOOT_ENTRIES);
        }
        if (spot.swap()) {
            return of(InstallerScreenTexts.HOLDS_SWAP);
        }
        return spot.usedMb() > 0L ? of(InstallerScreenTexts.HOLDS_FILES) : of(InstallerScreenTexts.NOTHING);
    }

    /* What Next does with the row chosen in the table, or why it cannot. */
    private static String nextSays(final InstallerFlow flow) {
        final InstallPlan.Plan plan = flow.plan();
        final String needs = tableSize(flow.footprintMb());
        if (!plan.ok() || plan.system() == null || plan.system().fs() == null) {
            return of(InstallPlan.reason(plan.problem() == null ? InstallPlan.Problem.NO_ROOM : plan.problem(),
                    flow.systemName()));
        }
        final String fs = plan.system().fs().displayName();
        if (plan.wholeDisk() && flow.eraseSlot() != plan.slot()) {
            // A disk that holds nothing is laid out afresh, the empty partitions on it with it.
            return of(InstallerScreenTexts.NEXT_WHOLE.with(flow.systemName(), needs, diskIndexOf(flow, plan.slot())));
        }
        if (!plan.made().contains(plan.systemPartition())) {
            return of(InstallerScreenTexts.NEXT_ON_PARTITION.with(flow.systemName(), needs));
        }
        return freeBefore(flow, plan.system()) ? of(InstallerScreenTexts.NEXT_IN_FREE.with(flow.systemName(), needs,
                fs)) : of(InstallerScreenTexts.NEXT_FORMATTED.with(flow.systemName(), needs, fs));
    }

    /* The three sub-lines of Ubuntu's choice beside another system: what is shrunk, and what is made. */
    private static int ubuntuBeside(final GuiGraphics g, final IInstallerPen pen, final InstallerFrames.Frame f,
                                    final InstallerFlow flow, final InstallPlan.Plan plan, final int y,
                                    final List<Integer> rowMap) {
        final InstallerFrames.Paint p = f.paint();
        final int step = pen.row();
        final DiskSpot other = flow.besideSpot();
        final PartitionEntry shrunk = plan.layout() == null ? null : plan.layout().find(plan.shrunk());
        final PartitionEntry system = plan.system();
        if (other == null || shrunk == null || system == null || system.fs() == null) {
            return y;
        }
        final String device = deviceOf(flow, other);
        final int x = f.x() + 30;
        int ty = y;
        pen.say(g, of(InstallerScreenTexts.UBUNTU_SHRINKS.with(device, other.fileSystem() == null ? ""
                : other.fileSystem().displayName())), x, ty, p.text());
        ty += step;
        final String name = UBUNTU_INDENT + device + "   " + other.holds() + "   ";
        pen.say(g, name, x, ty, p.text());
        final String field = "[ " + ubuntuSize(shrunk.sizeMb()) + " ]";
        pen.say(g, field, x + pen.width(name), ty, p.accent());
        pen.say(g, " " + of(InstallerScreenTexts.UBUNTU_OTHER.with(ubuntuSize(other.sizeMb()),
                ubuntuSize(other.usedMb()))), x + pen.width(name + field), ty, p.text());
        rowMap.add(-1);
        ty += step;
        final PartitionEntry swap = plan.swap();
        final long made = system.sizeMb() + (swap == null ? 0L : swap.sizeMb());
        final String what = swap == null
                ? of(InstallerScreenTexts.UBUNTU_NEW_NO_SWAP.with(ubuntuSize(made), system.fs().displayName(),
                ubuntuSize(system.sizeMb())))
                : of(InstallerScreenTexts.UBUNTU_NEW.with(ubuntuSize(made), system.fs().displayName(),
                ubuntuSize(system.sizeMb()), ubuntuSize(swap.sizeMb())));
        pen.say(g, UBUNTU_INDENT + flow.systemName() + "   " + what, x, ty, p.text());
        rowMap.add(-1);
        ty += step;
        pen.say(g, UBUNTU_INDENT + of(InstallerScreenTexts.SIZE_KEYS), x, ty, p.dim());
        rowMap.add(-1);
        return ty + step;
    }

    private static String ubuntuLabel(final InstallerFlow flow, final DiskChoices.Option option) {
        final InstallerFlow.Disk disk = option.disk();
        return switch (option.kind()) {
            case ALONGSIDE -> {
                final DiskSpot other = disk == null ? null : InstallPlan.besideOf(flow, disk);
                yield of(InstallerScreenTexts.UBUNTU_ALONGSIDE.with(other == null ? "" : other.holds()));
            }
            case ENTIRE -> disk == null ? "" : disk.hasSystem()
                    ? of(InstallerScreenTexts.UBUNTU_ENTIRE.with("Disk " + diskIndexOf(flow, disk.slot()),
                    disk.holds()))
                    : of(InstallerScreenTexts.UBUNTU_ENTIRE_EMPTY.with("Disk " + diskIndexOf(flow, disk.slot())));
            case CUSTOM -> of(InstallerScreenTexts.UBUNTU_CUSTOM);
        };
    }

    /* A partition's name beside another system: its letter when Frames reads it, else Linux's device name. */
    private static String deviceOf(final InstallerFlow flow, final DiskSpot spot) {
        final List<DiskChoices.Pick> picks = DiskChoices.picks(flow);
        final Map<Integer, String> letters = framesLetters(picks);
        for (int i = 0; i < picks.size(); i++) {
            if (picks.get(i).spot().equals(spot) && letters.containsKey(i)) {
                return letters.get(i);
            }
        }
        final InstallerFlow.Disk target = flow.target();
        final int disk = target == null ? 0 : diskIndexOf(flow, target.slot());
        return "/dev/sd" + (char) ('a' + disk) + spot.number();
    }

    /* Whether the system's partition was made in what was free space before. */
    private static boolean freeBefore(final InstallerFlow flow, final PartitionEntry system) {
        final InstallerFlow.Disk target = flow.target();
        if (target == null) {
            return true;
        }
        for (final DiskSpot spot : target.spots()) {
            if (spot.isFree() && spot.startMb() <= system.startMb() && system.startMb() < spot.endMb()) {
                return true;
            }
        }
        return target.spotAt(system.startMb()) == null || flow.eraseSlot() == target.slot();
    }

    private static long roomAfter(final DiskChoices.Pick pick) {
        final DiskLayout layout = pick.disk().layout();
        final PartitionEntry partition = layout.find(pick.spot().number());
        return partition == null ? 0L : layout.roomAfter(partition);
    }

    private static int diskIndexOf(final InstallerFlow flow, final int slot) {
        for (int i = 0; i < flow.disks().size(); i++) {
            if (flow.disks().get(i).slot() == slot) {
                return i;
            }
        }
        return 0;
    }

    private static String swapSize(final int mb) {
        return mb % MB_PER_GB == 0 ? mb / MB_PER_GB + "g" : mb + "m";
    }

    /** A paragraph broken at its words to the width it has; answers where the next line goes. */
    static int paragraph(final GuiGraphics g, final IInstallerPen pen, final String text, final int x,
                                 final int y, final int room, final int colour) {
        int ty = y;
        final StringBuilder line = new StringBuilder();
        for (final String word : text.split(" ")) {
            final String tried = line.isEmpty() ? word : line + " " + word;
            if (pen.width(tried) > room && !line.isEmpty()) {
                pen.say(g, line.toString(), x, ty, colour);
                ty += pen.row();
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(tried);
            }
        }
        if (!line.isEmpty()) {
            pen.say(g, line.toString(), x, ty, colour);
            ty += pen.row();
        }
        return ty;
    }

    /**
     * What a page drew that the screen has to know to answer the mouse.
     *
     * @param top    where its first row is
     * @param rowMap for each row drawn from {@code top}, the choice it is, or -1 for a line that chooses nothing
     * @param links  the table's links in their order, each {x, y, w, h} or null when it does nothing now
     */
    record Drawn(int top, List<Integer> rowMap, List<int[]> links) {

        Drawn {
            rowMap = List.copyOf(rowMap);
            // A link that does nothing now is null, which a copied list would refuse.
            links = Collections.unmodifiableList(new ArrayList<>(links));
        }

        /** The choice under a row drawn at that place, or -1. */
        int choiceAt(final int row) {
            return row >= 0 && row < this.rowMap.size() ? this.rowMap.get(row) : -1;
        }

        /** The link under that point, or -1. */
        int linkAt(final double mouseX, final double mouseY) {
            for (int i = 0; i < this.links.size(); i++) {
                final int[] box = this.links.get(i);
                if (box != null && mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1]
                        && mouseY < box[1] + box[3]) {
                    return i;
                }
            }
            return -1;
        }
    }

    /** The table's links, by their place under it. */
    static final class Link {

        static final int NEW = 0;
        static final int DELETE = 1;
        static final int FORMAT = 2;
        static final int EXTEND = 3;

        private Link() {
        }
    }

    /** The rows of bsdinstall's ZFS Configuration that choose something: Install and the pool's disks. */
    static final class Pool {

        static final int INSTALL = 0;
        static final int TYPE = 1;
        static final int CHOICES = 2;

        private Pool() {
        }
    }
}
