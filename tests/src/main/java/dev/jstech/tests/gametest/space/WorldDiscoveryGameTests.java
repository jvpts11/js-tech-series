/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MineralFamilies;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.space.api.WorldProperties;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.exploration.SpaceMilestones;
import dev.jstech.space.exploration.SpaceProperties;
import dev.jstech.space.exploration.WorldDiscovery;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.tests.JsTests;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a team knows of each world, as J's Space keeps it: home known whole and the Moon's and Mars' places known from
 * its sky; each property's share only growing; a crew landing only once what kills there is known, unless the server
 * says otherwise; a world known whole reaching the milestone. And the minerals of the Moon and Mars in the series'
 * catalogue.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WorldDiscoveryGameTests {

    private static final String ARENA = "empty";
    private static final double HALF = 0.5;

    private WorldDiscoveryGameTests() {
    }

    @GameTest(template = ARENA)
    public static void discovery_beginsWithHomeAndWhatItsSkyShows(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final String team = newTeam();
        helper.assertTrue(WorldDiscovery.knownShare(server, team, SolarSystemWorlds.OVERWORLD) == 1,
                "home is known whole");
        helper.assertTrue(WorldDiscovery.canSendProbe(server, team, SolarSystemWorlds.MOON)
                && WorldDiscovery.canSendProbe(server, team, SolarSystemWorlds.MARS),
                "the Moon's and Mars' places are known from the Overworld's sky, so probes can go");
        helper.assertTrue(WorldDiscovery.known(server, team, SolarSystemWorlds.MOON, SpaceProperties.ATMOSPHERE) == 0,
                "but nothing else of them");
        helper.assertFalse(WorldDiscovery.canLandCrew(server, team, SolarSystemWorlds.MOON),
                "so no crew lands there yet");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void learn_onlyGrowsAndOpensTheLandingDoor(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final String team = newTeam();
        final ResourceLocation moon = SolarSystemWorlds.MOON;
        helper.assertTrue(WorldDiscovery.learn(server, team, moon, SpaceProperties.ATMOSPHERE, HALF),
                "a team comes to know half of the atmosphere");
        helper.assertFalse(WorldDiscovery.learn(server, team, moon, SpaceProperties.ATMOSPHERE, HALF / 2),
                "and knowing less of it changes nothing");
        helper.assertTrue(WorldDiscovery.known(server, team, moon, SpaceProperties.ATMOSPHERE) == HALF,
                "it still knows half");
        for (final ResourceLocation essential : new ResourceLocation[] {SpaceProperties.ATMOSPHERE,
                SpaceProperties.TEMPERATURE, SpaceProperties.RADIATION, SpaceProperties.GRAVITY,
                SpaceProperties.RELIEF}) {
            WorldDiscovery.learn(server, team, moon, essential, 1);
        }
        helper.assertTrue(WorldDiscovery.canLandCrew(server, team, moon), "with what kills known, a crew lands");
        helper.assertTrue(WorldDiscovery.knownShare(server, team, moon) < 1, "though the Moon is not known whole");
        final boolean rule = SpaceServerConfig.FILE.get(SpaceServerConfig.LANDING_NEEDS_THE_ESSENTIALS);
        try {
            SpaceServerConfig.FILE.set(SpaceServerConfig.LANDING_NEEDS_THE_ESSENTIALS, false);
            helper.assertTrue(WorldDiscovery.canLandCrew(server, team, SolarSystemWorlds.MARS),
                    "a server that lets crews land blind lets them land on Mars knowing nothing");
        } finally {
            SpaceServerConfig.FILE.set(SpaceServerConfig.LANDING_NEEDS_THE_ESSENTIALS, rule);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void worldKnownWhole_reachesTheMilestone(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final String team = newTeam();
        helper.assertFalse(TeamProgress.reached(server, team, SpaceMilestones.WORLD_KNOWN_WHOLE),
                "a new team has known no world whole");
        for (final ResourceLocation property : WorldProperties.all().keySet()) {
            WorldDiscovery.learn(server, team, SolarSystemWorlds.MARS, property, 1);
        }
        helper.assertTrue(WorldDiscovery.knownShare(server, team, SolarSystemWorlds.MARS) == 1, "Mars is known whole");
        helper.assertTrue(TeamProgress.reached(server, team, SpaceMilestones.WORLD_KNOWN_WHOLE),
                "and the team reached the milestone of the first world known whole");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void minerals_ofTheMoonAndMarsAreInTheSeriesCatalogue(final GameTestHelper helper) {
        for (final Material mineral : SeriesMinerals.ofTheMoonAndMars()) {
            helper.assertTrue(mineral.mineral().isPresent() && mineral.id().getNamespace().equals("jscore"),
                    mineral.id() + " is a mineral of the series, shared by every mod");
        }
        helper.assertTrue(SeriesMinerals.ILMENITE.mineral().filter(of -> of.main().material()
                .equals(Elements.of(Element.TITANIUM)) && of.family() == MineralFamilies.OXIDE).isPresent(),
                "ilmenite is titanium's oxide");
        helper.assertFalse(SeriesMinerals.all().contains(SeriesMinerals.OLIVINE),
                "and none of them joins the first industry's veins");
        helper.succeed();
    }

    /* A team of its own, so each test starts knowing only what every team knows. */
    private static String newTeam() {
        return CoreTeams.solo(UUID.randomUUID());
    }
}
