/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.multiblock.Projectables;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The terminal every multiblock of the series' mods shows at its place, in the recipe viewers' page and the projector's
 * ghost, is a block a player can place: it has an item, so the page lists it among the materials and draws it in the
 * shape.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MultiblockTerminalItemGameTests {

    private static final String ARENA = "empty";

    private MultiblockTerminalItemGameTests() {
    }

    @GameTest(template = ARENA)
    public static void terminal_everyShownMultiblockNamesATerminalAPlayerPlaces(final GameTestHelper helper) {
        final List<String> unplaceable = new ArrayList<>();
        int shown = 0;
        for (final ResourceLocation id : Projectables.all().keySet()) {
            // The test mod's own fixtures stand in tests only, never in a player's game.
            if (JsTests.MODID.equals(id.getNamespace())) {
                continue;
            }
            final Optional<String> terminal = Projectables.preview(id, 0).flatMap(ShapePreview::terminalBlock);
            if (terminal.isEmpty()) {
                continue;
            }
            shown++;
            final ResourceLocation block = ResourceLocation.parse(terminal.get());
            if (!BuiltInRegistries.BLOCK.containsKey(block)
                    || BuiltInRegistries.BLOCK.get(block).asItem() == Items.AIR) {
                unplaceable.add(id + " -> " + block);
            }
        }
        helper.assertTrue(shown > 0, "some multiblock names its terminal");
        helper.assertTrue(unplaceable.isEmpty(), unplaceable.size() + " multiblocks name a terminal no item places: "
                + unplaceable);
        helper.succeed();
    }
}
