/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.industrial.client.CoalGeneratorScreen;
import dev.jstech.industrial.client.ProcessingMachineScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * The Industrial machines as a player uses them: using one opens its own screen, with a frame under every slot of
 * its menu, the progress and the stored energy the machine holds, all placed by the machine's layout.
 */
public final class IndustrialScreensClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 60;

    private static final BlockPos COMPRESSOR = new BlockPos(4, 2, 2);
    private static final BlockPos GENERATOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER = new BlockPos(5, 2, 4);

    private IndustrialScreensClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void industrialMachines_openTheirOwnScreens(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(COMPRESSOR, IndustrialModule.COMPRESSOR.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    world.setBlock(GENERATOR, IndustrialModule.COAL_GENERATOR.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    final CompressorBlockEntity compressor = world.blockEntity(COMPRESSOR, CompressorBlockEntity.class);
                    // Its one input is the first slot.
                    compressor.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
                    compressor.getEnergy().setEnergyStored(compressor.getEnergy().getMaxEnergyStored() / 2);
                    world.blockEntity(GENERATOR, CoalGeneratorBlockEntity.class).getInventory()
                            .setStackInSlot(CoalGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL, 16));
                })
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .thenRightClick(SETTLE, COMPRESSOR)
                .thenAwaitScreen(ProcessingMachineScreen.class, SCREEN_WAIT)
                .thenScreenshot(20, "compressor")
                .thenAssert(0, () -> ctx.screen(ProcessingMachineScreen.class).getMenu().slots.size() == 2 + 36,
                        "the Compressor's menu carries its input, its output and the player's inventory")
                .then(0, () -> ctx.player().closeContainer())
                .thenRightClick(SETTLE, GENERATOR)
                .thenAwaitScreen(CoalGeneratorScreen.class, SCREEN_WAIT)
                .thenScreenshot(20, "coal-generator")
                .thenAssert(0, () -> ctx.screen(CoalGeneratorScreen.class).getMenu().generator().isBurning(),
                        "the generator's burning reaches the client's menu")
                .then(0, () -> ctx.player().closeContainer());
    }
}
