/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

import dev.jstech.core.blockentity.IFieldPart;
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

/**
 * The workings of a robots' charging station, for a block entity to hold: a store of charge, filled by cables, given
 * every half second to each robot standing within two blocks, as much as it takes. A robot told to recharge finds the
 * nearest station working. A block entity declares it as a part of its fields, calls {@link #tick} each server tick,
 * and offers its store to cables through {@link #offer}.
 */
public final class RobotCharger implements IFieldPart {

    private final EnergyStorage store;
    private final int flow;

    /* Every half second it charges the robots in reach. */
    private static final int EVERY = 10;
    /** How near a robot stands to be charged, in blocks. */
    public static final double REACH = 2.0;
    private static final String SAVED = "charger";

    /**
     * A station holding {@code capacity} FE, giving each robot at most {@code flow} FE every half second, and running
     * {@code onChange} when what it holds changes.
     */
    public RobotCharger(final int capacity, final int flow, final Runnable onChange) {
        if (capacity <= 0 || flow <= 0) {
            throw new IllegalArgumentException("a charging station holds and gives something");
        }
        this.flow = flow;
        this.store = new EnergyStorage(capacity) {
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

    /** Offers the stations of the block entities of {@code type} to cables, on every side. */
    public static <B extends BlockEntity> void offer(final RegisterCapabilitiesEvent event,
                                                     final BlockEntityType<B> type,
                                                     final Function<B, RobotCharger> charger) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type,
                (entity, side) -> charger.apply(entity).store);
    }

    /** Its store of charge, in FE, a joule each. */
    public EnergyStorage store() {
        return store;
    }

    /** Charges the robots in reach of the station at {@code pos} every half second; called each server tick. */
    public void tick(final ServerLevel level, final BlockPos pos) {
        if (level.getGameTime() % EVERY != 0) {
            return;
        }
        RobotChargers.heard(level, pos);
        for (final RobotEntity robot : level.getEntitiesOfClass(RobotEntity.class, new AABB(pos).inflate(REACH),
                robot -> robot.isAlive() && robot.batteryCapacity() > 0)) {
            final int given = robot.energy().receiveEnergy(Math.min(flow, store.getEnergyStored()), false);
            if (given > 0) {
                store.extractEnergy(given, false);
            }
        }
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put(SAVED, store.serializeNBT(registries));
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        if (tag.contains(SAVED)) {
            store.deserializeNBT(registries, tag.get(SAVED));
        }
    }
}
