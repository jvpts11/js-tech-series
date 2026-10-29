/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space;

import com.mojang.logging.LogUtils;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.datagen.JsSpaceDataGenerators;
import dev.jstech.space.exploration.SpaceMilestones;
import dev.jstech.space.exploration.SpaceProperties;
import dev.jstech.space.exploration.WorldDiscovery;
import dev.jstech.space.geology.LooseStones;
import dev.jstech.space.geology.SpaceOres;
import dev.jstech.space.geology.SpaceRocks;
import dev.jstech.space.guide.MissionHandbook;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.life.LunarSuit;
import dev.jstech.space.life.SpaceFlames;
import dev.jstech.space.life.SuitAir;
import dev.jstech.space.life.SuitControls;
import dev.jstech.space.life.SuitData;
import dev.jstech.space.life.SuitModuleItems;
import dev.jstech.space.life.SuitSupplies;
import dev.jstech.space.platform.LandingPlatform;
import dev.jstech.space.flight.FlightComputer;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.RocketDesigner;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketPartModels;
import dev.jstech.space.rocket.RocketShapes;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketKits;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.network.SpacePayloads;
import dev.jstech.space.universe.FarMaps;
import dev.jstech.space.universe.LandingCabins;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.SpaceRespawns;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.space.universe.WorldSizes;
import dev.jstech.space.worldgen.MarsBiomes;
import dev.jstech.space.worldgen.MarsWorld;
import dev.jstech.space.worldgen.MoonBiomes;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.space.wreck.RocketWreckage;
import dev.jstech.space.wreck.StageImpacts;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * The space mod of the J's Tech Series: rockets, satellites, the worlds of the Solar System and of other stars, and
 * the starships that reach them. It is built on the core alone, as every mod of the series is; what it does with
 * another mod of the series comes through what the core gives them both, or behind a guard.
 */
@Mod(JsSpace.MODID)
public final class JsSpace {

    public static final String MODID = "jsspace";

    public static final Logger LOGGER = LogUtils.getLogger();

    public JsSpace(final IEventBus modEventBus, final ModContainer modContainer) {
        LOGGER.info("J's Space {} loaded.", modContainer.getModInfo().getVersion());
        // The Mission Handbook's item and the style it is drawn in; the handbook itself is written in Markdown.
        MissionHandbook.declare();
        // The firsts of spaceflight a team reaches, which any mod reads and the advancements of the same firsts follow.
        SpaceMilestones.declare();
        // What can be known of a world, and what each team knows of each.
        SpaceProperties.declare();
        WorldDiscovery.register();
        // The star systems of the three galaxies, and the template every system's space is made from on its first
        // visit.
        StarSystems.declare();
        SystemSpaces.declare();
        // The worlds the skeleton holds: the Overworld, the Moon and Mars, each its generator, modifiers and data.
        SolarSystemWorlds.declare();
        // Each round world's size, fixed as it first loads, with its border at the far side and the poles.
        WorldSizes.listen();
        // Where those who die on another world come back: a bed with air, the cabin landed in, or home.
        LandingCabins.register();
        SpaceRespawns.declare();
        // What the server sends a player's game: the number the Overworld's sphere is drawn from, as they join.
        SpacePayloads.register(modEventBus);
        // The way up from a world to its space and back, with no loading screen, and the far map seen on the way.
        SpaceClimbs.listen();
        FarMaps.listen();
        // The ground of the Moon and of Mars: regoliths, sands, rocks and ices, the ores in their rocks, and the
        // stones lying loose on them.
        SpaceRocks.declare();
        SpaceOres.declare();
        LooseStones.declare();
        // The Moon's and Mars' ground, their biomes and their landmarks, as their spheres show them from orbit.
        MoonBiomes.declare();
        MoonWorld.declare();
        MarsBiomes.declare();
        MarsWorld.declare();
        // Life support: the lunar suit, the suits' air and modules, and each world's dangers and the sky's radiation
        // on it; the open flames that go out where the air holds too little oxygen.
        LunarSuit.declare(modEventBus);
        SuitData.declare();
        SuitModuleItems.declare();
        SuitSupplies.declare();
        SuitControls.declare();
        LifeSupport.listen();
        // The rockets' propellant and parts of the skeleton: methalox, and the parts in the diameters of 3 and 5
        // blocks.
        SkeletonPropellants.declare();
        SkeletonRocketParts.declare();
        // The blocks every part is built of, and where each goes in each part.
        RocketKitBlocks.declare();
        SkeletonRocketKits.declare();
        RocketPartModels.declare();
        // Their formed models' shapes, each in the stacks it stands in, its faces reaching into the parts beside it.
        RocketShapes.declare();
        // The Flight Computer every rocket carries: its transfer and landing pages, and its landing maps.
        FlightComputer.declare();
        // The Rocket Hangar a rocket is built in: its terminal, its crane, its doors and its platforms; and its design
        // screen, where a rocket is designed.
        HangarBlocks.declare();
        RocketDesigner.declare();
        // Where a spent stage goes: the landing platform it comes back to, or the ground it strikes, its crater and
        // its wreckage.
        LandingPlatform.declare();
        RocketWreckage.declare();
        StageImpacts.listen();
        SuitAir.listen();
        SpaceFlames.declare();
        // Everything it declares registers at once: blocks, items, block entities and the rest.
        SpaceContent.CONTENT.register(modEventBus);
        // The world's settings: its consequences, each on unless a server turns it off, and its rules of balance.
        SpaceServerConfig.register(modEventBus, modContainer);
        // The mod's data generation, which runs only in a data run.
        JsSpaceDataGenerators.listen(modEventBus);
    }
}
