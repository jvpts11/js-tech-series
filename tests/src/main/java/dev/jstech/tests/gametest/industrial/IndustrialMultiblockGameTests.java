/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.industrial.crc.CrcContent;
import dev.jstech.industrial.multiblock.CasingMaterial;
import dev.jstech.industrial.multiblock.IndustrialMultiblock;
import dev.jstech.industrial.multiblock.SizeGives;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestOvenBattery;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The rules of J's Industrial's multiblocks: built of a material of their time, brick, wood or iron before the T3 and
 * steel from it, which sets how their blocks stand and are broken; what their size gives, a battery of ovens one more
 * recipe at once for each oven; and a terminal that is the machine's whole screen and keeps what it holds while the
 * structure stands apart.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialMultiblockGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos TERMINAL = new BlockPos(2, 2, 1);
    private static final int OVENS = 3;
    /** A brick of the front, beside the terminal. */
    private static final BlockPos FRONT_BRICK = TERMINAL.west();

    private IndustrialMultiblockGameTests() {
    }

    @GameTest(template = ARENA)
    public static void casingMaterial_isOfItsTime(final GameTestHelper helper) {
        helper.assertTrue(CasingMaterial.of(IndustrialTier.T0).equals(List.of(CasingMaterial.BRICK,
                CasingMaterial.WOOD, CasingMaterial.IRON)) && CasingMaterial.of(IndustrialTier.T2)
                .equals(CasingMaterial.of(IndustrialTier.T1)), "brick, wood and iron before the T3");
        helper.assertTrue(CasingMaterial.of(IndustrialTier.T3).equals(List.of(CasingMaterial.STEEL))
                && CasingMaterial.of(IndustrialTier.T9).equals(List.of(CasingMaterial.STEEL)), "steel from it");
        refused(helper, IndustrialTier.T3, CasingMaterial.BRICK, "a multiblock of the T3 of brick is refused");
        refused(helper, IndustrialTier.T2, CasingMaterial.STEEL, "and one of the T2 of steel");
        helper.assertTrue(CrcContent.CRC.material() == CasingMaterial.STEEL
                && CrcContent.CRC.tier() == IndustrialTier.T3, "the CRC, of the T3, is of steel");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void casing_standsSoundsAndIsBrokenAsItsMaterial(final GameTestHelper helper) {
        final BlockState brick = TestOvenBattery.BRICKS.get().defaultBlockState();
        helper.assertTrue(brick.getDestroySpeed(helper.getLevel(), BlockPos.ZERO) == CasingMaterial.BRICK.strength()
                && brick.getSoundType() == SoundType.STONE && brick.requiresCorrectToolForDrops(),
                "a brick of the battery stands, sounds and drops as brick does");
        helper.assertTrue(TestOvenBattery.BRICKS.declaredTags().contains(BlockTags.MINEABLE_WITH_PICKAXE),
                "and is mined with a pickaxe");
        final BlockState casing = CrcContent.CASING.get().defaultBlockState();
        helper.assertTrue(casing.getDestroySpeed(helper.getLevel(), BlockPos.ZERO) == CasingMaterial.STEEL.strength()
                && casing.getSoundType() == SoundType.METAL && casing.is(BlockTags.MINEABLE_WITH_PICKAXE),
                "the CRC's casing as steel does");
        helper.assertTrue(casing.getMapColor(helper.getLevel(), BlockPos.ZERO) == MapColor.SAND,
                "painted as the CRC paints it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void sizeGives_aRecipeAtOnceForEachSlice(final GameTestHelper helper) {
        helper.assertTrue(SizeGives.NOTHING.equals(IndustrialMultiblock.of(IndustrialTier.T1, CasingMaterial.BRICK)
                .size()), "a multiblock's size gives nothing unless it says so");
        try {
            SizeGives.recipesPerSlice(0);
            helper.fail("a slice giving no recipe is refused");
        } catch (final IllegalArgumentException expected) {
            // As it should.
        }
        build(helper, OVENS);
        final TestOvenBattery.TerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
        terminal.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, OVENS * 2));
        helper.onEachTick(() -> terminal.power().generate(terminal.power().capacity()));
        helper.succeedWhen(() -> {
            final MachineStructure structure = terminal.structure();
            helper.assertTrue(structure != null && structure.formed() && structure.repeats() == OVENS,
                    "the battery of three ovens forms");
            helper.assertTrue(structure.parallel() == OVENS, "and runs three at once; got " + structure.parallel());
            helper.assertTrue(terminal.getInventory().getStackInSlot(1).getCount() >= OVENS
                    && terminal.getInventory().getStackInSlot(1).getCount() % OVENS == 0,
                    "it bakes three stones at a time; got " + terminal.getInventory().getStackInSlot(1));
        });
    }

    @GameTest(template = ARENA)
    public static void terminal_isTheWholeMachineScreenAndKeepsWhatItHoldsWhileApart(final GameTestHelper helper) {
        build(helper, 1);
        final TestOvenBattery.TerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(terminal), "the battery of one oven forms"))
                .thenExecute(() -> {
                    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    final MachineMenu menu = new MachineMenu(0, player.getInventory(), terminal);
                    helper.assertTrue(menu.tabs() == MachineScreenLayout.TABS, "its terminal opens every tab of a "
                            + "machine's screen; got " + menu.tabs());
                    helper.assertTrue(terminal.layout().upgrades() == 2 && terminal.power().protection() != null,
                            "the two upgrade slots of a T1 machine and its protection among them");
                    terminal.getInventory().setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
                })
                .thenExecute(() -> helper.setBlock(FRONT_BRICK, Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(terminal.status() == MachineStatus.NOT_FORMED,
                        "a brick gone undoes it; it is " + terminal.status()))
                .thenExecute(() -> helper.assertTrue(terminal.getInventory().getStackInSlot(0).is(Items.DIAMOND)
                        && terminal.getInventory().getStackInSlot(0).getCount() == 5,
                        "its terminal keeps what it held"))
                .thenExecute(() -> helper.setBlock(FRONT_BRICK, TestOvenBattery.BRICKS.get()))
                .thenWaitUntil(() -> helper.assertTrue(formed(terminal), "the brick back forms it again"))
                .thenExecute(() -> helper.assertTrue(terminal.getInventory().getStackInSlot(0).getCount() == 5,
                        "with what it held"))
                .thenSucceed();
    }

    /* Builds a battery of {@code ovens} ovens, its depth along z, the terminal last. */
    private static void build(final GameTestHelper helper, final int ovens) {
        for (int dz = 0; dz <= ovens + 1; dz++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        helper.setBlock(TERMINAL.offset(dx, dy, dz), TestOvenBattery.BRICKS.get());
                    }
                }
            }
        }
        helper.setBlock(TERMINAL, TestOvenBattery.TERMINAL.get());
    }

    private static boolean formed(final TestOvenBattery.TerminalBlockEntity terminal) {
        return terminal.structure() != null && terminal.structure().formed();
    }

    private static void refused(final GameTestHelper helper, final IndustrialTier tier,
                                final CasingMaterial material, final String message) {
        try {
            IndustrialMultiblock.of(tier, material);
            helper.fail(message);
        } catch (final IllegalArgumentException expected) {
            // As it should.
        }
    }
}
