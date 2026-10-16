/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core;

import com.mojang.logging.LogUtils;
import dev.jstech.core.advancement.Advancements;
import dev.jstech.core.advancement.CoreTriggers;
import dev.jstech.core.api.CoreRegisterEvent;
import dev.jstech.core.audio.AudioNetwork;
import dev.jstech.core.audio.AudioSettings;
import dev.jstech.core.audio.media.MediaCommand;
import dev.jstech.core.audio.media.MediaLedgers;
import dev.jstech.core.audio.media.MediaNetwork;
import dev.jstech.core.audio.media.MediaServerEvents;
import dev.jstech.core.blockentity.ClientUpdates;
import dev.jstech.core.blockentity.FieldCapabilities;
import dev.jstech.core.cable.CableBreaking;
import dev.jstech.core.call.MenuCalls;
import dev.jstech.core.cable.CableRuns;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.cable.EndSettings;
import dev.jstech.core.overhead.OverheadLines;
import dev.jstech.core.machine.DataMachines;
import dev.jstech.core.machine.MachineWrench;
import dev.jstech.core.machine.Machines;
import dev.jstech.core.signal.SignalRadio;
import dev.jstech.core.command.CoreCommands;
import dev.jstech.core.command.SeriesCommands;
import dev.jstech.core.command.TeamProgressCommands;
import dev.jstech.core.data.DataRegistries;
import dev.jstech.core.datagen.JsCoreDataGenerators;
import dev.jstech.core.developer.LiveReload;
import dev.jstech.core.diagnostic.Bandwidth;
import dev.jstech.core.diagnostic.CoreHealth;
import dev.jstech.core.diagnostic.CostMap;
import dev.jstech.core.diagnostic.DiagnosticCommands;
import dev.jstech.core.diagnostic.DiagnosticNotifications;
import dev.jstech.core.diagnostic.DiagnosticReports;
import dev.jstech.core.diagnostic.Sections;
import dev.jstech.core.dimension.DimensionEffects;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.heat.BlockConductivity;
import dev.jstech.core.heat.HeatPorts;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.hologram.Holograms;
import dev.jstech.core.input.KeyActions;
import dev.jstech.core.mechanical.MechanicalPorts;
import dev.jstech.core.mechanical.ShaftLinkItem;
import dev.jstech.core.multipart.CoreParts;
import dev.jstech.core.multipart.CoreSpaces;
import dev.jstech.core.network.NetworkMembers;
import dev.jstech.core.network.StateTables;
import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.network.transfer.BigPayloads;
import dev.jstech.core.robot.CoreRobotTasks;
import dev.jstech.core.config.ConfigCommands;
import dev.jstech.core.conveyor.Conveyors;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigSync;
import dev.jstech.core.config.CoreConfigKeys;
import dev.jstech.core.energy.CoreEnergy;
import dev.jstech.core.event.CoreEventDispatcher;
import dev.jstech.core.font.CoreFonts;
import dev.jstech.core.gui.screen.ControlElements;
import dev.jstech.core.gui.screen.CoreElements;
import dev.jstech.core.gui.style.StyleSettings;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideGifts;
import dev.jstech.core.guide.GuideSounds;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.integration.accessories.AccessoriesIntegration;
import dev.jstech.core.integration.curios.CuriosIntegration;
import dev.jstech.core.integration.ftbchunks.FtbChunksIntegration;
import dev.jstech.core.integration.ftbteams.FtbTeamsIntegration;
import dev.jstech.core.integration.mekanism.MekanismIntegration;
import dev.jstech.core.integration.opac.OpacIntegration;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.jstechseries.ElementNaming;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.jstechseries.SeriesMods;
import dev.jstech.core.jstechseries.SimplePath;
import dev.jstech.core.jstechseries.builder.MultiblockBuilder;
import dev.jstech.core.jstechseries.containment.ContainmentGuide;
import dev.jstech.core.jstechseries.containment.SharedContainment;
import dev.jstech.core.jstechseries.projector.ProjectorGuide;
import dev.jstech.core.jstechseries.projector.StructureProjector;
import dev.jstech.core.jstechseries.energy.ClampMeter;
import dev.jstech.core.jstechseries.energy.EnergyConverters;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.energy.JsEnergyGuide;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.energy.JsEnergySettings;
import dev.jstech.core.jstechseries.lines.LinesGuide;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.core.jstechseries.probe.NetworkProbe;
import dev.jstech.core.jstechseries.probe.SignalProbe;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.core.jstechseries.tools.ToolsGuide;
import dev.jstech.core.language.LanguageRegistry;
import dev.jstech.core.log.LogCommand;
import dev.jstech.core.material.CatalogueItems;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.MaterialData;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialNames;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.material.MineralFamilies;
import dev.jstech.core.menu.MenuSync;
import dev.jstech.core.model.network.ModelAnimations;
import dev.jstech.core.model.network.ModelCommands;
import dev.jstech.core.multiblock.LevelStructures;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.notification.NotificationSettings;
import dev.jstech.core.notification.NotificationSounds;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.operation.OperationTypeRegistry;
import dev.jstech.core.persistence.NetworkRegistry;
import dev.jstech.core.progression.Eras;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.progression.PlayerProgress;
import dev.jstech.core.progression.ProgressionTriggers;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.protection.OwnedProtection;
import dev.jstech.core.protection.PlayerLimits;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.protection.ProtectionCommands;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.StateNetwork;
import dev.jstech.core.task.Tasks;
import dev.jstech.core.team.CorePermissions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import org.slf4j.Logger;

/**
 * The shared library of the J's Tech Series. It has no gameplay of its own: it carries what every mod of
 * the series is built on, and every one of them requires it. The one thing it adds to the game is the
 * material catalogue, the items several mods process and trade.
 */
@Mod(JsCore.MODID)
public final class JsCore {

    public static final String MODID = "jscore";

    public static final Logger LOGGER = LogUtils.getLogger();

    private static final CoreEventDispatcher EVENTS = new CoreEventDispatcher();
    private static final OperationTypeRegistry OPERATIONS = new OperationTypeRegistry();
    private static final LanguageRegistry LANGUAGES =
            new LanguageRegistry();

    /**
     * The series' internal event bus: an orchestrator posts the lifecycle of its Operations here and any mod
     * subscribes, without a class dependency between them. Posted on the server thread.
     */
    public static CoreEventDispatcher events() {
        return EVENTS;
    }

    /** The Operation types the mods of the series (and addons) declare, by id such as {@code jsc:select}. */
    public static OperationTypeRegistry operations() {
        return OPERATIONS;
    }

    /**
     * The languages the computers of this world can be programmed in.
     *
     * <p>An addon may add one and may take one away, this series' own included: everything that deals in
     * programs resolves through here by extension rather than by naming a language.
     */
    public static LanguageRegistry languages() {
        return LANGUAGES;
    }

    public JsCore(final IEventBus modEventBus, final ModContainer modContainer) {
        LOGGER.info("J's Core {} loaded.", modContainer.getModInfo().getVersion());
        // The series' worlds open with no warning about experimental settings: its data is no experiment.
        SeriesMods.declareStable();
        // The data network lives on the level and the chunks as attachments the core owns.
        CoreAttachments.register(modEventBus);
        // The cable block every mod's cables are laid in, and the screen of a line's end, are the Core's own content.
        CoreCables.declare();
        // The space of parts alone and the microblock, which any block space holds as a cable's does.
        CoreSpaces.declare();
        EndSettings.declare();
        OverheadLines.declare();
        // The menu every machine of every mod opens, behind its five tabs, and what a wrench shows of a machine.
        Machines.declare();
        MachineWrench.declare();
        // The fonts every mod can draw in, declared with the licence and credit they come under.
        CoreFonts.declare();
        // The manuals' binder style, which every manual of the series is drawn in unless it brings its own.
        CoreGuide.declare();
        // The series' one creative tab of what its mods share, showing only what the installed mods use.
        JsTechTab.declare();
        // The open catalogue every mod adds to: the forms, the properties, the mineral families and the rocks ores sit
        // in; the series' own materials; the values data packs give them; and the data of the things the installed
        // mods use, made as the server loads its data.
        Forms.declare();
        MaterialProperties.declare();
        Thermal.declare();
        MineralFamilies.declare();
        HostRocks.declare();
        MaterialNames.declare();
        SeriesMaterials.declare();
        ElementNaming.declare();
        MaterialValues.declare();
        MaterialData.declare();
        // Where a belt or a chain remembers the first pulley it was used on.
        ShaftLinkItem.declare();
        // What the manuals sound like: the game's own book and button sounds, in the Manuals channel.
        GuideSounds.declare();
        // What the notifications sound like, one of the game's own sounds for each level.
        NotificationSounds.declare();
        // The clamp meter, the series' instrument that reads the energy through a line or a block, and its page.
        ClampMeter.declare();
        SharedEnergy.declare();
        EnergyConverters.declare();
        SharedContainment.declare();
        StructureProjector.declare();
        MultiblockBuilder.declare();
        SharedTools.declare();
        NetworkProbe.declare();
        SignalProbe.declare();
        SharedPipes.declare();
        JsEnergyGuide.declare();
        ContainmentGuide.declare();
        ProjectorGuide.declare();
        ToolsGuide.declare();
        LinesGuide.declare();
        CoreItems.register(modEventBus);
        // The units energy is counted in, FE first, and the energy an item holds; then J's Energy, the series' own,
        // with its joule and the grids of its voltage classes.
        CoreEnergy.register(modEventBus);
        JsEnergy.register(modEventBus);
        // The components an item keeps its mode, its fluid and its stacks in.
        ItemStates.register(modEventBus);
        // The Core's own keys, declared before the player's game makes its key bindings.
        CoreKeys.declare();
        // Soft integrations: each one checks for its mod and stays a no-op without it.
        MekanismIntegration.bootstrap();
        FtbTeamsIntegration.bootstrap();
        FtbChunksIntegration.bootstrap();
        OpacIntegration.bootstrap();
        AccessoriesIntegration.bootstrap();
        CuriosIntegration.bootstrap();
        // The series' permissions, asked of the server's permission system, and what each player may have.
        CorePermissions.listen();
        PlayerLimits.register();
        // The balance of the Operations engine is series-wide, so the Core owns the world's balance file.
        ConfigFiles.register(CoreConfigKeys.FILE, modEventBus, modContainer);
        // J's Energy's settings: whether it is on, what the wrong voltage does, and how it speaks FE.
        ConfigFiles.register(JsEnergySettings.FILE, modEventBus, modContainer);
        // A player's sound preferences, read on their own game only.
        ConfigFiles.register(AudioSettings.FILE, modEventBus, modContainer);
        // The styles a player chose for the screens of every mod, read on their own game only.
        ConfigFiles.register(StyleSettings.FILE, modEventBus, modContainer);
        // Where a player's notifications appear and what they silenced, read on their own game only.
        ConfigFiles.register(NotificationSettings.FILE, modEventBus, modContainer);
        // The kinds of component a screen's file may hold, before any file is read.
        CoreElements.declare();
        ControlElements.declare();
        // What the Core keeps with a world: each dimension's data networks, and the ledger of its recordings.
        CoreStates.register(NetworkRegistry.NETWORKS);
        MediaLedgers.register();
        // How far each player has come along each progression axis, and the advancement triggers every mod stands on.
        PlayerProgress.register();
        TeamProgress.register();
        ProgressionTriggers.listen();
        // The manuals each player was handed as they first joined, so each is handed once.
        GuideGifts.register();
        CoreTriggers.register(modEventBus);
        // The series' commands under /jstech: the Core's own, the recordings' upkeep, the log's levels, and what
        // the server's administrators read of the series' health and cost.
        CoreCommands.declare();
        TeamProgressCommands.declare();
        ModelCommands.declare();
        ConfigCommands.declare();
        ProtectionCommands.declare();
        MediaCommand.declare();
        LogCommand.declare();
        DiagnosticCommands.declare();
        // The Core's part of /jstech health, before any mod adds its own, and what its operators are notified of.
        CoreHealth.declare();
        DiagnosticNotifications.declare();
        // The multiblock patterns of every mod, read from data and declared in code.
        MultiblockPatterns.register();
        DataMachines.register();
        // What each dimension is like to stand in, read from data.
        DimensionRulesData.register();
        // The tasks every robot knows, before a world reads its robots back.
        CoreRobotTasks.declare();
        // The chunks machines keep loaded for their owners, under the game's own forced-chunk file.
        modEventBus.addListener(RegisterTicketControllersEvent.class, ChunkLoaders::onRegisterControllers);
        // What each part of the Core answers on the mod's bus and the game's, declared here one class at a time
        // rather than found by a scan for annotations, so what runs is what this list says.
        Advancements.listen();
        AudioNetwork.listen(modEventBus);
        Bandwidth.listen();
        BigPayloads.listen(modEventBus);
        CableBreaking.listen();
        ClampMeter.listen(modEventBus);
        NetworkProbe.listen(modEventBus);
        SignalProbe.listen(modEventBus);
        MultiblockBuilder.listen(modEventBus);
        SharedEnergy.listen(modEventBus);
        EnergyConverters.listen(modEventBus);
        SharedContainment.listen(modEventBus);
        ClientPayloads.listen();
        ClientUpdates.listen();
        CoreCables.listen(modEventBus);
        CoreSpaces.listen(modEventBus);
        CableRuns.listen(modEventBus);
        EndSettings.listen(modEventBus);
        OverheadLines.listen(modEventBus);
        SignalRadio.listen();
        Machines.listen(modEventBus);
        MachineWrench.listen(modEventBus);
        HeatPorts.listen(modEventBus);
        BlockConductivity.listen(modEventBus);
        MechanicalPorts.listen(modEventBus);
        Conveyors.listen(modEventBus);
        NetworkMembers.listen();
        CoreEnergy.listen(modEventBus);
        ConfigSync.listen(modEventBus);
        CoreParts.listen(modEventBus);
        CoreStates.listen();
        CostMap.listen();
        DataRegistries.listen(modEventBus);
        DiagnosticReports.listen();
        DimensionEffects.listen();
        FieldCapabilities.listen(modEventBus);
        CoreGrids.listen();
        GuideGifts.listen();
        Holograms.listen(modEventBus);
        JsCoreDataGenerators.listen(modEventBus);
        JsTechTab.listen(modEventBus);
        KeyActions.listen(modEventBus);
        CatalogueItems.listen(modEventBus);
        MaterialLiquids.listen(modEventBus);
        MaterialUses.listen(modEventBus);
        SimplePath.register(modEventBus);
        MediaNetwork.listen(modEventBus);
        MediaServerEvents.listen();
        ModelAnimations.listen(modEventBus);
        Notifications.listen(modEventBus);
        LevelStructures.listen();
        OwnedProtection.listen();
        Protection.listen();
        RuntimeDimensions.listen();
        RuntimePacks.listen(modEventBus);
        Sections.listen();
        SeriesCommands.listen();
        StateNetwork.listen(modEventBus);
        // The one payload every screen's call travels in, the one the menus' values travel in, and the server's work
        // away from the tick.
        MenuCalls.listen(modEventBus);
        MenuSync.listen(modEventBus);
        Tasks.listen();
        // In a development run, the mods' data reloaded as it changes.
        LiveReload.listen();
        /*
         * One moment for anything to be added, and one for the door to close. Everything of the series adds
         * itself through the same event an addon does, so the way in is the one that is tested every time
         * the game starts. It is posted on the main thread rather than while the mods load side by side,
         * because what it fills is shared and would otherwise be filled from several threads at once.
         */
        modEventBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(
                () -> ModLoader.postEvent(new CoreRegisterEvent(LANGUAGES, OPERATIONS))));
        // Every cable is registered by now: two lines given one lane stop the game here, not in a world.
        modEventBus.addListener(FMLCommonSetupEvent.class, event -> CoreCables.checkLanes());
        modEventBus.addListener(FMLLoadCompleteEvent.class, event -> event.enqueueWork(() -> {
            LANGUAGES.freeze();
            OPERATIONS.freeze();
            StateTables.freeze();
            Eras.freeze();
            Knowledges.freeze();
        }));
    }
}
