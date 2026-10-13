/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.client.hologram.HologramView;
import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.hologram.Holograms;
import dev.jstech.core.look.LookTexts;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The Core's HUD kit and holograms as a player sees them: two HUD elements of the test mod stacked in their corner,
 * one under the other, and a hologram the server puts up in front of the player, read on their game and taken down
 * again.
 */
public final class HudHologramClientTests {

    private static final int SETTLE = 4;
    private static final int ARRIVE = 40;
    private static final ResourceLocation HOLOGRAM = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "sign");
    /* The tall bar's height, as the test mod declares it. */
    private static final int TALL_HEIGHT = 24;

    private HudHologramClientTests() {
    }

    @ClientTest(timeoutTicks = 300)
    public static void hud_stacksEveryModsElementsInTheirCorner(final ClientTestContext ctx) {
        ctx.then(0, () -> TestHud.show(true))
                .thenAssert(SETTLE, () -> {
                    final Map<ResourceLocation, HudStack.Point> placed =
                            HudElements.placedIn(HudStack.Corner.TOP_RIGHT);
                    return placed.containsKey(TestHud.TALL) && placed.containsKey(TestHud.SHORT)
                            && placed.get(TestHud.TALL).y() + TALL_HEIGHT <= placed.get(TestHud.SHORT).y();
                }, "the two bars stand in the top right corner, the second under the first")
                .thenScreenshot(2, "hud")
                .then(0, () -> TestHud.show(false))
                .thenAssert(SETTLE, () -> !HudElements.placedIn(HudStack.Corner.TOP_RIGHT).containsKey(TestHud.TALL),
                        "an element that does not show takes no place");
    }

    @ClientTest(timeoutTicks = 300)
    public static void hologram_floatsWhereTheServerPutsIt(final ClientTestContext ctx) {
        ctx.thenServer(0, level -> {
                    final ServerPlayer player = level.players().getFirst();
                    final Vec3 ahead = player.getEyePosition().add(player.getLookAngle().scale(3.0));
                    Holograms.show(level, HOLOGRAM, ahead, List.of(Text.literal("Test hologram"),
                            LookTexts.WORKING.with(42)), 0);
                })
                .thenWaitUntil(() -> HologramView.linesOf(HOLOGRAM).size() == 2, ARRIVE,
                        "the hologram reached the game")
                .thenScreenshot(SETTLE, "hologram")
                .thenServer(0, level -> Holograms.hide(level, HOLOGRAM, level.players().getFirst().position()))
                .thenWaitUntil(() -> HologramView.linesOf(HOLOGRAM).isEmpty(), ARRIVE, "and was taken down");
    }
}
