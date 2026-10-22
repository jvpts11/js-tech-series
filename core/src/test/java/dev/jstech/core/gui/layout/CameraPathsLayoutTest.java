/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CameraPathsLayoutTest {

    /** The smallest screen, common ones, and a large one. */
    private static final List<int[]> SCREENS = List.of(new int[] {426, 240}, new int[] {480, 320},
            new int[] {640, 360}, new int[] {960, 540});
    /** The tools' labels at three quarters of the game's font, the longer of the series' two languages, in order. */
    private static final List<Integer> TABS = List.of(27, 23, 23, 32, 36, 18, 18, 41, 59, 45, 32, 27, 18, 18, 32);
    /**
     * The four buttons' labels, the longer of the two languages in the game's font: Fly and record or Stop
     * recording, Add here, Play, Save to file.
     */
    private static final List<Integer> LABELS = List.of(84, 78, 30, 92);

    @Test
    void layout_isCleanOnEveryScreenWithManyKeyframes() {
        for (final int[] screen : SCREENS) {
            final GuiLayout layout = CameraPathsLayout.layout(screen[0], screen[1], TABS, 40, LABELS);
            assertTrue(layout.isClean(), screen[0] + "x" + screen[1] + ": " + layout.overlaps() + " "
                    + layout.outOfBounds());
        }
    }

    @Test
    void of_keepsTheButtonsTheRowsAndTheHintInsideTheKeyframesPanel() {
        for (final int[] screen : SCREENS) {
            final StudioLayout.Rect body = StudioLayout.of(screen[0], screen[1], TABS).body();
            final CameraPathsLayout.Laid laid = CameraPathsLayout.of(body, 40, LABELS);
            for (final StudioLayout.Rect button : laid.buttons()) {
                assertTrue(button.within(laid.keyframes()), "a button at " + screen[0]);
            }
            for (int i = 0; i < laid.rows(); i++) {
                assertTrue(CameraPathsLayout.keyframeRow(laid, i).within(laid.keyframes()), "a row at " + screen[0]);
            }
            for (int i = 0; i < laid.pathRows(); i++) {
                assertTrue(CameraPathsLayout.pathRow(laid, i).within(laid.list()), "a path at " + screen[0]);
            }
            assertTrue(laid.hintY() + CameraPathsLayout.HINT_LINES * CameraPathsLayout.SMALL_LINE <= laid.fileY(),
                    "the hint above the file at " + screen[0]);
            assertTrue(laid.mapArea().within(laid.map()), "the map in its panel at " + screen[0]);
        }
    }

    @Test
    void of_putsTheButtonsUnderTheRowsShown() {
        final StudioLayout.Rect body = StudioLayout.of(960, 540, TABS).body();
        final CameraPathsLayout.Laid few = CameraPathsLayout.of(body, 3, LABELS);
        assertEquals(3, few.rows());
        assertTrue(few.buttons().get(0).y() > CameraPathsLayout.keyframeRow(few, 2).bottom());
        final CameraPathsLayout.Laid none = CameraPathsLayout.of(body, 0, LABELS);
        assertEquals(1, none.rows(), "a row is kept for the table even with none yet");
    }

    @Test
    void of_putsTheButtonsTwoToARowWhenOneIsTooNarrow() {
        final StudioLayout.Rect body = StudioLayout.of(426, 240, TABS).body();
        final CameraPathsLayout.Laid laid = CameraPathsLayout.of(body, 4, LABELS);
        assertEquals(laid.buttons().get(0).y(), laid.buttons().get(1).y());
        assertTrue(laid.buttons().get(2).y() > laid.buttons().get(0).bottom());
    }

    @Test
    void fit_keepsEveryPlaceAndTheOriginInsideTheMap() {
        final StudioLayout.Rect area = new StudioLayout.Rect(300, 50, 120, 160);
        final List<double[]> places = List.of(new double[] {-20, 4}, new double[] {35, -60}, new double[] {2, 18});
        final CameraPathsLayout.Fit fit = CameraPathsLayout.fit(area, places);
        for (final double[] place : places) {
            final double[] on = fit.at(place[0], place[1]);
            assertTrue(area.contains(on[0], on[1]), place[0] + ", " + place[1]);
        }
        final double[] origin = fit.at(0, 0);
        assertTrue(area.contains(origin[0], origin[1]));
        final double[] east = fit.at(10, 0);
        final double[] south = fit.at(0, 10);
        assertTrue(east[0] > origin[0] && south[1] > origin[1], "east to the right, south down");
    }

    @Test
    void fit_spansAFewBlocksAtLeast() {
        final CameraPathsLayout.Fit fit = CameraPathsLayout.fit(new StudioLayout.Rect(0, 0, 100, 100), List.of());
        assertEquals((100.0 - 2 * CameraPathsLayout.MAP_MARGIN) / CameraPathsLayout.LEAST_SPAN, fit.scale(), 1e-9);
    }
}
