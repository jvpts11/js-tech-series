/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.guide.ComputersGuide;
import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.guide.ManualScreen;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.core.guide.GuidePiece;
import net.minecraft.client.Minecraft;

/**
 * The scenes of the Guide to Operations, each drawn on its page as the game draws its blocks: the first computer
 * turned and brought nearer, and every other scene shown as the book opens it, so each can be seen and checked by
 * eye.
 */
public final class GuideScenesClientTests {

    private static final int SETTLE = 4;
    private static final int OPEN = 40;
    /** How far a drag turns the first computer, in pixels across and down. */
    private static final int DRAG_ACROSS = 40;
    private static final int DRAG_DOWN = 10;
    /** How many notches of the wheel bring it nearer. */
    private static final double NEARER = 2;

    private GuideScenesClientTests() {
    }

    @ClientTest(timeoutTicks = 300)
    public static void firstComputer_isDrawnTurnedAndBroughtNearer(final ClientTestContext ctx) {
        final String scene = scene("first_computer");
        shown(ctx, "first_computer", "scene_first_computer")
                .then(0, () -> {
                    final int[] at = manual().scenePoint(scene, false);
                    manual().mouseClicked(at[0], at[1], 0);
                    manual().mouseDragged(at[0] + DRAG_ACROSS, at[1] + DRAG_DOWN, 0, DRAG_ACROSS, DRAG_DOWN);
                    manual().mouseReleased(at[0] + DRAG_ACROSS, at[1] + DRAG_DOWN, 0);
                })
                .then(SETTLE, () -> {
                    final int[] at = manual().scenePoint(scene, false);
                    manual().mouseScrolled(at[0], at[1], 0, NEARER);
                })
                .thenAssert(SETTLE, () -> !manual().sceneView(scene).orElseThrow().equals(declared(scene)),
                        "the first computer turns and comes nearer")
                .thenScreenshot(2, "scene_first_computer_turned")
                .then(0, GuideScenesClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void mainframe_isDrawnWithItsPanelOff(final ClientTestContext ctx) {
        closed(shown(ctx, "mainframes", "scene_mainframe"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void caseFront_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "case_front", "scene_case_front"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void openCase_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "assembly_screen", "scene_open_case"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void serverRack_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "server_racks", "scene_server_rack"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void supercomputer_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "supercomputers", "scene_supercomputer"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void datacenter_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "datacenters", "scene_datacenter"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void clusterManagementComputer_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "cluster_management_computers", "scene_cluster_management_computer"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void joinedMonitors_areDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "monitors", "scene_monitors"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void peripheralsOnAHub_areDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "peripheral_cables", "scene_peripheral_cables"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void dockStation_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "dock_station", "scene_dock_station"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void redstoneInterface_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "redstone_interfaces", "scene_redstone_interface"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void firstNetwork_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "first_network", "scene_first_network"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void routers_areDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "routers", "scene_routers"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void failover_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "failover", "scene_failover"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void storage_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "storage_servers", "scene_storage"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void buses_areDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "buses", "scene_buses"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void autocraftingLine_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "how_autocrafting_works", "scene_autocrafting"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void patternEncoder_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "pattern_encoders", "scene_pattern_encoder"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void inputRouterAndReceivingBus_areDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "input_routers_and_receiving_buses", "scene_input_router"));
    }

    @ClientTest(timeoutTicks = 300)
    public static void networkGateway_isDrawn(final ClientTestContext ctx) {
        closed(shown(ctx, "network_gateway", "scene_network_gateway"));
    }

    /* Opens the Guide at the entry, shows the spread holding its scene and photographs it as the book shows it. */
    private static ClientTestContext shown(final ClientTestContext ctx, final String entry, final String label) {
        final String scene = scene(entry);
        return ctx.then(0, () -> GuideClient.open(ComputersGuide.GUIDE_TO_OPERATIONS, "jsc:" + entry))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> toScene(scene))
                .thenAssert(SETTLE, () -> manual().scenePoint(scene, false) != null,
                        "the scene of " + entry + " is drawn on its page")
                .thenScreenshot(2, label);
    }

    private static void closed(final ClientTestContext ctx) {
        ctx.then(0, GuideScenesClientTests::close).thenAwaitNoScreen(OPEN);
    }

    /** The id of an entry's first scene. */
    private static String scene(final String entry) {
        return "jsc:" + entry + "#1";
    }

    /** How the book first shows a scene, as its entry declares it. */
    private static GuideBlock.SceneView declared(final String id) {
        final GuideBook book = manual().book();
        return book.pages().stream().flatMap(page -> page.pieces().stream())
                .filter(piece -> piece instanceof GuidePiece.Scene scene && scene.id().equals(id))
                .map(piece -> ((GuidePiece.Scene) piece).scene().view()).findFirst().orElseThrow();
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
