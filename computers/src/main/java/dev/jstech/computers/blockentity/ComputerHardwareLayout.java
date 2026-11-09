/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.hardware.CaseCooling;
import dev.jstech.computers.hardware.CaseParts;

/**
 * The hardware-slot map of a computer block: where each component category sits in the assembly {@code ItemStackHandler}.
 * The 5.25-inch bays come last, so a machine saved before it had them keeps every part where it was; how many of them
 * a machine really has is its case's to say, up to {@code bayCount}.
 *
 * <p>A workstation's case seats its memory by processor ({@link CaseParts#memoryPerProcessor}): its first processor
 * and that one's memory sit where a one-socket machine's do, and the second processor and its memory come after the
 * bays, for the same reason the bays came last. {@link #cpuSlot} and {@link #ramSlot} find each one wherever it sits.
 *
 * @param cpuStart      the first processor's slot
 * @param cpuCount      the processors in all
 * @param firstCpus     how many of them sit from {@code cpuStart} on; the rest come after the bays
 * @param ramStart      the first module's slot
 * @param ramCount      the memory modules in all
 * @param firstRam      how many of them sit from {@code ramStart} on; the rest come after the later processors
 * <p>A computer its case cools has a cooler slot over each processor and the slots for its case's fans after all of
 * those, for the same reason again; one cooled from outside it has neither.
 *
 * @param laterStart    where the later processors start, after the bays, followed by the later modules
 * @param ramPerCpu     the modules each processor on the board opens, or 0 when the memory does not go by processor
 * @param coolerStart   the first processor's cooler slot, the others after it in the processors' order
 * @param coolerCount   the cooler slots, one for each processor, or none
 * @param fanStart      the first fan slot, the seats numbered as the case numbers them: rear, top, front
 * @param fanCount      the fan slots, as many as the case with the most seats has, or none
 */
public record ComputerHardwareLayout(
        int motherboardSlot,
        int cpuStart, int cpuCount, int firstCpus,
        int ramStart, int ramCount, int firstRam,
        int pcieStart, int pcieCount,
        int psuSlot,
        int diskStart, int diskCount,
        int bayStart, int bayCount,
        int laterStart, int ramPerCpu,
        int coolerStart, int coolerCount,
        int fanStart, int fanCount,
        int totalSlots) {

    /**
     * The map of a case counting {@code parts} and cooling as {@code cooling} says: the board first, then the
     * processors, the memory, the cards, the supply, the disks and the bays, each kind right after the one before; then
     * a workstation's later processors and their memory; then the coolers and the fans of a case that cools.
     */
    public static ComputerHardwareLayout of(final CaseParts parts, final CaseCooling cooling) {
        final boolean rows = parts.memoryPerProcessor() > 0;
        final int firstCpus = rows ? Math.min(1, parts.processors()) : parts.processors();
        final int firstRam = rows ? Math.min(parts.memoryPerProcessor(), parts.memory()) : parts.memory();
        final int cpuStart = 1;
        final int ramStart = cpuStart + firstCpus;
        final int pcieStart = ramStart + firstRam;
        final int psuSlot = pcieStart + parts.cards();
        final int diskStart = psuSlot + 1;
        final int bayStart = diskStart + parts.disks();
        final int laterStart = bayStart + parts.bays();
        final int coolerStart = laterStart + (parts.processors() - firstCpus) + (parts.memory() - firstRam);
        final int coolers = cooling.equals(CaseCooling.NONE) ? 0 : parts.processors();
        final int fanStart = coolerStart + coolers;
        final int total = fanStart + cooling.fanSlots();
        return new ComputerHardwareLayout(0, cpuStart, parts.processors(), firstCpus, ramStart, parts.memory(),
                firstRam, pcieStart, parts.cards(), psuSlot, diskStart, parts.disks(), bayStart, parts.bays(),
                laterStart, parts.memoryPerProcessor(), coolerStart, coolers, fanStart, cooling.fanSlots(), total);
    }

    /** The slot of processor {@code index}, from 0. */
    public int cpuSlot(final int index) {
        return index < firstCpus ? cpuStart + index : laterStart + index - firstCpus;
    }

    /** Which processor, from 0, the slot holds; -1 for a slot that holds none. */
    public int cpuIndex(final int slot) {
        if (slot >= cpuStart && slot < cpuStart + firstCpus) {
            return slot - cpuStart;
        }
        if (slot >= laterStart && slot < laterStart + cpuCount - firstCpus) {
            return firstCpus + slot - laterStart;
        }
        return -1;
    }

    /** The slot of memory module {@code index}, from 0. */
    public int ramSlot(final int index) {
        return index < firstRam ? ramStart + index : laterStart + (cpuCount - firstCpus) + index - firstRam;
    }

    /** How many of its modules the case uses beside {@code processors} processors on its board. */
    public int ramFor(final int processors) {
        return ramPerCpu == 0 ? ramCount : Math.min(ramCount, ramPerCpu * Math.max(1, processors));
    }

    public boolean isCpu(final int slot) {
        return slot >= cpuStart && slot < cpuStart + firstCpus
                || slot >= laterStart && slot < laterStart + cpuCount - firstCpus;
    }

    public boolean isRam(final int slot) {
        final int laterRam = laterStart + cpuCount - firstCpus;
        return slot >= ramStart && slot < ramStart + firstRam
                || slot >= laterRam && slot < laterRam + ramCount - firstRam;
    }

    public boolean isPcie(final int slot) {
        return slot >= pcieStart && slot < pcieStart + pcieCount;
    }

    public boolean isDisk(final int slot) {
        return slot >= diskStart && slot < diskStart + diskCount;
    }

    public boolean isBay(final int slot) {
        return slot >= bayStart && slot < bayStart + bayCount;
    }

    public boolean isCooler(final int slot) {
        return slot >= coolerStart && slot < coolerStart + coolerCount;
    }

    public boolean isFan(final int slot) {
        return slot >= fanStart && slot < fanStart + fanCount;
    }
}
