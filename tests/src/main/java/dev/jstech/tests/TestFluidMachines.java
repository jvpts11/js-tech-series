/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.fluid.IValveRule;
import dev.jstech.core.fluid.LineValveBlockEntity;
import dev.jstech.core.fluid.ReliefValveBlockEntity;
import dev.jstech.core.heat.BoilOff;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.BoxShape;
import dev.jstech.core.multiblock.InsideRule;
import dev.jstech.core.multiblock.Limit;
import dev.jstech.core.multiblock.Place;
import dev.jstech.industrial.line.IndustrialPump;
import dev.jstech.industrial.line.IndustrialTank;
import dev.jstech.industrial.line.PumpSource;
import dev.jstech.industrial.line.TankRating;
import dev.jstech.industrial.machine.MachineRating;
import dev.jstech.industrial.multiblock.CasingMaterial;
import dev.jstech.industrial.multiblock.IndustrialMultiblock;
import dev.jstech.industrial.multiblock.SizeGives;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * J's Industrial's fluid contract, on blocks of the test mod's own: a tank of liquid and a vessel of gas at twenty
 * atmospheres, each a box of a size the player chooses, its room inside its volume; a water pump, a drainage pump and
 * a well pump;
 * and J's Core's valves: one that lets through one way always, one opened by hand, one by a signal, one of gas, and a
 * relief valve set to ten atmospheres.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestFluidMachines {

    public static final IndustrialMultiblock TANK = IndustrialMultiblock.of(IndustrialTier.T1, CasingMaterial.IRON)
            .growing(SizeGives.blocksInside());
    public static final IndustrialMultiblock VESSEL = IndustrialMultiblock.of(IndustrialTier.T3,
            CasingMaterial.STEEL).growing(SizeGives.blocksInside());
    public static final TankRating TANK_RATING = TankRating.liquid(IndustrialTier.T1, BoilOff.BARE);
    public static final TankRating VESSEL_RATING = TankRating.gas(IndustrialTier.T3, 20, BoilOff.BARE);
    public static final MachineRating PUMP_RATING = MachineRating.of(IndustrialTier.T1, JsEnergy.LOW, 1_000L);
    /** What the water pump draws a tick, its tank, and how far the drainage pump reaches. */
    public static final int PUMPS = 100;
    public static final int PUMP_TANK = 4_000;
    public static final int DRAINS_WITHIN = 4;
    public static final int DRAINS_EVERY = 5;
    /** What the well pump draws a take, and how many ticks a take is. */
    public static final int WELL_DRAWS = 250;
    public static final int WELL_EVERY = 5;
    /** What a valve lets through a tick, and the pressure the relief valve opens above. */
    public static final long VALVE_RATE = 100L;
    public static final long RELIEF_AT = 10L;

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<Block> TANK_CASING = TANK.built(TestSounds.CONTENT.block("test_tank_casing",
            Block::new)).named("Test tank casing").look(IBlockLook.cubeAll("test_tank_casing"))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).register();
    public static final BlockEntry<Block> VESSEL_CASING = VESSEL.built(TestSounds.CONTENT.block(
            "test_vessel_casing", Block::new)).named("Test vessel casing")
            .look(IBlockLook.cubeAll("test_vessel_casing")).tag(BlockTags.MINEABLE_WITH_PICKAXE).register();
    public static final BlockEntry<DeviceBlock> LIQUID_TANK = device("test_liquid_tank", "Test liquid tank",
            () -> TestFluidMachines.LIQUID_TANK_ENTITY.get());
    public static final BlockEntry<DeviceBlock> GAS_VESSEL = device("test_gas_vessel", "Test gas vessel",
            () -> TestFluidMachines.GAS_VESSEL_ENTITY.get());
    public static final BlockEntry<DeviceBlock> WATER_PUMP = device("test_water_pump", "Test water pump",
            () -> TestFluidMachines.WATER_PUMP_ENTITY.get());
    public static final BlockEntry<DeviceBlock> DRAINAGE_PUMP = device("test_drainage_pump", "Test drainage pump",
            () -> TestFluidMachines.DRAINAGE_PUMP_ENTITY.get());
    public static final BlockEntry<DeviceBlock> WELL_PUMP = device("test_well_pump", "Test well pump",
            () -> TestFluidMachines.WELL_PUMP_ENTITY.get());
    public static final BlockEntry<DeviceBlock> CHECK_VALVE = valve("test_check_valve", "Test check valve");
    public static final BlockEntry<DeviceBlock> HAND_VALVE = valve("test_hand_valve", "Test hand valve");
    public static final BlockEntry<DeviceBlock> SIGNAL_VALVE = valve("test_signal_valve", "Test signal valve");
    public static final BlockEntry<DeviceBlock> GAS_VALVE = TestSounds.CONTENT.block("test_gas_valve",
            Device.of(() -> TestFluidMachines.GAS_VALVE_ENTITY.get()).ticks(LineValveBlockEntity::serverTick)::block)
            .named("Test gas valve").look(IBlockLook.cubeAll("test_gas_valve")).register();
    public static final BlockEntry<DeviceBlock> RELIEF_VALVE = TestSounds.CONTENT.block("test_relief_valve",
            Device.of(() -> TestFluidMachines.RELIEF_VALVE_ENTITY.get())
                    .ticks(ReliefValveBlockEntity::serverTick)::block)
            .named("Test relief valve").look(IBlockLook.cubeAll("test_relief_valve")).register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquidTank>> LIQUID_TANK_ENTITY =
            TestSounds.CONTENT.blockEntity("test_liquid_tank", LiquidTank::new, LIQUID_TANK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasVessel>> GAS_VESSEL_ENTITY =
            TestSounds.CONTENT.blockEntity("test_gas_vessel", GasVessel::new, GAS_VESSEL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterPump>> WATER_PUMP_ENTITY =
            TestSounds.CONTENT.blockEntity("test_water_pump", WaterPump::new, WATER_PUMP);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrainagePump>> DRAINAGE_PUMP_ENTITY =
            TestSounds.CONTENT.blockEntity("test_drainage_pump", DrainagePump::new, DRAINAGE_PUMP);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WellPump>> WELL_PUMP_ENTITY =
            TestSounds.CONTENT.blockEntity("test_well_pump", WellPump::new, WELL_PUMP);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LineValveBlockEntity>> VALVE_ENTITY =
            TestSounds.CONTENT.blockEntity("test_valve", (pos, state) -> new LineValveBlockEntity(
                    TestFluidMachines.VALVE_ENTITY.get(), pos, state, MatterState.LIQUID, VALVE_RATE,
                    ruleOf(state)), CHECK_VALVE, HAND_VALVE, SIGNAL_VALVE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LineValveBlockEntity>> GAS_VALVE_ENTITY =
            TestSounds.CONTENT.blockEntity("test_gas_valve", (pos, state) -> new LineValveBlockEntity(
                    TestFluidMachines.GAS_VALVE_ENTITY.get(), pos, state, MatterState.GAS, VALVE_RATE,
                    IValveRule.ALWAYS), GAS_VALVE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReliefValveBlockEntity>>
            RELIEF_VALVE_ENTITY = TestSounds.CONTENT.blockEntity("test_relief_valve", (pos, state) ->
            new ReliefValveBlockEntity(TestFluidMachines.RELIEF_VALVE_ENTITY.get(), pos, state, VALVE_RATE,
                    RELIEF_AT), RELIEF_VALVE);

    /** The tank's shape: a box of its casing, its terminal on a face, nothing inside, three to seven a side. */
    public static final BoxShape TANK_SHAPE = BoxShape.of(Place.of(BlockMatch.blocks("jstests:test_tank_casing")),
            Place.of(BlockMatch.blocks("jstests:test_tank_casing")), InsideRule.empty(), new Limit(3, 7),
            new Limit(3, 7));
    public static final BoxShape VESSEL_SHAPE = BoxShape.of(Place.of(BlockMatch.blocks("jstests:test_vessel_casing")),
            Place.of(BlockMatch.blocks("jstests:test_vessel_casing")), InsideRule.empty(), new Limit(3, 7),
            new Limit(3, 7));

    private TestFluidMachines() {
    }

    /** Declares the blocks, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        LineValveBlockEntity.register(event, VALVE_ENTITY.get());
        LineValveBlockEntity.register(event, GAS_VALVE_ENTITY.get());
        ReliefValveBlockEntity.register(event, RELIEF_VALVE_ENTITY.get());
    }

    private static <E extends MachineBlockEntity> BlockEntry<DeviceBlock> device(final String id,
                                                                                 final String english,
                                                                                 final Supplier<BlockEntityType<E>>
                                                                                         type) {
        return TestSounds.CONTENT.block(id, Device.of(type).ticks(MachineBlockEntity::serverTick)::block)
                .named(english).look(IBlockLook.cubeAll(id)).register();
    }

    private static BlockEntry<DeviceBlock> valve(final String id, final String english) {
        return TestSounds.CONTENT.block(id, Device.of(() -> TestFluidMachines.VALVE_ENTITY.get())
                        .ticks(LineValveBlockEntity::serverTick)::block)
                .named(english).look(IBlockLook.cubeAll(id)).register();
    }

    /* A liquid valve's rule, by its block: by hand, by a signal, or always. */
    private static IValveRule ruleOf(final BlockState state) {
        if (state.is(HAND_VALVE.get())) {
            return IValveRule.BY_HAND;
        }
        return state.is(SIGNAL_VALVE.get()) ? IValveRule.BY_SIGNAL : IValveRule.ALWAYS;
    }

    /** The test tank of liquid. */
    public static final class LiquidTank extends IndustrialTank {

        public LiquidTank(final BlockPos pos, final BlockState state) {
            super(LIQUID_TANK_ENTITY.get(), pos, state, TANK_RATING, TANK, TANK_SHAPE);
        }
    }

    /** The test vessel of gas. */
    public static final class GasVessel extends IndustrialTank {

        public GasVessel(final BlockPos pos, final BlockState state) {
            super(GAS_VESSEL_ENTITY.get(), pos, state, VESSEL_RATING, VESSEL, VESSEL_SHAPE);
        }
    }

    /** The test water pump. */
    public static final class WaterPump extends IndustrialPump {

        public WaterPump(final BlockPos pos, final BlockState state) {
            super(WATER_PUMP_ENTITY.get(), pos, state, PUMP_RATING, PumpSource.endlessWater(PUMPS), PUMP_TANK);
        }
    }

    /** The test drainage pump. */
    public static final class DrainagePump extends IndustrialPump {

        public DrainagePump(final BlockPos pos, final BlockState state) {
            super(DRAINAGE_PUMP_ENTITY.get(), pos, state, PUMP_RATING, PumpSource.drains(DRAINS_WITHIN,
                    DRAINS_EVERY), PUMP_TANK);
        }
    }

    /** The test well pump. */
    public static final class WellPump extends IndustrialPump {

        public WellPump(final BlockPos pos, final BlockState state) {
            super(WELL_PUMP_ENTITY.get(), pos, state, PUMP_RATING, PumpSource.well(WELL_DRAWS, WELL_EVERY),
                    PUMP_TANK);
        }
    }
}
