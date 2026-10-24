/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.machine;

import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.LoadFollower;
import dev.jstech.core.machine.IMachineDamage;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineSides;
import dev.jstech.core.machine.MachineStatus;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A generator of J's Industrial: a port of J's Energy of its rating's class that follows the load, as a power station
 * with a governor does (industrial/docs/design/energy.md). Its port holds a round of what it pushes into the machines
 * against it ({@link IndustrialMachines#generatorEnergy}); each tick it makes again what was drawn from it, up to its
 * rating's power, through J's Core's {@link LoadFollower}, so it burns only what is drawn: a generator nothing draws
 * from rests and burns nothing. Damaged, it makes half as much.
 */
public abstract class IndustrialGenerator extends MachineBlockEntity {

    private final MachineRating rating;
    private final LoadFollower follower;
    private final MachineEmissions emissions;

    private static final int TICKS_A_SECOND = 20;

    /** A generator of {@code rating} with {@code slots} slots, following its load as {@code follower} does. */
    protected IndustrialGenerator(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                  final int slots, final MachineRating rating, final LoadFollower follower) {
        super(type, pos, state, slots, IndustrialMachines.generatorEnergy(rating));
        this.rating = Objects.requireNonNull(rating, "rating");
        this.follower = Objects.requireNonNull(follower, "follower");
        this.emissions = new MachineEmissions(rating.emissions());
        fields().whenBroken(this.emissions::stop);
        // A generator placed beside a machine feeds it, unless its Sides tab says otherwise.
        sides().setAutomation(MachineResource.ENERGY, MachineSides.Automation.NONE.pushing(true,
                (int) Math.min(Integer.MAX_VALUE, rating.joulesPerTick() * TICKS_A_SECOND)));
    }

    /** A generator of {@code rating} with {@code slots} slots, making at once whatever is drawn, up to its power. */
    protected IndustrialGenerator(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                  final int slots, final MachineRating rating) {
        this(type, pos, state, slots, rating, LoadFollower.instant(rating.joulesPerTick()));
    }

    /** What it says of itself: its tier, its port's class, its power and what it gives off. */
    public MachineRating rating() {
        return this.rating;
    }

    /** How its output follows the load. */
    public LoadFollower follower() {
        return this.follower;
    }

    /**
     * A tick of it: it offers again what the grid drew since the last, as its follower lets it and its fuel covers,
     * and burns the fuel for what it made; with nothing drawn it rests, and with no fuel it waits.
     */
    @Override
    protected MachineStatus work(final ServerLevel level) {
        final EnergyStore port = Objects.requireNonNull(power().portStore(), "a generator has a port");
        final long room = port.capacity() - port.stored();
        if (room <= 0 && this.follower.floor() <= 0) {
            this.follower.settle();
            return MachineStatus.OUTPUT_FULL;
        }
        final long share = (long) Math.floor(room * damageRule().workShare(health()));
        this.follower.draw(share, false);
        final long made = burn(this.follower.settle());
        if (made <= 0) {
            this.follower.stop();
            return MachineStatus.WAITING;
        }
        power().generate(made);
        return MachineStatus.WORKING;
    }

    @Override
    protected IMachineDamage damageRule() {
        return IndustrialMachines.DAMAGE;
    }

    @Override
    protected void settled(final ServerLevel level, final MachineStatus status) {
        super.settled(level, status);
        this.emissions.settle(level, this.worldPosition, status == MachineStatus.WORKING, false);
    }

    /** Burns its fuel for up to {@code joules}; how many joules its fuel covered. */
    protected abstract long burn(long joules);
}
