/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.guide.ManualScreen;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.core.guide.GuidePiece;
import dev.jstech.space.guide.MissionHandbook;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;

/**
 * The Mission Handbook's scenes of the multiblocks of J's Space, each drawn on its page as the game draws its blocks:
 * the Rocket Hangar, a hundred and ten blocks tall, past the grid a scene once had, also turned by a drag; the launch
 * pad, its trench dug into the ground; the crawler-transporter; the propellant depot; both landing platforms; the
 * ship landing pad; and the Small Shipyard. Each is photographed as the book opens it, and its picture must hold the
 * colours of the blocks drawn in it, not the bare page.
 */
public final class HandbookScenesClientTests {

    private static final int SETTLE = 4;
    private static final int OPEN = 40;
    /* How far a drag turns the hangar, in pixels across and down. */
    private static final int DRAG_ACROSS = 60;
    private static final int DRAG_DOWN = 10;
    /*
     * The points read round a scene's middle, in the GUI's pixels: a square of them this many each way from it, this
     * far apart; and how many colours a picture of blocks shows among them at least, where a bare page shows one or
     * two.
     */
    private static final int SAMPLES = 4;
    private static final int SAMPLE_STEP = 4;
    private static final int COLOURS = 4;

    private HandbookScenesClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void rocketHangar_isDrawnWholeAndTurns(final ClientTestContext ctx) {
        final String scene = scene("hangar", 1);
        shown(ctx, "hangar", 1, "handbook_scene_hangar")
                .then(0, () -> {
                    final int[] at = manual().scenePoint(scene, false);
                    manual().mouseClicked(at[0], at[1], 0);
                    manual().mouseDragged(at[0] + DRAG_ACROSS, at[1] + DRAG_DOWN, 0, DRAG_ACROSS, DRAG_DOWN);
                    manual().mouseReleased(at[0] + DRAG_ACROSS, at[1] + DRAG_DOWN, 0);
                })
                .thenAssert(SETTLE, () -> manual().sceneView(scene).map(view -> view.yaw() != declaredYaw(scene))
                        .orElse(false), "the hangar turns under the drag")
                .then(SETTLE, () -> drawn(ctx, scene, "the hangar, turned"))
                .thenScreenshot(2, "handbook_scene_hangar_turned")
                .then(0, HandbookScenesClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void launchPad_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "launch_pad", 1, "handbook_scene_launch_pad"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void crawler_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "crawler", 1, "handbook_scene_crawler"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void propellantDepot_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "propellant_depot", 1, "handbook_scene_propellant_depot"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void landingPlatformOfThree_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "landing_platform", 1, "handbook_scene_landing_platform_small"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void landingPlatformOfFive_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "landing_platform", 2, "handbook_scene_landing_platform_large"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void shipLandingPad_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "ship_landing_pad", 1, "handbook_scene_ship_pad"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void smallShipyard_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "shipyard", 1, "handbook_scene_shipyard"));
    }

    /*
     * Opens the handbook at the entry, shows the spread holding its {@code number}th scene, checks its picture holds
     * blocks and photographs it as the book shows it.
     */
    private static ClientTestContext shown(final ClientTestContext ctx, final String entry, final int number,
                                           final String label) {
        final String scene = scene(entry, number);
        return ctx.then(0, () -> GuideClient.open(MissionHandbook.MANUAL, "jsspace:" + entry))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> toScene(scene))
                .thenAssert(SETTLE, () -> manual().scenePoint(scene, false) != null,
                        "the scene of " + entry + " is drawn on its page")
                .then(SETTLE, () -> drawn(ctx, scene, entry))
                .thenScreenshot(2, label);
    }

    /* Fails unless the picture round the middle of that scene shows the many colours of blocks, not the page. */
    private static void drawn(final ClientTestContext ctx, final String scene, final String what) {
        final int[] middle = manual().scenePoint(scene, false);
        final Set<Integer> colours = new HashSet<>();
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -SAMPLES; dx <= SAMPLES; dx++) {
                for (int dy = -SAMPLES; dy <= SAMPLES; dy++) {
                    colours.add(frame.pixel(middle[0] + dx * SAMPLE_STEP, middle[1] + dy * SAMPLE_STEP));
                }
            }
        }
        ctx.assertTrue(colours.size() >= COLOURS, "the scene of " + what + " shows its blocks: " + colours.size()
                + " colours round its middle");
    }

    private static void closed(final ClientTestContext ctx) {
        ctx.then(0, HandbookScenesClientTests::close).thenAwaitNoScreen(OPEN);
    }

    /** The id of an entry's {@code number}th scene. */
    private static String scene(final String entry, final int number) {
        return "jsspace:" + entry + "#" + number;
    }

    /** How far the book first turns a scene, as its entry declares it. */
    private static float declaredYaw(final String id) {
        final GuideBook book = manual().book();
        return book.pages().stream().flatMap(page -> page.pieces().stream())
                .filter(piece -> piece instanceof GuidePiece.Scene scene && scene.id().equals(id))
                .map(piece -> ((GuidePiece.Scene) piece).scene().view().yaw()).findFirst().orElseThrow();
    }

    private static ManualScreen manual() {
        return (ManualScreen) Minecraft.getInstance().screen;
    }

    /* Shows the spread holding the scene of that id: a scene too tall for what is left of a page starts the next. */
    private static void toScene(final String id) {
        final GuideBook book = manual().book();
        for (int page = 0; page < book.pages().size(); page++) {
            if (book.pages().get(page).pieces().stream()
                    .anyMatch(piece -> piece instanceof GuidePiece.Scene scene && scene.id().equals(id))) {
                manual().goToPage(page);
                return;
            }
        }
    }

    private static void close() {
        Minecraft.getInstance().setScreen(null);
    }
}
