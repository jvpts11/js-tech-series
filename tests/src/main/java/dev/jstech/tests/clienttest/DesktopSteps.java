/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import org.jetbrains.annotations.Nullable;

/**
 * What every client test of a desktop does the same way: starting a program from the Start menu, finding a
 * program's window by its title, and turning a point inside a window into a point on the desktop. Kept in one
 * place so a renamed title or a changed window frame is a change to one file.
 */
public final class DesktopSteps {

    private DesktopSteps() {
    }

    /**
     * Opens the Start menu and clicks {@code label}. A program the menu does not list fails here, with the labels
     * it does list, rather than clicking somewhere else and failing later for a reason that points nowhere.
     */
    public static void launch(final ClientTestContext ctx, final String label) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        ctx.click(desktop.startButtonX(), desktop.startButtonY());
        final int item = desktop.launcherLabels().indexOf(label);
        ctx.assertTrue(item >= 0, "the Start menu must list " + label + "; got " + desktop.launcherLabels());
        ctx.click(desktop.startMenuItemX(item), desktop.startMenuItemY(item));
    }

    /**
     * The program of type {@code type} in the window titled {@code title}, or null while the desktop is not the
     * open screen, the window is not up or holds another program, so a wait polls instead of failing.
     */
    @Nullable
    public static <T> T app(final ClientTestContext ctx, final String title, final Class<T> type) {
        final DesktopScreen desktop = ctx.openScreen(DesktopScreen.class);
        if (desktop == null) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(title);
        return window != null && type.isInstance(window.app()) ? type.cast(window.app()) : null;
    }

    /**
     * Converts a point inside the content of the window titled {@code title} into desktop coordinates. A window
     * that draws its own frame has no title bar or inset to step over.
     */
    public static int[] contentPoint(final ClientTestContext ctx, final String title, final int[] local) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(title);
        if (window == null) {
            throw new ClientTestFailure("the " + title + " window is gone");
        }
        return contentPoint(window, local);
    }

    /** The same for a window already in hand. */
    public static int[] contentPoint(final DesktopWindow window, final int[] local) {
        if (local == null) {
            throw new ClientTestFailure("no such control on the window");
        }
        final int insetX = window.ownFrame() ? 0 : DesktopWindow.CONTENT_INSET;
        final int insetY = window.ownFrame() ? 0 : DesktopWindow.TITLE_H + DesktopWindow.CONTENT_INSET;
        return new int[]{window.x() + insetX + local[0], window.y() + insetY + local[1]};
    }
}
