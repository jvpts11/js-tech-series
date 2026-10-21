/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.testkit.client.ClientTest;
import dev.jstech.testkit.client.ClientTestHelper;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

/**
 * The Core's test kit's client tests as a mod built on the Core writes them, with the kit's own mark and helper and
 * nothing of the series': a chest set down, opened with a right click as a player opens it, and closed again.
 */
public final class TestKitClientTests {

    private static final BlockPos CHEST = new BlockPos(3, 2, 5);

    private TestKitClientTests() {
    }

    @ClientTest(timeoutTicks = 300)
    public static void kit_opensAndClosesAScreenAsAPlayer(final ClientTestHelper helper) {
        helper.thenServer(0, level -> level.setBlockAndUpdate(helper.abs(CHEST), Blocks.CHEST.defaultBlockState()))
                .thenTeleport(1, new BlockPos(3, 2, 7), Direction.NORTH)
                .thenRightClick(2, CHEST)
                .thenAwaitScreen(ContainerScreen.class, 60)
                .thenAssert(0, () -> helper.screen(ContainerScreen.class).getMenu().slots.size() >= 27,
                        "the chest's screen has its slots")
                .then(1, () -> helper.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(60)
                .thenServer(0, level -> level.removeBlock(helper.abs(CHEST), false));
    }
}
