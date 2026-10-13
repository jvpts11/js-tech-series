/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.jstech.core.api.client.GpuSurface;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * A surface made, cleared and let go in the middle of a frame leaves the game drawing into its own target, as it was:
 * the game's targets bind the window's framebuffer when they are made and let go, and the rest of such a frame went
 * into the window, the whole world seeming to flash for that one frame.
 */
public final class RenderTargetsClientTests {

    private RenderTargetsClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void surface_madeAndLetGo_leavesTheGameDrawingWhereItWas(final ClientTestContext ctx) {
        final boolean[] kept = new boolean[2];
        ctx.then(2, () -> {
                    final RenderTarget main = ctx.mc().getMainRenderTarget();
                    main.bindWrite(true);
                    final GpuSurface surface = new GpuSurface(64, 32);
                    surface.target();
                    kept[0] = bound() == main.frameBufferId;
                    surface.close();
                    kept[1] = bound() == main.frameBufferId;
                })
                .thenAssert(0, () -> kept[0], "the game's own target is still bound after a surface is made")
                .thenAssert(0, () -> kept[1], "and after it is let go");
    }

    private static int bound() {
        return GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
    }
}
