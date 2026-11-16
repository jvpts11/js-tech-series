/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Each chain machine's icon, what the recipe viewers show it by, is the formed machine drawn small enough to fit its
 * slot: it fills a good share of the slot and nothing of it spills into the gap between two slots.
 */
public final class ChainMachineIconsClientTests {

    private static final int SETTLE = 6;
    /**
     * A tenth of a slot drawn at least: a long thin machine, as the Wire Drawer, fills little of its slot once its
     * longest side fits, while a machine shrunk to a dot by a wrong size draws a handful of pixels.
     */
    private static final double DRAWN_SHARE = 0.1;
    /** The share of the gap round a slot that must stay the bare ground, a pixel or two of an edge's blur allowed. */
    private static final double BARE_SHARE = 0.95;
    private static final int SLOT = 16;
    /** How far a pixel's channels may stray from the ground's and still be the ground. */
    private static final int GROUND_TOLERANCE = 24;
    private static final int CHANNEL = 0xFF;

    private ChainMachineIconsClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void icons_drawEachFormedMachineFittedInItsSlot(final ClientTestContext ctx) {
        final List<ItemStack> stacks = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        for (final ChainMachine machine : ChainMachines.all()) {
            stacks.add(new ItemStack(ChainMachines.terminalOf(machine).get()));
            names.add(machine.id());
        }
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> ctx.mc().setScreen(new IconSheet(stacks)))
                .thenScreenshot(SETTLE, "chain-machine-icons")
                .then(0, () -> {
                    try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
                        for (int i = 0; i < stacks.size(); i++) {
                            final int left = IconSheet.LEFT + i % IconSheet.COLUMNS * IconSheet.CELL;
                            final int top = IconSheet.TOP + i / IconSheet.COLUMNS * IconSheet.CELL;
                            int drawn = 0;
                            int gap = 0;
                            int bare = 0;
                            for (int y = -1; y <= SLOT; y++) {
                                for (int x = -1; x <= SLOT; x++) {
                                    final boolean ground = ground(frame.pixel((int) ((left + x + 0.5)
                                            * IconSheet.SCALE), (int) ((top + y + 0.5) * IconSheet.SCALE)));
                                    if (x < 0 || y < 0 || x == SLOT || y == SLOT) {
                                        gap++;
                                        bare += ground ? 1 : 0;
                                    } else if (!ground) {
                                        drawn++;
                                    }
                                }
                            }
                            ctx.assertTrue(drawn >= SLOT * SLOT * DRAWN_SHARE, names.get(i) + "'s icon draws the "
                                    + "machine: " + drawn + " of " + SLOT * SLOT + " pixels");
                            ctx.assertTrue(bare >= gap * BARE_SHARE, names.get(i) + "'s icon stays in its slot: "
                                    + (gap - bare) + " of " + gap + " pixels of the gap drawn on");
                        }
                    }
                });
    }

    /* Whether a pixel (ARGB) is the sheet's bare ground. */
    private static boolean ground(final int argb) {
        return Math.abs((argb >> 16 & CHANNEL) - IconSheet.GREY) <= GROUND_TOLERANCE
                && Math.abs((argb >> 8 & CHANNEL) - IconSheet.GREY) <= GROUND_TOLERANCE
                && Math.abs((argb & CHANNEL) - IconSheet.GREY) <= GROUND_TOLERANCE;
    }

    /* The icons as a slot draws them, in rows on a plain ground, each so many times its size. */
    private static final class IconSheet extends Screen {

        private final List<ItemStack> stacks;

        private static final int SCALE = 2;
        private static final int CELL = 20;
        private static final int COLUMNS = 8;
        private static final int LEFT = 4;
        private static final int TOP = 4;
        private static final int GREY = 0x8B;

        private IconSheet(final List<ItemStack> stacks) {
            super(Component.empty());
            this.stacks = List.copyOf(stacks);
        }

        @Override
        public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            g.fill(0, 0, this.width, this.height, 0xFF << 24 | GREY << 16 | GREY << 8 | GREY);
            g.pose().pushPose();
            g.pose().scale(SCALE, SCALE, SCALE);
            for (int i = 0; i < this.stacks.size(); i++) {
                g.renderItem(this.stacks.get(i), LEFT + i % COLUMNS * CELL, TOP + i / COLUMNS * CELL);
            }
            g.pose().popPose();
        }
    }
}
