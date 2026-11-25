/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WindowGeometryTest {

    @Test
    void resolve_floatingNeverRendersBelowTheMinimum() {
        /*
         * The shipped bug: a window persisted below the (since-grown) minimum rendered a tiny content area, so
         * the Network Interactor's details panel was clipped to a few pixels and slid over the grid. The
         * resolved size MUST clamp up to the minimum; this test fails if that clamp is ever dropped again.
         */
        final WindowGeometry.Rect r = WindowGeometry.resolve(10, 20, 200, 100, 313, 158, false, 800, 600, 24);
        assertEquals(313, r.w(), "width clamps up to the minimum");
        assertEquals(158, r.h(), "height clamps up to the minimum");
        assertEquals(10, r.x(), "x is kept");
        assertEquals(20, r.y(), "y is kept");
    }

    @Test
    void resolve_floatingKeepsItsSizeWhenAtOrAboveTheMinimum() {
        final WindowGeometry.Rect r = WindowGeometry.resolve(10, 20, 400, 300, 313, 158, false, 800, 600, 24);
        assertEquals(400, r.w());
        assertEquals(300, r.h());
    }

    @Test
    void resolve_floatingClampsOnlyTheAxisThatIsTooSmall() {
        final WindowGeometry.Rect r = WindowGeometry.resolve(0, 0, 200, 300, 313, 158, false, 800, 600, 24);
        assertEquals(313, r.w(), "narrow width clamps to the minimum");
        assertEquals(300, r.h(), "tall enough height is kept");
    }

    @Test
    void resolve_keptInsideABiggerWindowShrinksToTheWorkAreaAndMovesIn() {
        /*
         * The shipped bug: the Crafting Manager opened on a big desktop kept its size when the game's window, and
         * the desktop with it, shrank; its buttons and the ends of its lists lay under the taskbar, out of reach.
         */
        final WindowGeometry.Rect r = WindowGeometry.resolve(110, 106, 380, 300, 280, 160, false, 400, 220, 24, 0,
                true);
        assertEquals(380, r.w(), "a width that fits is kept");
        assertEquals(196, r.h(), "the height shrinks to the work area above the taskbar");
        assertEquals(20, r.x(), "it moves left until its right edge is on the desktop");
        assertEquals(0, r.y(), "and up until its bottom is above the taskbar");
    }

    @Test
    void resolve_keptInsideNeverShrinksBelowTheMinimum() {
        final WindowGeometry.Rect r = WindowGeometry.resolve(50, 50, 380, 300, 280, 160, false, 250, 150, 24, 0,
                true);
        assertEquals(280, r.w(), "never narrower than the minimum, on a desktop narrower than that");
        assertEquals(160, r.h(), "never lower than the minimum");
        assertEquals(0, r.x(), "it starts at the desktop's left edge");
        assertEquals(0, r.y(), "and at the top of the work area");
    }

    @Test
    void resolve_keptInsideLeavesAWindowThatFitsWhereItIs() {
        final WindowGeometry.Rect r = WindowGeometry.resolve(40, 60, 300, 200, 280, 160, false, 800, 600, 24, 0,
                true);
        assertEquals(40, r.x());
        assertEquals(60, r.y());
        assertEquals(300, r.w());
        assertEquals(200, r.h());
    }

    @Test
    void resolve_keptInsideStaysUnderATopPanel() {
        final WindowGeometry.Rect r = WindowGeometry.resolve(10, 2, 300, 200, 280, 160, false, 800, 600, 0, 24,
                true);
        assertEquals(24, r.y(), "a window above the top panel comes down under it");
    }

    @Test
    void resolve_maximizedFillsTheDesktopAboveTheTaskbar() {
        final WindowGeometry.Rect r = WindowGeometry.resolve(10, 20, 200, 100, 313, 158, true, 800, 600, 24);
        assertEquals(0, r.x());
        assertEquals(0, r.y());
        assertEquals(800, r.w(), "maximized width is the desktop width");
        assertEquals(576, r.h(), "maximized height is the desktop minus the taskbar");
    }

    @Test
    void resolve_maximizedStartsBelowATopPanel() {
        /*
         * A GNOME-style desktop reserves its bar at the top and nothing at the bottom: the maximized window
         * starts under the bar and runs to the bottom edge.
         */
        final WindowGeometry.Rect r = WindowGeometry.resolve(10, 20, 200, 100, 313, 158, true, 800, 600, 0, 24);
        assertEquals(0, r.x());
        assertEquals(24, r.y(), "maximized top is the top panel's bottom edge");
        assertEquals(800, r.w());
        assertEquals(576, r.h(), "maximized height is the desktop minus the top panel");
    }

    @Test
    void resolve_maximizedFitsBetweenTopAndBottomReserves() {
        final WindowGeometry.Rect r = WindowGeometry.resolve(0, 0, 10, 10, 10, 10, true, 800, 600, 24, 24);
        assertEquals(24, r.y());
        assertEquals(552, r.h(), "both reserves are subtracted");
    }

    @Test
    void scissor_addsThePoseTranslationToTheLocalRectangle() {
        /*
         * GuiGraphics.enableScissor ignores the pose, so a desktop app MUST add the pose translation to its
         * window-local scissor coords or the clip is offset from the drawn content, the bug that clipped the
         * Network Interactor's details text and grid cells in the wrong place. Absolute = pose-origin + local.
         */
        final WindowGeometry.Rect r = WindowGeometry.scissor(40, 50, 5, 6, 105, 56);
        assertEquals(45, r.x(), "x = poseX + x1");
        assertEquals(56, r.y(), "y = poseY + y1");
        assertEquals(100, r.w(), "width = x2 - x1");
        assertEquals(50, r.h(), "height = y2 - y1");
    }

    @Test
    void scissor_underAScaledPoseShrinksTheClipTowardsTheOrigin() {
        // A desktop drawn at three quarters: local 100..200 sits on the screen at 40 + 75 .. 40 + 150.
        final WindowGeometry.Rect r = WindowGeometry.scissor(40f, 50f, 0.75f, 0.75f, 100, 20, 200, 60);
        assertEquals(115, r.x(), "x = poseX + x1 * scale");
        assertEquals(65, r.y(), "y = poseY + y1 * scale");
        assertEquals(75, r.w(), "width scales with the pose");
        assertEquals(30, r.h(), "height scales with the pose");
        final WindowGeometry.Rect whole = WindowGeometry.scissor(40f, 50f, 1f, 1f, 5, 6, 105, 56);
        assertEquals(45, whole.x(), "at scale one it is the plain sum");
        assertEquals(100, whole.w());
    }
}
