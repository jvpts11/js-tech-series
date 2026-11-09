/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.portable.MobileSystem;

/**
 * How a family of mobile systems draws its screen (computers/docs/design/wireless.md), each in its year's look as the
 * K0.11 pages drew it: Frames Pocket's ({@link FramesPocketLook}), qOS's ({@link QosLook}), Droidix's
 * ({@link DroidixLook}). A look draws the page a frame is on and marks what can be pressed as it draws; how its lock
 * lets go and whether its alerts are pulled down from the top are its own too.
 */
public sealed interface IMobileLook permits FramesPocketLook, QosLook, DroidixLook {

    /** The look of {@code system}. */
    static IMobileLook of(final MobileSystem system) {
        return switch (system.family()) {
            case FRAMES_POCKET -> new FramesPocketLook(system);
            case QOS -> new QosLook(system);
            case DROIDIX -> new DroidixLook(system);
        };
    }

    /** Draws the page {@code frame} is on. */
    void paint(MobileCanvas canvas, MobileFrame frame);

    /** Draws the system's own start, after its maker's mark, {@code progress} of the way through, from 0 to 1. */
    void boot(MobileCanvas canvas, MobileFrame frame, float progress);

    /** How its lock lets go. */
    Unlock unlock();

    /** Whether a pull down from the top of the screen brings its alerts over the page. */
    boolean pullsAlerts();

    /** How a lock lets go: a slider drawn across, a swipe up from the foot, or a tap on its soft key. */
    enum Unlock {
        SLIDE,
        SWIPE_UP,
        TAP
    }
}
