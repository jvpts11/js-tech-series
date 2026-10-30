/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.space.api.ShipFinish;
import dev.jstech.space.api.ShipFinishes;
import dev.jstech.space.api.ShipKit;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.ship.ShipPartModels;
import dev.jstech.space.ship.SmallShipFinishes;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The starship parts' formed models: the three finishes declared and named; every piece of every part's kit a model in
 * each of them; and the Orbiter drawn with a model a piece, each where its anchor stands along the spine.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipPartModelGameTests {

    private static final String ARENA = "empty";

    private ShipPartModelGameTests() {
    }

    @GameTest(template = ARENA)
    public static void everyPiece_hasItsModelInEachFinishAndTheOrbiterIsPlaced(final GameTestHelper helper) {
        final Map<ResourceLocation, ShipFinish> finishes = ShipFinishes.all(helper.getLevel());
        helper.assertTrue(finishes.keySet().containsAll(ShipPartModels.FINISHES.stream().map(SmallShipFinishes::id)
                .toList()), "the three finishes: " + finishes.keySet());
        finishes.values().forEach(finish -> helper.assertTrue(Language.getInstance().has(finish.name()),
                "the finish is named: " + finish.name()));
        final Map<ResourceLocation, ShipKit> kits = ShipKits.all(helper.getLevel());
        int pieces = 0;
        for (final Map.Entry<ResourceLocation, ShipKit> kit : kits.entrySet()) {
            if (!kit.getKey().getPath().startsWith("small_")) {
                continue;
            }
            pieces += kit.getValue().pieces().size();
            for (final String finish : ShipPartModels.FINISHES) {
                final List<ShipPartModels.Placed> placed = ShipPartModels.of(ShipParts.all(helper.getLevel()), kits,
                        List.of(), List.of(kit.getKey()), SmallShipFinishes.id(finish));
                helper.assertTrue(placed.size() == kit.getValue().pieces().size(), kit.getKey() + " has a model a "
                        + "piece in " + finish);
            }
        }
        helper.assertTrue(pieces == 29, "the catalogue's kits have 29 pieces: " + pieces);
        final List<ShipPartModels.Placed> orbiter = ShipPartModels.of(ShipParts.all(helper.getLevel()), kits,
                List.of(part("orbiter_cockpit"), part("airlock"), part("bay"), part("reactor_flush")),
                List.of(part("twin_bells"), part("side_intakes"), part("blended_delta"), part("twin_canted_fins"),
                        part("blisters"), part("wheels")), SmallShipFinishes.id("enamel"));
        helper.assertTrue(orbiter.size() == 12, "the Orbiter: twelve models: " + orbiter.size());
        helper.assertTrue(orbiter.get(2).z() == 8 && orbiter.get(4).z() == 15
                        && orbiter.get(4).model().getPath().equals("ship/enamel/small_twin_bells"),
                "its hold at 8 blocks from the nose, its tail engine at the spine's end, in enamel: " + orbiter);
        helper.succeed();
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }
}
