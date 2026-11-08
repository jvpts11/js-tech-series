/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.UpsBlock;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.power.UpsRating;
import dev.jstech.computers.power.UpsReading;
import dev.jstech.computers.power.UpsTexts;
import dev.jstech.core.bigblock.BigBlockPartBlockEntity;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.energy.EnergyFaults;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.energy.WrongClass;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.look.IDescribed;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * An MF PowerGuard at work (computers/docs/design/power.md). The line fills its inlet, and every computer standing
 * against it, on any face, is fed from there: while the line brings what they draw, it feeds them and charges the
 * battery on top; when the line goes or falls short, the battery carries them and their systems say so, each in its own
 * way; at a tenth of its charge it has them shut down cleanly, so nothing is lost. Above what it carries it is in
 * bypass: they run straight from the line, unprotected.
 *
 * <p>Every tick it tells the computers it feeds how it stands, for the battery their systems show; its own players are
 * sent it as it changes by a percent or a minute, for its lamps.
 */
public final class UpsBlockEntity extends SyncedBlockEntity implements IDescribed {

    private final UpsRating rating;
    private final UpsInlet inlet;
    private final UpsBattery battery = new UpsBattery();
    /* The computers it fed this tick, kept so its tick makes no list of its own. */
    private final List<AbstractComputerBlockEntity> loads = new ArrayList<>(Direction.values().length);
    /* What they drew in its last tick, in watts, for what it says to a player looking at it. */
    private double load;

    public UpsBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.UPS_BE.get(), pos, state);
        this.rating = state.getBlock() instanceof UpsBlock ups ? ups.rating() : UpsRating.POWERGUARD_300;
        this.inlet = new UpsInlet(this.rating, this::markDirtyOnly, this::struck);
        fields().part("Energy", this.inlet.store()).save();
        fields().part("Battery", new BatteryPart()).save().toClient();
    }

    /** Its server tick. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final UpsBlockEntity ups) {
        if (level instanceof ServerLevel server) {
            ups.tick(server);
        }
    }

    /** The energy port on {@code side}: the inlet on its back and sides, where the cable plugs in; none elsewhere. */
    @Nullable
    public IEnergyPort energyPort(@Nullable final Direction side) {
        return takesLineOn(side) ? this.inlet.store() : null;
    }

    /** What the game's FE capability sees on {@code side}: the inlet, while the series speaks FE; nothing otherwise. */
    @Nullable
    public IEnergyStorage feEnergy(@Nullable final Direction side) {
        return takesLineOn(side) ? this.inlet.fe() : null;
    }

    /** What it holds and carries. */
    public UpsRating rating() {
        return this.rating;
    }

    /** The era of its model. */
    public Era era() {
        return getBlockState().getBlock() instanceof UpsBlock ups ? ups.era() : HardwareEra.STANDARD;
    }

    /** How it stands, on either side: on the client as its players were last told. */
    public UpsReading reading() {
        return this.level != null && this.level.isClientSide() ? this.battery.shown()
                : this.battery.reading(this.rating, this.load);
    }

    /** What its battery holds, in joules. */
    public long charge() {
        return this.battery.charge();
    }

    /** Sets what its battery holds, for a test to start from. */
    public void setCharge(final long joules) {
        this.battery.setCharge(joules);
        markDirtyOnly();
    }

    @Override
    public void describe(final List<Text> lines, final boolean details) {
        final UpsReading reading = reading();
        if (reading.present()) {
            lines.add(UpsTexts.mode(reading.mode()));
            if (reading.mode() == UpsReading.Mode.BYPASS) {
                lines.add(UpsTexts.BYPASS_WHY.text());
            }
        }
        lines.add(UpsTexts.battery(reading));
        if (details) {
            lines.add(UpsBlock.holds(this.rating));
        }
    }

    /*
     * A tick: what the computers against it draw, where the power comes from, their feed, the battery's charge, and
     * what they are told: that they run on the battery as the line goes, and, with the battery low, to shut down.
     */
    private void tick(final ServerLevel level) {
        gatherLoads(level);
        double watts = 0.0;
        for (final AbstractComputerBlockEntity computer : this.loads) {
            watts += computer.isRunning() ? computer.lineWatts() : 0;
        }
        final long chargeBefore = this.battery.charge();
        final UpsReading.Mode was = this.battery.mode();
        final UpsReading.Mode mode = this.battery.carry(this.rating, this.inlet, watts);
        for (final AbstractComputerBlockEntity computer : this.loads) {
            this.inlet.feed(computer.energyPort());
        }
        this.battery.charge(this.rating, this.inlet, watts);
        this.inlet.settle();
        this.load = watts;
        final UpsReading reading = this.battery.reading(this.rating, watts);
        final boolean wentOnBattery = mode == UpsReading.Mode.ON_BATTERY && was != UpsReading.Mode.ON_BATTERY;
        final boolean low = this.battery.low(this.rating);
        for (final AbstractComputerBlockEntity computer : this.loads) {
            computer.upsTold(reading, level.getGameTime());
            // One already closing down, to switch off or to start over, is let finish: told again it would never end.
            if (!computer.isRunning() || computer.goingDown()) {
                continue;
            }
            if (wentOnBattery) {
                UpsNotices.onBattery(computer.getBlockPos(), computer, computer.consoleViewers(level),
                        ClientReplication.keyboardOf(computer.console()), reading);
            }
            if (low) {
                // As real UPS software does: the system shuts itself down cleanly before the battery runs out.
                computer.logEvent(level, EventLevel.WARN, UpsNotices.LOG_SHUTDOWN.text());
                computer.shutDown();
            }
        }
        if (this.battery.charge() != chargeBefore) {
            markDirtyOnly();
        }
        if (this.battery.shownChanged(reading)) {
            fields().syncToClients();
        }
    }

    /*
     * The computers standing against it, each once: a computer's own block, or a block of a Mainframe, which feeds the
     * Mainframe. A rack is not among them: it has its own UPS.
     */
    private void gatherLoads(final ServerLevel level) {
        this.loads.clear();
        for (final Direction face : Direction.values()) {
            BlockEntity against = Loaded.blockEntity(level, this.worldPosition.relative(face));
            if (against instanceof BigBlockPartBlockEntity part && part.originPos() != null) {
                against = Loaded.blockEntity(level, part.originPos());
            }
            if (against instanceof AbstractComputerBlockEntity computer && !this.loads.contains(computer)) {
                this.loads.add(computer);
            }
        }
    }

    /* Whether the cable plugs in on {@code side}: its back and either side. */
    private boolean takesLineOn(@Nullable final Direction side) {
        return side != null && UpsBlock.INLET.matches(getBlockState(), side);
    }

    /* A line two classes too high met the inlet: the smoke, or the arc flash that takes the UPS. */
    private void struck(final WrongClass what) {
        if (this.level instanceof ServerLevel server) {
            EnergyFaults.strike(server, this.worldPosition, what);
        }
    }

    /* The inlet's store changes every tick: the block is only marked for saving, and wakes no neighbour. */
    private void markDirtyOnly() {
        if (this.level != null) {
            this.level.blockEntityChanged(this.worldPosition);
        }
    }

    /* The battery's charge in the save; how it stands for the players who see it. */
    private final class BatteryPart implements IFieldPart {

        @Override
        public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
            battery.save(tag);
        }

        @Override
        public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
            battery.load(tag);
        }

        @Override
        public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
            UpsBattery.writeShown(tag, reading());
        }

        @Override
        public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
            battery.readShown(tag);
        }
    }
}
