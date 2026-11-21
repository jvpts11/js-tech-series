/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.agent.AgentPlace;
import dev.jstech.core.agent.AgentPlaces;
import dev.jstech.space.JsSpace;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The places J's Space tells an agent of: in the Solar System's space, each world's low orbit over its middle on the
 * space's floor, the floor under the player and the descent under it; in a world, a formed shipyard round the player.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpacePlacesGameTests {

    private static final String EMPTY = "empty";
    private static final String SHIPYARD = "shipyard";
    /* The shipyard pattern's lowest corner in its arena, as its other tests stand it. */
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    /* How near a place is to be where it is told, in blocks. */
    private static final double NEAR = 0.01;

    private SpacePlacesGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void space_tellsEachWorldsLowOrbitTheFloorAndTheDescent(final GameTestHelper helper) {
        final ServerLevel space = SystemSpaces.spaceOf(helper.getLevel().getServer(), StarSystems.SOLAR_SYSTEM);
        final ServerPlayer pilot = FakePlayerFactory.get(space, new GameProfile(UUID.randomUUID(), "places_pilot"));
        pilot.setPos(100, SystemLayout.ORBIT_Y + 40, -200);
        final List<AgentPlace> places = spaceOf(AgentPlaces.of(space, pilot));
        for (final SolarBody body : SolarBody.values()) {
            final double[] middle = SystemLayout.centre(body);
            helper.assertTrue(places.stream().anyMatch(place -> place.kind().equals("low_orbit")
                    && place.name().equals(body.name().toLowerCase(Locale.ROOT)) && place.at().distanceTo(new Vec3(
                    middle[0], SystemLayout.ORBIT_Y, middle[2])) < NEAR), body + "'s low orbit is told, over its "
                    + "middle on the floor: " + places);
        }
        helper.assertTrue(places.stream().anyMatch(place -> place.kind().equals("space_floor")
                && place.at().distanceTo(new Vec3(100, SystemLayout.ORBIT_Y, -200)) < NEAR), "the floor under the "
                + "player: " + places);
        helper.assertTrue(places.stream().anyMatch(place -> place.kind().equals("descent")
                && place.at().y == SystemLayout.ORBIT_Y - SpaceClimbs.UNDER_THE_FLOOR), "and how deep under it a "
                + "ship goes down: " + places);
        helper.succeed();
    }

    @GameTest(template = SHIPYARD, timeoutTicks = 400)
    public static void world_tellsAFormedShipyardRoundThePlayer(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final ServerPlayer crew = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "places_crew"));
        crew.setPos(Vec3.atCenterOf(terminal));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(terminal)
                        instanceof ShipyardTerminalBlockEntity dock && dock.formed(), "the dock forms"))
                .thenExecute(() -> {
                    final List<AgentPlace> places = spaceOf(AgentPlaces.of(level, crew));
                    helper.assertTrue(places.stream().anyMatch(place -> place.kind().equals(SHIPYARD)
                            && place.at().distanceTo(Vec3.atCenterOf(terminal)) < NEAR), "the formed dock is told "
                            + "at its terminal: " + places);
                    helper.assertTrue(places.stream().noneMatch(place -> place.kind().equals("low_orbit")),
                            "and no low orbit, in a world");
                })
                .thenSucceed();
    }

    /* The places J's Space told. */
    private static List<AgentPlace> spaceOf(final List<AgentPlaces.Found> found) {
        return found.stream().filter(each -> each.modid().equals(JsSpace.MODID)).map(AgentPlaces.Found::place)
                .toList();
    }
}
