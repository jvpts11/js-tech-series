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
import dev.jstech.core.machine.DataMachineBlock;
import dev.jstech.core.machine.DataMachineBlockEntity;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.BoxShape;
import dev.jstech.core.multiblock.InsideRule;
import dev.jstech.core.multiblock.Limit;
import dev.jstech.core.multiblock.MultiblockPortBlock;
import dev.jstech.core.multiblock.MultiblockPortBlockEntity;
import dev.jstech.core.multiblock.MultiblockPorts;
import dev.jstech.core.multiblock.Place;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.core.multiblock.RedstonePortBlock;
import dev.jstech.core.multiblock.RedstonePortBlockEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Multiblock machines of the test mod's own, built by the player block by block. A tank of any size from three to
 * seven a side, iron on its edges, iron or glass on its faces with hoppers among them as its valves, and empty inside,
 * its terminal on a face: it runs as many recipes at once as it has valves. And a smelter of the same build whose
 * faces take ports of their own (items in, items out, energy in, redstone): it has three lanes, so ports of up to
 * three colours each feed their own, and runs two recipes at once in each. And machines declared in their files
 * alone: a baker, a furnace on the test furnace's pattern, and a terminal whose file is nowhere.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestStructures {

    /** The tank's shape: iron edges, iron, glass or hopper faces, air inside, three to seven a side. */
    public static final BoxShape TANK_SHAPE = BoxShape.of(Place.of(BlockMatch.blocks("minecraft:iron_block")),
                    Place.of(BlockMatch.blocks("minecraft:iron_block", "minecraft:glass", "minecraft:hopper"))
                            .counting("valve", BlockMatch.blocks("minecraft:hopper")),
                    InsideRule.empty(), new Limit(3, 7), new Limit(3, 7))
            .limit("valve", 0, 4);
    /** The smelter's shape: iron edges, iron, glass or ports of their own on its faces, air inside. */
    public static final BoxShape SMELTER_SHAPE = BoxShape.of(Place.of(BlockMatch.blocks("minecraft:iron_block")),
            Place.of(new BlockMatch(List.of("minecraft:iron_block", "minecraft:glass"),
                    List.of(MultiblockPorts.PORTS_TAG))),
            InsideRule.empty(), new Limit(3, 5), new Limit(3, 5));
    /** The smelter's lanes: one for each colour of port it keeps apart. */
    public static final ProcessingMachineBlockEntity.Layout SMELTER_LAYOUT =
            ProcessingMachineBlockEntity.Layout.items(1, 1).withLanes(3);
    /** How many ticks the smelter takes to make stone of cobblestone. */
    public static final int SMELTS_IN = 5;
    private static final int ENERGY = 1_000;
    private static final int SMELTER_ENERGY = 10_000;
    private static final long SMELTER_SPENDS = 10L;

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<DeviceBlock> TANK = TestSounds.CONTENT.block("test_tank_terminal",
            Device.of(() -> TestStructures.TANK_ENTITY.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test tank terminal").look(IBlockLook.cubeAll("test_tank_terminal")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK_ENTITY =
            TestSounds.CONTENT.blockEntity("test_tank_terminal", TankBlockEntity::new, TANK);
    public static final BlockEntry<DeviceBlock> SMELTER = TestSounds.CONTENT.block("test_smelter_terminal",
            Device.of(() -> TestStructures.SMELTER_ENTITY.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test smelter terminal").look(IBlockLook.cubeAll("test_smelter_terminal")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmelterBlockEntity>> SMELTER_ENTITY =
            TestSounds.CONTENT.blockEntity("test_smelter_terminal", SmelterBlockEntity::new, SMELTER);
    public static final BlockEntry<MultiblockPortBlock> ITEM_INPUT = port("test_item_input_port",
            "Test item input port");
    public static final BlockEntry<MultiblockPortBlock> ITEM_OUTPUT = port("test_item_output_port",
            "Test item output port");
    public static final BlockEntry<MultiblockPortBlock> ENERGY_INPUT = port("test_energy_port", "Test energy port");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortBlockEntity>> PORT_ENTITY =
            TestSounds.CONTENT.blockEntity("test_port", PortBlockEntity::new, ITEM_INPUT, ITEM_OUTPUT,
                    ENERGY_INPUT);
    public static final BlockEntry<RedstonePortBlock> REDSTONE = TestSounds.CONTENT.block("test_redstone_port",
            properties -> new RedstonePortBlock(properties, () -> TestStructures.REDSTONE_ENTITY.get()))
            .named("Test redstone port").look(IBlockLook.cubeAll("test_redstone_port")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstonePortBlockEntity>>
            REDSTONE_ENTITY = TestSounds.CONTENT.blockEntity("test_redstone_port",
            (pos, state) -> new RedstonePortBlockEntity(TestStructures.REDSTONE_ENTITY.get(), pos, state), REDSTONE);
    /** A machine declared in its file alone: a box of ports that bakes clay, three at once. */
    public static final BlockEntry<DataMachineBlock> BAKER = dataMachine("test_baker", "Test baker", "baker");
    /** One whose file names the test furnace's pattern by its id. */
    public static final BlockEntry<DataMachineBlock> DATA_FURNACE = dataMachine("test_data_furnace",
            "Test data furnace", "data_furnace");
    /** One whose block names a file there is not. */
    public static final BlockEntry<DataMachineBlock> NO_MACHINE = dataMachine("test_no_machine", "Test no machine",
            "nowhere");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DataMachineBlockEntity>> DATA_ENTITY =
            TestSounds.CONTENT.blockEntity("test_data_machine",
                    (pos, state) -> new DataMachineBlockEntity(TestStructures.DATA_ENTITY.get(), pos, state), BAKER,
                    DATA_FURNACE, NO_MACHINE);
    /** What the baker's file says it works: clay into bricks. */
    public static final ProcessingKind BAKING = TestSounds.CONTENT.processing("test_baking", "Test Baking",
            () -> TestStructures.BAKER.get());

    private TestStructures() {
    }

    /** Declares the terminals and the ports, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        MultiblockPorts.register(event, PORT_ENTITY.get());
    }

    /* The terminal of the machine of the test mod's file {@code machine}. */
    private static BlockEntry<DataMachineBlock> dataMachine(final String name, final String english,
                                                            final String machine) {
        return TestSounds.CONTENT.block(name, properties -> new DataMachineBlock(properties,
                        Device.of(() -> TestStructures.DATA_ENTITY.get()).ticks(MachineBlockEntity::serverTick)
                                .opensMenu(MachineMenu::new),
                        ResourceLocation.fromNamespaceAndPath(JsTests.MODID, machine)))
                .named(english).look(IBlockLook.cubeAll(name)).register();
    }

    /* A port block of the test mod, its kind its block's. */
    private static BlockEntry<MultiblockPortBlock> port(final String name, final String english) {
        return TestSounds.CONTENT.block(name, properties -> new MultiblockPortBlock(properties,
                () -> TestStructures.PORT_ENTITY.get())).named(english).look(IBlockLook.cubeAll(name)).register();
    }

    /** The tank's terminal: works whenever its structure stands, running a recipe at once for each valve. */
    public static final class TankBlockEntity extends MachineBlockEntity {

        public TankBlockEntity(final BlockPos pos, final BlockState state) {
            super(TANK_ENTITY.get(), pos, state, 0, MachineEnergy.fe(ENERGY, ENERGY, 0));
            formsWith(MachineStructure.of(TANK_SHAPE).parallel(formed -> Math.max(1, formed.count("valve"))));
        }

        @Override
        protected MachineStatus work(final ServerLevel level) {
            return MachineStatus.WORKING;
        }
    }

    /** The smelter's terminal: cobblestone into stone in each lane, two at once. */
    public static final class SmelterBlockEntity extends ProcessingMachineBlockEntity {

        public SmelterBlockEntity(final BlockPos pos, final BlockState state) {
            super(SMELTER_ENTITY.get(), pos, state, SMELTER_LAYOUT, MachineEnergy.fe(SMELTER_ENERGY, SMELTER_ENERGY,
                    0), SMELTER_SPENDS);
            formsWith(MachineStructure.of(SMELTER_SHAPE).parallel(formed -> 2));
        }

        @Override
        protected Optional<Processing> process(final Level level, final ProcessingInput input) {
            return input.items().getFirst().is(Items.COBBLESTONE)
                    ? Optional.of(Processing.single(0, new ItemStack(Items.STONE), SMELTS_IN)) : Optional.empty();
        }
    }

    /** A port of the test mod: items in, items out or energy in, as its block is. */
    public static final class PortBlockEntity extends MultiblockPortBlockEntity {

        public PortBlockEntity(final BlockPos pos, final BlockState state) {
            super(PORT_ENTITY.get(), pos, state, state.is(ITEM_INPUT.get()) ? PortKind.ITEM_INPUT
                    : state.is(ITEM_OUTPUT.get()) ? PortKind.ITEM_OUTPUT : PortKind.ENERGY_INPUT);
        }
    }
}
