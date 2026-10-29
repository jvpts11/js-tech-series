/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.moving.BlockAboard;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The blocks what moving structures do aboard is proved with, each given its part by the test mod's own data map
 * ({@code data/jscore/data_maps/block/block_aboard.json}): a decoupler, a docking clamp, a drill, a harvester, a
 * plough, a placer, a seat and a helm. Their block states and models are written by hand. And the couch pod, whose
 * formed model lays the sitter of the seat inside it down, as a capsule lays its crew on their couches.
 */
public final class TestAboard {

    /* As strong as stone. */
    private static final UnaryOperator<BlockBehaviour.Properties> STONE_STRONG = properties -> properties
            .strength(1.5F, 6.0F);
    /** A decoupler, firing at a signal, pushing its two sides apart. */
    public static final BlockEntry<TestPhysics.FacingBlock> DECOUPLER = block("test_decoupler", "Test decoupler",
            TestPhysics.FacingBlock::new);
    /** A docking clamp, closing at a signal behind it. */
    public static final BlockEntry<TestPhysics.FacingBlock> CLAMP = block("test_clamp", "Test clamp",
            TestPhysics.FacingBlock::new);
    /** A drill, mining as an iron pickaxe does. */
    public static final BlockEntry<TestPhysics.FacingBlock> DRILL = block("test_drill", "Test drill",
            TestPhysics.FacingBlock::new);
    /** A harvester, taking ripe crops. */
    public static final BlockEntry<TestPhysics.FacingBlock> HARVESTER = block("test_harvester", "Test harvester",
            TestPhysics.FacingBlock::new);
    /** A plough, tilling the ground. */
    public static final BlockEntry<TestPhysics.FacingBlock> PLOUGH = block("test_plough", "Test plough",
            TestPhysics.FacingBlock::new);
    /** A placer, setting the blocks the structure carries. */
    public static final BlockEntry<TestPhysics.FacingBlock> PLACER = block("test_placer", "Test placer",
            TestPhysics.FacingBlock::new);
    /** A seat. */
    public static final BlockEntry<Block> SEAT = block("test_seat", "Test seat", Block::new);
    /** A helm, a seat whose sitter steers. */
    public static final BlockEntry<Block> HELM = block("test_helm", "Test helm", Block::new);
    /** The couch pod's pattern: the test turbine's, with a seat in the middle of its top layer. */
    public static final ResourceLocation COUCH_POD_PATTERN_ID = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_kit_couch_pod");
    /** How high over its block a sitter on the couch pod's couch lies. */
    public static final double COUCH_HEIGHT = 0.1;
    /** The couch pod's model, the test turbine's own, which lays the sitter of its seat down. */
    public static final DeclaredModel COUCH_POD_MODEL = TestSounds.CONTENT.model("test_kit_couch_pod")
            .file("jsmodels/test_kit_turbine.bbmodel")
            .texture("minecraft:textures/block/iron_block.png")
            .formedFrom(COUCH_POD_PATTERN_ID)
            .aboard(SEAT.getId(), new BlockAboard(Optional.of(new BlockAboard.Seat(COUCH_HEIGHT, false, true)),
                    Optional.empty(), Optional.empty(), Optional.empty()))
            .register();
    public static final MultiblockPattern COUCH_POD_PATTERN = MultiblockPatterns.declare(COUCH_POD_PATTERN_ID,
            MultiblockPattern.builder("test_kit_couch_pod")
                    .layer("III", "III", "I#I")
                    .layer("III", "ISI", "III")
                    .where('I', BlockMatch.blocks("minecraft:iron_block"))
                    .where('S', BlockMatch.blocks("jstests:test_seat"))
                    .terminal("jstests:test_kit_couch_pod")
                    .build());
    public static final BlockEntry<DeviceBlock> COUCH_POD = TestSounds.CONTENT.block("test_kit_couch_pod",
                    Device.of(() -> TestAboard.COUCH_POD_ENTITY.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test kit couch pod").look(IBlockLook.cubeAll("test_kit_turbine")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CouchPodEntity>> COUCH_POD_ENTITY =
            TestSounds.CONTENT.blockEntity("test_kit_couch_pod", CouchPodEntity::new, COUCH_POD);

    private TestAboard() {
    }

    /** Declares the blocks, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static <B extends Block> BlockEntry<B> block(final String id, final String name,
                                                         final Function<BlockBehaviour.Properties, B> factory) {
        return TestSounds.CONTENT.block(id, factory).properties(STONE_STRONG).named(name)
                .look(IBlockLook.cubeAll(id)).register();
    }

    /** The couch pod: a structure that merges its blocks, its seat among them, into it once formed. */
    public static final class CouchPodEntity extends MachineBlockEntity {

        public CouchPodEntity(final BlockPos pos, final BlockState state) {
            super(COUCH_POD_ENTITY.get(), pos, state, 0, MachineEnergy.fe(0, 0, 0));
            formsWith(MachineStructure.of(COUCH_POD_PATTERN).merges());
        }

        @Override
        protected MachineStatus work(final ServerLevel level) {
            return MachineStatus.WORKING;
        }
    }
}
