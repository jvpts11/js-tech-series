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
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.industrial.machine.IndustrialProcessingMachine;
import dev.jstech.industrial.machine.MachineRating;
import dev.jstech.industrial.multiblock.CasingMaterial;
import dev.jstech.industrial.multiblock.IndustrialMultiblock;
import dev.jstech.industrial.multiblock.SizeGives;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A multiblock declared as J's Industrial declares its own: a battery of brick ovens of the T1, of blocks of its own
 * and its terminal, from one oven to four, each oven a slice of its pattern and one more recipe at once. Its terminal
 * is a machine of J's Industrial, baking cobblestone into stone.
 */
public final class TestOvenBattery {

    /** What it says of itself: of the T1, of brick, an oven more at once for each slice. */
    public static final IndustrialMultiblock BATTERY = IndustrialMultiblock.of(IndustrialTier.T1, CasingMaterial.BRICK)
            .growing(SizeGives.recipesPerSlice(1));
    public static final MachineRating RATING = MachineRating.of(IndustrialTier.T1, JsEnergy.LOW, 2_000L);
    /** How many ovens it has at most, and how long it takes to bake, in ticks. */
    public static final int MOST_OVENS = 4;
    public static final int BAKES_IN = 5;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_oven_battery");

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<Block> BRICKS = BATTERY.built(TestSounds.CONTENT.block("test_oven_bricks",
                    Block::new)).named("Test oven bricks").look(IBlockLook.cubeAll("test_oven_bricks"))
            .item().tab(TestItems.ITEMS).register();
    /* With an item, as every terminal, so a wrench takes it away whole. */
    public static final BlockEntry<DeviceBlock> TERMINAL = BATTERY.built(TestSounds.CONTENT.block(
                    "test_oven_battery_terminal", Device.of(() -> TestOvenBattery.TERMINAL_ENTITY.get())
                            .ticks(MachineBlockEntity::serverTick).opensMenu(MachineMenu::new)::block))
            .named("Test oven battery terminal").look(IBlockLook.cubeAll("test_oven_battery_terminal"))
            .item().tab(TestItems.ITEMS).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TerminalBlockEntity>> TERMINAL_ENTITY =
            TestSounds.CONTENT.blockEntity("test_oven_battery_terminal", TerminalBlockEntity::new, TERMINAL);

    /**
     * Three wide and two high, its terminal in the middle of the front's bottom row: a front, its ovens behind, each a
     * row repeated, and a back.
     */
    public static final MultiblockPattern PATTERN = MultiblockPatterns.declare(ID,
            MultiblockPattern.builder("test_oven_battery")
                    .layer("B#B", "BBB", "BBB")
                    .layer("BBB", "BBB", "BBB")
                    .where('B', BlockMatch.blocks("jstests:test_oven_bricks"))
                    .repeatRow(1, 1, MOST_OVENS)
                    .terminal("jstests:test_oven_battery_terminal")
                    .build());

    private TestOvenBattery() {
    }

    /** Declares the battery, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Its terminal: cobblestone into stone, a recipe at once for each oven. */
    public static final class TerminalBlockEntity extends IndustrialProcessingMachine {

        public TerminalBlockEntity(final BlockPos pos, final BlockState state) {
            super(TERMINAL_ENTITY.get(), pos, state, RATING, ProcessingMachineBlockEntity.Layout.items(1, 1));
            formsWith(BATTERY.structure(PATTERN));
        }

        @Override
        protected Optional<Processing> process(final Level level, final ProcessingInput input) {
            return input.items().getFirst().is(Items.COBBLESTONE)
                    ? Optional.of(Processing.single(0, new ItemStack(Items.STONE), BAKES_IN)) : Optional.empty();
        }
    }
}
