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
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.data.DataContainerScreen;
import dev.jstech.core.client.gui.data.DataScreen;
import dev.jstech.core.client.gui.data.DataScreenView;
import dev.jstech.core.gui.flex.Placed;
import dev.jstech.core.text.GameText;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestScreens;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.function.BooleanSupplier;
import net.minecraft.resources.ResourceLocation;

/**
 * A screen declared in a file draws its still parts once: its window, panels, slot frames and fixed words go into a
 * picture laid down frame after frame, and what moves is drawn over it. It looks exactly as it does drawn each frame
 * the old way, and as its reference picture.
 */
public final class StillPartsClientTests {

    private static final int WAIT = 60;
    private static final int FRAMES = 20;
    private static final int SETTLE = 2;
    /* A screen with a section a value hides, a fixed word inside it. */
    private static final ResourceLocation HIDDEN = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_still_hidden");

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

    /**
     * A fixed word inside a section a value hides leaves the still picture with its section: the window with the
     * section hidden while it is open looks as the same window opened with the section hidden from the start.
     */
    @ClientTest(timeoutTicks = 400)
    public static void stillParts_leaveWithTheSectionThatHoldsThem(final ClientTestContext ctx) {
        final boolean[] shown = {true};
        final NativeImage[] hiddenWhileOpen = new NativeImage[1];
        final DataScreen[] first = new DataScreen[1];
        ctx.afterTest(() -> {
            ctx.mc().setScreen(null);
            if (hiddenWhileOpen[0] != null) {
                hiddenWhileOpen[0].close();
            }
        });
        ctx.then(0, () -> {
                    first[0] = sectionScreen(() -> shown[0]);
                    ctx.mc().setScreen(first[0]);
                })
                .thenAwaitScreen(DataScreen.class, WAIT)
                .thenWaitUntil(() -> steady(ctx).visible(), WAIT, "the fixed word to show in its section")
                .then(SETTLE, () -> shown[0] = false)
                .thenWaitUntil(() -> !steady(ctx).visible(), WAIT, "the section to hide, the word with it")
                .then(SETTLE, () -> hiddenWhileOpen[0] = window(ctx))
                .then(0, () -> ctx.mc().setScreen(sectionScreen(() -> false)))
                .thenWaitUntil(() -> ctx.screen(DataScreen.class) != first[0] && !steady(ctx).visible(), WAIT,
                        "the same window to open with its section hidden from the start")
                .then(SETTLE, () -> {
                    try (NativeImage fromTheStart = window(ctx)) {
                        ctx.assertSamePicture("section-hidden", hiddenWhileOpen[0], fromTheStart);
                    }
                });
    }

    /* The screen with a section shown while {@code shown} says so. */
    private static DataScreen sectionScreen(final BooleanSupplier shown) {
        return new DataScreen(HIDDEN, GameText.component(TestScreens.TITLE),
                screen -> screen.value("shown", shown::getAsBoolean), null);
    }

    /* The fixed word inside the section. */
    private static UiComponent steady(final ClientTestContext ctx) {
        return ctx.screen(DataScreen.class).find("steady").orElseThrow().component();
    }

    /* The window of the data screen open now, as it is drawn. */
    private static NativeImage window(final ClientTestContext ctx) {
        final DataScreenView view = ctx.screen(DataScreen.class).view();
        return ctx.capture(view.left(), view.top(), view.width(), view.height());
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
