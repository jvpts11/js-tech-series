/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableModel;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * A portable's face held up, in the GUI's pixels, as the K0.10 faces have it: how large it is, where its screen sits
 * in it, and the buttons round the screen that do something when pressed: the PDAs' four program buttons and their
 * pad, the qPhones' and the qPad Air's home button, the Landmark's four keys.
 *
 * @param width   how wide the face is
 * @param height  how tall
 * @param screenX where its screen starts across it
 * @param screenY where its screen starts down it
 * @param cutout  how far in from its screen's top right a camera's hole in the glass reaches; 0 for none
 * @param buttons its buttons
 */
public record PortableFace(int width, int height, int screenX, int screenY, int cutout, List<Button> buttons) {

    /** The face of {@code model}. */
    public static PortableFace of(final PortableModel model) {
        return switch (model) {
            case HIGHTIDE_POCKET -> new PortableFace(174, 294, 15, 37, 0, pda(251));
            case HIGHTIDE_POCKET_PRO -> new PortableFace(174, 284, 15, 31, 0, pda(245));
            case QPHONE -> new PortableFace(150, 282, 11, 43, 0, List.of(round(75, 258, 13, MobileTap.HOME)));
            case MOTOROLLER_LANDMARK -> new PortableFace(130, 262, 11, 25, 0, landmark());
            case QPHONE_5 -> new PortableFace(130, 274, 11, 41, 0, List.of(round(65, 253, 13, MobileTap.HOME)));
            case HIGHTIDE_WAVE -> new PortableFace(139, 284, 11, 35, 0, List.of());
            case QPAD_AIR -> new PortableFace(214, 302, 17, 31, 0, List.of(round(107, 286, 10, MobileTap.HOME)));
            case SUNSUNG_NEBULA_TAB -> new PortableFace(358, 238, 19, 19, 0, List.of());
            case QPHONE_X -> new PortableFace(124, 255, 6, 6, 0, List.of());
            case SUNSUNG_NEBULA_S10 -> new PortableFace(122, 250, 5, 7, 18, List.of());
            case QPAD_PRO -> new PortableFace(218, 302, 11, 11, 0, List.of());
            case SUNSUNG_NEBULA_TAB_S7 -> new PortableFace(372, 240, 10, 10, 0, List.of());
        };
    }

    /** The button at {@code (x, y)} on the face, or null where there is none. */
    @Nullable
    public MobileTap buttonAt(final double x, final double y) {
        for (final Button button : buttons) {
            if (x >= button.x() && x < button.x() + button.w() && y >= button.y() && y < button.y() + button.h()) {
                return button.tap();
            }
        }
        return null;
    }

    /*
     * A PDA's four program buttons, as a Pocket PC's launched its own programs: the Today, the network, the alerts and
     * the settings; and its pad, whose press goes to the Today.
     */
    private static List<Button> pda(final int y) {
        return List.of(new Button(21, y, 17, 11, MobileTap.HOME), new Button(45, y, 17, 11,
                MobileTap.open(MobileApp.NETWORK)), new Button(113, y, 17, 11, MobileTap.open(MobileApp.ALERTS)),
                new Button(137, y, 17, 11, MobileTap.SETTINGS), round(87, y + 5, 13, MobileTap.HOME));
    }

    /* The Landmark's four keys under its screen: back, then home on the three others. */
    private static List<Button> landmark() {
        return List.of(new Button(17, 233, 14, 10, MobileTap.BACK), new Button(44, 233, 14, 10, MobileTap.HOME),
                new Button(71, 233, 14, 10, MobileTap.HOME), new Button(98, 233, 14, 10, MobileTap.HOME));
    }

    /* A round button of radius {@code r} round {@code (cx, cy)}, pressed anywhere in its square. */
    private static Button round(final int cx, final int cy, final int r, final MobileTap tap) {
        return new Button(cx - r, cy - r, 2 * r, 2 * r, tap);
    }

    /** A button on the face: where it is and what pressing it does. */
    public record Button(int x, int y, int w, int h, MobileTap tap) {
    }
}
