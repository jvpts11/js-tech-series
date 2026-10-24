/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.industrial;

import dev.jstech.core.client.machine.MachineScreen;
import dev.jstech.core.client.recipeview.RecipeViewers;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * The Industrial machines as a player uses them: using one opens the screen every machine of the Core's has, its Main
 * tab with the machine's slots on its face, the progress and the stored energy the machine holds.
 */
public final class IndustrialScreensClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 60;

    private static final BlockPos COMPRESSOR = new BlockPos(4, 2, 2);
    /* Against the Compressor, so it feeds it and both are seen working. */
    private static final BlockPos GENERATOR = new BlockPos(5, 2, 2);
    private static final BlockPos PLAYER = new BlockPos(5, 2, 4);

    private IndustrialScreensClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void industrialMachines_openTheStandardMachineScreen(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(COMPRESSOR, IndustrialModule.COMPRESSOR.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    world.setBlock(GENERATOR, IndustrialModule.COAL_GENERATOR.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    final CompressorBlockEntity compressor = world.blockEntity(COMPRESSOR, CompressorBlockEntity.class);
                    // Its one input is the first slot.
                    compressor.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
                    compressor.power().generate(compressor.power().capacity() / 2);
                    world.blockEntity(GENERATOR, CoalGeneratorBlockEntity.class).getInventory()
                            .setStackInSlot(CoalGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL, 16));
                })
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .thenRightClick(SETTLE, COMPRESSOR)
                .thenAwaitScreen(MachineScreen.class, SCREEN_WAIT)
                .thenScreenshot(20, "compressor")
                .thenAssert(0, () -> ctx.screen(MachineScreen.class).getMenu().faceSlots().size() == 2,
                        "the Compressor's face carries its input and its output")
                .then(0, () -> {
                    final MachineScreen screen = ctx.screen(MachineScreen.class);
                    ctx.click(screen.getGuiLeft() + MachineScreenLayout.faceX(MachineScreenLayout.arrowX(1) + 4),
                            screen.getGuiTop() + MachineScreenLayout.faceY(MachineScreenLayout.laneY(0, 1)
                                    + MachineScreenLayout.LANE_PITCH / 2 - 1));
                })
                .thenAssert(SETTLE, () -> !RecipeViewers.any() || ctx.mc().screen != null
                        && !(ctx.mc().screen instanceof MachineScreen), "a click on its arrow opens its recipes")
                .thenScreenshot(SETTLE, "compressor-recipes")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenRightClick(SETTLE, COMPRESSOR)
                .thenAwaitScreen(MachineScreen.class, SCREEN_WAIT)
                .then(0, () -> ctx.player().closeContainer())
                .thenRightClick(SETTLE, GENERATOR)
                .thenAwaitScreen(MachineScreen.class, SCREEN_WAIT)
                .thenScreenshot(20, "coal-generator")
                .thenAssert(0, () -> ctx.screen(MachineScreen.class).getMenu().faceSlots().size() == 1,
                        "the generator's face carries its fuel slot")
                .thenAssert(0, () -> ctx.screen(MachineScreen.class).getMenu().machine()
                                instanceof CoalGeneratorBlockEntity generator && generator.isBurning(),
                        "the generator's burning reaches the client's menu")
                .then(0, () -> ctx.player().closeContainer());
    }
}
