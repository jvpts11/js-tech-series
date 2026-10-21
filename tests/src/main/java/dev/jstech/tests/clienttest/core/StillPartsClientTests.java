/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.gui.component.StillLayer;
import dev.jstech.core.client.gui.data.DataContainerScreen;
import dev.jstech.core.client.gui.data.DataScreenView;
import dev.jstech.core.gui.flex.Placed;
import dev.jstech.tests.TestScreens;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;

/**
 * A screen declared in a file draws its still parts once: its window, panels, slot frames and fixed words go into a
 * picture laid down frame after frame, and what moves is drawn over it. It looks exactly as it does drawn each frame
 * the old way, and as its reference picture.
 */
public final class StillPartsClientTests {

    private static final int WAIT = 60;
    private static final int FRAMES = 20;
    private static final int SETTLE = 2;

    private StillPartsClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void stillParts_areDrawnOnceAndLookAsDrawnEachFrame(final ClientTestContext ctx) {
        final int[] first = new int[1];
        final NativeImage[] layered = new NativeImage[1];
        ctx.afterTest(() -> {
            StillLayer.bypass(false);
            ctx.mc().setScreen(null);
            if (layered[0] != null) {
                layered[0].close();
            }
        });
        ctx.thenServer(0, level -> ctx.serverPlayer().openMenu(TestScreens.provider()))
                .thenAwaitScreen(DataContainerScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final DataScreenView view = ctx.screen(DataContainerScreen.class).view();
                    ctx.assertTrue(!view.stillOnes().isEmpty(), "the window's still parts are found");
                    first[0] = view.stillPaints();
                })
                .thenAssert(FRAMES, () -> ctx.screen(DataContainerScreen.class).view().stillPaints() == first[0],
                        "the still parts are not drawn again while nothing of them changes")
                .then(0, () -> {
                    final int[] area = machine(ctx);
                    layered[0] = ctx.capture(area[0], area[1], area[2], area[3]);
                    StillLayer.bypass(true);
                })
                .then(SETTLE, () -> {
                    final int[] area = machine(ctx);
                    try (NativeImage each = ctx.capture(area[0], area[1], area[2], area[3])) {
                        ctx.assertSamePicture("drawn-each-frame", each, layered[0]);
                    }
                    StillLayer.bypass(false);
                })
                .then(SETTLE, () -> {
                    final int[] area = machine(ctx);
                    ctx.matchesReference("test-machine", area[0], area[1], area[2], area[3]);
                });
    }

    /*
     * The machine's part of the screen, above the player's inventory, which other tests fill as they please; the
     * layout's places are from the window's top left.
     */
    private static int[] machine(final ClientTestContext ctx) {
        final DataContainerScreen<?> screen = ctx.screen(DataContainerScreen.class);
        final Placed inventory = screen.view().placed().orElseThrow().find("inventory").orElseThrow();
        return new int[] {screen.getGuiLeft(), screen.getGuiTop(), screen.view().width(), inventory.y()};
    }
}
