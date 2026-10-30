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
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.api.ShipPlace;
import dev.jstech.space.ship.ShipChecks;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The starship's parts as a data pack's files write them, and a design of them checked: a part read from its file is
 * the part; a ship of a cockpit, an airlock, a hold, a reactor, a tail engine and wheels flies; one whose part has an
 * id no part has is told unknown; and the design is saved and read back whole. Then the small ship's catalogue: its 25
 * parts in ten kinds, each named, and the example ships built of them.
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

    @GameTest(template = ARENA)
    public static void catalogue_twentyFivePartsNamedAndItsExampleShipsFly(final GameTestHelper helper) {
        final Map<ResourceLocation, ShipPart> parts = ShipParts.declared();
        final Map<ShipPartKind, Integer> kinds = new EnumMap<>(ShipPartKind.class);
        int small = 0;
        for (final ShipPart part : parts.values()) {
            if (part.shipClass() == ShipClass.SMALL) {
                small++;
                kinds.merge(part.kind(), 1, Integer::sum);
                helper.assertTrue(Language.getInstance().has(part.name()), "the part is named: " + part.name());
            }
        }
        helper.assertTrue(small == 25, "the small ship has 25 parts: " + small);
        helper.assertTrue(kinds.size() == 10 && !kinds.containsKey(ShipPartKind.MODULE),
                "in ten kinds, the functional modules apart: " + kinds);
        final ShipDesign orbiter = new ShipDesign("ORBITER", catalogue("orbiter_cockpit", "airlock", "bay",
                "reactor_flush"), catalogue("twin_bells", "side_intakes", "blended_delta", "twin_canted_fins",
                "blisters", "wheels"));
        final ShipDesign hauler = new ShipDesign("HAULER", catalogue("cab_cockpit", "airlock", "reactor_fins",
                "ramp"), catalogue("magnetic_nozzle", "nacelle_intake", "stub_wings", "quads", "legs"));
        final ShipDesign courier = new ShipDesign("COURIER", catalogue("greenhouse_cockpit", "docking",
                "side_door", "reactor_fins"), catalogue("cluster", "side_intakes", "swept_wings", "single_fin",
                "quads", "wheels"));
        for (final ShipDesign ship : List.of(orbiter, hauler, courier)) {
            helper.assertTrue(ship.problems(parts).isEmpty(), ship.name() + " flies: " + ship.problems(parts));
        }
        final ShipDesign surveyor = new ShipDesign("SURVEYOR", catalogue("orbiter_cockpit", "corridor",
                "reactor_flush", "ramp"), catalogue("magnetic_nozzle", "nacelle_intake", "stub_wings",
                "twin_canted_fins", "blisters", "legs"));
        helper.assertTrue(surveyor.problems(parts).equals(Set.of(ShipChecks.Problem.NO_AIRLOCK)),
                "a ship with no airlock nor docking module is told so: " + surveyor.problems(parts));
        final ShipDesign full = new ShipDesign("FULL", catalogue("orbiter_cockpit", "airlock", "bay",
                "side_door", "reactor_flush"), catalogue("twin_bells", "wheels"));
        helper.assertTrue(full.problems(parts).contains(ShipChecks.Problem.TOO_MUCH),
                "a small ship takes one hold: " + full.problems(parts));
        helper.succeed();
    }

    /* The catalogue's parts of {@code paths}, by id. */
    private static List<ResourceLocation> catalogue(final String... paths) {
        final List<ResourceLocation> ids = new ArrayList<>();
        for (final String path : paths) {
            ids.add(SmallShipParts.id("small_" + path));
        }
        return ids;
    }

    private static ShipPart part(final ShipPartKind kind, final ShipPlace place, final boolean airlock) {
        return new ShipPart("jstests.ship." + kind.serializedName(), kind, ShipClass.SMALL, place, 0, 0, airlock,
                false);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
