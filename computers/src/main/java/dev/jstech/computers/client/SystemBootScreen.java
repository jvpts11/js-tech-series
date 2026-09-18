/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.core.gui.Phosphor;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * The system coming up: what the monitor shows between the self-test ending and the desktop or the prompt opening.
 *
 * <p>The machine is the one bringing it up, the one keeping the time, and the one that worked out every line shown
 * here from its own parts and its own network. This screen joins it wherever it has got to and waits: the machine
 * is what puts the system in front of the player when it is ready, so closing this changes nothing.
 */
public final class SystemBootScreen extends AbstractComputerScreen<MonitorSessionMenu> {

    private static final int W = 340;
    private static final int H = 214;

    /** Drawn for this long when the machine did not say, which only a stale packet leaves. */
    private static final int FALLBACK_TICKS = 60;

    /**
     * What the machine last said it was bringing up, kept until the screen showing it is built.
     *
     * <p>The machine sends what is on the glass and then opens the session, in that order, so by the time
     * this screen is made the lines it draws are already here. It is one machine's worth because one monitor
     * is being looked at at a time.
     */
    @Nullable
    private static Starting pending;

    private final BootSequence sequence;
    private final int totalTicks;
    /** Whether a dark monitor follows this rather than a system, which is what a machine going down leaves. */
    private final boolean endsDark;

    private int ticks;

    public SystemBootScreen(final MonitorSessionMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
        final Starting starting = pending != null ? pending
                : new Starting(BootSequence.NONE, FALLBACK_TICKS, FALLBACK_TICKS, false);
        this.sequence = starting.sequence();
        this.totalTicks = starting.totalTicks() > 0 ? starting.totalTicks() : FALLBACK_TICKS;
        this.ticks = Math.max(0, this.totalTicks - Math.max(0, starting.remainingTicks()));
        this.endsDark = starting.endsDark();
    }

    /** What the machine is bringing up, said before the session that shows it is opened. */
    public static void expect(final BootSequence sequence, final int remainingTicks, final int totalTicks,
                              final boolean endsDark) {
        pending = new Starting(sequence, remainingTicks, totalTicks, endsDark);
    }

    /** One machine coming up: what it prints, how far along it is, and whether the glass goes dark after. */
    private record Starting(BootSequence sequence, int remainingTicks, int totalTicks, boolean endsDark) {
    }

    @Override
    protected void containerTick() {
        super.containerTick();
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
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = this.leftPos;
        final int y = this.topPos;
        final HardwareEra era = screenEra() == null ? HardwareEra.STANDARD : screenEra();
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
        return this.getMenu().monitorPos();
    }

    /** The machine's own generation, so the bezel is the monitor that machine would really have. */
    @Override
    @Nullable
    protected HardwareEra screenEra() {
        return this.getMenu().hardwareEra();
    }
}
