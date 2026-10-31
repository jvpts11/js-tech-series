/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space;

import dev.jstech.core.client.advancement.WideBackgrounds;
import dev.jstech.core.client.multiblock.PlanHologram;
import dev.jstech.space.client.crawler.CrawlerRenderer;
import dev.jstech.space.client.depot.DepotRenderer;
import dev.jstech.space.client.launch.LaunchPanelScreen;
import dev.jstech.space.client.flight.FlightComputerScreen;
import dev.jstech.space.client.flight.LandingMapsClient;
import dev.jstech.space.client.hangar.HangarTerminalRenderer;
import dev.jstech.space.client.hangar.RocketDesignScene;
import dev.jstech.space.client.hangar.RocketHolograms;
import dev.jstech.space.client.hangar.RocketDesignScreen;
import dev.jstech.space.client.launcher.ArmRenderer;
import dev.jstech.space.client.launcher.CraneRenderer;
import dev.jstech.space.client.launcher.LauncherDeckRenderer;
import dev.jstech.space.client.life.LunarSuitClient;
import dev.jstech.space.client.life.SuitControlsHud;
import dev.jstech.space.client.life.SuitLamps;
import dev.jstech.space.client.life.SunGlare;
import dev.jstech.space.client.platform.LandingPlatformRenderer;
import dev.jstech.space.client.rocket.FlightComputerRenderer;
import dev.jstech.space.client.ship.FlightConsoleRenderer;
import dev.jstech.space.client.ship.ShipBoostClient;
import dev.jstech.space.client.ship.ShipChaseCamera;
import dev.jstech.space.client.ship.ShipExhaust;
import dev.jstech.space.client.ship.ShipHud;
import dev.jstech.space.client.shipyard.ShipDesignScreen;
import dev.jstech.space.client.shipyard.ShipHolograms;
import dev.jstech.space.client.shipyard.ShipyardRenderer;
import dev.jstech.space.client.universe.SystemSpaceRenderer;
import dev.jstech.space.flight.FlightComputer;
import dev.jstech.space.hangar.RocketDesigner;
import dev.jstech.space.launch.LaunchPanel;
import dev.jstech.space.network.SpacePayloads;
import dev.jstech.space.shipyard.ShipDesigner;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The space mod's entry point on a player's game. */
@Mod(value = JsSpace.MODID, dist = Dist.CLIENT)
public final class JsSpaceClient {

    /* The night sky behind the mod's advancement tabs: a 16-pixel tile of it, and the whole sky, 400 by 200. */
    private static final String SKY = "jsspace:textures/gui/advancements/sky.png";
    private static final String WIDE_SKY = "jsspace:textures/gui/advancements/sky_wide.png";
    private static final int WIDE_SKY_WIDTH = 400;
    private static final int WIDE_SKY_HEIGHT = 200;

    public JsSpaceClient(final IEventBus modEventBus, final ModContainer modContainer) {
        // A tab that names the tile as its background is drawn with the whole sky, once, sliding slower than the tree.
        WideBackgrounds.declare(SKY, WIDE_SKY, WIDE_SKY_WIDTH, WIDE_SKY_HEIGHT);
        // The worlds of the Solar System's space, drawn as spheres in their places, and the Sun among the stars.
        SystemSpaceRenderer.listen(modEventBus);
        // The lamps on suits' helmets, lighting the way; the lunar suit worn, drawn with its model; and the Sun's glare
        // where no air softens it, which the gold visor cuts.
        SuitLamps.listen();
        LunarSuitClient.listen(modEventBus);
        SunGlare.listen(modEventBus);
        // The suit's controls on the HUD, stacked with the hazard devices, and up close on their key.
        SuitControlsHud.listen(modEventBus);
        // The target painted across a formed landing platform.
        LandingPlatformRenderer.listen(modEventBus);
        // A formed Small Shipyard, drawn whole as the approved dock, its builder's screen, and the ship it builds, each
        // part drawn with its model once it has formed, and its hologram till then.
        ShipyardRenderer.listen(modEventBus);
        // A ship that has left its dock, drawn from its flight console wherever it goes, and its boost's key.
        FlightConsoleRenderer.listen(modEventBus);
        ShipBoostClient.listen();
        ShipExhaust.listen();
        // The HUD on its canopy, for the pilot at its helm, and the camera behind it in the third person.
        ShipHud.listen(modEventBus);
        ShipChaseCamera.listen();
        modEventBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(ShipDesigner.MENU.get(),
                ShipDesignScreen::new));
        PlanHologram.show(ShipHolograms::shown);
        // The Flight Computer's screen, and the landing maps it is sent.
        modEventBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(FlightComputer.MENU.get(),
                FlightComputerScreen::new));
        SpacePayloads.onLandingMap(LandingMapsClient::receive);
        // The Rocket Hangar's design screen, and the rocket it draws in the hangar while it is open.
        modEventBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(RocketDesigner.MENU.get(),
                RocketDesignScreen::new));
        RocketDesignScene.listen();
        // The rocket the hangar builds, each part drawn with its model once it has formed, and its hologram till then.
        HangarTerminalRenderer.listen(modEventBus);
        PlanHologram.show(RocketHolograms::shown);
        // A formed mobile launcher's deck, the crane on its tower's top and its arms, drawn whole as the approved ones.
        LauncherDeckRenderer.listen(modEventBus);
        CraneRenderer.listen(modEventBus);
        ArmRenderer.listen(modEventBus);
        // The rocket's formed parts drawn by its flight computer once the hangar lets it go, and a formed crawler.
        FlightComputerRenderer.listen(modEventBus);
        CrawlerRenderer.listen(modEventBus);
        // A formed propellant depot, its sphere on its legs, drawn whole as the approved one.
        DepotRenderer.listen(modEventBus);
        // The Launch Panel's console, the approved LCC-1.
        modEventBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(LaunchPanel.MENU.get(),
                LaunchPanelScreen::new));
    }
}
