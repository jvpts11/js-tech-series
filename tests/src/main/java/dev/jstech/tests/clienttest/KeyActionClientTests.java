/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.input.KeyAction;
import dev.jstech.core.input.KeyActions;
import dev.jstech.core.item.ItemStates;
import dev.jstech.tests.TestItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The declared key actions on a player's game: each is one of the game's keys, and a press of one the server has a
 * part in reaches the server, which changes the mode of the item in the player's hand.
 */
public final class KeyActionClientTests {

    private KeyActionClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void keyAction_pressReachesTheServer(final ClientTestContext ctx) {
        final InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_9);
        final KeyMapping mapping = KeyActionsClient.mapping(CoreKeys.CHANGE_ITEM_MODE);
        // Undone also when a wait below times out: the binding lives in the running client, the tool in the hand.
        ctx.afterTest(() -> {
            mapping.setKey(InputConstants.UNKNOWN);
            KeyMapping.resetMapping();
            ctx.server().submit(() -> holder(ctx).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
        });
        ctx.thenAssert(0, () -> {
            final List<KeyMapping> keys = List.of(ctx.mc().options.keyMappings);
            for (final KeyAction action : KeyActions.all()) {
                if (!keys.contains(KeyActionsClient.mapping(action))) {
                    return false;
                }
            }
            return true;
        }, "every declared action is one of the game's keys")
                .thenServer(0, level -> holder(ctx).setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(TestItems.MODES_ONLY.get())))
                .thenWaitUntil(() -> ctx.player().getMainHandItem().is(TestItems.MODES_ONLY.get()), 40,
                        "the player's game to see the tool in hand")
                .then(0, () -> {
                    mapping.setKey(key);
                    KeyMapping.resetMapping();
                    KeyMapping.click(key);
                })
                .thenWaitUntilServer(level -> ItemStates.mode(holder(ctx).getMainHandItem())
                                .equals(Optional.of(TestItems.MARK)), 40,
                        "the press to change the mode on the server",
                        level -> "the tool is in " + ItemStates.mode(holder(ctx).getMainHandItem()));
    }

    private static ServerPlayer holder(final ClientTestContext ctx) {
        return ctx.serverLevel().getServer().getPlayerList().getPlayers().getFirst();
    }
}
