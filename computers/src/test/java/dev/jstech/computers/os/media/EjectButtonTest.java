/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EjectButtonTest {

    @Test
    void pressedAt_takesTheButtonAndHalfAPixelRoundIt() {
        final EjectButton button = new EjectButton(48, 23, 56, 25, 4);
        assertTrue(button.pressedAt(52, 24));
        assertTrue(button.pressedAt(46, 21));
        assertTrue(button.pressedAt(58, 27));
        assertFalse(button.pressedAt(45.9, 24));
        assertFalse(button.pressedAt(52, 27.1));
    }

    @Test
    void ejectButton_isOnEveryDriveThatReadsADiscAndOnNoOther() {
        for (final MediaDriveType drive : MediaDriveType.values()) {
            boolean readsADisc = false;
            for (final MediaFormat format : MediaFormat.values()) {
                readsADisc |= drive.accepts(format) && format.onTray();
            }
            assertEquals(readsADisc, drive.ejectButton() != null, drive.serializedName());
        }
    }

    @Test
    void onTray_isTheOpticalDiscsOnly() {
        assertTrue(MediaFormat.CD.onTray());
        assertTrue(MediaFormat.DVD.onTray());
        assertTrue(MediaFormat.BLU_RAY.onTray());
        assertFalse(MediaFormat.FLOPPY.onTray());
        assertFalse(MediaFormat.USB.onTray());
    }
}
