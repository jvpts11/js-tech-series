/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.term.TermPainter;
import dev.jstech.computers.gui.layout.CommandPromptLayout;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * A console drawn in the terminal font: at a whole number of the screen's pixels to each of the font's, and with the
 * block characters filling their cells so a bar of them is one solid stripe, as on a real terminal.
 */
public final class TerminalFontClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 1_200;

    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private static final ResourceLocation UNIX = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "unix");
    /** How many full blocks the bar is. */
    private static final int BLOCKS = 8;
    private static final String BAR = Character.toString(0x2588).repeat(BLOCKS);

    private TerminalFontClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void console_drawsCrispCellsAndJoinsItsBlocks(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(MACHINE, MainframeBlockEntity.class);
                    final ItemStackHandler inv = machine.getInventory();
                    inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
                    inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                            new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
                    inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
                    inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    machine.installOs(UNIX);
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> said(ctx, "Console Login"), SCREEN_WAIT, "the console to sign in")
                .thenAssert(1, () -> crisp(ctx), "the terminal font is drawn at a whole number of screen pixels")
                .thenAssert(0, () -> largest(ctx), "the console's eighty columns fill its glass with crisp cells")
                .then(SETTLE, () -> ctx.type("echo " + BAR))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> said(ctx, BAR), SCREEN_WAIT, "echo to print the bar of blocks")
                .thenScreenshot(4, "terminal-font")
                .thenAssert(0, () -> longestStripe(ctx) >= expectedStripe(ctx),
                        "the bar of blocks is one solid stripe as wide as its cells");
    }

    private static boolean said(final ClientTestContext ctx, final String words) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        return prompt.scrollbackText().stream().anyMatch(l -> l.contains(words));
    }

    private static boolean crisp(final ClientTestContext ctx) {
        final double pixels = ctx.screen(CommandPromptScreen.class).textScale() * ctx.mc().getWindow().getGuiScale();
        return pixels >= 1 && Math.abs(pixels - Math.round(pixels)) < 1e-4;
    }

    /** How many screen pixels wide the bar's cells come out, in the size of the font the console picked. */
    private static int expectedStripe(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        final double pixels = prompt.textScale() * ctx.mc().getWindow().getGuiScale();
        return (int) Math.round(BLOCKS * prompt.face().width() * pixels) - 1;
    }

    /**
     * Whether the console drew its columns in the largest crisp cells they leave room for: at least as wide as the
     * small size's at one screen pixel each, and no wider than the glass holds eighty of.
     */
    private static boolean largest(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        final double gui = ctx.mc().getWindow().getGuiScale();
        final double cell = prompt.face().width() * prompt.textScale() * gui;
        final double glass = (prompt.getXSize() - CommandPromptLayout.GLASS_LEFT
                - CommandPromptLayout.GLASS_RIGHT_MARGIN) * gui;
        return cell >= TermPainter.CELL && 80 * cell <= glass + 1e-6;
    }

    /**
     * The longest run of one ink along any row of the console's glass. The glass holds only its ground and the
     * letters on it, and no letter is wider than a cell, so only a stripe of blocks makes a long run.
     */
    private static int longestStripe(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        final double gui = ctx.mc().getWindow().getGuiScale();
        final int left = (int) Math.ceil((prompt.getGuiLeft() + CommandPromptLayout.GLASS_LEFT) * gui);
        final int right = (int) ((prompt.getGuiLeft() + prompt.getXSize() - CommandPromptLayout.GLASS_RIGHT_MARGIN)
                * gui);
        final int top = (int) Math.ceil((prompt.getGuiTop() + CommandPromptLayout.glassTop(true)) * gui);
        final int bottom = (int) ((prompt.getGuiTop() + CommandPromptLayout.glassBottom(prompt.getYSize())) * gui);
        try (NativeImage image = Screenshot.takeScreenshot(ctx.mc().getMainRenderTarget())) {
            final Map<Integer, Integer> counts = new HashMap<>();
            for (int y = top; y < bottom; y++) {
                for (int x = left; x < right; x++) {
                    counts.merge(image.getPixelRGBA(x, y), 1, Integer::sum);
                }
            }
            final int ground = counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                    .orElse(0);
            int longest = 0;
            for (int y = top; y < bottom; y++) {
                int run = 0;
                int colour = ground;
                for (int x = left; x < right; x++) {
                    final int here = image.getPixelRGBA(x, y);
                    run = here == colour ? run + 1 : 1;
                    colour = here;
                    if (here != ground) {
                        longest = Math.max(longest, run);
                    }
                }
            }
            return longest;
        }
    }
}
