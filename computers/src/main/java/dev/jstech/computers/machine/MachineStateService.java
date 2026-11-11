/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.AccessPointBlockEntity;
import dev.jstech.computers.hardware.PsuCondition;
import dev.jstech.computers.hardware.PsuConditions;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.ThermalState;
import dev.jstech.computers.hardware.WifiCardSpec;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.power.UpsReading;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.storage.DiskHealth;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.wireless.WirelessLink;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The machine's own readings as its programs read them: what it takes from the line, the supply and the UPS that feed
 * it, how hot it runs, and how it reaches its network by air. Each is a picture taken when it is asked for, read from
 * what the machine already keeps for its firmware and its system's tools, so a program sees what the Task Manager sees.
 */
public final class MachineStateService {

    private final IOsHost machine;
    private final IComputerTerminalHost terminal;
    /** The same machine as a block: a computer, or the rack of a server, which answers for that server. */
    private final BlockEntity block;
    private final ServerLevel level;

    /** The tenths of a percent in a whole percent: a disk keeps its damage and bad sectors in tenths. */
    private static final int PERMILLE = 10;

    /** A power supply as a program reads it. */
    public record Supply(String name, int ratingWatts, int units, int unitsFailed, boolean degraded, boolean failed) {
    }

    /** A UPS as a program reads it. */
    public record Ups(String name, boolean onBattery, boolean bypass, int chargePercent, long minutesLeft) {
    }

    /**
     * One drive's health as a program reads it: whether the disk died, its damage and bad sectors in whole percent,
     * the items corruption holds, and the tick a Defrag pass last ended on it, -1 when none has.
     */
    public record DiskCondition(String drive, boolean dead, int damagePercent, int badSectorsPercent,
                                long corruptedItems, long lastDefragTick) {
    }

    /** The machine's Wi-Fi as a program reads it: blank and nought for what it does not have. */
    public record Wifi(boolean joined, String accessPoint, String standard, int speed, int signalPercent) {

        /** No Wi-Fi card, or a machine that cannot hold one. */
        static final Wifi NONE = new Wifi(false, "", "", 0, 0);
    }

    MachineStateService(final IOsHost machine, final IComputerTerminalHost terminal, final BlockEntity block,
                        final ServerLevel level) {
        this.machine = machine;
        this.terminal = terminal;
        this.block = block;
        this.level = level;
    }

    /** What the machine takes from the line now, in watts: its parts over its supply's efficiency; nothing when off. */
    public int drawWatts() {
        return this.machine.isRunning() ? this.terminal.lineWatts() : 0;
    }

    /** The supply that feeds the machine and how worn it is, or null when it has none. */
    @Nullable
    public Supply supply() {
        final ItemStack psu = this.machine.psuStack();
        final PsuSpec spec = PsuConditions.spec(psu);
        if (spec == null) {
            return null;
        }
        final PsuCondition condition = PsuConditions.of(psu);
        return new Supply(psu.getHoverName().getString(), spec.wattage(), spec.units(), condition.failedUnits(spec),
                condition.degraded(spec), condition.failed(spec));
    }

    /** The UPS that carries the machine, or null when none does. */
    @Nullable
    public Ups ups() {
        final UpsReading reading = this.machine.upsReading();
        if (!reading.present()) {
            return null;
        }
        return new Ups(this.machine.upsName().english(), reading.onBattery(),
                reading.mode() == UpsReading.Mode.BYPASS, reading.percent(), reading.minutesLeft());
    }

    /** How the machine's heat stands; unknown for a machine with no temperature of its own. */
    public ThermalState thermal() {
        return this.machine.thermal();
    }

    /**
     * The health of every drive with a disk or medium in it, in letter order, as the machine's Defrag reads it
     * (computers/docs/design/storage-devices.md).
     */
    public List<DiskCondition> disks() {
        final ComputerConsoleState console = this.machine.console();
        final List<DiskCondition> all = new ArrayList<>();
        for (final DriveTable.Drive drive : DriveTable.of(this.block, this.level).all()) {
            if (drive.disk().isEmpty()) {
                continue;
            }
            final DiskHealth health = DriveVolumes.health(drive.disk());
            final UUID volume = drive.disk().get(ComputingComponents.DISK_VOLUME.get());
            all.add(new DiskCondition(String.valueOf(drive.drive()), health.dead(), health.damage() / PERMILLE,
                    health.badSectors() / PERMILLE, health.corruptedItems(),
                    console == null ? -1L : console.defrag().lastRun(volume)));
        }
        return all;
    }

    /** The health of drive {@code letter}, whatever its case, or null when the machine has no disk there. */
    @Nullable
    public DiskCondition disk(final String letter) {
        for (final DiskCondition one : this.disks()) {
            if (one.drive().equalsIgnoreCase(letter)) {
                return one;
            }
        }
        return null;
    }

    /** The machine's Wi-Fi: its card's standard, and the access point it joined with the speed of that link. */
    public Wifi wifi() {
        if (!(this.block instanceof AbstractComputerBlockEntity computer)) {
            return Wifi.NONE;
        }
        final WifiCardSpec card = computer.wifiCard();
        if (card == null) {
            return Wifi.NONE;
        }
        final WirelessLink link = computer.wirelessLink(this.level);
        if (link == null) {
            return new Wifi(false, "", card.standard().cardName(), 0, 0);
        }
        final int signal = Loaded.blockEntity(this.level, link.point()) instanceof AccessPointBlockEntity point
                ? point.signalPercent(this.block.getBlockPos()) : 0;
        return new Wifi(true, AccessPointBlockEntity.hostname(link.pointNode()), card.standard().cardName(),
                link.itemsPerTick(), signal);
    }
}
