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
import dev.jstech.computers.advancement.PendingAwards;
import dev.jstech.computers.advancement.ProgramTravels;
import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.api.JsComputersApi;
import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.audio.catalog.SoundfoundryCatalog;
import dev.jstech.computers.audio.catalog.SoundfoundryCommand;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.datagen.JscDataGenerators;
import dev.jstech.computers.guide.ComputersGuide;
import dev.jstech.computers.integration.jei.payload.JeiPayloads;
import dev.jstech.computers.machine.FileCopyJobs;
import dev.jstech.computers.machine.MachineListing;
import dev.jstech.computers.machine.SigmaLanguage;
import dev.jstech.computers.operation.ComputingOperations;
import dev.jstech.computers.operation.payload.ComputingPayloads;
import dev.jstech.computers.operation.payload.PatternStudioPayloads;
import dev.jstech.computers.operation.payload.ScreenSessions;
import dev.jstech.computers.operation.payload.crafting.CraftingPayloads;
import dev.jstech.computers.operation.payload.update.UpdatePayloads;
import dev.jstech.computers.operation.payload.workshop.WorkshopPayloads;
import dev.jstech.computers.os.OsBootstrap;
import dev.jstech.computers.os.OsMotions;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ComponentKinds;
import dev.jstech.computers.program.cli.man.CommandGuidePages;
import dev.jstech.computers.trace.IsmsTraces;
import dev.jstech.core.audio.media.MediaKeepers;
import dev.jstech.core.jstechseries.operation.RegisterOperationTypesEvent;
import dev.jstech.core.state.CoreStates;
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
        // The permission nodes of its actions, before the game gathers every mod's.
        ComputersPermissions.declare();

        /*
         * What this mod adds to the Core is added at the moment the Core opens for it, and by the same event
         * an addon would use. Nothing here reaches into the Core's registries on its own, so the way in is
         * the way that is tried every time the game starts rather than a path only addons take.
         */
        modEventBus.addListener(RegisterOperationTypesEvent.class,
                event -> ComputingOperations.register(event.operations()));

        /*
         * The same, the other way round: this mod opens its own registries once, its languages among them,
         * and closes them when the loading is done, so that what a world can install does not change under
         * somebody playing it. Its own languages go in through the same event an addon's do.
         */
        modEventBus.addListener(ComputersRegisterEvent.class, JsComputers::addLanguages);
        modEventBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(
                () -> ModLoader.postEvent(new ComputersRegisterEvent())));
        // The programs' runtime finds the mods' kinds of component through the list kept outside it.
        ComponentKinds.install();
        modEventBus.addListener(FMLLoadCompleteEvent.class, event -> event.enqueueWork(() -> {
            OsRegistry.freeze();
            ComponentKinds.freeze();
            JsComputersApi.languages().freeze();
            // Every system and every mod's commands are known now, so each command's page in the manuals is too.
            CommandGuidePages.register();
        }));

        // The mod's chapter of the manuals and its own Guide to Operations, with the manual's item.
        ComputersGuide.declare();
        ComputingModule.register(modEventBus);
        // The buses mounted on cables, registered with the Core's parts.
        ComputingParts.register(modEventBus);
        MachineOperators.register(modEventBus);
        ProgramTravels.register(modEventBus);
        // What each player earned while away, kept with the world until they join.
        CoreStates.register(PendingAwards.WAITING);
        // Songs players bring from their own computers, kept on a computer's disk.
        MusicImports.register();
        // The catalogue's songs stay on the server however long nobody plays them.
        MediaKeepers.register(SoundfoundryCatalog::media);
        // How each desktop moves, declared before the data generation writes it and the packs replace it.
        OsMotions.declare();

        // What the mod answers on the buses, each part declared here rather than finding its own way in.
        OsBootstrap.listen(modEventBus);
        ComputingCapabilities.listen(modEventBus);
        ComputingPayloads.listen(modEventBus);
        PatternStudioPayloads.listen(modEventBus);
        UpdatePayloads.listen(modEventBus);
        WorkshopPayloads.listen(modEventBus);
        JeiPayloads.listen(modEventBus);
        JscDataGenerators.listen(modEventBus);
        MachineOperators.listen();
        PendingAwards.listen();
        SoundfoundryCatalog.listen();
        SoundfoundryCommand.listen();
        FileCopyJobs.listen();
        CraftingPayloads.listen();
        ScreenSessions.listen();
        IsmsTraces.listen();
    }

    /**
     * The mod's own languages, at the one moment the registry is open.
     *
     * <p>Sigma Sharp and Sigma are languages like any other as far as the machines are concerned: they go in the
     * same registry an addon would use, and can be taken out of it by one. The full language goes in first, so it
     * is the one a list of languages opens on. What they compile to is not their own: the machines run listings
     * themselves, so that extension is kept back from every language first.
     */
    private static void addLanguages(final ComputersRegisterEvent event) {
        event.languages().reserve(MachineListing.EXTENSION);
        event.languages().register(SigmaLanguage.SIGMA_SHARP);
        event.languages().register(SigmaLanguage.SIGMA);
    }
}
