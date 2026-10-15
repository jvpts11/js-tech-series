/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.heat.BoilOff;
import dev.jstech.core.heat.Boiler;
import dev.jstech.core.heat.FluidPass;
import dev.jstech.core.heat.HeatBody;
import dev.jstech.core.heat.HeatMath;
import dev.jstech.core.heat.HeatPorts;
import dev.jstech.core.heat.HeatSource;
import dev.jstech.core.heat.IHeatHolder;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterTank;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Blocks of the test mod that hold heat, to prove the Core's heat: a litre of copper, a litre of iron, the same litre
 * of copper with ten square metres of fins as a radiator, and a heater that puts fifty kilowatts into a litre of iron
 * up to its limit; and vessels with a cubic metre of iron for a wall, a boiler in the open air, a boiler of low
 * pressure, a heat exchanger whose oil passes through the wall, a cold tank open to the place's heat and a vacuum
 * flask.
 */
public final class TestHeat {

    /** A litre: small enough that a test sees its temperature move in a few seconds. */
    public static final double LITRE = 0.001;
    /** The radiator's fins, in square metres. */
    public static final double FINS = 10.0;
    /** What the heater puts in, in watts: enough to warm a litre of copper beside it within seconds. */
    public static final double HEATER_WATTS = 50_000.0;
    /** The hottest the heater makes itself, in kelvin. */
    public static final double HEATER_LIMIT = 700.0;
    /** What a vessel's wall passes to what boils or flows through it, in watts a kelvin. */
    public static final double VESSEL_CONDUCTANCE = 100_000.0;
    /** The pressure the low-pressure boiler holds, in kilopascals: water boils there at about 44 °C. */
    public static final double LOW_PRESSURE = 10.0;
    /** How much each of a vessel's liquid tanks holds, in millibuckets. */
    public static final int VESSEL_TANK = 8_000;
    /** How much a vessel's gas tank holds, in millibuckets at the open air's pressure. */
    public static final long VESSEL_GAS = 10_000_000L;
    /**
     * How the cold tank lets the place's heat in, in watts a square metre a kelvin: a hundred times a bare tank, as if
     * finned, so a test sees its nitrogen boil off within seconds.
     */
    public static final double OPEN_TANK = 1_000.0;
    /** A cold tank's surface, in square metres. */
    public static final double TANK_SURFACE = 10.0;

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<DeviceBlock> COPPER = TestSounds.CONTENT.block("test_heat_copper",
            Device.of(() -> TestHeat.BLOCK_ENTITY.get())::block)
            .named("Test heat copper").look(IBlockLook.cubeAll("test_heat_copper")).register();
    public static final BlockEntry<DeviceBlock> IRON = TestSounds.CONTENT.block("test_heat_iron",
            Device.of(() -> TestHeat.BLOCK_ENTITY.get())::block)
            .named("Test heat iron").look(IBlockLook.cubeAll("test_heat_iron")).register();
    public static final BlockEntry<DeviceBlock> RADIATOR = TestSounds.CONTENT.block("test_radiator",
            Device.of(() -> TestHeat.BLOCK_ENTITY.get())::block)
            .named("Test radiator").look(IBlockLook.cubeAll("test_radiator")).register();
    public static final BlockEntry<DeviceBlock> HEATER = TestSounds.CONTENT.block("test_heater",
            Device.of(() -> TestHeat.BLOCK_ENTITY.get()).ticks(HeatBlockEntity::tick)::block)
            .named("Test heater").look(IBlockLook.cubeAll("test_heater")).register();
    public static final BlockEntry<DeviceBlock> BOILER = TestSounds.CONTENT.block("test_boiler",
            Device.of(() -> TestHeat.VESSEL.get()).ticks(VesselBlockEntity::tick)::block)
            .named("Test boiler").look(IBlockLook.cubeAll("test_boiler")).register();
    public static final BlockEntry<DeviceBlock> LOW_BOILER = TestSounds.CONTENT.block("test_low_boiler",
            Device.of(() -> TestHeat.VESSEL.get()).ticks(VesselBlockEntity::tick)::block)
            .named("Test low-pressure boiler").look(IBlockLook.cubeAll("test_low_boiler")).register();
    public static final BlockEntry<DeviceBlock> EXCHANGER = TestSounds.CONTENT.block("test_exchanger",
            Device.of(() -> TestHeat.VESSEL.get()).ticks(VesselBlockEntity::tick)::block)
            .named("Test heat exchanger").look(IBlockLook.cubeAll("test_exchanger")).register();
    public static final BlockEntry<DeviceBlock> COLD_TANK = TestSounds.CONTENT.block("test_cold_tank",
            Device.of(() -> TestHeat.VESSEL.get()).ticks(VesselBlockEntity::tick)::block)
            .named("Test cold tank").look(IBlockLook.cubeAll("test_cold_tank")).register();
    public static final BlockEntry<DeviceBlock> DEWAR = TestSounds.CONTENT.block("test_dewar",
            Device.of(() -> TestHeat.VESSEL.get()).ticks(VesselBlockEntity::tick)::block)
            .named("Test vacuum flask").look(IBlockLook.cubeAll("test_dewar")).register();
    /* One block entity for the four that hold heat, which reads from its block what it is; another for the vessels. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatBlockEntity>> BLOCK_ENTITY =
            TestSounds.CONTENT.blockEntity("test_heat", HeatBlockEntity::new, COPPER, IRON, RADIATOR, HEATER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VesselBlockEntity>> VESSEL =
            TestSounds.CONTENT.blockEntity("test_vessel", VesselBlockEntity::new, BOILER, LOW_BOILER, EXCHANGER,
                    COLD_TANK, DEWAR);

    private static final HeatSource HEATING = HeatSource.heater(HEATER_WATTS, HEATER_LIMIT);

    private TestHeat() {
    }

    /** Declares the blocks, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** A block that holds heat, of the material and with the fins its block says, the heater heating itself. */
    public static final class HeatBlockEntity extends SyncedBlockEntity implements IHeatHolder {

        private final HeatBody heat;

        public HeatBlockEntity(final BlockPos pos, final BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
            final boolean copper = state.is(COPPER.get()) || state.is(RADIATOR.get());
            this.heat = HeatBody.of(Thermal.of(Elements.of(copper ? Element.COPPER : Element.IRON), false), LITRE)
                    .exposed(state.is(RADIATOR.get()) ? FINS : 0.0);
            HeatPorts.keep(this, this.heat);
        }

        /** The heater puts its watts in, each tick, up to its limit. */
        public static void tick(final Level level, final BlockPos pos, final BlockState state,
                                final HeatBlockEntity heater) {
            HEATING.apply(heater.heat, HeatMath.TICK_SECONDS);
        }

        @Override
        public HeatBody heatBody() {
            return this.heat;
        }
    }

    /**
     * A vessel: a cubic metre of iron for its wall, a tank for the liquid that goes in, one for the liquid that comes
     * out and one of gas. A boiler boils what goes in into the gas, the low-pressure one at its own pressure; the
     * exchanger passes what goes in through its wall into its other form; a cold tank lets the place's heat boil off
     * what it holds into the gas, the vacuum flask hardly at all.
     */
    public static final class VesselBlockEntity extends SyncedBlockEntity implements IHeatHolder {

        private final HeatBody heat = HeatBody.of(Thermal.of(Elements.of(Element.IRON), false), 1.0);
        private final FluidTank in = new FluidTank(VESSEL_TANK);
        private final FluidTank out = new FluidTank(VESSEL_TANK);
        private final MatterTank gas = new MatterTank(MatterState.GAS, VESSEL_GAS, 1L, () -> { });
        private final Boiler boiler = Boiler.of(VESSEL_CONDUCTANCE);
        private final FluidPass pass = FluidPass.of(VESSEL_CONDUCTANCE);
        private final BoilOff boilOff;

        public VesselBlockEntity(final BlockPos pos, final BlockState state) {
            super(VESSEL.get(), pos, state);
            if (state.is(LOW_BOILER.get())) {
                this.boiler.at(LOW_PRESSURE);
            }
            this.boilOff = BoilOff.of(state.is(DEWAR.get()) ? BoilOff.VACUUM_JACKETED : OPEN_TANK, TANK_SURFACE);
            HeatPorts.keep(this, this.heat);
            fields().part("Boiler", this.boiler).save();
            fields().part("Pass", this.pass).save();
            fields().part("BoilOff", this.boilOff).save();
        }

        /** Each tick a boiler boils, the exchanger passes its liquid through its wall, and a cold tank boils off. */
        public static void tick(final Level level, final BlockPos pos, final BlockState state,
                                final VesselBlockEntity vessel) {
            if (state.is(EXCHANGER.get())) {
                vessel.pass.run(vessel.heat, vessel.in, vessel.out, HeatMath.TICK_SECONDS);
            } else if (state.is(COLD_TANK.get()) || state.is(DEWAR.get())) {
                vessel.boilOff.vent(level, pos, vessel.in, vessel.gas, HeatMath.TICK_SECONDS);
            } else {
                vessel.boiler.boil(level, pos, vessel.heat, vessel.in, vessel.gas, HeatMath.TICK_SECONDS);
            }
        }

        @Override
        public HeatBody heatBody() {
            return this.heat;
        }

        /** The tank the liquid goes in. */
        public FluidTank in() {
            return this.in;
        }

        /** The tank the exchanger's liquid comes out in. */
        public FluidTank out() {
            return this.out;
        }

        /** The tank a boiler's gas comes out in. */
        public MatterTank gas() {
            return this.gas;
        }

        /** The boiler. */
        public Boiler boiler() {
            return this.boiler;
        }
    }
}
