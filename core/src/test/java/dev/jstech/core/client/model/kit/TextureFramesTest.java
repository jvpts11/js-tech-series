/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextureFramesTest {

    @Test
    void of_onePictureIsStill() {
        assertSame(TextureFrames.STILL, TextureFrames.of(1, List.of(), 2));
        assertTrue(TextureFrames.STILL.still());
        assertEquals(0.0F, TextureFrames.STILL.vOffset(123));
    }

    @Test
    void frameAt_showsEveryFrameInTurnWhenNoneIsListed() {
        final TextureFrames frames = TextureFrames.of(4, List.of(), 2);
        assertEquals(0, frames.frameAt(0));
        assertEquals(0, frames.frameAt(1));
        assertEquals(1, frames.frameAt(2));
        assertEquals(3, frames.frameAt(7));
        assertEquals(0, frames.frameAt(8));
    }

    @Test
    void frameAt_followsTheListedOrderAndTicks() {
        final TextureFrames frames = TextureFrames.of(3, List.of(new int[] {2, 5}, new int[] {0, -1}), 1);
        assertEquals(2, frames.frameAt(0));
        assertEquals(2, frames.frameAt(4));
        assertEquals(0, frames.frameAt(5));
        assertEquals(2, frames.frameAt(6));
    }

    @Test
    void vOffset_coversOneFrameOfTheStrip() {
        final TextureFrames frames = TextureFrames.of(4, List.of(), 1);
        assertEquals(0.25F, frames.vScale());
        assertEquals(0.5F, frames.vOffset(2));
    }

    @Test
    void frameAt_wrapsNegativeTicksAndIndices() {
        final TextureFrames frames = TextureFrames.of(2, List.of(new int[] {-1, 1}), 1);
        assertEquals(1, frames.frameAt(-3));
    }
}
