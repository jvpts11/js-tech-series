/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.jstech.space.api.ShipClass;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.api.ShipPlace;
import dev.jstech.space.ship.ShipChecks;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The starship's parts as a data pack's files write them, and a design of them checked: a part read from its file is
 * the part; a ship of a cockpit, an airlock, a hold, a reactor, a tail engine and wheels flies; one whose part has an
 * id no part has is told unknown; and the design is saved and read back whole.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipPartsGameTests {

    private static final String ARENA = "empty";

    private ShipPartsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void part_readFromItsFileAndADesignOfThemFlies(final GameTestHelper helper) {
        final ShipPart hold = ShipPart.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"name\":"
                + "\"jstests.ship.bay\",\"kind\":\"hold\",\"class\":\"small\",\"place\":\"spine\",\"length\":4.8,"
                + "\"room\":3}")).getOrThrow();
        helper.assertTrue(hold.equals(new ShipPart("jstests.ship.bay", ShipPartKind.HOLD, ShipClass.SMALL,
                ShipPlace.SPINE, 4.8, 3, false, false)), "a part read from its file is the part: " + hold);
        final Map<ResourceLocation, ShipPart> parts = Map.of(
                id("cockpit"), part(ShipPartKind.COCKPIT, ShipPlace.NOSE, false),
                id("airlock"), part(ShipPartKind.HULL, ShipPlace.SPINE, true),
                id("bay"), hold,
                id("reactor"), part(ShipPartKind.REACTOR, ShipPlace.SPINE, false),
                id("bells"), part(ShipPartKind.MAIN_ENGINE, ShipPlace.SPINE_END, false),
                id("wheels"), part(ShipPartKind.GEAR, ShipPlace.UNDER, false));
        final ShipDesign design = new ShipDesign("TEST", List.of(id("cockpit"), id("airlock"), id("bay"),
                id("reactor")), List.of(id("bells"), id("wheels")));
        helper.assertTrue(design.problems(parts).isEmpty(), "the ship flies: " + design.problems(parts));
        final ShipDesign unknown = new ShipDesign("TEST", List.of(id("cockpit"), id("nothing")), List.of());
        helper.assertTrue(unknown.problems(parts).equals(Set.of(ShipChecks.Problem.UNKNOWN_PART)),
                "a part no part is is unknown");
        final ShipDesign back = ShipDesign.CODEC.parse(JsonOps.INSTANCE, ShipDesign.CODEC.encodeStart(
                JsonOps.INSTANCE, design).getOrThrow()).getOrThrow();
        helper.assertTrue(back.equals(design), "the design is saved and read back whole");
        helper.succeed();
    }

    private static ShipPart part(final ShipPartKind kind, final ShipPlace place, final boolean airlock) {
        return new ShipPart("jstests.ship." + kind.serializedName(), kind, ShipClass.SMALL, place, 0, 0, airlock,
                false);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
