/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.IExpansionCardItem;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.PsuItem;
import dev.jstech.computers.item.RamItem;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The parts installed in one computer: the slots they sit in, and what they add up to.
 *
 * <p>The build is worked out from the slots and kept until something in them changes, since it is read
 * many times a tick and the slots change rarely. What this computer will ACCEPT in a slot is not here:
 * that is the machine's own policy, which each kind of computer settles for itself.
 */
final class ComputerHardware {

    private final AbstractComputerBlockEntity machine;
    private final ComputerHardwareLayout layout;
    private final ItemStackHandler handler;
    @Nullable
    private ComputerBuild cached;
    /*
     * Whether those parts make a computer that can be switched on, settled when the build is and not on every
     * ask. The question is asked of every machine on every tick, through isRunning, and answering it walks the
     * four lists of parts and makes a list and a verdict of its own to do it.
     */
    private boolean powered;
    private boolean dirty = true;

    ComputerHardware(final AbstractComputerBlockEntity machine, final ComputerHardwareLayout layout) {
        this.machine = machine;
        this.layout = layout;
        this.handler = new ItemStackHandler(layout.totalSlots()) {
            @Override
            protected void onContentsChanged(final int slot) {
                ComputerHardware.this.dirty = true;
                ComputerHardware.this.machine.hardwareChanged();
            }

            @Override
            public boolean isItemValid(final int slot, final ItemStack stack) {
                return ComputerHardware.this.machine.isValidForSlot(slot, stack);
            }

            @Override
            public int getSlotLimit(final int slot) {
                return 1;
            }
        };
    }

    ItemStackHandler handler() {
        return this.handler;
    }

    /** Where this computer's slots are: which one takes the board, which ones take disks, and how many. */
    ComputerHardwareLayout layout() {
        return this.layout;
    }

    /** Says the parts have changed, so the build is worked out again the next time anyone asks. */
    void markDirty() {
        this.dirty = true;
    }

    /** What the installed parts add up to, or null when they do not make a computer. */
    @Nullable
    ComputerBuild current() {
        if (this.dirty) {
            this.cached = compute();
            this.powered = this.cached != null && this.cached.isPowered();
            this.dirty = false;
        }
        return this.cached;
    }

    /** Whether the installed parts make a computer that can be switched on. */
    boolean valid() {
        current();
        return this.powered;
    }

    void save(final CompoundTag tag, final HolderLookup.Provider registries, final String key) {
        tag.put(key, this.handler.serializeNBT(registries));
    }

    void load(final CompoundTag tag, final HolderLookup.Provider registries, final String key) {
        if (tag.contains(key)) {
            this.handler.deserializeNBT(registries, tag.getCompound(key));
        }
    }

    /** What the whole machine can hold, in mB-equivalent, or nothing when the parts do not make a computer. */
    long capacity() {
        return valid() ? current().totalCapacity() : 0L;
    }

    long ramBuffer() {
        return valid() ? current().ramBuffer() : 0L;
    }

    /*
     * Slot availability as the installed board offers it, read from the board alone with no PSU needed, so the
     * assembly GUI lights up the usable slots the moment a board goes in.
     */

    int boardCpuSlots() {
        final MotherboardItem board = board();
        return board == null ? 0 : Math.min(this.layout.cpuCount(), board.spec().cpuSlots());
    }

    int boardRamSlots() {
        final MotherboardItem board = board();
        return board == null ? 0 : Math.min(this.layout.ramCount(), board.spec().ramSlots());
    }

    int boardPcieSlots() {
        final MotherboardItem board = board();
        return board == null ? 0 : Math.min(this.layout.pcieCount(), board.spec().pcieSlots());
    }

    int boardDiskSlots() {
        final MotherboardItem board = board();
        return board == null ? 0 : Math.min(this.layout.diskCount(), board.spec().diskSlots());
    }

    /**
     * The hardware era of the installed motherboard, or null when no board is present. Read from the board
     * alone, so the assembly GUI can adopt the era's skin the moment a board goes in.
     */
    @Nullable
    HardwareEra installedEra() {
        final MotherboardItem board = board();
        return board == null ? null : board.spec().era();
    }

    int installedCpus() {
        final ComputerBuild build = current();
        return build == null ? 0 : build.cpus().size();
    }

    int installedRam() {
        final ComputerBuild build = current();
        return build == null ? 0 : build.rams().size();
    }

    int installedGpus() {
        final ComputerBuild build = current();
        return build == null ? 0 : build.gpus().size();
    }

    /** The best CPU clock in MHz across the installed CPUs, or 0 when the parts make no computer. */
    int maxCpuMhz() {
        final ComputerBuild build = current();
        if (build == null) {
            return 0;
        }
        int max = 0;
        for (final CpuSpec cpu : build.cpus()) {
            max = Math.max(max, cpu.freqMhz());
        }
        return max;
    }

    /**
     * The usable VRAM in MB across the installed GPUs, or 0 when the parts make no computer. A card seated in
     * a slot older than itself contributes only what that slot's bandwidth allows.
     */
    int totalVramMb() {
        final ComputerBuild build = current();
        return build == null ? 0 : build.effectiveVramMb();
    }

    int installedDisks() {
        final ComputerBuild build = current();
        return build == null ? 0 : build.disks().size();
    }

    /** The board installed, or null when the board slot is empty or holds something else. */
    @Nullable
    private MotherboardItem board() {
        return this.handler.getStackInSlot(this.layout.motherboardSlot()).getItem()
                instanceof MotherboardItem board ? board : null;
    }

    @Nullable
    private ComputerBuild compute() {
        final ItemStack boardStack = this.handler.getStackInSlot(this.layout.motherboardSlot());
        if (!(boardStack.getItem() instanceof MotherboardItem motherboard)
                || !this.machine.isAcceptedBoard(boardStack)) {
            /*
             * A board this computer does not accept (wrong form factor or wrong era) yields no build, so a
             * direct setStackInSlot or a board installed before an era gate existed can never run the machine.
             */
            return null;
        }
        if (!(this.handler.getStackInSlot(this.layout.psuSlot()).getItem() instanceof PsuItem psu)) {
            return null;
        }
        /*
         * Every count is clamped to what the installed board exposes, so a part in a slot the board
         * does not offer is ignored.
         */
        final int cpuCount = Math.min(this.layout.cpuCount(), motherboard.spec().cpuSlots());
        final List<CpuSpec> cpus = new ArrayList<>();
        for (int i = 0; i < cpuCount; i++) {
            if (this.handler.getStackInSlot(this.layout.cpuStart() + i).getItem() instanceof CpuItem cpu) {
                cpus.add(cpu.spec());
            }
        }
        final int ramCount = Math.min(this.layout.ramCount(), motherboard.spec().ramSlots());
        final List<RamSpec> rams = new ArrayList<>();
        for (int i = 0; i < ramCount; i++) {
            if (this.handler.getStackInSlot(this.layout.ramStart() + i).getItem() instanceof RamItem ram) {
                rams.add(ram.spec());
            }
        }
        final int pcieCount = Math.min(this.layout.pcieCount(), motherboard.spec().pcieSlots());
        final List<IExpansionCardSpec> pcieCards = new ArrayList<>();
        for (int i = 0; i < pcieCount; i++) {
            if (this.handler.getStackInSlot(this.layout.pcieStart() + i).getItem()
                    instanceof IExpansionCardItem card) {
                pcieCards.add(card.cardSpec());
            }
        }
        final int diskCount = Math.min(this.layout.diskCount(), motherboard.spec().diskSlots());
        final List<DiskSpec> disks = new ArrayList<>();
        for (int i = 0; i < diskCount; i++) {
            if (this.handler.getStackInSlot(this.layout.diskStart() + i).getItem() instanceof DiskItem disk) {
                disks.add(disk.spec());
            }
        }
        return new ComputerBuild(motherboard.spec(), cpus, pcieCards, rams, psu.spec(), disks);
    }
}
