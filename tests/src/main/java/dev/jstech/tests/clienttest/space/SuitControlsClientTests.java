/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.gui.HudSnap;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import dev.jstech.space.client.life.SuitControlsArt;
import dev.jstech.space.client.life.SuitControlsHud;
import dev.jstech.space.client.life.SuitControlsScreen;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.life.LunarSuit;
import dev.jstech.space.life.SuitAir;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * The suit's controls on a player's game, as the approved device: worn, the lunar suit's controls show in the
 * dosimeter's corner, the white case with its black face; holed, the PSI lamp lights red and the screen's rim reddens;
 * with little air left, the O2 lamp lights amber; on their key, they are held up twice the size, and their light's
 * button lights the face.
 */
public final class SuitControlsClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    /* Places on the case at a unit of 1: its white edge, its black face, the O2 and PSI lamps left of their words. */
    private static final int[] CASE_EDGE = {2, 40};
    private static final int[] FACE = {100, 20};
    private static final int[] O2_LAMP = {56, 54};
    private static final int[] PSI_LAMP = {74, 54};
    /* Seconds of air low enough to warn. */
    private static final int LOW_AIR = 60;
    private static final int SETTLE = 6;
    private static final int CHANNEL = 0xFF;
    private static final int WHITE = 180;
    private static final int BLACK = 40;
    private static final int LIT = 150;

    private SuitControlsClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void controls_showTheSuitsStateAndAreHeldUpClose(final ClientTestContext ctx) {
        final int[] rimBefore = new int[3];
        final int[] faceDark = new int[3];
        ctx.afterTest(() -> {
            ctx.mc().execute(() -> ctx.mc().setScreen(null));
            ctx.server().execute(() -> {
                for (final EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                        EquipmentSlot.FEET)) {
                    ctx.serverPlayer().setItemSlot(slot, ItemStack.EMPTY);
                }
            });
        });
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .then(SETTLE, () -> copy(pixel(ctx, 2, ctx.mc().getWindow().getGuiScaledHeight() / 2), rimBefore))
                .thenServer(0, level -> {
                    final ServerPlayer player = ctx.serverPlayer();
                    player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(LunarSuit.HELMET.get()));
                    player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(LunarSuit.SUIT.get()));
                    player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(LunarSuit.TROUSERS.get()));
                    player.setItemSlot(EquipmentSlot.FEET, new ItemStack(LunarSuit.BOOTS.get()));
                })
                .thenWaitUntil(() -> SuitControlsHud.reading(ctx.player()).isPresent(), 100,
                        "the suit to reach the player's game")
                .then(SETTLE, () -> {
                    final int[] edge = onCase(ctx, CASE_EDGE);
                    final int[] face = onCase(ctx, FACE);
                    final int[] psi = onCase(ctx, PSI_LAMP);
                    ctx.assertTrue(min(edge) > WHITE, "the controls' case is the suit's white: " + str(edge));
                    ctx.assertTrue(max(face) < BLACK, "their face is black: " + str(face));
                    ctx.assertTrue(psi[0] < LIT, "sealed, the PSI lamp is dark: " + str(psi));
                })
                .thenScreenshot(0, "suit-controls-sealed")
                .thenServer(0, level -> SuitAir.hole(ctx.serverPlayer().getItemBySlot(EquipmentSlot.CHEST)))
                .thenWaitUntil(() -> SuitControlsHud.reading(ctx.player()).map(r -> r.state()
                        == SuitControlsHud.State.HOLED).orElse(false), 100, "the hole to reach the player's game")
                .then(SETTLE, () -> {
                    final int[] psi = onCase(ctx, PSI_LAMP);
                    final int[] rim = pixel(ctx, 2, ctx.mc().getWindow().getGuiScaledHeight() / 2);
                    ctx.assertTrue(psi[0] > LIT && psi[0] > psi[2] * 2, "holed, the PSI lamp lights red: "
                            + str(psi));
                    ctx.assertTrue(rim[0] - rim[2] > rimBefore[0] - rimBefore[2] + 10,
                            "and the screen's rim reddens: " + str(rim) + " from " + str(rimBefore));
                })
                .thenScreenshot(0, "suit-controls-holed")
                .thenServer(0, level -> {
                    final ItemStack chest = ctx.serverPlayer().getItemBySlot(EquipmentSlot.CHEST);
                    SuitAir.patch(chest);
                    final SpaceSuit suit = SpaceSuits.declared().get(LunarSuit.ID);
                    LifeSupport.setAir(level, chest, suit, LOW_AIR);
                })
                .thenWaitUntil(() -> SuitControlsHud.reading(ctx.player()).map(r -> r.state()
                        == SuitControlsHud.State.LOW).orElse(false), 100, "the low air to reach the player's game")
                .then(SETTLE, () -> {
                    final int[] o2 = onCase(ctx, O2_LAMP);
                    ctx.assertTrue(o2[0] > LIT && o2[1] > LIT / 2 && o2[2] < o2[1],
                            "with little air left, the O2 lamp lights amber: " + str(o2));
                })
                .thenScreenshot(0, "suit-controls-low")
                .then(0, () -> ctx.mc().setScreen(new SuitControlsScreen()))
                .thenAwaitScreen(SuitControlsScreen.class, 40)
                .then(SETTLE, () -> {
                    final SuitControlsScreen screen = ctx.screen(SuitControlsScreen.class);
                    copy(pixel(ctx, screen.left() + FACE[0] * SuitControlsScreen.UNIT,
                            screen.top() + FACE[1] * SuitControlsScreen.UNIT), faceDark);
                    ctx.click(screen.left() + (SuitControlsArt.LIGHT_X + 2) * SuitControlsScreen.UNIT,
                            screen.top() + (SuitControlsArt.LIGHT_Y + 2) * SuitControlsScreen.UNIT);
                })
                .then(SETTLE, () -> {
                    final SuitControlsScreen screen = ctx.screen(SuitControlsScreen.class);
                    final int[] face = pixel(ctx, screen.left() + FACE[0] * SuitControlsScreen.UNIT,
                            screen.top() + FACE[1] * SuitControlsScreen.UNIT);
                    ctx.assertTrue(face[1] > faceDark[1] + 10, "held up, the light's button lights the face: "
                            + str(face) + " from " + str(faceDark));
                })
                .thenScreenshot(0, "suit-controls-up-close");
    }

    /* The colour of the place {@code at} of the controls' case as the HUD shows it, at a unit of 1. */
    private static int[] onCase(final ClientTestContext ctx, final int[] at) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final HudSnap.Box box = HudElements.declaredBoxes(ctx.mc(), width, height, Set.of(SuitControlsHud.HUD))
                .get(SuitControlsHud.HUD);
        return pixel(ctx, box.x() + at[0], box.y() + at[1]);
    }

    private static int[] pixel(final ClientTestContext ctx, final int x, final int y) {
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            final int argb = frame.pixel(x, y);
            return new int[] {argb >> 16 & CHANNEL, argb >> 8 & CHANNEL, argb & CHANNEL};
        }
    }

    private static void copy(final int[] from, final int[] to) {
        System.arraycopy(from, 0, to, 0, to.length);
    }

    private static int min(final int[] colour) {
        return Math.min(colour[0], Math.min(colour[1], colour[2]));
    }

    private static int max(final int[] colour) {
        return Math.max(colour[0], Math.max(colour[1], colour[2]));
    }

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }
}
