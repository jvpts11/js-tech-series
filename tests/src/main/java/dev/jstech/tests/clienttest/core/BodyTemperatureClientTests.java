/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.gui.data.DataScreen;
import dev.jstech.core.client.gui.data.TextView;
import dev.jstech.core.client.hazard.BodyTemperatureClient;
import dev.jstech.core.client.hazard.HazardsClient;
import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.hazard.BodyHeat;
import dev.jstech.core.hazard.BodyTemperature;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.jstechseries.hazard.client.BodyScreen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

/**
 * A body's warmth on a player's game, to look at: the HUD's element in its corner with the core cold, in mild
 * hypothermia and the screen's edge frosted, then hot, in heat exhaustion and the edge hazed, and gone once the core is
 * normal again; and the body's screen, declared in its file, with the cold core among its bands and what is worn.
 */
public final class BodyTemperatureClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final int WAIT = 200;
    private static final int SETTLE = 10;
    private static final double COLD = 34.6 + BodyHeat.FREEZING;
    private static final double HOT = 39.2 + BodyHeat.FREEZING;
    private static final double USED_TO_COLD = -0.5;
    private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET};
    private static final Item[] LEATHER = {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS,
            Items.LEATHER_BOOTS};

    private BodyTemperatureClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void hud_showsTheCoreOutOfItsBandColdAndHot(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final ServerPlayer player = ctx.serverPlayer();
            BodyTemperature.setCore(player, BodyHeat.NORMAL);
            player.setGameMode(GameType.CREATIVE);
        }));
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .thenServer(0, level -> {
                    ctx.serverPlayer().setGameMode(GameType.SURVIVAL);
                    core(ctx, COLD);
                })
                .thenWaitUntil(() -> HazardsClient.level(BodyTemperature.HYPOTHERMIA.id()) >= BodyTemperature.MILD
                        && shown(), WAIT, "the cold core on the HUD")
                .thenScreenshot(SETTLE, "body-temperature-cold")
                .thenServer(0, level -> core(ctx, HOT))
                .thenWaitUntil(() -> HazardsClient.level(BodyTemperature.OVERHEATING.id())
                        >= BodyTemperature.EXHAUSTION && BodyTemperatureClient.now().coreKelvin() > BodyHeat.NORMAL,
                        WAIT, "the hot core on the HUD")
                .thenScreenshot(SETTLE, "body-temperature-hot")
                .thenServer(0, level -> core(ctx, BodyHeat.NORMAL))
                .thenWaitUntil(() -> !shown(), WAIT, "the element gone once the core is normal");
    }

    @ClientTest(timeoutTicks = 1200)
    public static void bodyScreen_showsTheColdCoreAndWhatIsWorn(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final ServerPlayer player = ctx.serverPlayer();
            BodyTemperature.setCore(player, BodyHeat.NORMAL);
            BodyTemperature.setAcclimation(player, 0);
            for (final EquipmentSlot slot : WORN) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
            player.setGameMode(GameType.CREATIVE);
        }));
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .thenServer(0, level -> {
                    final ServerPlayer player = ctx.serverPlayer();
                    player.setGameMode(GameType.SURVIVAL);
                    for (int k = 0; k < WORN.length; k++) {
                        player.setItemSlot(WORN[k], new ItemStack(LEATHER[k]));
                    }
                    BodyTemperature.setAcclimation(player, USED_TO_COLD);
                    core(ctx, COLD);
                })
                .thenWaitUntil(() -> BodyTemperatureClient.now().coreKelvin() < BodyHeat.NORMAL - 1
                        && BodyTemperatureClient.now().acclimation() < USED_TO_COLD / 2
                        && Float.isFinite(BodyTemperatureClient.now().letsOut()), WAIT,
                        "the cold core, what the body is used to and what it wears told to the player's game")
                .then(0, BodyScreen::open)
                .thenAwaitScreen(DataScreen.class, WAIT)
                .thenAssert(SETTLE, () -> ctx.screen(DataScreen.class).file().equals(BodyScreen.FILE)
                        && said(ctx, "worn_name").contains("Leather") && said(ctx, "core_line").contains("34.")
                        && said(ctx, "wind").equals("kept out") && shown(ctx, "used_line") && !shown(ctx, "wash")
                        && ctx.screen(DataScreen.class).view().guiLayout().isClean(),
                        "the screen names what is worn, the cold core in the game's language and the wind kept out, "
                                + "says the body is used to the cold, asks for no shower, and nothing overlaps")
                .thenScreenshot(SETTLE, "body-screen-cold")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT);
    }

    /* What the text the body's screen's file named {@code id} says now. */
    private static String said(final ClientTestContext ctx, final String id) {
        return ctx.screen(DataScreen.class).view().find(id).map(node -> node.component() instanceof TextView text
                ? text.text() : "").orElse("");
    }

    /* Whether the body's screen shows the component its file named {@code id}. */
    private static boolean shown(final ClientTestContext ctx, final String id) {
        return ctx.screen(DataScreen.class).view().find(id).map(node -> node.component() != null
                && node.component().visible()).orElse(false);
    }

    /* Sets the player's core to {@code kelvin} and weighs their hazards at once, so they are told. */
    private static void core(final ClientTestContext ctx, final double kelvin) {
        BodyTemperature.setCore(ctx.serverPlayer(), kelvin);
        Hazards.second(ctx.serverPlayer(), ctx.serverLevel());
    }

    private static boolean shown() {
        return HudElements.placedIn(HudStack.Corner.BOTTOM_LEFT).containsKey(BodyTemperatureClient.HUD);
    }
}
