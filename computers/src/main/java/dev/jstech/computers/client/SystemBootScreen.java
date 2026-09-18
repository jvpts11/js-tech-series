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
import dev.jstech.computers.os.boot.BootSplash;
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

    /** Where the wall of text begins, and how much air is left at the right edge. */
    private static final int MARGIN = 12;

    /** What a step that went well is written in, which is the one colour these logs ever used for it. */
    private static final int GOOD = 0xFF5FE07A;

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
    private final BootSplash splash;
    private final int totalTicks;
    /** Whether a dark monitor follows this rather than a system, which is what a machine going down leaves. */
    private final boolean endsDark;
    /** The desktop coming up behind the system's own lines, by the last part of its id, or nothing. */
    private final String desktopId;
    /** The distribution by name, which a desktop's own loading screen puts at its foot. */
    private final String systemName;

    private int ticks;

    public SystemBootScreen(final MonitorSessionMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
        final Starting starting = pending != null ? pending
                : new Starting(BootSequence.NONE, FALLBACK_TICKS, FALLBACK_TICKS, false, BootSplash.PLAIN,
                        "", "");
        this.sequence = starting.sequence();
        this.splash = starting.splash();
        this.totalTicks = starting.totalTicks() > 0 ? starting.totalTicks() : FALLBACK_TICKS;
        this.ticks = Math.max(0, this.totalTicks - Math.max(0, starting.remainingTicks()));
        this.endsDark = starting.endsDark();
        this.desktopId = starting.desktopId();
        this.systemName = starting.systemName();
    }

    /** What the machine is bringing up, said before the session that shows it is opened. */
    public static void expect(final BootSequence sequence, final int remainingTicks, final int totalTicks,
                              final boolean endsDark, final BootSplash splash, final String desktopId,
                              final String systemName) {
        pending = new Starting(sequence, remainingTicks, totalTicks, endsDark, splash, desktopId, systemName);
    }

    /** One machine coming up: what it prints, how far along it is, and what it comes up behind. */
    private record Starting(BootSequence sequence, int remainingTicks, int totalTicks, boolean endsDark,
                            BootSplash splash, String desktopId, String systemName) {
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
         * A machine with a desktop hands over to it before the desktop is there to click: the system's own
         * lines stop and that desktop's picture takes the rest of the wait, which is the moment a player
         * finds out which of them this machine runs. A machine going down never reaches it.
         */
        final int through = this.totalTicks > 0 ? this.ticks * 100 / this.totalTicks : 0;
        if (!this.endsDark && DesktopSplashArt.has(this.desktopId) && through >= DesktopSplashArt.FROM) {
            final int within = (through - DesktopSplashArt.FROM) * 100
                    / Math.max(1, 100 - DesktopSplashArt.FROM);
            DesktopSplashArt.draw(g, font, this.desktopId, this.systemName, era, x, y, W, H, this.ticks,
                    Math.min(100, within));
            return;
        }

        /*
         * A system with a picture of its own comes up behind it and says nothing else. That is what those
         * screens were: no list of what the machine was finding, just the thing itself for as long as it took.
         * The one exception is the newest edition's first start, which greets the machine by name, and the
         * words for that travel in the sequence and are drawn over the picture rather than instead of it.
         */
        if (BootSplashArt.paintsItsOwnGround(this.splash)) {
            BootSplashArt.draw(g, font, this.splash, x, y, W, H, this.ticks, this.totalTicks,
                    this.endsDark, this.sequence.title(), this.sequence.subtitle());
            return;
        }

        /*
         * A machine of the earliest age wears its tube's one colour, as its self-test does; the later ones show
         * the plain white on black their own sequences did.
         */
        final boolean phosphor = era == HardwareEra.VINTAGE;
        final int text = phosphor ? Phosphor.green(0xFFB8B8B8) : 0xFFE6ECF6;
        final int dim = phosphor ? Phosphor.green(0xFF707070) : 0xFF7D8A9C;
        final int good = phosphor ? Phosphor.green(0xFFB8B8B8) : GOOD;

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

        int ty = y + 12;
        if (!this.sequence.title().isEmpty()) {
            wall(g, this.sequence.title(), x + MARGIN, ty, text);
            ty += WALL_ROW;
        }
        if (!this.sequence.subtitle().isEmpty()) {
            wall(g, this.sequence.subtitle(), x + MARGIN, ty, dim);
            ty += WALL_ROW;
        }
        ty += 4;

        final int labelAt = x + MARGIN + markColumn();
        final int valueAt = x + MARGIN + valueColumn();
        // Each step appears once the machine has got that far, so the screen fills as the work is done.
        final int shown = this.sequence.shownAt(this.ticks, this.totalTicks);
        for (int i = 0; i < shown; i++) {
            final BootSequence.Line line = this.sequence.lines().get(i);
            if (!line.mark().isEmpty()) {
                wall(g, line.mark(), x + MARGIN, ty, line.good() ? good : dim);
            }
            wall(g, line.label(), labelAt, ty, text);
            if (!line.value().isEmpty()) {
                /*
                 * A line with nothing at its head is one of the systems that ran dots out to its answer, which
                 * is how a machine of that age tied a question on the left to what came of it on the right.
                 */
                if (line.mark().isEmpty()) {
                    leader(g, labelAt + wallWidth(line.label()) + 3, valueAt - 3, ty, dim);
                }
                wall(g, line.value(), valueAt, ty, line.good() ? good : dim);
            }
            ty += WALL_ROW;
        }
    }

    /** How far in the labels start: past the widest mark in this sequence, or at the edge when there is none. */
    private int markColumn() {
        int widest = 0;
        for (final BootSequence.Line line : this.sequence.lines()) {
            widest = Math.max(widest, wallWidth(line.mark()));
        }
        return widest == 0 ? 0 : widest + 6;
    }

    /** How far in the answers start: past the longest label, and never so far that the widest answer runs off. */
    private int valueColumn() {
        int widest = 0;
        int widestValue = 0;
        for (final BootSequence.Line line : this.sequence.lines()) {
            if (line.value().isEmpty()) {
                continue;
            }
            widest = Math.max(widest, wallWidth(line.label()));
            widestValue = Math.max(widestValue, wallWidth(line.value()));
        }
        final int room = W - 2 * MARGIN - widestValue;
        return Math.min(room, markColumn() + widest + 10);
    }

    /** The row of dots between a question and its answer, drawn to fill exactly the gap between them. */
    private void leader(final GuiGraphics g, final int from, final int to, final int y, final int color) {
        final int step = Math.max(1, wallWidth("."));
        final int dots = (to - from) / step;
        if (dots <= 1) {
            return;
        }
        wall(g, ".".repeat(dots), from, y, color);
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
