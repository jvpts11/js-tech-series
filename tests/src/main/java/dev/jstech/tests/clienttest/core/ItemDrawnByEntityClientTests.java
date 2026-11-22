/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Every item of the series drawn by an entity's renderer has one of its own: the game's own draws only its own
 * chests, shields and the like, and leaves anything else empty, in a slot, in a hand and in the recipe viewers, as
 * the CDU's item was.
 */
public final class ItemDrawnByEntityClientTests {

    /* The namespaces of the series' mods. */
    private static final Set<String> SERIES = Set.of("jscore", "jsc", "jsindustrial", "jsspace", "jstests");

    private ItemDrawnByEntityClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void items_drawnByAnEntityHaveARendererOfTheirOwn(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final List<String> empty = new ArrayList<>();
            for (final Item item : BuiltInRegistries.ITEM) {
                if (!SERIES.contains(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                    continue;
                }
                final ItemStack stack = new ItemStack(item);
                if (ctx.mc().getItemRenderer().getModel(stack, null, null, 0).isCustomRenderer()
                        && IClientItemExtensions.of(stack).getCustomRenderer()
                        == ctx.mc().getItemRenderer().getBlockEntityRenderer()) {
                    empty.add(BuiltInRegistries.ITEM.getKey(item).toString());
                }
            }
            ctx.assertTrue(empty.isEmpty(), "drawn by an entity with none of their own, so drawn empty: " + empty);
        });
    }
}
