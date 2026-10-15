/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.inventory.NamedSubject;
import dev.jstech.core.inventory.Transfers;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What a machine does by itself through its faces ({@link MachineSides.Automation}): it pushes what it makes into the
 * block beside each face that gives out, and pulls what it works from the block beside each face that takes in, every
 * {@link #EVERY} ticks, each resource at most its rate a second, only what its filter lets pass.
 */
public final class MachineTransfers {

    /** How often a machine pushes and pulls, in ticks. */
    public static final int EVERY = 10;
    private static final int TICKS_A_SECOND = 20;

    private MachineTransfers() {
    }

    /** One round of {@code machine}'s pushing and pulling, in {@code level}. */
    public static void run(final ServerLevel level, final MachineBlockEntity machine) {
        final MachineSides sides = machine.sides();
        for (final MachineResource resource : sides.resources()) {
            final MachineSides.Automation auto = sides.automation(resource);
            if (!auto.push() && !auto.pull()) {
                continue;
            }
            for (final Direction side : Direction.values()) {
                final FaceMode mode = sides.mode(resource, machine.getBlockState(), side);
                final BlockPos beside = machine.getBlockPos().relative(side);
                if (auto.push() && mode.givesOut()) {
                    move(level, machine, resource, beside, side.getOpposite(), true, share(auto.pushRate()),
                            auto.filter());
                }
                if (auto.pull() && mode.takesIn()) {
                    move(level, machine, resource, beside, side.getOpposite(), false, share(auto.pullRate()),
                            auto.filter());
                }
            }
        }
    }

    /* What a rate a second comes to in one round. */
    private static int share(final int perSecond) {
        return Math.max(1, perSecond * EVERY / TICKS_A_SECOND);
    }

    private static void move(final ServerLevel level, final MachineBlockEntity machine,
                             final MachineResource resource, final BlockPos beside, final Direction face,
                             final boolean out, final int most, final ItemFilter filter) {
        switch (resource) {
            case ITEMS -> {
                final IItemHandler there = level.getCapability(Capabilities.ItemHandler.BLOCK, beside, face);
                if (there != null) {
                    Transfers.moveItems(out ? machine.itemsOut() : there, out ? there : machine.itemsIn(), filter,
                            most);
                }
            }
            case FLUIDS -> {
                final IFluidHandler there = level.getCapability(Capabilities.FluidHandler.BLOCK, beside, face);
                final IFluidHandler mine = out ? machine.fluidsOut() : machine.fluidsIn();
                if (there != null && mine != null) {
                    Transfers.moveFluid(out ? mine : there, out ? there : mine, most);
                }
            }
            case ENERGY -> moveEnergy(level, machine, beside, face, out, most);
            case GAS, SUPERCRITICAL, SLURRY, PLASMA, EXOTIC -> resource.matter().ifPresent(state ->
                    moveMatter(level, machine, state, beside, face, out, most, filter));
            case HEAT -> {
                // Heat flows by itself, from hot to cold, through the machine's heat port.
            }
        }
    }

    /* Energy pushed out of a generator's store into the block beside, or pulled from it, FE or a port alike. */
    private static void moveEnergy(final ServerLevel level, final MachineBlockEntity machine, final BlockPos beside,
                                   final Direction face, final boolean out, final int most) {
        final IEnergyStorage fe = machine.power().feStore();
        if (fe != null) {
            final IEnergyStorage there = level.getCapability(Capabilities.EnergyStorage.BLOCK, beside, face);
            if (there != null) {
                Transfers.moveEnergy(out ? fe : there, out ? there : fe, most);
            }
            return;
        }
        final EnergyStore store = machine.power().portStore();
        final IEnergyPort there = level.getCapability(EnergyPorts.PORT, beside, face);
        if (store == null || there == null || there.energy() != store.energy()
                || there.portClass() != store.portClass()) {
            return;
        }
        final IEnergyPort from = out ? store : there;
        final IEnergyPort to = out ? there : store;
        final long fits = to.receive(from.extract(most, true), true);
        if (fits > 0) {
            to.receive(from.extract(fits, false), false);
        }
    }

    /* Matter of one state pushed from the machine's outputs into the block beside, or pulled into its inputs. */
    private static void moveMatter(final ServerLevel level, final MachineBlockEntity machine, final MatterState state,
                                   final BlockPos beside, final Direction face, final boolean out, final int most,
                                   final ItemFilter filter) {
        final @Nullable IMatterHandler there = MatterCapabilities.block(state)
                .map(capability -> level.getCapability(capability, beside, face)).orElse(null);
        if (there == null) {
            return;
        }
        if (out) {
            for (final MatterTank tank : machine.matterOut(state)) {
                final Optional<MatterStack> offered = tank.extractAny(most, true);
                if (offered.isEmpty() || !passes(filter, offered.get())) {
                    continue;
                }
                final long fits = there.insert(offered.get(), true);
                if (fits > 0) {
                    there.insert(offered.get().withAmount(fits), false);
                    tank.extract(offered.get().material(), fits, false);
                    return;
                }
            }
            return;
        }
        final Optional<MatterStack> offered = there.extractAny(most, true);
        if (offered.isEmpty() || !passes(filter, offered.get())) {
            return;
        }
        final List<MatterTank> inputs = machine.matterIn(state);
        long fits = 0L;
        for (final MatterTank tank : inputs) {
            fits += tank.insert(offered.get().withAmount(offered.get().amount() - fits), true);
            if (fits >= offered.get().amount()) {
                break;
            }
        }
        if (fits <= 0) {
            return;
        }
        final long taken = there.extract(offered.get().material(), fits, false);
        long left = taken;
        for (final MatterTank tank : inputs) {
            if (left <= 0) {
                break;
            }
            left -= tank.insert(offered.get().withAmount(left), false);
        }
    }

    private static boolean passes(final ItemFilter filter, final MatterStack stack) {
        return filter.allows(new NamedSubject(stack.material().id().toString()));
    }
}
