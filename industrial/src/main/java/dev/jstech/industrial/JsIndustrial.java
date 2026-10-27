/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial;

import com.mojang.logging.LogUtils;
import dev.jstech.core.research.Discoveries;
import dev.jstech.industrial.circuit.Circuit;
import dev.jstech.industrial.config.IndustrialServerConfig;
import dev.jstech.industrial.crc.CrcCalls;
import dev.jstech.industrial.crc.CrcContent;
import dev.jstech.industrial.crc.CrcStructure;
import dev.jstech.industrial.datagen.JsIndustrialDataGenerators;
import dev.jstech.industrial.discovery.ElementDiscovery;
import dev.jstech.industrial.guide.IndustrialGuide;
import dev.jstech.industrial.integration.computers.ComputersIntegration;
import dev.jstech.industrial.line.IndustrialPipeMarks;
import dev.jstech.industrial.line.PneumaticPost;
import dev.jstech.industrial.world.IndustrialDeposits;
import dev.jstech.industrial.material.IndustrialMaterials;
import dev.jstech.industrial.research.IndustrialResearch;
import dev.jstech.industrial.research.ResearchLink;
import dev.jstech.industrial.tool.HandScrap;
import dev.jstech.industrial.tool.HandTools;
import dev.jstech.industrial.tool.NoEnchanting;
import dev.jstech.industrial.tool.ToolKinds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * The industrial mod of the J's Tech Series: energy, machines and the processing chains that feed the
 * network. It is built on the core alone; the computing mod drives its machines through the core's
 * capabilities and never the other way round. With the computing mod installed beside it, it brings the
 * computer that reads and commands them, behind a guard.
 */
@Mod(JsIndustrial.MODID)
public final class JsIndustrial {

    public static final String MODID = "jsindustrial";

    public static final Logger LOGGER = LogUtils.getLogger();

    public JsIndustrial(final IEventBus modEventBus, final ModContainer modContainer) {
        LOGGER.info("J's Industrial {} loaded.", modContainer.getModInfo().getVersion());
        // The mod's chapter of the manuals and its own Plant Drawings, with the folder's item.
        IndustrialGuide.declare();
        // What it uses of the series' catalogue (its metals in their forms, its ores and minerals, their scraps), the
        // hand tools of the start and the scraps drawing by hand leaves, and the elements discovered by being made.
        IndustrialMaterials.declare();
        // Its deposit types, each bringing together the minerals that form together, in place of the Core's plain
        // veins of the shared metals.
        IndustrialDeposits.declare();
        HandTools.declare();
        // Its tools and armour take modules and never enchanting, and each kind of its tools may be left out.
        NoEnchanting.listen();
        ToolKinds.listen(modEventBus);
        HandScrap.register(modEventBus);
        ElementDiscovery.declare();
        // Making a tier's circuit moves a player to that tier.
        Circuit.declare();
        // The world's settings: the first, whether its research locks what its discoveries open.
        IndustrialServerConfig.register(modEventBus, modContainer);
        Discoveries.gate(MODID, IndustrialServerConfig::requireResearch);
        // The kinds of data its instruments give and its discoveries ask for, an item each.
        IndustrialResearch.declare();
        // The Research Link Cable, whose runs join research computers into groups.
        ResearchLink.declare();
        // The marks of what its pipes carry: acids, molten metal and salt, ultra-pure water, the corrosive gases, and
        // the hydrogen and slurries that wear pipes through.
        IndustrialPipeMarks.declare();
        // The capsule tubes of its pneumatic post, whose runs join the stations on them.
        PneumaticPost.declare();
        // Hot water, the coolant's water warmed; and the Computational Research Centre, its shape and its content.
        IndustrialFluids.declare();
        CrcStructure.declare();
        CrcContent.register(modEventBus);
        // What its Research tab asks of the server: a discovery researched now, or queued.
        CrcCalls.declare();
        IndustrialModule.register(modEventBus);
        // With J's Computers installed: the Industrial Controller Computers, their Fieldbus Cards and their cable.
        ComputersIntegration.declare(modEventBus);
        // The mod's data generation, which runs only in a data run.
        JsIndustrialDataGenerators.listen(modEventBus);
    }
}
