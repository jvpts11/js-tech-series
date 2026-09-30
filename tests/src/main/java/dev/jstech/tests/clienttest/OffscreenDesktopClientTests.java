/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.OffscreenDesktop;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A computer's desktop drawn with no desktop screen open, the way a monitor will show it in the world: onto a screen
 * of the test's own, with no container behind it and nothing asked of the machine.
 */
public final class OffscreenDesktopClientTests {

    private static final int SETTLE = 4;
    private static final int FRAME_WAIT = 80;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");
    /** What the test's screen clears to before the desktop draws: a colour no desktop wears. */
    private static final int BACKDROP = 0xFFFF00FF;

    private OffscreenDesktopClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void offscreenDesktop_drawsAComputersDesktopWithNoDesktopScreenOpen(final ClientTestContext ctx) {
        final int[] ram = new int[2];
        final Canvas canvas = new Canvas();
        ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(FRAMES_XP);
                    world.placeMonitor(MONITOR, Direction.EAST);
                    ram[0] = computer.ramTotalMb();
                    ram[1] = computer.ramReservedMb();
                })
                // The identity the monitor hands its screen: the machine, the monitor, the system and the desktop
                // Frames bundles, and the machine's memory.
                .then(SETTLE, () -> {
                    canvas.desktop = new OffscreenDesktop(ctx.abs(COMPUTER), ctx.abs(MONITOR), FRAMES_XP, FRAMES_XP,
                            ram[0], ram[1]);
                    ctx.mc().setScreen(canvas);
                })
                .thenWaitUntil(() -> canvas.frames >= 3, FRAME_WAIT, "the desktop to be drawn a few times")
                .thenScreenshot(2, "offscreen-desktop")
                .thenAssert(0, () -> ctx.mc().screen == canvas && !(ctx.mc().screen instanceof DesktopScreen),
                        "no desktop screen is open while the desktop draws")
                .thenAssert(0, () -> canvas.painted, "the desktop draws itself rather than a crash screen")
                .thenAssert(0, () -> !canvas.desktop.launcherLabels().isEmpty(),
                        "the desktop lists what Frames XP can start before the machine says anything")
                .thenAssert(0, () -> rgb(ctx.pixel(1, 1)) == rgb(BACKDROP),
                        "outside the monitor the test's own screen still shows")
                .thenAssert(0, () -> rgb(ctx.pixel(canvas.width / 2, canvas.height / 2)) != rgb(BACKDROP),
                        "the monitor's glass shows the desktop, not the screen behind it")
                .then(0, () -> ctx.mc().setScreen(null));
    }

    private static int rgb(final int argb) {
        return argb & 0xFFFFFF;
    }

    /**
     * A screen that is not the desktop's: it clears to a colour no desktop wears and draws the desktop onto itself,
     * counting the frames the desktop was drawn in.
     */
    private static final class Canvas extends Screen {

        @Nullable
        private OffscreenDesktop desktop;
        private int frames;
        private boolean painted;

        Canvas() {
            super(Component.empty());
        }

        @Override
        public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            g.fill(0, 0, width, height, BACKDROP);
            if (desktop != null) {
                painted = desktop.paint(g, width, height, mouseX, mouseY, partialTick);
                frames++;
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
