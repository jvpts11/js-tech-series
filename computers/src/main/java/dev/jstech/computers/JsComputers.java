/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.integration.mekanism.MekanismIntegration;
import dev.jstech.computers.machine.MachineListing;
import dev.jstech.computers.machine.SigmaLanguage;
import dev.jstech.computers.operation.ComputingOperations;
import dev.jstech.computers.registry.JscCreativeModeTabs;
import dev.jstech.core.JsCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Main mod entry point for J's Computers.
 */
@Mod(JsComputers.MODID)
public class JsComputers {

    public static final String MODID = "jsc";

    public static final Logger LOGGER = LogUtils.getLogger();

    public JsComputers(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("J's Computers {} loaded.", modContainer.getModInfo().getVersion());

        // The settings of the machines themselves, beside the series' balance file rather than inside it.
        ComputersServerConfig.register(modEventBus, modContainer);

        // The Operation types the network runs, declared in the core registry for every other mod to see.
        ComputingOperations.register();

        /*
         * Σ# is a language like any other as far as the machines are concerned: it goes in the same
         * registry an addon would use, and can be taken out of it by one. What it compiles to is not its own:
         * the machines run listings themselves, so that extension is kept back from every language first.
         */
        JsCore.languages().reserve(MachineListing.EXTENSION);
        JsCore.languages().register(
                SigmaLanguage.INSTANCE);

        ComputingModule.register(modEventBus);
        JscCreativeModeTabs.register(modEventBus);

        // Soft integrations: each one checks for its mod and stays a no-op without it.
        MekanismIntegration.bootstrap();
    }
}
