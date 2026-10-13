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
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.machine.IUpgrade;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.machine.UpgradeEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * A processing machine of the test mod, to prove the Core's: two inputs, two outputs, an upgrade slot, a tank in and a
 * tank out, working the recipes of its own kind, and the speed upgrade that makes it work faster at a higher cost.
 */
public final class TestPress {

    /** The test press's recipes, a recipe type of the test mod. */
    public static final ProcessingKind PRESSING =
            TestSounds.CONTENT.processing("test_pressing", "Test Pressing", () -> TestPress.BLOCK);
    /** How its slots and tanks are laid out. */
    public static final ProcessingMachineBlockEntity.Layout LAYOUT =
            ProcessingMachineBlockEntity.Layout.items(2, 2).withUpgrades(1).withTanks(1, 1, 4_000);
    /** What it spends a tick on a recipe that names no energy of its own. */
    public static final int ENERGY_PER_TICK = 4;
    /** Twice as fast, at half as much energy again. */
    public static final UpgradeEffect SPEED = new UpgradeEffect(2.0, 1.5);

    /* Its block state and model are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<PressBlock> BLOCK = TestSounds.CONTENT.block("test_press", PressBlock::new)
            .named("Test press").look(IBlockLook.cubeAll("test_press")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestPressBlockEntity>> BLOCK_ENTITY =
            TestSounds.CONTENT.blockEntity("test_press", TestPressBlockEntity::new, BLOCK);
    public static final ItemEntry<SpeedUpgrade> UPGRADE = TestSounds.CONTENT.item("test_speed_upgrade",
            SpeedUpgrade::new).named("Test Speed Upgrade").look(IItemLook.HANDMADE).tab(TestItems.ITEMS)
            .register();

    private TestPress() {
    }

    /** Declares the machine, its kind and its upgrade, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The press's block, which ticks its block entity on the server. */
    public static final class PressBlock extends Block implements EntityBlock {

        public PressBlock(final Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new TestPressBlockEntity(pos, state);
        }

        @Override
        @Nullable
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                      final BlockEntityType<T> type) {
            if (level.isClientSide() || type != BLOCK_ENTITY.get()) {
                return null;
            }
            return (tickLevel, pos, tickState, entity) -> ProcessingMachineBlockEntity.serverTick(tickLevel, pos,
                    tickState, (TestPressBlockEntity) entity);
        }
    }

    /** The speed upgrade, every one in the slot counting. */
    public static final class SpeedUpgrade extends Item implements IUpgrade {

        public SpeedUpgrade(final Properties properties) {
            super(properties);
        }

        @Override
        public UpgradeEffect effect(final ItemStack stack) {
            return SPEED;
        }
    }
}
