/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space;

import com.mojang.logging.LogUtils;
import dev.jstech.space.datagen.JsSpaceDataGenerators;
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
        // Everything it declares registers at once: blocks, items, block entities and the rest.
        SpaceContent.CONTENT.register(modEventBus);
        // The mod's data generation, which runs only in a data run.
        JsSpaceDataGenerators.listen(modEventBus);
    }
}
