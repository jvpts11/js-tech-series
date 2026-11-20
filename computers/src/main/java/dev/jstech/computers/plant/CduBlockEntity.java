/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.plant;

import dev.jstech.computers.block.ServerRackStructure;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.inventory.FaceEnergyStorage;
import dev.jstech.core.inventory.FaceFluidHandler;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.inventory.ProductFluidHandler;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.util.Loaded;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The CDU at work (computers/docs/design/temperature.md): the coolant distribution unit between the racks and the
 * plant. Each second its pumps take the racks' Hot Water back and send them Cold Water, up to {@link #MOST_A_SECOND}
 * millibuckets (64 kW, eight racks), its plate exchanger carrying the heat across: for every millibucket it cools, a
 * millibucket of the plant's Cold Water comes out of it hot, for the Chiller. Its pumps draw their full
 * {@link #PUMP_WATTS} only at full load. Its four water ports are in its roof, its energy at its back.
 */
public class CduBlockEntity extends ProcessingMachineBlockEntity {

    /** The most of the racks' water it cools a second, in millibuckets: 64 kW, eight racks. */
    public static final int MOST_A_SECOND = 64;
    /** What its pumps draw at full load, in watts; an estimate. */
    public static final int PUMP_WATTS = 1_500;
    private static final int TANK = 4_000;
    private static final int TICKS = 20;
    private static final int OUTPUTS = 2;
    private static final long STORE_SECONDS = 4;
    private static final Layout LAYOUT = new Layout(0, 0, 0, 2, 2, TANK);
    private static final int TOP = ServerRackStructure.HEIGHT - 1;
    private static final Set<ResourceLocation> HOT = Set.of(CoolingWater.HOT_WATER.id());
    private static final Set<ResourceLocation> COLD = Set.of(CoolingWater.COLD_WATER.id());
    /* The plant's COLD inlet takes plain water as Cold Water, as every COLD inlet does: how a loop is first filled. */
    private static final Set<ResourceLocation> COLD_OR_WATER = Set.of(CoolingWater.COLD_WATER.id(),
            BuiltInRegistries.FLUID.getKey(Fluids.WATER));

    public CduBlockEntity(final BlockPos pos, final BlockState state) {
        super(CoolingPlant.CDU_BE.get(), pos, state, LAYOUT, MachineEnergy.of(JsEnergy.ENERGY, JsEnergy.LOW,
                PUMP_WATTS * STORE_SECONDS, 2L * PUMP_WATTS / TICKS), PUMP_WATTS / TICKS);
    }

    /** The CDU {@code part} is a block of, or none while it is bound to none that is loaded. */
    public static @Nullable CduBlockEntity of(final CduPartBlockEntity part) {
        final BlockPos origin = part.originPos();
        return origin == null || part.getLevel() == null ? null
                : Loaded.blockEntity(part.getLevel(), origin) instanceof CduBlockEntity cdu ? cdu : null;
    }

    /** Which way its back looks: the face its energy is taken on. */
    public Direction back() {
        return facing().getOpposite();
    }

    /**
     * The water port at {@code part}, one of its own blocks, from above: in its roof, as one faces its front, the
     * plant's COLD inlet at the front right and its HOT outlet at the back right, the racks' COLD outlet at the front
     * left and their HOT inlet at the back left; none elsewhere.
     */
    public @Nullable IFluidHandler waterPort(final BlockPos part) {
        if (part.equals(at(0, TOP, 0))) {
            return port(fluidsIn(), FaceMode.IN, COLD_OR_WATER);
        }
        if (part.equals(at(0, TOP, 1))) {
            return port(fluidsOut(), FaceMode.OUT, HOT);
        }
        if (part.equals(at(1, TOP, 0))) {
            return port(fluidsOut(), FaceMode.OUT, COLD);
        }
        return part.equals(at(1, TOP, 1)) ? port(fluidsIn(), FaceMode.IN, HOT) : null;
    }

    /** Its energy's port, of J's Energy, at the bottom block at its back left as one faces its front, or none. */
    public @Nullable IEnergyPort energyClassPort(final BlockPos part) {
        return part.equals(powerEntry()) ? power().portStore() : null;
    }

    /** The same port, as FE, for a cable of FE. */
    public @Nullable IEnergyStorage feEnergyPort(final BlockPos part) {
        final EnergyStore store = power().portStore();
        if (!part.equals(powerEntry()) || store == null || !store.energy().acceptsFe()) {
            return null;
        }
        return new FaceEnergyStorage(store.asFe(), FaceMode.IN);
    }

    /* Its pumps draw energy unless the server lets the computers and the machines run free, as the chain's do. */
    @Override
    public boolean takesEnergy() {
        return ComputersServerConfig.drawEnergy();
    }

    /* A second's work, as far as the racks' hot water, the plant's cold water and both outlets' room allow. */
    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        final int racks = PlantTanks.holding(input, CoolingWater.HOT.get());
        final int cold = PlantTanks.holding(input, CoolingWater.COLD.get());
        final int plant = cold >= 0 ? cold : PlantTanks.holding(input, Fluids.WATER);
        final int moved = Math.min(Math.min(MOST_A_SECOND, PlantTanks.amount(input, racks)),
                Math.min(PlantTanks.amount(input, plant), Math.min(PlantTanks.room(this, OUTPUTS,
                        CoolingWater.COLD.get()), PlantTanks.room(this, OUTPUTS, CoolingWater.HOT.get()))));
        if (moved <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Processing(List.of(), List.of(new Take(racks, moved), new Take(plant, moved)),
                List.of(), List.of(new FluidStack(CoolingWater.COLD.get(), moved),
                new FluidStack(CoolingWater.HOT.get(), moved)), TICKS,
                PlantTanks.energyPerTick(PUMP_WATTS, moved, MOST_A_SECOND), List.of(), List.of(), List.of(),
                List.of()));
    }

    private BlockPos powerEntry() {
        return at(1, 0, 1);
    }

    private BlockPos at(final int w, final int h, final int d) {
        return ServerRackStructure.at(worldPosition, facing(), w, h, d);
    }

    private Direction facing() {
        return getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    private static @Nullable IFluidHandler port(final @Nullable IFluidHandler tanks, final FaceMode mode,
                                                final Set<ResourceLocation> products) {
        return tanks == null ? null : new ProductFluidHandler(new FaceFluidHandler(tanks, mode, ItemFilter.EVERYTHING),
                products);
    }
}
