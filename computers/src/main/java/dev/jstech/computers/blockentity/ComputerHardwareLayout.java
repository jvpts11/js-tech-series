/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.hardware.CaseParts;

/**
 * The hardware-slot map of a computer block: where each component category sits in the assembly {@code ItemStackHandler}.
 * The 5.25-inch bays come last, so a machine saved before it had them keeps every part where it was; how many of them
 * a machine really has is its case's to say, up to {@code bayCount}.
 */
public record ComputerHardwareLayout(
        int motherboardSlot,
        int cpuStart, int cpuCount,
        int ramStart, int ramCount,
        int pcieStart, int pcieCount,
        int psuSlot,
        int diskStart, int diskCount,
        int bayStart, int bayCount,
        int totalSlots) {

    /**
     * The map of a case counting {@code parts}: the board first, then the processors, the memory, the cards, the
     * supply, the disks and the bays, each kind right after the one before.
     */
    public static ComputerHardwareLayout of(final CaseParts parts) {
        final int cpuStart = 1;
        final int ramStart = cpuStart + parts.processors();
        final int pcieStart = ramStart + parts.memory();
        final int psuSlot = pcieStart + parts.cards();
        final int diskStart = psuSlot + 1;
        final int bayStart = diskStart + parts.disks();
        return new ComputerHardwareLayout(0, cpuStart, parts.processors(), ramStart, parts.memory(), pcieStart,
                parts.cards(), psuSlot, diskStart, parts.disks(), bayStart, parts.bays(), bayStart + parts.bays());
    }

    public boolean isCpu(final int slot) {
        return slot >= cpuStart && slot < cpuStart + cpuCount;
    }

    public boolean isRam(final int slot) {
        return slot >= ramStart && slot < ramStart + ramCount;
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
}
