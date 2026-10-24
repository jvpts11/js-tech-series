/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPartBlockEntity;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.MultiblockPorts;
import dev.jstech.core.multiblock.PortKind;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A multiblock of the test mod, to prove the Core's: a terminal and the casing round it, four of whose parts are
 * ports (items in on the north, items out on the south, energy in on the west, gas in on the east), declared as a
 * pattern in code and written to its file by the generator.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestMultiblocks {

    /** The pattern's id, the file the generator writes and a datapack overrides. */
    public static final ResourceLocation FURNACE_ID =
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_furnace");

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<TerminalBlock> TERMINAL = TestSounds.CONTENT.block("test_furnace_terminal",
            TerminalBlock::new).named("Test furnace terminal").look(IBlockLook.cubeAll("test_furnace_terminal"))
            .register();
    public static final BlockEntry<PartBlock> PART = TestSounds.CONTENT.block("test_furnace_part", PartBlock::new)
            .named("Test furnace part").look(IBlockLook.cubeAll("test_furnace_part")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestFurnaceBlockEntity>> TERMINAL_BE =
            TestSounds.CONTENT.blockEntity("test_furnace_terminal", TestFurnaceBlockEntity::new, TERMINAL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PartBlockEntity>> PART_BE =
            TestSounds.CONTENT.blockEntity("test_furnace_part", PartBlockEntity::new, PART);

    /**
     * Three wide and deep, the terminal in the middle: items in at the north, out at the south, energy at the west,
     * gas at the east.
     */
    public static final MultiblockPattern FURNACE = MultiblockPatterns.declare(FURNACE_ID,
            MultiblockPattern.builder("test_furnace")
                    .layer("PIP", "E#G", "POP")
                    .where('P', BlockMatch.blocks("jstests:test_furnace_part"))
                    .where('I', BlockMatch.blocks("jstests:test_furnace_part"))
                    .where('O', BlockMatch.blocks("jstests:test_furnace_part"))
                    .where('E', BlockMatch.blocks("jstests:test_furnace_part"))
                    .where('G', BlockMatch.blocks("jstests:test_furnace_part"))
                    .port('I', PortKind.ITEM_INPUT)
                    .port('O', PortKind.ITEM_OUTPUT)
                    .port('E', PortKind.ENERGY_INPUT)
                    .port('G', PortKind.GAS_INPUT)
                    .terminal("jstests:test_furnace_terminal")
                    .build());

    /** A pattern its mod declares not to be shown before it is built, to prove the projector leaves it out. */
    public static final ResourceLocation HIDDEN_ID = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_hidden");
    public static final MultiblockPattern HIDDEN = MultiblockPatterns.declare(HIDDEN_ID,
            MultiblockPattern.builder("test_hidden")
                    .layer("#P")
                    .where('P', BlockMatch.blocks("jstests:test_furnace_part"))
                    .build(), false);

    private TestMultiblocks() {
    }

    /** Declares the blocks and the pattern, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        MultiblockPorts.register(event, PART_BE.get());
    }

    /** The terminal, whose block entity forms the structure when asked. */
    public static final class TerminalBlock extends Block implements EntityBlock {

        public TerminalBlock(final Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new TestFurnaceBlockEntity(pos, state);
        }
    }

    /** A part of the casing, one of the ports when the pattern says so. */
    public static final class PartBlock extends Block implements EntityBlock {

        public PartBlock(final Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new PartBlockEntity(pos, state);
        }
    }

    /** A part's block entity, which remembers its terminal and its port. */
    public static final class PartBlockEntity extends MultiblockPartBlockEntity {

        public PartBlockEntity(final BlockPos pos, final BlockState state) {
            super(PART_BE.get(), pos, state);
        }
    }
}
