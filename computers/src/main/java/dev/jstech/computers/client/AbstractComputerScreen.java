/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.client.monitor.PowerStrip;
import dev.jstech.computers.client.theme.MonitorFrameStyle;
import dev.jstech.computers.menu.IMonitorMenu;
import dev.jstech.core.client.live.TubeFilter;
import dev.jstech.core.client.gui.theme.EraTheme;
import dev.jstech.core.client.gui.theme.EraThemes;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Base for the computing container screens. It resolves the screen's per-era skin and binds it for the render pass,
 * so every computing screen paints in the host computer's hardware-era theme; a screen with no host era (a topology
 * element, a board-less assembly, a program with no era source) falls back to the frozen STANDARD skin. It also
 * writes a machine's own words as a text wall. The mouse test against a rectangle and the menu buttons come from
 * J's Core's container screen.
 */
public abstract class AbstractComputerScreen<T extends AbstractContainerMenu> extends CoreContainerScreen<T> {

    /** The skin this screen paints with for the current render pass; STANDARD until {@link #init} resolves it. */
    protected EraTheme theme = EraThemes.STANDARD;

    /**
     * Where a label goes when the screen draws none.
     *
     * <p>A computer screen that fills a monitor writes its own headings and has no use for the two a
     * container screen puts up by itself, and there is no switch for turning them off; putting them far
     * enough out is how it is done.
     */
    protected static final int OFF_SCREEN = -10_000;

    /** The size a machine's own words are written at, and the step from one line of them to the next. */
    protected static final float WALL_SCALE = TextWall.SCALE;
    protected static final int WALL_ROW = TextWall.ROW;

    protected AbstractComputerScreen(final T menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
    }

    /** Draws one line of a text wall with its top left corner at {@code (x, y)}. */
    protected void wall(final GuiGraphics g, final String line, final int x, final int y, final int color) {
        TextWall.draw(g, font, line, x, y, color);
    }

    /** The same, centred on {@code cx}. */
    protected void wallCentered(final GuiGraphics g, final String line, final int cx, final int y, final int color) {
        TextWall.centered(g, font, line, cx, y, color);
    }

    /** The same, ending at {@code right} rather than starting at a left edge. */
    protected void wallRight(final GuiGraphics g, final String line, final int right, final int y, final int color) {
        TextWall.right(g, font, line, right, y, color);
    }

    /** How wide that line comes out at wall size, in screen pixels. */
    protected int wallWidth(final String line) {
        return TextWall.width(font, line);
    }

    /** That line cut to the room it has, so a name somebody else chose cannot run past its column. */
    protected String wallClip(final String line, final int room) {
        return TextWall.clip(font, line, room);
    }

    /**
     * The hardware era whose skin this screen should wear, or {@code null} to use the STANDARD default. The base
     * returns {@code null}; a screen running on a host computer overrides this to report its host's era so the GUI
     * adopts that era's skin. Resolved fresh every {@link #containerTick}, so inserting or removing a board repaints
     * the GUI in the new era live.
     */
    @Nullable
    protected HardwareEra screenEra() {
        return null;
    }

    /**
     * Outer bounds of the monitor body drawn around this screen's glass (the bezel and its chin), in screen
     * coordinates. A recipe viewer placing its panel beside the monitor reads this so the panel sits next to the
     * bezel rather than over it.
     */
    public MonitorFrameStyle.Geometry frameBounds() {
        final HardwareEra era = screenEra();
        return MonitorFrameStyle
                .forEra(era == null ? HardwareEra.STANDARD : era)
                .geometry(leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void init() {
        super.init();
        this.theme = EraThemes.ofNullable(screenEra());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Re-resolve so a board swap (which changes the host era) updates the skin without reopening the GUI.
        this.theme = EraThemes.ofNullable(screenEra());
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        /*
         * Bind this screen's era skin for the render pass (background, widgets, labels) and always restore the
         * default afterwards, so any unthemed draw stays on the frozen STANDARD look. A subclass that overrides
         * render still routes through here via super.render(), so its background and widgets get the bound skin;
         * its post-super draws are tooltips (vanilla-styled, palette-agnostic) so they are unaffected by the skin.
         */
        JsTechTheme.bind(theme);
        try {
            super.render(graphics, mouseX, mouseY, partialTick);
        } finally {
            JsTechTheme.unbind();
        }
        /*
         * A screen on a monitor reaches the player through the monitor's tube: a monochrome one shows every colour as
         * its phosphor, the sixteen-colour one as the nearest of its sixteen. Done to the whole glass once it is drawn,
         * so whatever drew on it goes through the tube.
         */
        if (menu instanceof IMonitorMenu at) {
            TubeFilter.filterScreen(graphics, leftPos, topPos, imageWidth, imageHeight,
                    PowerStrip.tubeAt(at.monitorPos()));
        }
    }
}
