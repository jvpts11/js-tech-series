/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

/**
 * How Frames 7 rated a machine from its parts: five scores from 1.0 to 7.9, kept in tenths, for the processor, the
 * memory, the desktop's graphics, the graphics of games and the main disk. The machine's own score, its base score, is
 * the lowest of the five, because a machine is only as quick as its slowest part. Its desktop needs a graphics score
 * of 3.0 for its effects; under that it falls back to its basic look.
 *
 * <p>Each score grows with the logarithm of what the part does in this game's own measure (what a processor
 * orchestrates a tick, what a card works through, the memory's size, the disk's kind), so doubling a part adds about
 * a point, and the scale reaches the top only with the parts of the latest age. A card that is part of the processor
 * runs the basic look whatever it scores, because it borrows the machine's memory to draw.
 *
 * @param processor the processor's score
 * @param memory    the memory's
 * @param graphics  the desktop's graphics
 * @param gaming    the graphics of games
 * @param disk      the main disk's
 */
public record ExperienceIndex(int processor, int memory, int graphics, int gaming, int disk) {

    /** The lowest and the highest score, in tenths. */
    public static final int LOWEST = 10;
    public static final int HIGHEST = 79;
    /** The graphics score a desktop's effects need, in tenths. */
    public static final int EFFECTS_FROM = 30;
    /** What a machine with no parts to rate scores everywhere. */
    public static final ExperienceIndex NONE = new ExperienceIndex(LOWEST, LOWEST, LOWEST, LOWEST, LOWEST);

    /** The highest a card that is part of the processor can score: under the effects, as those always were. */
    private static final int INTEGRATED_CAP = EFFECTS_FROM - 1;
    /** The graphics score video memory allows, by the memory: under 128 MB no effects at all. */
    private static final int[][] VRAM_CAPS = {{128, 29}, {256, 49}, {512, 59}, {1024, 69}};
    /** The disks' scores by their kind: a spinning disk, a solid one, one on the processor's own lanes. */
    private static final int HDD = 56;
    private static final int SSD = 74;
    private static final int NVME = 79;
    private static final double ROUNDING = 1e-9;

    public ExperienceIndex {
        processor = clamp(processor);
        memory = clamp(memory);
        graphics = clamp(graphics);
        gaming = clamp(gaming);
        disk = clamp(disk);
    }

    /** The scores of a machine built of {@code build} with {@code ramMb} of memory. */
    public static ExperienceIndex of(final ComputerBuild build, final int ramMb) {
        long capacity = 0;
        CpuSpec firstCpu = null;
        for (final CpuSpec cpu : build.cpus()) {
            capacity += cpu.orchestrationCapacity();
            if (firstCpu == null) {
                firstCpu = cpu;
            }
        }
        // A Pentium 4 560 near 4.3, a Core 2 Duo E6600 near 5.6, a Core i7 920 near 7.2: what Frames 7 gave them.
        final int processor = score(1.0 + 0.92 * log2(capacity / 9.5));
        final int memory = score(1.0 + 0.9 * log2(ramMb / 64.0));
        GpuSpec best = null;
        for (final GpuSpec gpu : build.gpus()) {
            if (best == null || gpu.power() > best.power()) {
                best = gpu;
            }
        }
        final int graphics;
        final int gaming;
        if (best != null) {
            final int cap = vramCap(best.vramMb());
            // A Radeon X1300 under 3.0, a GeForce 7300 GS just over it, an 8800 GT near 6.0.
            graphics = Math.min(cap, score(1.0 + log2(best.power() / 12.0)));
            gaming = Math.min(cap, score(1.0 + 0.9 * log2(best.power() / 12.0)));
        } else if (firstCpu != null && firstCpu.hasIntegratedGraphics()) {
            final IntegratedGraphics igp = firstCpu.design().graphics();
            final double power = igp.units() * (double) igp.mhz() / 1000.0;
            graphics = Math.min(INTEGRATED_CAP, score(1.0 + log2(power / 12.0)));
            gaming = Math.min(INTEGRATED_CAP, score(1.0 + 0.9 * log2(power / 12.0)));
        } else {
            graphics = LOWEST;
            gaming = LOWEST;
        }
        return new ExperienceIndex(processor, memory, graphics, gaming, diskScore(build));
    }

    /** The machine's own score, its base score: the lowest of the five. */
    public int base() {
        return Math.min(Math.min(Math.min(processor, memory), Math.min(graphics, gaming)), disk);
    }

    /** Whether the desktop's graphics can run its effects; under 3.0 it runs its basic look. */
    public boolean runsEffects() {
        return graphics >= EFFECTS_FROM;
    }

    /** A score as Frames wrote it, a whole and a tenth: 5.9. */
    public static String shown(final int tenths) {
        return tenths / 10 + "." + tenths % 10;
    }

    private static int diskScore(final ComputerBuild build) {
        if (build.disks().isEmpty()) {
            return LOWEST;
        }
        return switch (build.fastestDiskTier()) {
            case HDD -> HDD;
            case SSD -> SSD;
            case NVME -> NVME;
        };
    }

    private static int vramCap(final int vramMb) {
        for (final int[] step : VRAM_CAPS) {
            if (vramMb < step[0]) {
                return step[1];
            }
        }
        return HIGHEST;
    }

    /*
     * A score from its value, cut down to the tenth below as Frames cut it. The nudge keeps a value that is a whole
     * tenth on paper, such as two gigabytes at 5.5, from landing a hair under it in floating point.
     */
    private static int score(final double value) {
        return clamp((int) Math.floor(value * 10.0 + ROUNDING));
    }

    private static int clamp(final int tenths) {
        return Math.max(LOWEST, Math.min(HIGHEST, tenths));
    }

    private static double log2(final double value) {
        return value <= 0.0 ? -10.0 : Math.log(value) / Math.log(2.0);
    }
}
