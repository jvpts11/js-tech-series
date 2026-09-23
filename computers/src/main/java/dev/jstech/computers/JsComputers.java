/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.advancement.MachineOperators;
import dev.jstech.computers.advancement.ProgramTravels;
import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.integration.mekanism.MekanismIntegration;
import dev.jstech.computers.machine.MachineListing;
import dev.jstech.computers.machine.SigmaLanguage;
import dev.jstech.computers.operation.ComputingOperations;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.core.api.CoreRegisterEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
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

        /*
         * What this mod adds to the Core is added at the moment the Core opens for it, and by the same event
         * an addon would use. Nothing here reaches into the Core's registries on its own, so the way in is
         * the way that is tried every time the game starts rather than a path only addons take.
         */
        modEventBus.addListener(CoreRegisterEvent.class, JsComputers::addToTheCore);

        /*
         * The same, the other way round: this mod opens its own registries once, and closes them when the
         * loading is done, so that what a world can install does not change under somebody playing it.
         */
        modEventBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(
                () -> ModLoader.postEvent(new ComputersRegisterEvent())));
        modEventBus.addListener(FMLLoadCompleteEvent.class, event -> event.enqueueWork(OsRegistry::freeze));

        ComputingModule.register(modEventBus);
        MachineOperators.register(modEventBus);
        ProgramTravels.register(modEventBus);

        // Soft integrations: each one checks for its mod and stays a no-op without it.
        MekanismIntegration.bootstrap();
    }

    /**
     * What this mod puts in the Core's registries, at the one moment they are open.
     *
     * <p>Sigma Sharp and Sigma are languages like any other as far as the machines are concerned: they go in the
     * same registry an addon would use, and can be taken out of it by one. The full language goes in first, so it
     * is the one a list of languages opens on. What they compile to is not their own: the machines run listings
     * themselves, so that extension is kept back from every language first.
     */
    private static void addToTheCore(final CoreRegisterEvent event) {
        ComputingOperations.register(event.operations());
        event.languages().reserve(MachineListing.EXTENSION);
        event.languages().register(SigmaLanguage.SIGMA_SHARP);
        event.languages().register(SigmaLanguage.SIGMA);
    }
}
