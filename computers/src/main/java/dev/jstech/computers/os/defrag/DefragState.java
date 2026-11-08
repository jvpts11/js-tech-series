/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.defrag;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What a machine's Defrag shows, as the machine tells it to whoever is looking (computers/docs/design/storage.md): its
 * disks with how full and how hurt each is, the pass under way, the last pass's end, and the weekly schedule.
 *
 * <p>It travels as lines of text, the way the Vintage screens do, asked for by name as a file is, so DEFRAG's screen,
 * the Frames windows and fsck all read one answer. Pure: no game types.
 *
 * @param look     how the system shows it
 * @param disks    the machine's disks, the system disk first
 * @param running  the pass under way, or null
 * @param last     how the last pass ended, or null when none has
 * @param schedule whether the weekly schedule is on, where the system has one
 */
public record DefragState(DefragLook look, List<Disk> disks, @Nullable Run running, @Nullable Result last,
                          boolean schedule) {

    /** What a path of this kind begins with, so the machine knows the Defrag is being asked for. */
    public static final String SCHEME = "defrag:";

    /** What a request asks besides being told: start a pass, stop it, or turn the schedule on or off. */
    public static final String LOOK = "look";
    public static final String START = "start";
    public static final String STOP = "stop";
    public static final String PAUSE = "pause";
    public static final String SCHEDULE = "schedule";

    private static final String SEP = "|";
    private static final String SPLIT = "\\|";

    public DefragState {
        disks = List.copyOf(disks);
    }

    /**
     * One disk.
     *
     * @param index      its place among the machine's disks, the system disk first
     * @param drive      what the system calls it
     * @param tier       its kind: 0 a hard disk, 1 an SSD, 2 NVMe
     * @param fileSystem the file system it carries, by name
     * @param capacityMb what its label says it holds
     * @param freeMb     what it has free
     * @param used       how full it is, in permille
     * @param damage     how much of what it stores is corrupted, in permille
     * @param bad        how much of it is lost to bad sectors, in permille
     * @param dead       whether it died of its corruption
     * @param corrupted  how many corrupted things wait for the Defrag
     * @param lastRun    the game time a pass last ended on it, or -1
     * @param types      how many kinds of things it stores, which a Unix's fsck counts among its files
     */
    public record Disk(int index, String drive, int tier, String fileSystem, long capacityMb, long freeMb, int used,
                       int damage, int bad, boolean dead, long corrupted, long lastRun, int types) {
    }

    /** The pass under way: on which disk, how far, what it has done so far, and whether it is paused. */
    public record Run(int disk, int percent, long found, long recovered, long lost, int badBefore, boolean paused) {
    }

    /** How a pass ended: what it found and gave back, and the bad sectors before and after it. */
    public record Result(int disk, long found, long recovered, long lost, int badBefore, int badAfter) {
    }

    /** The request for that action on that disk, as a name the machine is asked for. */
    public static String path(final String action, final int disk) {
        return SCHEME + action + ":" + Math.max(0, disk);
    }

    /** Whether a name asks for the Defrag at all. */
    public static boolean names(@Nullable final String path) {
        return path != null && path.startsWith(SCHEME);
    }

    /** The action a request asks for; a look when it cannot be read. */
    public static String actionOf(final String path) {
        final String[] parts = path.substring(SCHEME.length()).split(":", -1);
        return parts.length > 0 ? parts[0] : LOOK;
    }

    /** The disk a request names; the system disk when it cannot be read. */
    public static int diskOf(final String path) {
        final String[] parts = path.substring(SCHEME.length()).split(":", -1);
        return parts.length > 1 ? whole(parts[1]) : 0;
    }

    /** The disk of that index, or null. */
    @Nullable
    public Disk disk(final int index) {
        for (final Disk disk : this.disks) {
            if (disk.index() == index) {
                return disk;
            }
        }
        return null;
    }

    /** The state as the lines that travel. */
    public String write() {
        final List<String> lines = new ArrayList<>();
        lines.add(String.join(SEP, "look", this.look.serializedName(), this.schedule ? "1" : "0"));
        for (final Disk d : this.disks) {
            lines.add(String.join(SEP, "disk", n(d.index()), clean(d.drive()), n(d.tier()), clean(d.fileSystem()),
                    n(d.capacityMb()), n(d.freeMb()), n(d.used()), n(d.damage()), n(d.bad()), d.dead() ? "1" : "0",
                    n(d.corrupted()), n(d.lastRun()), n(d.types())));
        }
        if (this.running != null) {
            final Run r = this.running;
            lines.add(String.join(SEP, "run", n(r.disk()), n(r.percent()), n(r.found()), n(r.recovered()),
                    n(r.lost()), n(r.badBefore()), r.paused() ? "1" : "0"));
        }
        if (this.last != null) {
            final Result r = this.last;
            lines.add(String.join(SEP, "last", n(r.disk()), n(r.found()), n(r.recovered()), n(r.lost()),
                    n(r.badBefore()), n(r.badAfter())));
        }
        return String.join("\n", lines);
    }

    /** The state those lines tell; lines it cannot read are skipped. */
    public static DefragState read(final String text) {
        DefragLook look = DefragLook.DOS;
        boolean schedule = false;
        final List<Disk> disks = new ArrayList<>();
        Run running = null;
        Result last = null;
        for (final String line : (text == null ? "" : text).split("\n")) {
            final String[] p = line.split(SPLIT, -1);
            switch (p[0]) {
                case "look" -> {
                    if (p.length >= 3) {
                        look = DefragLook.byName(p[1]);
                        schedule = "1".equals(p[2]);
                    }
                }
                case "disk" -> {
                    if (p.length >= 14) {
                        disks.add(new Disk(whole(p[1]), p[2], whole(p[3]), p[4], big(p[5]), big(p[6]), whole(p[7]),
                                whole(p[8]), whole(p[9]), "1".equals(p[10]), big(p[11]), big(p[12]), whole(p[13])));
                    }
                }
                case "run" -> {
                    if (p.length >= 8) {
                        running = new Run(whole(p[1]), whole(p[2]), big(p[3]), big(p[4]), big(p[5]), whole(p[6]),
                                "1".equals(p[7]));
                    }
                }
                case "last" -> {
                    if (p.length >= 7) {
                        last = new Result(whole(p[1]), big(p[2]), big(p[3]), big(p[4]), whole(p[5]), whole(p[6]));
                    }
                }
                default -> {
                    // A line from a newer machine, or nothing.
                }
            }
        }
        return new DefragState(look, disks, running, last, schedule);
    }

    private static String n(final long value) {
        return Long.toString(value);
    }

    private static String clean(final String text) {
        return text == null ? "" : text.replace(SEP, "/").replace("\n", " ");
    }

    private static int whole(final String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (final NumberFormatException notANumber) {
            return 0;
        }
    }

    private static long big(final String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (final NumberFormatException notANumber) {
            return 0L;
        }
    }
}
