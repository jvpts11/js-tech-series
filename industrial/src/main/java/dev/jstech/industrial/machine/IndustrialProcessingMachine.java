/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.machine;

import dev.jstech.core.machine.IMachineDamage;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.machine.UpgradeEffect;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.industrial.line.AirPort;
import dev.jstech.industrial.line.CompressedAir;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A processing machine of J's Industrial, J's Core's processing machine under the mod's rules
 * (industrial/docs/design/machines.md): a port of J's Energy of its rating's class, drawing its rating's power as it
 * works; its upgrade slots by its tier; upgrades that add what they gain and multiply what they save; damaged, half
 * its speed and a quarter more drawn, its upgrades idle; as it works, what its rating says it gives off, its radiation
 * held in by a Lead Shielding in its upgrade slots; and, for a machine with an air port, compressed air spent each tick
 * it works, for fifteen percent more speed, a gain added beside its upgrades'.
 */
public abstract class IndustrialProcessingMachine extends ProcessingMachineBlockEntity {

    private final MachineRating rating;
    private final MachineEmissions emissions;
    private final @Nullable AirPort air;

    /** A machine of {@code rating} with the slots and tanks of {@code layout}, its upgrade slots set by its tier. */
    protected IndustrialProcessingMachine(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                          final MachineRating rating, final Layout layout) {
        this(type, pos, state, rating, layout, 0L);
    }

    /**
     * A machine of {@code rating} with the slots and tanks of {@code layout} and, unless {@code airPerTick} is none,
     * an air port spending that many millibuckets of air a tick it works.
     */
    protected IndustrialProcessingMachine(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                          final MachineRating rating, final Layout layout, final long airPerTick) {
        super(type, pos, state, layout.withUpgrades(IndustrialMachines.upgradeSlots(rating.tier())),
                IndustrialMachines.energy(rating), rating.joulesPerTick());
        if (airPerTick < 0) {
            throw new IllegalArgumentException("a machine spends some air a tick or none, not " + airPerTick);
        }
        this.rating = Objects.requireNonNull(rating, "rating");
        this.emissions = new MachineEmissions(rating.emissions());
        this.air = airPerTick == 0 ? null : AirPort.machine(fields(), airPerTick, this::setChanged);
        fields().whenBroken(this.emissions::stop);
    }

    /** What it says of itself: its tier, its port's class, its power and what it gives off. */
    public MachineRating rating() {
        return this.rating;
    }

    /** Its air port, for a machine that has one. */
    public Optional<AirPort> airPort() {
        return Optional.ofNullable(this.air);
    }

    /** Whether something in its upgrade slots holds its radiation in. */
    public boolean shielded() {
        for (int slot = layout().firstUpgrade(); slot < layout().slots(); slot++) {
            if (getInventory().getStackInSlot(slot).is(IndustrialMachines.SHIELDS_RADIATION)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected IMachineDamage damageRule() {
        return IndustrialMachines.DAMAGE;
    }

    @Override
    protected UpgradeEffect combineUpgrades(final List<UpgradeEffect.Counted> each) {
        return UpgradeEffect.addGains(each);
    }

    /** What its upgrades do to it, and its air while its port holds a tick of it, as its damage leaves them. */
    @Override
    protected UpgradeEffect effect() {
        final UpgradeEffect upgrades = upgradeEffect();
        return damageRule().effect(health(), this.air != null && this.air.ready()
                ? upgrades.withSpeed(upgrades.speed() + CompressedAir.SPEED_GAIN) : upgrades);
    }

    @Override
    protected void settled(final ServerLevel level, final MachineStatus status) {
        super.settled(level, status);
        this.emissions.settle(level, this.worldPosition, status == MachineStatus.WORKING, shielded());
        if (this.air != null && status == MachineStatus.WORKING) {
            this.air.spend();
        }
    }

    @Override
    protected Set<MachineResource> resources() {
        final Set<MachineResource> resources = super.resources();
        if (this.air != null) {
            MachineResource.of(MatterState.GAS).ifPresent(resources::add);
        }
        return resources;
    }

    @Override
    protected @Nullable IMatterHandler matterHandler(final MatterState state) {
        final IMatterHandler own = super.matterHandler(state);
        return this.air != null && state == MatterState.GAS ? this.air.with(own) : own;
    }
}
