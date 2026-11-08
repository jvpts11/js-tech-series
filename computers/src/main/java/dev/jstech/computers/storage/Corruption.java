/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import dev.jstech.computers.hardware.StorageTier;
import java.util.function.DoubleSupplier;

/**
 * The rules of storage corruption and of the Defrag that repairs it (computers/docs/design/storage.md): how much an
 * event corrupts, when the damage reaches the disk itself as bad sectors and when it kills it, how likely each kind of
 * disk is to give a corrupted item back, and how long a pass takes.
 *
 * <p>Corruption never happens in normal play, only through a network conflict or a thermal trip (and later viruses and
 * hacking). Every number here is an estimate, to be tuned in play.
 *
 * <p>Pure: no game types, so the rules are held to account without a world. Shares are in permille of what a disk
 * holds, which is how the damage of a disk is told: a disk whose stored data is 70% corrupted is at 700.
 */
public final class Corruption {

    /** What a network conflict corrupts of every disk of both networks: 10%. */
    public static final double CONFLICT_SHARE = 0.10;
    /** The chance a thermal trip corrupts each disk its machine had in use, and how much of it then: 25% and 5%. */
    public static final double TRIP_CHANCE = 0.25;
    public static final double TRIP_SHARE = 0.05;
    /** Past half the data corrupted, the disk itself is hurt: it loses the capacity the damage goes over this by. */
    public static final int BAD_SECTORS_FROM = 500;
    /** Past nine tenths corrupted, the disk dies. */
    public static final int DIES_PAST = 900;
    /** The whole of a disk, in permille. */
    public static final int WHOLE = 1000;

    /* How long a pass takes on each kind of disk: 3 game minutes on a hard disk, 1 on an SSD, 30 seconds on NVMe. */
    private static final int HDD_PASS_TICKS = 3 * 60 * 20;
    private static final int SSD_PASS_TICKS = 60 * 20;
    private static final int NVME_PASS_TICKS = 30 * 20;
    /* A hard disk gives back 95% whatever the damage. */
    private static final int HDD_CHANCE = 95;
    /* An SSD gives back 70% at first, 10 points less for every tenth more damage, and nothing from 70% on. */
    private static final int SSD_CHANCE = 70;
    private static final int SSD_STEP = 10;
    private static final int SSD_GIVES_UP_AT = 700;
    /* NVMe gives back 60% at first, 15 points less for every tenth more damage, and nothing from 50% on. */
    private static final int NVME_CHANCE = 60;
    private static final int NVME_STEP = 15;
    private static final int NVME_GIVES_UP_AT = 500;
    /* A tenth of the damage, in permille: each one an SSD or NVMe disk goes past costs its chance a step. */
    private static final int TENTH = 100;
    /* A pass mends a quarter of the bad sectors left. */
    private static final int MENDED_SHARE = 4;

    private Corruption() {
    }

    /**
     * How many of {@code count} things a share of {@code share} takes: the whole part of the product, and one more by
     * chance for what is left over, so a share of a small pile is still taken now and then rather than never.
     *
     * @param random draws a number from 0 (inclusive) to 1 (exclusive)
     */
    public static long share(final long count, final double share, final DoubleSupplier random) {
        if (count <= 0L || share <= 0.0) {
            return 0L;
        }
        if (share >= 1.0) {
            return count;
        }
        final double exact = count * share;
        final long whole = (long) exact;
        final long taken = whole + (random.getAsDouble() < exact - whole ? 1L : 0L);
        return Math.min(count, taken);
    }

    /** How much of a disk's stored data is corrupted, in permille: 0 for a disk holding nothing. */
    public static int damage(final long corruptedWeight, final long usedWeight) {
        if (usedWeight <= 0L || corruptedWeight <= 0L) {
            return 0;
        }
        return (int) Math.min(WHOLE, corruptedWeight * WHOLE / usedWeight);
    }

    /** The capacity a disk at that damage loses to bad sectors, in permille: what the damage goes over half by. */
    public static int badSectors(final int damage) {
        return Math.max(0, damage - BAD_SECTORS_FROM);
    }

    /** Whether a disk at that damage dies. */
    public static boolean dies(final int damage) {
        return damage > DIES_PAST;
    }

    /** The bad sectors left after a pass mends a quarter of {@code bad}, rounded so the last one is mended too. */
    public static int afterPass(final int bad) {
        if (bad <= 0) {
            return 0;
        }
        return bad - (bad + MENDED_SHARE - 1) / MENDED_SHARE;
    }

    /** What a disk of {@code capacity} still holds with {@code bad} permille of it lost to bad sectors. */
    public static long usable(final long capacity, final int bad) {
        if (bad <= 0) {
            return capacity;
        }
        return capacity * Math.max(0, WHOLE - Math.min(WHOLE, bad)) / WHOLE;
    }

    /**
     * The chance, in percent, that a pass gives back a corrupted item on a disk of {@code tier} at {@code damage}
     * permille: 95 on a hard disk; on an SSD 70 less 10 for every tenth of damage, nothing from 70% on; on NVMe 60
     * less 15 for every tenth, nothing from 50% on.
     */
    public static int recoveryChance(final StorageTier tier, final int damage) {
        final int tenths = Math.max(0, damage) / TENTH;
        return switch (tier) {
            case HDD -> HDD_CHANCE;
            case SSD -> damage >= SSD_GIVES_UP_AT ? 0 : Math.max(0, SSD_CHANCE - SSD_STEP * tenths);
            case NVME -> damage >= NVME_GIVES_UP_AT ? 0 : Math.max(0, NVME_CHANCE - NVME_STEP * tenths);
        };
    }

    /** How long a pass takes on a disk of {@code tier}, in ticks. */
    public static int passTicks(final StorageTier tier) {
        return switch (tier) {
            case HDD -> HDD_PASS_TICKS;
            case SSD -> SSD_PASS_TICKS;
            case NVME -> NVME_PASS_TICKS;
        };
    }
}
