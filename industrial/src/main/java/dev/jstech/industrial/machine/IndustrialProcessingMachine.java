/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.machine;

import dev.jstech.core.machine.IMachineDamage;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.machine.UpgradeEffect;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A processing machine of J's Industrial, J's Core's processing machine under the mod's rules
 * (industrial/docs/design/machines.md): a port of J's Energy of its rating's class, drawing its rating's power as it
 * works; its upgrade slots by its tier; upgrades that add what they gain and multiply what they save; damaged, half
 * its speed and a quarter more drawn, its upgrades idle; and, as it works, what its rating says it gives off, its
 * radiation held in by a Lead Shielding in its upgrade slots.
 */
public abstract class IndustrialProcessingMachine extends ProcessingMachineBlockEntity {

    private final MachineRating rating;
    private final MachineEmissions emissions;

    /** A machine of {@code rating} with the slots and tanks of {@code layout}, its upgrade slots set by its tier. */
    protected IndustrialProcessingMachine(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                          final MachineRating rating, final Layout layout) {
        super(type, pos, state, layout.withUpgrades(IndustrialMachines.upgradeSlots(rating.tier())),
                IndustrialMachines.energy(rating), rating.joulesPerTick());
        this.rating = Objects.requireNonNull(rating, "rating");
        this.emissions = new MachineEmissions(rating.emissions());
        fields().whenBroken(this.emissions::stop);
    }

    /** What it says of itself: its tier, its port's class, its power and what it gives off. */
    public MachineRating rating() {
        return this.rating;
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

    @Override
    protected void settled(final ServerLevel level, final MachineStatus status) {
        super.settled(level, status);
        this.emissions.settle(level, this.worldPosition, status == MachineStatus.WORKING, shielded());
    }
}
