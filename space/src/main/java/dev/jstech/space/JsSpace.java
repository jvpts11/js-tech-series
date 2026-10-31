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
import dev.jstech.space.exploration.WorldCoverage;
import dev.jstech.space.exploration.WorldDiscovery;
import dev.jstech.space.geology.LooseStones;
import dev.jstech.space.geology.SpaceOres;
import dev.jstech.space.geology.SpaceRocks;
import dev.jstech.space.guide.MissionHandbook;
import dev.jstech.space.landing.ShipLandingBlocks;
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
import dev.jstech.space.crawler.CrawlerBlocks;
import dev.jstech.space.cruise.CruiseTexts;
import dev.jstech.space.cruise.RocketCruise;
import dev.jstech.space.cruise.SolarStorms;
import dev.jstech.space.descent.DescentTexts;
import dev.jstech.space.descent.RocketDescent;
import dev.jstech.space.telemetry.TelemetryLine;
import dev.jstech.space.depot.DepotBlocks;
import dev.jstech.space.launch.LaunchPanel;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.pad.PadBlocks;
import dev.jstech.space.hangar.RocketDesigner;
import dev.jstech.space.rocket.RocketAscent;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketPartModels;
import dev.jstech.space.rocket.RocketShapes;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketKits;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.StageFlights;
import dev.jstech.space.rocket.StageLanding;
import dev.jstech.space.network.SpacePayloads;
import dev.jstech.space.ship.ShipAutopilotControl;
import dev.jstech.space.ship.ShipBoost;
import dev.jstech.space.ship.ShipDocks;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipPartModels;
import dev.jstech.space.ship.ShipShapes;
import dev.jstech.space.ship.SmallShipFinishes;
import dev.jstech.space.ship.SmallShipKits;
import dev.jstech.space.ship.SmallShipMakers;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipDesigner;
import dev.jstech.space.shipyard.ShipyardBlocks;
import dev.jstech.space.shipyard.ShipyardStructure;
import dev.jstech.space.spacecraft.SkeletonSpacecraft;
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
        WorldCoverage.register();
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
        // The Rocket Hangar a rocket is built in: its terminal, its crane, its doors, its platforms and its item ports;
        // and its design screen, where a rocket is designed.
        HangarBlocks.declare(modEventBus);
        RocketDesigner.declare();
        // The mobile launcher a rocket stands on in its hangar: its deck, its umbilical tower and their arms.
        LauncherBlocks.declare();
        // The crawler-transporter that carries the launcher, its tower and its rocket along the crawlerway.
        CrawlerBlocks.declare();
        // The launch pad the crawler brings the launcher to: its flame trench, its deflector and its service port.
        PadBlocks.declare(modEventBus);
        // The propellant depot that feeds the pad: its welded sphere on its legs, its vent stack and transfer pump.
        DepotBlocks.declare(modEventBus);
        // The Launch Panel by the pad, where a launch is counted down.
        LaunchPanel.declare();
        // A rocket's climb off its pad, saved with its structure; its spent stages let go on their way, falling or
        // flying back to land on their platforms.
        RocketAscent.declare();
        // A rocket's cruise between worlds, saved with its structure; what its crew is told, and the solar storms.
        RocketCruise.declare();
        CruiseTexts.declare();
        SolarStorms.listen();
        // A rocket's coming down to a world, saved with its structure, and what its crew is told of it.
        RocketDescent.declare();
        DescentTexts.declare();
        // The telemetry line, whose stretches are the ground systems.
        TelemetryLine.declare();
        StageLanding.declare();
        StageFlights.listen();
        // The kinds of satellite and of probe a spacecraft is built as.
        SkeletonSpacecraft.declare();
        // The small starship's makers and catalogue of parts, the blocks they are built of and where each goes, the
        // finishes a ship is painted in and the parts' formed models.
        SmallShipMakers.declare();
        SmallShipParts.declare();
        ShipKitBlocks.declare();
        SmallShipKits.declare();
        SmallShipFinishes.declare();
        ShipPartModels.declare();
        ShipShapes.declare();
        ShipFlight.declare();
        ShipBoost.declare();
        ShipAutopilotControl.declare();
        ShipDocks.listen();
        // Where a ship sets down: the ship landing pad, and the runway beacon a wheeled ship lines up on.
        ShipLandingBlocks.declare();
        // The Small Shipyard, the dry dock a small starship is built in: its shape, its blocks and its formed model.
        ShipyardStructure.declare();
        ShipyardBlocks.declare();
        ShipDesigner.declare();
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
