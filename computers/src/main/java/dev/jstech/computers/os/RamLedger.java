/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * The memory ledger of one computer: what its RAM is spent on, in megabytes, and whether one more thing
 * fits. The system takes its share first, then the desktop it booted, then the services it runs, then the
 * windows the player opened; a script process joins the list the same way. Pure arithmetic: the hosts and
 * the desktop screen fill it from the registries, and the same rules run on both sides.
 *
 * <p>Each entry carries two sizes, and they answer different questions. The megabytes are what the machine gave it
 * and cannot give twice, so they are what says whether one more thing fits. The bytes are what is really held this
 * moment, which is the number that moves while a program runs and the one a person watches.
 */
public final class RamLedger {

    /**
     * A RAM module's marketed size per buffer item it stages: a 4 MB SIMM stages one item, a 2 GB DDR2 stick
     * five hundred and twelve. The buffer is what the network uses; the megabytes are what the programs use.
     */
    public static final int MB_PER_BUFFER_ITEM = 4;

    /** What an entry of the ledger is. The System Monitor and the Task Manager receive it by its name. */
    public enum Kind implements IStableName {
        SYSTEM("system"),
        DESKTOP("desktop"),
        SERVICE("service"),
        WINDOW("window"),
        PROCESS("process"),
        /** The system's memory lent to graphics on the processor's die, as video memory, for what it is showing. */
        GRAPHICS("graphics");

        private static final StableNames<Kind> NAMES = StableNames.of(Kind.class);

        private final String serializedName;

        Kind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }

        /** The kind a name stands for, or null for a name no kind declares. */
        public static Kind find(final String name) {
            return NAMES.find(name);
        }
    }

    /** The bytes in a megabyte, for the things whose hold is counted in bytes as it happens. */
    public static final long BYTES_PER_MB = 1024L * 1024L;

    /**
     * One thing holding memory: a label to show, the megabytes it was given, the bytes it is holding of them,
     * what it is, and the number it answers to where it has one.
     *
     * <p>The two sizes are the two questions a person asks of memory. What was given is what the machine
     * promised and cannot promise twice, so it is what decides whether one more thing fits. What is held is what
     * is really in there this moment, which is what moves while a program runs. Everything but a running program
     * holds all of what it was given, so for those the two are the same number.
     *
     * <p>Only a script process has a number: two of them can be started from the same file, so the name
     * alone cannot say which is which when one of them is to be ended. A service and a window have a key instead, the
     * program a service is and the key a window goes by, which is what ends one of them.
     */
    public record Entry(String name, int mb, long heldBytes, Kind kind, int id, String key) {

        public Entry(final String name, final int mb, final Kind kind) {
            this(name, mb, mb * BYTES_PER_MB, kind, 0, "");
        }
    }

    private final int totalMb;
    private final List<Entry> entries = new ArrayList<>();

    public RamLedger(final int totalMb) {
        this.totalMb = Math.max(0, totalMb);
    }

    /** Records {@code mb} held by {@code name}; nothing is recorded for a zero or negative size. */
    public RamLedger add(final String name, final int mb, final Kind kind) {
        return add(name, mb, kind, 0);
    }

    /** The same, for something that answers to a number of its own. */
    public RamLedger add(final String name, final int mb, final Kind kind, final int id) {
        return add(name, mb, Math.max(0, mb) * BYTES_PER_MB, kind, id);
    }

    /** The same, for a service or a window, which answers to the key of the program or of the window. */
    public RamLedger add(final String name, final int mb, final Kind kind, final String key) {
        if (mb > 0) {
            entries.add(new Entry(name, mb, mb * BYTES_PER_MB, kind, 0, key));
        }
        return this;
    }

    /**
     * The same, for a running program, which was given {@code mb} and is holding {@code heldBytes} of them this
     * moment. It is listed by the room it was given, since that is what the machine promised it.
     */
    public RamLedger add(final String name, final int mb, final long heldBytes, final Kind kind, final int id) {
        if (mb > 0) {
            entries.add(new Entry(name, mb, Math.max(0, heldBytes), kind, id, ""));
        }
        return this;
    }

    public int totalMb() {
        return totalMb;
    }

    public int usedMb() {
        int sum = 0;
        for (final Entry entry : entries) {
            sum += entry.mb;
        }
        return sum;
    }

    /** The megabytes held by entries of one kind. */
    public int usedMb(final Kind kind) {
        int sum = 0;
        for (final Entry entry : entries) {
            if (entry.kind == kind) {
                sum += entry.mb;
            }
        }
        return sum;
    }

    /** The bytes really being held this moment, which is what moves while a program runs. */
    public long heldBytes() {
        long sum = 0L;
        for (final Entry entry : entries) {
            sum += entry.heldBytes;
        }
        return sum;
    }

    public int freeMb() {
        return Math.max(0, totalMb - usedMb());
    }

    /** Whether {@code mb} more would still fit. */
    public boolean fits(final int mb) {
        return mb <= totalMb - usedMb();
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    /**
     * How much is really held, as a person reads it: bytes while there are few, then kilobytes, then megabytes
     * with one decimal. A program that has just started holds a few hundred bytes, and one that has been
     * building a list for a while holds megabytes; a single unit would show one of the two as nothing.
     */
    public static String heldLabel(final long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < BYTES_PER_MB) {
            return bytes / 1024L + " KB";
        }
        final long tenths = bytes * 10L / BYTES_PER_MB;
        return tenths / 10L + "." + tenths % 10L + " MB";
    }

    /**
     * The leading items of {@code items} whose weights add up to {@code budgetMb} or less, in order: the
     * first item past the budget and everything after it are dropped, so the oldest windows survive.
     */
    public static <T> List<T> withinBudget(final List<T> items, final ToIntFunction<T> weight, final int budgetMb) {
        final List<T> kept = new ArrayList<>(items.size());
        int used = 0;
        for (final T item : items) {
            final int mb = Math.max(0, weight.applyAsInt(item));
            if (used + mb > budgetMb) {
                break;
            }
            used += mb;
            kept.add(item);
        }
        return kept;
    }
}
