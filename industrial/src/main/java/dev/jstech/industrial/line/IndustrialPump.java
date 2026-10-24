/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.line;

import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.machine.IMachineDamage;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineSides;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineTransfers;
import dev.jstech.core.machine.UpgradeEffect;
import dev.jstech.industrial.machine.IndustrialMachines;
import dev.jstech.industrial.machine.MachineRating;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A pump of J's Industrial, which takes fluids from the world, as its {@link PumpSource} says, into a tank it pushes
 * out of by itself into the pipes and machines on its faces (industrial/docs/design/fluids-and-gases.md): a machine on
 * the contract of every machine of the mod, drawing its rating's power while it pumps, and pumping half as fast
 * damaged. Machines push what they make; pumps are what takes from the world.
 */
public abstract class IndustrialPump extends MachineBlockEntity {

    private final MachineRating rating;
    private final PumpSource source;
    private final FieldFluidTank tank;
    private final PumpSource.Memory memory = new PumpSource.Memory();
    private int since;

    /** A pump of {@code rating} taking from {@code source}, holding {@code tankMillibuckets} until it pushes them. */
    protected IndustrialPump(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                             final MachineRating rating, final PumpSource source, final int tankMillibuckets) {
        super(type, pos, state, 0, IndustrialMachines.energy(rating));
        this.rating = Objects.requireNonNull(rating, "rating");
        this.source = Objects.requireNonNull(source, "source");
        this.tank = fields().fluid("Tank", tankMillibuckets).save().toClient().outputOnly().exposed();
        // What it pumps goes out by itself, twice a second, as a machine's discharge pump sends it.
        sides().setAutomation(MachineResource.FLUIDS, MachineSides.Automation.NONE.pushing(true,
                tankMillibuckets * MachineTransfers.EVERY));
    }

    /** What it says of itself. */
    public MachineRating rating() {
        return this.rating;
    }

    /** Where it takes from. */
    public PumpSource source() {
        return this.source;
    }

    /** What it has pumped and not yet pushed out. */
    public FieldFluidTank tank() {
        return this.tank;
    }

    /**
     * A tick of it: with room in its tank and its power, it takes what its source gives, once in as many ticks as a
     * take is; damaged, as many more as its damage slows it.
     */
    @Override
    protected MachineStatus work(final ServerLevel level) {
        final int room = this.tank.getCapacity() - this.tank.getFluidAmount();
        if (room <= 0) {
            return MachineStatus.OUTPUT_FULL;
        }
        final double share = damageRule().workShare(health());
        if (share <= 0) {
            return MachineStatus.DAMAGED;
        }
        this.since++;
        if (this.since < Math.max(1L, Math.round(this.source.every() / share))) {
            return MachineStatus.WORKING;
        }
        final long draw = damageRule().effect(health(), UpgradeEffect.NONE).energyPerTick(this.rating.joulesPerTick());
        if (power().stored() < draw) {
            return MachineStatus.NO_ENERGY;
        }
        final FluidStack taken = this.source.take(level, this.worldPosition, room, this.memory);
        if (taken.isEmpty()) {
            // Nothing to take: it waits, and tries again as soon as it wakes.
            return MachineStatus.WAITING;
        }
        this.since = 0;
        power().spend(draw);
        this.tank.fillInside(taken, IFluidHandler.FluidAction.EXECUTE);
        return MachineStatus.WORKING;
    }

    @Override
    protected IMachineDamage damageRule() {
        return IndustrialMachines.DAMAGE;
    }

    @Override
    protected Set<MachineResource> resources() {
        return EnumSet.of(MachineResource.FLUIDS, MachineResource.ENERGY);
    }

    @Override
    protected @Nullable IFluidHandler fluidsOut() {
        return this.tank;
    }
}
