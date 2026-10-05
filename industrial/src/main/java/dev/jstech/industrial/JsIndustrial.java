/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial;

import com.mojang.logging.LogUtils;
import dev.jstech.industrial.guide.IndustrialGuide;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * The industrial mod of the J's Tech Series: energy, machines and the processing chains that feed the
 * network. It is built on the core alone; the computing mod drives its machines through the core's
 * capabilities and never the other way round.
 */
@Mod(JsIndustrial.MODID)
public final class JsIndustrial {

    public static final String MODID = "jsindustrial";

    public static final Logger LOGGER = LogUtils.getLogger();

    public JsIndustrial(final IEventBus modEventBus, final ModContainer modContainer) {
        LOGGER.info("J's Industrial {} loaded.", modContainer.getModInfo().getVersion());
        // The mod's chapter of the manuals and its own Plant Drawings, with the folder's item.
        IndustrialGuide.declare();
        IndustrialModule.register(modEventBus);
    }
}
