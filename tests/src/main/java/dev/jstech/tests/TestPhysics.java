/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.moving.StructureReadings;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.signal.SensorBlock;
import dev.jstech.core.signal.SensorBlockEntity;
import dev.jstech.core.signal.Signals;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The blocks the physics of moving structures is proved with, each given its forces by the test mod's own data map
 * ({@code data/jscore/data_maps/block/block_physics.json}): a thruster, a wheel, a burner, a light block for rafts
 * and decks, a lighter one for envelopes, a levitator and a magnet; and a flight sensor, which speaks what a sensor
 * aboard reads in redstone. Each is as strong as stone, so a blow a test means to break glass with leaves them whole.
 * Their block states and models are written by hand.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestPhysics {

    /* As strong as stone: as hard to mine, and as hard to break by a blow. */
    private static final UnaryOperator<BlockBehaviour.Properties> STONE_STRONG = properties -> properties
            .strength(1.5F, 6.0F);
    /** A rocket of 20 kilonewtons, pushing the way it faces while it has a signal. */
    public static final BlockEntry<FacingBlock> THRUSTER = block("test_thruster", "Test thruster", FacingBlock::new);
    /** A wheel on a spring, rolling the way it faces, driven by a signal behind it. */
    public static final BlockEntry<FacingBlock> WHEEL = block("test_wheel", "Test wheel", FacingBlock::new);
    /** A burner of five megawatts, always lit, heating the room above it. */
    public static final BlockEntry<Block> BURNER = block("test_burner", "Test burner", Block::new);
    /** A block of five kilograms, a raft's or a cart's deck. */
    public static final BlockEntry<Block> LIGHT = block("test_light_block", "Test light block", Block::new);
    /** A block of two hundred grams, a balloon's envelope. */
    public static final BlockEntry<Block> ENVELOPE = block("test_envelope", "Test envelope", Block::new);
    /** A levitator holding up 200 kilonewtons. */
    public static final BlockEntry<Block> HOVER = block("test_hover", "Test hover", Block::new);
    /** A magnet of 50 kilonewtons, pulling magnetic blocks within six. */
    public static final BlockEntry<Block> MAGNET = block("test_magnet", "Test magnet", Block::new);
    /** A flight sensor: what it reads of its flight aboard a moving structure, in redstone, its altitude first. */
    public static final BlockEntry<FlightSensorBlock> FLIGHT_SENSOR = block("test_flight_sensor",
            "Test flight sensor", FlightSensorBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlightSensorBlockEntity>> FLIGHT_ENTITY =
            TestSounds.CONTENT.blockEntity("test_flight_sensor", FlightSensorBlockEntity::new, FLIGHT_SENSOR);
    /**
     * The thrust pod's pattern: a thruster with its terminal on top, and a light block beside each; once formed, the
     * thruster and the light blocks merge into the pod.
     */
    public static final ResourceLocation POD_PATTERN_ID = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_kit_thrust_pod");
    public static final MultiblockPattern POD_PATTERN = MultiblockPatterns.declare(POD_PATTERN_ID,
            MultiblockPattern.builder("test_kit_thrust_pod")
                    .layer("T", "L")
                    .layer("#", "L")
                    .where('T', BlockMatch.blocks("jstests:test_thruster"))
                    .where('L', BlockMatch.blocks("jstests:test_light_block"))
                    .terminal("jstests:test_kit_thrust_pod")
                    .build());
    /** The thrust pod's terminal, of twenty kilograms, as the test mod's data map says. */
    public static final BlockEntry<DeviceBlock> POD = TestSounds.CONTENT.block("test_kit_thrust_pod",
                    Device.of(() -> TestPhysics.POD_ENTITY.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test kit thrust pod").look(IBlockLook.cubeAll("test_kit_turbine")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PodEntity>> POD_ENTITY =
            TestSounds.CONTENT.blockEntity("test_kit_thrust_pod", PodEntity::new, POD);

    private TestPhysics() {
    }

    /** Declares the blocks, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        Signals.offer(event, FLIGHT_ENTITY.get());
    }

    private static <B extends Block> BlockEntry<B> block(final String id, final String name,
                                                         final Function<BlockBehaviour.Properties, B> factory) {
        return TestSounds.CONTENT.block(id, factory).properties(STONE_STRONG).named(name)
                .look(IBlockLook.cubeAll(id)).register();
    }

    /** The thrust pod: a structure that merges its thruster into it once formed, and does nothing else. */
    public static final class PodEntity extends MachineBlockEntity {

        public PodEntity(final BlockPos pos, final BlockState state) {
            super(POD_ENTITY.get(), pos, state, 0, MachineEnergy.fe(0, 0, 0));
            formsWith(MachineStructure.of(POD_PATTERN).merges());
        }

        @Override
        protected MachineStatus work(final ServerLevel level) {
            return MachineStatus.WORKING;
        }
    }

    /** The flight sensor's block. */
    public static final class FlightSensorBlock extends SensorBlock {

        public FlightSensorBlock(final Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new FlightSensorBlockEntity(pos, state);
        }
    }

    /** The flight sensor: everything a sensor aboard reads, looking north. */
    public static final class FlightSensorBlockEntity extends SensorBlockEntity {

        public FlightSensorBlockEntity(final BlockPos pos, final BlockState state) {
            super(FLIGHT_ENTITY.get(), pos, state);
        }

        @Override
        public List<Reading> readings() {
            return level == null ? List.of() : StructureReadings.of(level, worldPosition);
        }
    }

    /** A block that faces one of the six ways, the way it was placed looking. */
    public static final class FacingBlock extends DirectionalBlock {

        public static final MapCodec<FacingBlock> CODEC = simpleCodec(FacingBlock::new);

        public FacingBlock(final Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        public BlockState getStateForPlacement(final BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
        }

        @Override
        protected MapCodec<? extends DirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }
    }
}
