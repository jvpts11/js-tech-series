/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.gui.help.HelpForm;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class HelpViewerLayoutTest {

    /** How wide the font draws a letter at most, which a button's words have to fit at. */
    private static final int LETTER = 6;

    @Test
    void layout_hasNothingOverlappingAndNothingOutsideTheWindowInEveryForm() {
        for (final HelpForm form : HelpForm.values()) {
            final GuiLayout layout = HelpViewerLayout.layout(form);

            assertTrue(layout.isClean(), form + ": overlaps " + layout.overlaps() + ", out of bounds "
                    + layout.outOfBounds());
        }
    }

    @Test
    void of_givesEveryFormThePartsItsSystemHad() {
        assertNotNull(HelpViewerLayout.of(HelpForm.FRAMES_XP).band(), "XP's blue band");
        assertNotNull(HelpViewerLayout.of(HelpForm.GET_HELP).search(), "Get Help's search across the top");
        assertNull(HelpViewerLayout.of(HelpForm.YELP).tree(), "GNOME's Help shows no tree");
        assertNotNull(HelpViewerLayout.of(HelpForm.YELP).trail(), "but the trail of where a page is");
        assertTrue(HelpViewerLayout.of(HelpForm.CDE).buttons().size() == 4, "CDE's four buttons");
        assertTrue(HelpViewerLayout.of(HelpForm.FRAMES_95).tabs().size() == 3, "Frames 95's three tabs");
    }

    @Test
    void of_keepsThePageClearOfTheTreeAndInsideTheWindow() {
        for (final HelpForm form : HelpForm.values()) {
            final HelpViewerLayout.Frame frame = HelpViewerLayout.of(form);
            final HelpViewerLayout.Box page = frame.page();

            assertTrue(page.x() + page.w() <= frame.width() && page.y() + page.h() <= frame.height(),
                    form + ": the page is inside the window");
            if (frame.tree() != null && form != HelpForm.FRAMES_95 && form != HelpForm.CDE) {
                assertTrue(frame.tree().x() + frame.tree().w() <= page.x(), form + ": the tree is left of the page");
            }
        }
    }

    @Test
    void of_givesEveryButtonRoomForItsWords() {
        for (final HelpForm form : HelpForm.values()) {
            for (final HelpViewerLayout.Button button : HelpViewerLayout.of(form).buttons()) {
                if (button.label() != null) {
                    assertTrue(button.label().english().length() * LETTER <= button.box().w() + LETTER,
                            form + ": " + button.label().english() + " fits its button");
                }
            }
        }
    }
}
