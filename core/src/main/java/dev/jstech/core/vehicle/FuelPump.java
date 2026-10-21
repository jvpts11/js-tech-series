/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The workings of a fuel pump, for a block entity to hold: a tank of liquid, a tank a gas is pressed into and a store
 * of charge, filled by pipes, gas lines and cables, and given every half second to each vehicle standing within three
 * blocks, as much of each as the vehicle takes. A vehicle takes into its fuel tank only what its engine burns and
 * charges its battery only while it has one, so one pump serves a diesel van, a gas bus and an electric car alike.
 *
 * <p>A block entity declares the pump as a part of its fields, so it is saved with them, calls {@link #tick} each
 * server tick, and hands its tanks to pipes through {@link #offer}.
 */
public final class FuelPump implements IFieldPart {

    private final FluidTank liquid;
    private final MatterTank gas;
    private final EnergyStorage charge;
    private final Flow flow;

    /* Every half second it fills the vehicles in reach. */
    private static final int EVERY = 10;
    /* It reaches the vehicles standing within three blocks of it. */
    private static final double REACH = 3;
    /* A gas is pressed into its tank as into a vehicle's, at two hundred times the open air's pressure. */
    private static final long GAS_PRESSURE = 200;
    private static final String LIQUID = "pump_liquid";
    private static final String GAS = "pump_gas";
    private static final String CHARGE = "pump_charge";

    /**
     * A pump holding {@code size}, giving each vehicle at most {@code flow} a fill, and running {@code onChange} when
     * what it holds changes.
     */
    public FuelPump(final Size size, final Flow flow, final Runnable onChange) {
        this.flow = flow;
        this.liquid = new FluidTank(size.liquid()) {
            @Override
            protected void onContentsChanged() {
                onChange.run();
            }
        };
        this.gas = new MatterTank(MatterState.GAS, size.gas(), GAS_PRESSURE, onChange);
        this.charge = new EnergyStorage(size.charge()) {
            @Override
            public int receiveEnergy(final int toReceive, final boolean simulate) {
                final int taken = super.receiveEnergy(toReceive, simulate);
                if (!simulate && taken > 0) {
                    onChange.run();
                }
                return taken;
            }

            @Override
            public int extractEnergy(final int toExtract, final boolean simulate) {
                final int given = super.extractEnergy(toExtract, simulate);
                if (!simulate && given > 0) {
                    onChange.run();
                }
                return given;
            }
        };
    }

    /**
     * Offers the pumps of the block entities of {@code type} to pipes, gas lines and cables on every side: its liquid,
     * its gas and its charge, as {@code pump} finds each one's pump.
     */
    public static <B extends BlockEntity> void offer(final RegisterCapabilitiesEvent event,
                                                     final BlockEntityType<B> type,
                                                     final Function<B, FuelPump> pump) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (entity, side) -> pump.apply(entity).liquid);
        event.registerBlockEntity(MatterCapabilities.GAS, type, (entity, side) -> pump.apply(entity).gas);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (entity, side) -> pump.apply(entity).charge);
    }

    /** Its liquid. */
    public FluidTank liquid() {
        return liquid;
    }

    /** Its gas. */
    public MatterTank gas() {
        return gas;
    }

    /** Its store of charge, in FE, a joule each. */
    public EnergyStorage charge() {
        return charge;
    }

    /** Fills the vehicles in reach of the pump at {@code pos} every half second; called each server tick. */
    public void tick(final ServerLevel level, final BlockPos pos) {
        if (level.getGameTime() % EVERY != 0) {
            return;
        }
        for (final VehicleEntity vehicle : level.getEntitiesOfClass(VehicleEntity.class, new AABB(pos).inflate(REACH),
                vehicle -> !vehicle.isRemoved())) {
            fill(vehicle);
        }
    }

    /** Gives {@code vehicle} what it takes, of each, up to the pump's flow; whether it took any. */
    public boolean fill(final VehicleEntity vehicle) {
        final VehicleHolds holds = vehicle.holds().orElse(null);
        if (holds == null) {
            return false;
        }
        boolean took = false;
        if (!liquid.isEmpty()) {
            final FluidStack offered = liquid.drain(flow.liquid(), IFluidHandler.FluidAction.SIMULATE);
            final int into = holds.liquid().fill(offered, IFluidHandler.FluidAction.EXECUTE);
            if (into > 0) {
                liquid.drain(into, IFluidHandler.FluidAction.EXECUTE);
                took = true;
            }
        }
        final Optional<MatterStack> pressed = gas.extractAny(flow.gas(), true);
        if (pressed.isPresent()) {
            final long into = holds.fillGas(pressed.get(), false);
            if (into > 0) {
                gas.extract(pressed.get().material(), into, false);
                took = true;
            }
        }
        final int charged = holds.receiveCharge(Math.min(flow.charge(), charge.getEnergyStored()), false);
        if (charged > 0) {
            charge.extractEnergy(charged, false);
            took = true;
        }
        return took;
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put(LIQUID, liquid.writeToNBT(registries, new CompoundTag()));
        final CompoundTag gasTag = new CompoundTag();
        gas.save(gasTag);
        tag.put(GAS, gasTag);
        tag.put(CHARGE, charge.serializeNBT(registries));
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        liquid.readFromNBT(registries, tag.getCompound(LIQUID));
        gas.load(tag.getCompound(GAS));
        if (tag.contains(CHARGE)) {
            charge.deserializeNBT(registries, tag.get(CHARGE));
        }
    }

    /**
     * What a pump holds: millibuckets of liquid, the volume its gas is pressed into, in millibuckets, and FE of charge.
     */
    public record Size(int liquid, long gas, int charge) {

        public Size {
            if (liquid < 0 || gas <= 0 || charge < 0) {
                throw new IllegalArgumentException("a pump holds no less than nothing and some gas");
            }
        }
    }

    /**
     * What a pump gives a vehicle at most each half second: millibuckets of liquid, millibuckets of gas as pressed and
     * FE of charge.
     */
    public record Flow(int liquid, long gas, int charge) {

        public Flow {
            if (liquid < 0 || gas < 0 || charge < 0) {
                throw new IllegalArgumentException("a pump gives no less than nothing");
            }
        }
    }
}
