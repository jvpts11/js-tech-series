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
import dev.jstech.space.guide.MissionHandbook;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemSpaces;
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
        // Everything it declares registers at once: blocks, items, block entities and the rest.
        SpaceContent.CONTENT.register(modEventBus);
        // The world's settings: its consequences, each on unless a server turns it off, and its rules of balance.
        SpaceServerConfig.register(modEventBus, modContainer);
        // The mod's data generation, which runs only in a data run.
        JsSpaceDataGenerators.listen(modEventBus);
    }
}
