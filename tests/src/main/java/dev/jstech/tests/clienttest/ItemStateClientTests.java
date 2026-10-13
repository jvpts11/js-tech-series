/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.tests.TestItems;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Items with state on a player's game: the tooltip of an item that holds everything says its mode, its energy, its
 * fluid and its stacks, and how to change the mode; the mode key is one of the game's keys.
 */
public final class ItemStateClientTests {

    private ItemStateClientTests() {
    }

    @ClientTest(timeoutTicks = 100)
    public static void tooltip_saysWhatTheItemHoldsAndHowToChangeItsMode(final ClientTestContext ctx) {
        ctx.thenAssert(0, () -> {
            final List<String> lines = tooltip(ctx, new ItemStack(TestItems.FULL.get()));
            // The amounts of energy are grouped the way the machine's own language groups them.
            return lines.contains("Mode: Scan") && lines.stream().anyMatch(line -> line.startsWith("Energy: 0 FE of "))
                    && lines.contains("No fluid, holds 4000 mB") && lines.contains("Holds 0 of 4 stacks")
                    && lines.contains("Give Change Item Mode a key to change the mode");
        }, "the full tool's tooltip says its mode, energy, fluid, stacks and how to change the mode")
                .thenAssert(0, () -> List.of(ctx.mc().options.keyMappings)
                        .contains(KeyActionsClient.mapping(CoreKeys.CHANGE_ITEM_MODE)),
                        "the mode key is among the game's keys");
    }

    private static List<String> tooltip(final ClientTestContext ctx, final ItemStack stack) {
        if (ctx.mc().level == null) {
            return List.of();
        }
        return stack.getTooltipLines(Item.TooltipContext.of(ctx.mc().level), ctx.player(), TooltipFlag.NORMAL)
                .stream().map(Component::getString).toList();
    }
}
