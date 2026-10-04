/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.core.client.gui.component.Grounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A machine's desktop drawn with no screen open: the same desktop its monitor's screen shows, sized to whatever it is
 * drawn into. It has no container, so a window with an inventory zone shows the zone empty, and it asks the machine
 * nothing, so it shows the desktop as the machine's identity makes it: the system's look and era, its panel and the
 * programs every copy of that system has. A monitor showing its desktop in the world draws it this way.
 */
public final class OffscreenDesktop implements DesktopSurface {

    private final DesktopState state;
    /** The size of the area the desktop was last drawn into, which it is laid out for. */
    private int width;
    private int height;

    public OffscreenDesktop(final BlockPos host, final BlockPos monitorPos, final ResourceLocation osId,
                            final ResourceLocation desktopId, final int ramTotalMb, final int ramReservedMb) {
        this.state = new DesktopState(this, host, monitorPos, osId, desktopId, ramTotalMb, ramReservedMb);
        state.prepare();
    }

    /**
     * Draws the desktop into an area of that size at the pose's origin, with the pointer at a point of it, the way the
     * monitor's screen draws it on a game window of that size: the monitor's frame, and the desktop inside its glass.
     * Returns false while a cooperative kernel's crash screen is all there is to draw.
     */
    public boolean paint(final GuiGraphics g, final int areaWidth, final int areaHeight, final int mouseX,
                         final int mouseY, final float partialTick) {
        this.width = areaWidth;
        this.height = areaHeight;
        final DesktopViewport view = state.view();
        /*
         * The grounds this desktop declares are in the units of the picture it is drawn into, not the screen's: they
         * start from none and are forgotten once it is drawn, so neither another monitor's nor the screen's text reads
         * them as its own.
         */
        Grounds.clear();
        try {
            return state.paint(g, (int) Math.floor(view.localX(mouseX)), (int) Math.floor(view.localY(mouseY)),
                    partialTick);
        } finally {
            Grounds.clear();
        }
    }

    /**
     * Shows the windows the machine has open, front-most last, on the workspace it has up: the layout the server
     * describes, mirrored as it changes. Nothing goes back to the machine.
     */
    public void showWindows(final List<OpenWindow> windows, final int workspace) {
        state.mirrorWindows(windows, workspace);
    }

    /**
     * How many times the programs this client has for any machine have changed hands; a face drawn with them shows its
     * windows again when this moves on, so a program opened or left at the machine reaches its face.
     */
    public static int programsGeneration() {
        return WindowLayouts.generation();
    }

    /** The programs its windows are drawn by, back-most first. */
    public List<IDesktopApp> windowPrograms() {
        final List<IDesktopApp> programs = new ArrayList<>();
        for (final DesktopWindow window : state.wm().all()) {
            programs.add(window.app());
        }
        return programs;
    }

    /** What the desktop lists as things it can start, in the order its launcher shows them. */
    public List<String> launcherLabels() {
        return state.launcherLabels();
    }

    @Override
    public int surfaceWidth() {
        return width;
    }

    @Override
    public int surfaceHeight() {
        return height;
    }

    @Override
    @Nullable
    public DesktopMenu container() {
        return null;
    }

    /** There is no screen to leave: a desktop drawn this way just stops being drawn. */
    @Override
    public void leave() {
    }

    /** A monitor's face in the world shows every window and menu where it ends. */
    @Override
    public boolean moves() {
        return false;
    }
}
