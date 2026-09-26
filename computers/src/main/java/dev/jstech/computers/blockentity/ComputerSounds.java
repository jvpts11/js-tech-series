/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.IAudible;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What a computer sounds like as a machine: its power button, an old machine's own start-up, and a hard drive
 * spinning up when it comes on, turning while it runs and winding down when it goes off. A solid-state disk makes
 * none of the drive's sounds, and neither does a machine with no disk.
 *
 * <p>The server hears the machine come on and go off, which catches every way it does (the button, the system
 * shutting down, a part taken out, the machine coming up by itself), and plays those moments once. The turning disk
 * is a running sound, which the client keeps playing for as long as the server says the disk turns.
 */
final class ComputerSounds implements IAudible {

    private final AbstractComputerBlockEntity machine;
    /**
     * Whether the machine was running at the last look. A machine just placed was off; one read back from the world
     * starts unknown and is taken as it is at its first look, so a world loading plays nothing.
     */
    @Nullable
    private Boolean wasRunning = Boolean.FALSE;
    /** The game time a spinning-up disk reaches its speed and is heard turning, or {@link #NOT_SPINNING_UP}. */
    private long turnsFrom = NOT_SPINNING_UP;
    /** Whether the disk is heard turning: worked out on the server and sent to the client. */
    private boolean turning;

    private static final long NOT_SPINNING_UP = -1L;
    /** The spin-up runs eight and a half seconds; the turning is heard from its end. */
    private static final int SPIN_UP_TICKS = 170;
    /** An old machine's own start-up carries its drives; the disk is heard turning just before that sound ends. */
    private static final int VINTAGE_TURNS_AFTER = 90;
    /** How often a running machine checks whether a hard drive went in or came out. */
    private static final int DISK_CHECK_TICKS = 20;
    private static final String NBT_TURNING = "DiskTurning";

    ComputerSounds(final AbstractComputerBlockEntity machine) {
        this.machine = machine;
    }

    @Override
    public double audioX() {
        return machine.getBlockPos().getX() + 0.5;
    }

    @Override
    public double audioY() {
        return machine.getBlockPos().getY() + 0.5;
    }

    @Override
    public double audioZ() {
        return machine.getBlockPos().getZ() + 0.5;
    }

    @Override
    public List<LoopRequest> loops() {
        return turning ? List.of(LoopRequest.of(ComputingSounds.HARD_DRIVE_IDLE)) : List.of();
    }

    /** The machine was read back from the world: whatever it is doing, it was doing it before anyone came near. */
    void loaded() {
        wasRunning = null;
    }

    void tick(final ServerLevel level) {
        final boolean running = machine.isRunning();
        if (wasRunning == null) {
            wasRunning = running;
            setTurning(level, running && hasHardDrive());
            return;
        }
        if (running != wasRunning) {
            wasRunning = running;
            if (running) {
                cameOn(level);
            } else {
                wentOff(level);
            }
            return;
        }
        final long now = level.getGameTime();
        if (turnsFrom != NOT_SPINNING_UP && now >= turnsFrom) {
            turnsFrom = NOT_SPINNING_UP;
            setTurning(level, running && hasHardDrive());
        } else if (running && now % DISK_CHECK_TICKS == 0) {
            reconsiderDisk(level);
        }
    }

    /** The player pressed the power button. */
    void pressed(final ServerLevel level) {
        Audio.at(level, machine.getBlockPos(), ComputingSounds.POWER_BUTTON);
    }

    void saveForClient(final CompoundTag tag) {
        tag.putBoolean(NBT_TURNING, turning);
    }

    void loadFromClient(@Nullable final CompoundTag tag) {
        turning = tag != null && tag.getBoolean(NBT_TURNING);
    }

    private void cameOn(final ServerLevel level) {
        if (machine.installedEra() == HardwareEra.VINTAGE) {
            Audio.at(level, machine.getBlockPos(), ComputingSounds.VINTAGE_STARTUP);
            turnsFrom = hasHardDrive() ? level.getGameTime() + VINTAGE_TURNS_AFTER : NOT_SPINNING_UP;
        } else if (hasHardDrive()) {
            spinUp(level);
        }
    }

    private void wentOff(final ServerLevel level) {
        final boolean spinning = turning || turnsFrom != NOT_SPINNING_UP;
        turnsFrom = NOT_SPINNING_UP;
        if (spinning && hasHardDrive()) {
            Audio.at(level, machine.getBlockPos(), ComputingSounds.HARD_DRIVE_SPIN_DOWN);
        }
        setTurning(level, false);
    }

    /* A drive taken out of a running machine stops being heard at once; one put in spins up. */
    private void reconsiderDisk(final ServerLevel level) {
        final boolean hardDrive = hasHardDrive();
        if (!hardDrive && turning) {
            setTurning(level, false);
        } else if (hardDrive && !turning && turnsFrom == NOT_SPINNING_UP) {
            spinUp(level);
        }
    }

    private void spinUp(final ServerLevel level) {
        Audio.at(level, machine.getBlockPos(), ComputingSounds.HARD_DRIVE_SPIN_UP);
        turnsFrom = level.getGameTime() + SPIN_UP_TICKS;
    }

    private void setTurning(final ServerLevel level, final boolean value) {
        if (turning == value) {
            return;
        }
        turning = value;
        final BlockPos pos = machine.getBlockPos();
        final BlockState state = machine.getBlockState();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    private boolean hasHardDrive() {
        for (final ItemStack disk : machine.diskStacks()) {
            if (disk.getItem() instanceof DiskItem drive && drive.spec().tier() == StorageTier.HDD) {
                return true;
            }
        }
        return false;
    }
}
