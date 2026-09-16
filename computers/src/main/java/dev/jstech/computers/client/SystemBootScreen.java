/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.core.gui.Phosphor;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The system coming up: what the monitor shows between the self-test ending and the desktop or the prompt opening.
 *
 * <p>The machine is the one bringing it up, the one keeping the time, and the one that worked out every line shown
 * here from its own parts and its own network. This screen joins it wherever it has got to and waits: the machine
 * is what puts the system in front of the player when it is ready, so closing this changes nothing.
 */
public final class SystemBootScreen extends Screen {

    private static final int W = 340;
    private static final int H = 214;

    /** Drawn for this long when the machine did not say, which only a stale packet leaves. */
    private static final int FALLBACK_TICKS = 60;

    private final BlockPos computerPos;
    private final BlockPos monitorPos;
    private final BootSequence sequence;
    private final int totalTicks;
    /** Whether a dark monitor follows this rather than a system, which is what a machine going down leaves. */
    private final boolean endsDark;

    private int ticks;

    public SystemBootScreen(final BlockPos computerPos, final BlockPos monitorPos, final BootSequence sequence,
                            final int remainingTicks, final int totalTicks, final boolean endsDark) {
        super(Component.literal("Starting"));
        this.computerPos = computerPos;
        this.monitorPos = monitorPos;
        this.sequence = sequence;
        this.totalTicks = totalTicks > 0 ? totalTicks : FALLBACK_TICKS;
        this.ticks = Math.max(0, this.totalTicks - Math.max(0, remainingTicks));
        this.endsDark = endsDark;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.ticks < this.totalTicks) {
            this.ticks++;
            return;
        }
        /*
         * A machine coming up is handed over by the machine itself, so this waits. A machine going down has
         * nothing left to hand over, so the screen sees itself out and leaves the monitor dark.
         */
        if (this.endsDark && Minecraft.getInstance().screen == this) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        /*
         * Screen.render paints the dimmed backdrop itself; painting it again after our own drawing would wash the
         * whole sequence out, so super runs FIRST and the content after.
         */
        super.render(g, mouseX, mouseY, partialTick);
        final int x = (width - W) / 2;
        final int y = (height - H) / 2;
        final HardwareEra era = era();
        MonitorFrame.renderBody(g, x, y, W, H, era, font);
        g.fill(x, y, x + W, y + H, 0xFF05070A);

        /*
         * A machine of the earliest age wears its tube's one colour, as its self-test does; the later ones show
         * the plain white on black their own sequences did.
         */
        final boolean phosphor = era == HardwareEra.VINTAGE;
        final int text = phosphor ? Phosphor.green(0xFFB8B8B8) : 0xFFE6ECF6;
        final int dim = phosphor ? Phosphor.green(0xFF707070) : 0xFF7D8A9C;

        /*
         * A system that reports nothing puts its name in the middle of the screen over a bar, which is what the
         * machines that tell you nothing while they load have always done; one that reports its steps writes from
         * the top corner, the way a machine reading out its own start does.
         */
        if (this.sequence.lines().isEmpty()) {
            g.drawCenteredString(font, this.sequence.title(), x + W / 2, y + H / 2 - 22, text);
            g.drawCenteredString(font, this.sequence.subtitle(), x + W / 2, y + H / 2 - 10, dim);
            drawBar(g, x, y);
            return;
        }

        int ty = y + 14;
        if (!this.sequence.title().isEmpty()) {
            g.drawString(font, this.sequence.title(), x + 12, ty, text, false);
            ty += 11;
        }
        if (!this.sequence.subtitle().isEmpty()) {
            g.drawString(font, this.sequence.subtitle(), x + 12, ty, dim, false);
            ty += 11;
        }
        ty += 6;

        // Each step appears once the machine has got that far, so the screen fills as the work is done.
        final int shown = this.sequence.shownAt(this.ticks, this.totalTicks);
        for (int i = 0; i < shown; i++) {
            final BootSequence.Line line = this.sequence.lines().get(i);
            g.drawString(font, line.label(), x + 12, ty, text, false);
            if (!line.value().isEmpty()) {
                g.drawString(font, line.value(), x + 130, ty, dim, false);
            }
            ty += 10;
        }
    }

    /** The bar that fills over however long this machine takes: all a system that says nothing ever gave you. */
    private void drawBar(final GuiGraphics g, final int x, final int y) {
        final int barW = 140;
        final int bx = x + (W - barW) / 2;
        final int by = y + H / 2 + 12;
        g.fill(bx, by, bx + barW, by + 3, 0xFF1D2530);
        g.fill(bx, by, bx + Math.min(barW, barW * this.ticks / this.totalTicks), by + 3, 0xFF39D6C4);
    }

    /** The monitor this is drawn on, for whatever asks. */
    public BlockPos monitor() {
        return this.monitorPos;
    }

    /** The host computer's era, so the monitor bezel matches the machine the system is coming up on. */
    private HardwareEra era() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(this.computerPos)
                instanceof AbstractComputerBlockEntity computer && computer.displayEra() != null) {
            return computer.displayEra();
        }
        return HardwareEra.STANDARD;
    }
}
