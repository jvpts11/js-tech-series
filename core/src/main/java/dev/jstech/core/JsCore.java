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
import dev.jstech.core.audio.AudioNetwork;
import dev.jstech.core.audio.AudioSettings;
import dev.jstech.core.audio.acoustics.AcousticCues;
import dev.jstech.core.audio.acoustics.Acoustics;
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
import dev.jstech.core.condition.DataConditions;
import dev.jstech.core.modpack.ModpackDumps;
import dev.jstech.core.modpack.ReloadReport;
import dev.jstech.core.pack.ContentPacks;
import dev.jstech.core.script.CoreScripts;
import dev.jstech.core.unify.UnificationSettings;
import dev.jstech.core.unify.UnifiedDrops;
import dev.jstech.core.loot.LootChanges;
import dev.jstech.core.recipe.RecipeChoiceSettings;
import dev.jstech.core.recipe.RecipeChoices;
import dev.jstech.core.variant.VariantData;
import dev.jstech.core.variant.Variants;
import dev.jstech.core.machine.CatalystWear;
import dev.jstech.core.machine.DataMachines;
import dev.jstech.core.machine.MachineWrench;
import dev.jstech.core.machine.Machines;
import dev.jstech.core.map.MapMarkers;
import dev.jstech.core.signal.SignalRadio;
import dev.jstech.core.command.CommandRoot;
import dev.jstech.core.command.CoreCommands;
import dev.jstech.core.command.TeamProgressCommands;
import dev.jstech.core.data.DataRegistries;
import dev.jstech.core.datagen.JsCoreDataGenerators;
import dev.jstech.core.developer.DeveloperMode;
import dev.jstech.core.developer.ThingTests;
import dev.jstech.core.developer.mcp.McpOnServer;
import dev.jstech.core.developer.studio.StudioLogs;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.developer.LiveReload;
import dev.jstech.core.diagnostic.Bandwidth;
import dev.jstech.core.diagnostic.CoreHealth;
import dev.jstech.core.diagnostic.CostMap;
import dev.jstech.core.diagnostic.DiagnosticCommands;
import dev.jstech.core.diagnostic.DiagnosticNotifications;
import dev.jstech.core.diagnostic.DiagnosticReports;
import dev.jstech.core.diagnostic.Sections;
import dev.jstech.core.dimension.DayClocks;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereDetector;
import dev.jstech.core.physics.Physics;
import dev.jstech.core.room.Rooms;
import dev.jstech.core.sky.Skies;
import dev.jstech.core.weather.Weathers;
import dev.jstech.core.dimension.DimensionEffects;
import dev.jstech.core.effect.EffectSettings;
import dev.jstech.core.effect.Effects;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.DimensionTravel;
import dev.jstech.core.explosion.Explosions;
import dev.jstech.core.fire.Fires;
import dev.jstech.core.hazard.BlockDensity;
import dev.jstech.core.hazard.BlockTemperatures;
import dev.jstech.core.hazard.BodyTemperature;
import dev.jstech.core.hazard.Clothing;
import dev.jstech.core.hazard.HazardMaps;
import dev.jstech.core.hazard.Pollution;
import dev.jstech.core.hazard.Radiation;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.hazard.ItemHazards;
import dev.jstech.core.hazard.Lungs;
import dev.jstech.core.hazard.MaterialHazards;
import dev.jstech.core.hazard.Resistances;
import dev.jstech.core.hazard.Shielding;
import dev.jstech.core.hazard.Withering;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.heat.BlockConductivity;
import dev.jstech.core.heat.HeatPorts;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.hologram.Holograms;
import dev.jstech.core.input.KeyActions;
import dev.jstech.core.inspect.CoreInspectors;
import dev.jstech.core.inspect.InspectCommands;
import dev.jstech.core.inspect.InspectWatch;
import dev.jstech.core.mechanical.MechanicalPorts;
import dev.jstech.core.mechanical.ShaftLinkItem;
import dev.jstech.core.multipart.CoreParts;
import dev.jstech.core.multipart.CoreSpaces;
import dev.jstech.core.jstechseries.network.DataGrids;
import dev.jstech.core.jstechseries.network.NetworkAttachments;
import dev.jstech.core.jstechseries.network.NetworkInspection;
import dev.jstech.core.jstechseries.network.NetworkMembers;
import dev.jstech.core.jstechseries.peripheral.PeripheralReconciler;
import dev.jstech.core.jstechseries.network.StateTables;
import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.network.transfer.BigPayloads;
import dev.jstech.core.module.CoreModuleEffects;
import dev.jstech.core.tool.Tools;
import dev.jstech.core.camera.CameraSettings;
import dev.jstech.core.camera.CameraViews;
import dev.jstech.core.module.Modules;
import dev.jstech.core.creature.CoreCreatureBehaviours;
import dev.jstech.core.creature.Creatures;
import dev.jstech.core.robot.CoreRobotConditions;
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
import dev.jstech.core.gui.AccessibilitySettings;
import dev.jstech.core.live.ScreenTouches;
import dev.jstech.core.gui.HudSettings;
import dev.jstech.core.gui.style.StyleSettings;
import dev.jstech.core.gui.tooltip.TooltipSettings;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideGifts;
import dev.jstech.core.guide.GuideTips;
import dev.jstech.core.guide.ManualLecterns;
import dev.jstech.core.guide.TornSheets;
import dev.jstech.core.guide.GuideSounds;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.integration.accessories.AccessoriesIntegration;
import dev.jstech.core.integration.create.CreateIntegration;
import dev.jstech.core.integration.curios.CuriosIntegration;
import dev.jstech.core.integration.ftbchunks.FtbChunksIntegration;
import dev.jstech.core.integration.crafttweaker.CraftTweakerIntegration;
import dev.jstech.core.integration.ftbquests.FtbQuestsIntegration;
import dev.jstech.core.integration.ftbteams.FtbTeamsIntegration;
import dev.jstech.core.integration.mekanism.MekanismIntegration;
import dev.jstech.core.integration.opac.OpacIntegration;
import dev.jstech.core.integration.sable.SableIntegration;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.item.SwitchedShapedRecipe;
import dev.jstech.core.jstechseries.ElementNaming;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SeriesDeposits;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.jstechseries.SeriesMods;
import dev.jstech.core.jstechseries.SimplePath;
import dev.jstech.core.jstechseries.builder.MultiblockBuilder;
import dev.jstech.core.jstechseries.containment.SharedContainment;
import dev.jstech.core.jstechseries.projector.StructureProjector;
import dev.jstech.core.jstechseries.persistor.PersistorSettings;
import dev.jstech.core.jstechseries.persistor.SpacePersistor;
import dev.jstech.core.jstechseries.energy.ClampMeter;
import dev.jstech.core.jstechseries.hazard.HazardDeviceSettings;
import dev.jstech.core.jstechseries.hazard.Dissonance;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import dev.jstech.core.jstechseries.fire.CharredBlocks;
import dev.jstech.core.jstechseries.fire.FireSafety;
import dev.jstech.core.jstechseries.fire.SeriesFire;
import dev.jstech.core.jstechseries.hazard.IonizingRadiation;
import dev.jstech.core.jstechseries.hazard.SeriesPollution;
import dev.jstech.core.jstechseries.hazard.WitheredGrass;
import dev.jstech.core.jstechseries.energy.EnergyConverters;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.energy.JsEnergySettings;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.core.jstechseries.probe.NetworkProbe;
import dev.jstech.core.jstechseries.probe.SignalProbe;
import dev.jstech.core.jstechseries.tools.PhysicsTool;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.core.light.CoreLights;
import dev.jstech.core.light.LightingSettings;
import dev.jstech.core.log.LogCommand;
import dev.jstech.core.material.CatalogueItems;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.VariantSettings;
import dev.jstech.core.material.MaterialData;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialNames;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.material.MineralFamilies;
import dev.jstech.core.menu.MenuSync;
import dev.jstech.core.model.DrawingSettings;
import dev.jstech.core.model.network.ModelAnimations;
import dev.jstech.core.model.network.ModelCommands;
import dev.jstech.core.moving.BlockAboard;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.core.multiblock.LevelStructures;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.notification.NotificationSettings;
import dev.jstech.core.notification.NotificationSounds;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.jstechseries.operation.OperationSettings;
import dev.jstech.core.jstechseries.operation.OperationTypeRegistry;
import dev.jstech.core.jstechseries.operation.RegisterOperationTypesEvent;
import dev.jstech.core.persistence.NetworkRegistry;
import dev.jstech.core.jstechseries.tier.Eras;
import dev.jstech.core.jstechseries.tier.SeriesAxes;
import dev.jstech.core.progression.KnowledgeShapedRecipe;
import dev.jstech.core.progression.KnowledgeShapelessRecipe;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.progression.PlayerProgress;
import dev.jstech.core.progression.ProgressionTriggers;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.protection.OwnedProtection;
import dev.jstech.core.protection.PlayerLimits;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.protection.ProtectionCommands;
import dev.jstech.core.region.AwayOwners;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.core.research.Discoveries;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.StateNetwork;
import dev.jstech.core.stepaside.StepAside;
import dev.jstech.core.task.Tasks;
import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.vehicle.VehicleControlKeys;
import dev.jstech.core.vehicle.Vehicles;
import dev.jstech.core.world.edit.EditCommands;
import dev.jstech.core.world.edit.WorldEdits;
import dev.jstech.core.world.event.WorldEvents;
import dev.jstech.core.world.field.ChunkFields;
import dev.jstech.core.world.live.LiveChanges;
import dev.jstech.core.worldgen.CoreFeatures;
import dev.jstech.core.worldgen.biome.BiomeClimates;
import dev.jstech.core.worldgen.modify.WorldgenModifiers;
import dev.jstech.core.worldgen.reservoir.Reservoirs;
import dev.jstech.core.worldgen.place.Landmarks;
import dev.jstech.core.worldgen.retro.Retrogen;
import dev.jstech.core.worldgen.structure.CoreStructures;
import dev.jstech.core.worldgen.structure.OperatorStructures;
import dev.jstech.core.worldgen.terrain.density.CoreDensities;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.core.worldgen.vein.Veins;
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

    public JsCore(final IEventBus modEventBus, final ModContainer modContainer) {
        LOGGER.info("J's Core {} loaded.", modContainer.getModInfo().getVersion());
        // The series' worlds open with no warning about experimental settings: its data is no experiment.
        SeriesMods.declareStable();
        // What the Core keeps on the levels and the chunks as attachments, the series' data networks among them.
        CoreAttachments.register(modEventBus);
        NetworkAttachments.register(modEventBus);
        // The cable block every mod's cables are laid in, and the screen of a line's end, are the Core's own content.
        CoreCables.declare();
        // The space of parts alone and the microblock, which any block space holds as a cable's does.
        CoreSpaces.declare();
        // The block a formed structure's plain blocks merge into, its terminal's model drawing them.
        FormedParts.declare();
        // Light standing in the air, the colours of light, its kinds as data, and the spotlights' light.
        CoreLights.declare(modEventBus);
        // Effects fired from the server for whoever is near, and the commands of the effects editor.
        Effects.listen(modEventBus);
        // The shapes of the land every mod's world generation lays (landforms, rock in layers, seas), and the
        // climates of biomes in real values, which heat meets where a biome has its own.
        CoreFeatures.register(modEventBus);
        // More kinds of density function, the worldgen modifiers datapacks write and the operator's structures.
        CoreDensities.register(modEventBus);
        WorldgenModifiers.listen();
        OperatorStructures.register(modEventBus);
        BiomeClimates.listen(modEventBus);
        // The veins every mod's ores lie in and the reservoirs of oil, gas and brines, their kinds as data.
        Veins.listen(modEventBus);
        Deposits.register();
        Reservoirs.register();
        // The structures every mod places: where the land suits them, sunk, founded, worn and growing; the landmarks
        // players discover; and the markers on their maps.
        CoreStructures.register(modEventBus);
        Landmarks.listen();
        MapMarkers.register();
        // The world changing: generation laid into old chunks, the surface changing with the climate, large edits set
        // a share each tick, the world's events, and fields over the chunks that spread and fade.
        Retrogen.listen();
        LiveChanges.listen();
        WorldEdits.listen();
        EditCommands.declare();
        WorldEvents.listen();
        // The weathers the world events bring: what falls, the fog, the wind, and what they do out in them.
        Weathers.listen();
        // Places, dimensions and bodies with laws of their own: gravity, time, light, space and matter.
        Physics.listen(modEventBus);
        // Sealed rooms: closed spaces found and their air kept, by real gases.
        Rooms.listen(modEventBus);
        // How sound travels: each player told the air round their head, and blasts ringing ears.
        Acoustics.listen();
        // Hazards: kinds of harm and what resists them, and what builds up in a body from where it is.
        Resistances.listen(modEventBus);
        Hazards.listen(modEventBus);
        HazardMaps.listen(modEventBus);
        ItemHazards.listen(modEventBus);
        // Radiation: given off by blocks, weakened by the distance and by the density of each block on its way.
        BlockDensity.listen(modEventBus);
        Shielding.listen(modEventBus);
        Radiation.listen();
        // Pollution: amounts over the chunks that spread, fade, are taken up by plants and harm bodies and the land.
        Pollution.listen();
        Lungs.listen();
        // A body's warmth: its core by the place through what it wears, hypothermia and heatstroke past its bands.
        Clothing.listen(modEventBus);
        BlockTemperatures.listen(modEventBus);
        BodyTemperature.listen(modEventBus);
        // The world showing contamination: grass dying and plants withering where it is high.
        Withering.listen();
        // Explosions decided by their energy: a shock wave at the speed of sound through blocks by their strength.
        Explosions.listen(modEventBus);
        // Fire that spreads by what burns, the oxygen and the wind, leaves its remains, and is put out by class.
        Fires.listen(modEventBus);
        // Moving structures: real blocks kept in the yard, drawn and collided where the structure is, moved by rule.
        MovingStructures.listen(modEventBus);
        // Their physics: mass, engines, wings, wheels, floating and balloons, as data, and what they meet.
        BlockPhysics.listen(modEventBus);
        // What blocks do aboard them, as data: seats and helms, docking clamps, decouplers and tools.
        BlockAboard.listen(modEventBus);
        // Vehicles as data: their parts, fuels and seats, their entity and items, and what pipes and hoppers reach.
        Vehicles.listen(modEventBus);
        // Modules in worn things: the hosts' sockets, the modules' effects and the energy they spend each second.
        CoreModuleEffects.declare();
        Modules.listen();
        // Tools: areas and veins mined, what they work on and their heat, building tools and copies of regions.
        Tools.listen();
        // Views from elsewhere: scenes along camera paths, the view of an entity or from a point.
        CameraViews.listen(modEventBus);
        // Screens on blocks' faces that a player presses with the crosshair.
        ScreenTouches.listen(modEventBus);
        ChunkFields.listen(modEventBus);
        EndSettings.declare();
        OverheadLines.declare();
        // The menu every machine of every mod opens, behind its five tabs, and what a wrench shows of a machine.
        Machines.declare();
        MachineWrench.declare();
        // What a worn-out catalyst becomes, as data.
        CatalystWear.listen(modEventBus);
        // The fonts every mod can draw in, declared with the licence and credit they come under.
        CoreFonts.declare();
        // The manuals' binder style, which every manual of the series is drawn in unless it brings its own.
        CoreGuide.declare();
        // A page of a manual torn out as a sheet, and the tip of the first time an item a manual explains is picked up.
        TornSheets.declare();
        GuideTips.declare();
        // The series' one creative tab of what its mods share, showing only what the installed mods use.
        JsTechTab.declare();
        // The open catalogue every mod adds to: the forms, the properties, the mineral families and the rocks ores sit
        // in; the series' own materials; the values data packs give them; and the data of the things the installed
        // mods use, made as the server loads its data.
        Forms.declare();
        MaterialProperties.declare();
        Thermal.declare();
        MaterialHazards.declare();
        MineralFamilies.declare();
        HostRocks.declare();
        MaterialNames.declare();
        SeriesMaterials.declare();
        ElementNaming.declare();
        MaterialValues.declare();
        MaterialData.declare();
        VariantData.declare();
        // The veins of the series' metals, which stand in for the game's scattered iron, copper and gold.
        SeriesDeposits.declare();
        // Where a belt or a chain remembers the first pulley it was used on.
        ShaftLinkItem.declare();
        // What the manuals sound like: the game's own book and button sounds, in the Manuals channel.
        GuideSounds.declare();
        // What the notifications sound like, one of the game's own sounds for each level.
        NotificationSounds.declare();
        // The ring in the ears after a blast, a cue silent until a pack gives it its sound, and the boom of a blast.
        AcousticCues.declare();
        Explosions.declare();
        // The clamp meter, the series' instrument that reads the energy through a line or a block, and its page.
        ClampMeter.declare();
        SharedEnergy.declare();
        EnergyConverters.declare();
        SharedContainment.declare();
        StructureProjector.declare();
        // The Space Persistor, the series' chunk loader, which keeps any mod's machines working far away.
        SpacePersistor.declare();
        // The Atmosphere Detector, which reads the air of a sealed room or a place and shows the room.
        AtmosphereDetector.declare();
        // The hazard devices (the Geiger counter, the dosimeter, the hazard map) and the grass contamination kills.
        HazardDevices.declare();
        WitheredGrass.declare();
        // The series' ionizing radiation, its contamination, and dissonance where the laws depart from ours.
        IonizingRadiation.declare();
        Dissonance.declare();
        // The series' kinds of pollution: soot, acid rain, heavy metals, fallout, and the world's carbon dioxide.
        SeriesPollution.declare();
        // What fire leaves, the extinguishers, the sprinkler and the detectors, and the soot fires give off.
        CharredBlocks.declare();
        FireSafety.declare();
        SeriesFire.declare();
        MultiblockBuilder.declare();
        SharedTools.declare();
        // The physics tool, and the shaped recipe a setting turns on and off that makes it.
        SwitchedShapedRecipe.declare();
        // The crafting recipes that ask for a piece of knowledge, shaped and shapeless.
        KnowledgeShapedRecipe.declare();
        KnowledgeShapelessRecipe.declare();
        PhysicsTool.declare();
        NetworkProbe.declare();
        SignalProbe.declare();
        SharedPipes.declare();
        // What content packs declare in files rather than in Java, read as the game starts, after the catalogue.
        ContentPacks.declare(modEventBus);
        // What the scripts of KubeJS and CraftTweaker reach of the Core, as JsCore.
        CoreItems.CONTENT.scriptBinding(CoreScripts.NAME, CoreScripts.class);
        CoreItems.register(modEventBus);
        // The units energy is counted in, FE first, and the energy an item holds; then J's Energy, the series' own,
        // with its joule and the grids of its voltage classes.
        CoreEnergy.register(modEventBus);
        JsEnergy.register(modEventBus);
        // The components an item keeps its mode, its fluid and its stacks in.
        ItemStates.register(modEventBus);
        // The Core's own keys, declared before the player's game makes its key bindings.
        CoreKeys.declare();
        VehicleControlKeys.declare();
        // Soft integrations: each one checks for its mod and stays a no-op without it.
        MekanismIntegration.bootstrap();
        FtbTeamsIntegration.bootstrap();
        FtbQuestsIntegration.bootstrap(modEventBus);
        CraftTweakerIntegration.bootstrap();
        FtbChunksIntegration.bootstrap();
        OpacIntegration.bootstrap();
        AccessoriesIntegration.bootstrap();
        CuriosIntegration.bootstrap();
        CreateIntegration.bootstrap();
        SableIntegration.bootstrap();
        // The series' permissions, asked of the server's permission system, and what each player may have.
        CorePermissions.listen();
        PlayerLimits.register();
        // The balance of the Core's systems in the world, and the series' Operations engine's own settings.
        ConfigFiles.register(CoreConfigKeys.FILE, modEventBus, modContainer);
        ConfigFiles.register(OperationSettings.FILE, modEventBus, modContainer);
        // J's Energy's settings: whether it is on, what the wrong voltage does, and how it speaks FE.
        ConfigFiles.register(JsEnergySettings.FILE, modEventBus, modContainer);
        // What the Space Persistor's chunks cost in power, which may be nothing.
        ConfigFiles.register(PersistorSettings.FILE, modEventBus, modContainer);
        ConfigFiles.register(OperatorStructures.FILE, modEventBus, modContainer);
        // Which item stands for a material several mods make, in a world's recipes and drops.
        ConfigFiles.register(UnificationSettings.FILE, modEventBus, modContainer);
        // What the Core makes of other mods' rocks, woods and metals, read as the game starts.
        ConfigFiles.register(VariantSettings.FILE, modEventBus, modContainer);
        // Whether a world's players choose between recipes that overlap, or leave that to Polymorph.
        ConfigFiles.register(RecipeChoiceSettings.FILE, modEventBus, modContainer);
        // Developer mode: the player's key, and the world's, which its players are sent.
        ConfigFiles.register(DeveloperMode.PLAYER_FILE, modEventBus, modContainer);
        ConfigFiles.register(DeveloperMode.SERVER_FILE, modEventBus, modContainer);
        // A player's sound preferences, read on their own game only.
        ConfigFiles.register(AudioSettings.FILE, modEventBus, modContainer);
        // The styles a player chose for the screens of every mod, read on their own game only.
        ConfigFiles.register(StyleSettings.FILE, modEventBus, modContainer);
        // Where a player's notifications appear and what they silenced, read on their own game only.
        ConfigFiles.register(NotificationSettings.FILE, modEventBus, modContainer);
        // Where a player's hazard devices show on their HUD and how big, read on their own game only.
        ConfigFiles.register(HazardDeviceSettings.FILE, modEventBus, modContainer);
        // Which of the Core's systems of looks step aside for a mod that does their job, on a player's game only.
        ConfigFiles.register(StepAside.FILE, modEventBus, modContainer);
        // How light looks on a player's game: coloured light, dynamic light, glow and their reach.
        ConfigFiles.register(LightingSettings.FILE, modEventBus, modContainer);
        // How effects look on a player's game: particles at once, the screen's shake and flash, heat haze.
        ConfigFiles.register(EffectSettings.FILE, modEventBus, modContainer);
        // Whether alike models, turning parts and particles are drawn many at once on a player's game.
        ConfigFiles.register(DrawingSettings.FILE, modEventBus, modContainer);
        ConfigFiles.register(CameraSettings.FILE, modEventBus, modContainer);
        // What the series adds to every item's tooltip on a player's game: the mod's name, the bar of wear.
        ConfigFiles.register(TooltipSettings.FILE, modEventBus, modContainer);
        // What makes the screens easier on a player: less movement.
        ConfigFiles.register(AccessibilitySettings.FILE, modEventBus, modContainer);
        // Where a player put the HUD's elements in the HUD editor, read on their own game only.
        ConfigFiles.register(HudSettings.FILE, modEventBus, modContainer);
        // Whether the manuals' tips come to a player, read on their own game only.
        ConfigFiles.register(GuideTips.FILE, modEventBus, modContainer);
        // The kinds of component a screen's file may hold, before any file is read.
        CoreElements.declare();
        ControlElements.declare();
        // What the Core keeps with a world: each dimension's data networks, and the ledger of its recordings.
        CoreStates.register(NetworkRegistry.NETWORKS);
        MediaLedgers.register();
        // How far each player has come along each progression axis, the series' two first, and the advancement
        // triggers every mod stands on.
        SeriesAxes.declare();
        PlayerProgress.register();
        TeamProgress.register();
        ProgressionTriggers.listen();
        // Research for any mod: which mods turned theirs off, sent to every player, and what that leaves open.
        Discoveries.register();
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
        // What those who build a modpack read of the game: the dumps, the reference, and what a reload did.
        ModpackDumps.declare();
        ReloadReport.listen();
        MediaCommand.declare();
        LogCommand.declare();
        DiagnosticCommands.declare();
        InspectCommands.declare();
        // The Core's part of /jstech health and of /jstech inspect, before any mod adds its own, and what its
        // operators are notified of.
        CoreHealth.declare();
        CoreInspectors.declare();
        NetworkInspection.declare();
        DiagnosticNotifications.declare();
        // The multiblock patterns of every mod, read from data and declared in code.
        MultiblockPatterns.register();
        DataMachines.register();
        // What each dimension is like to stand in, and what its sky holds, read from data.
        DimensionRulesData.register();
        Skies.register();
        // The conditions and tasks every robot knows, before a world reads its robots back.
        CoreRobotConditions.declare();
        CoreRobotTasks.declare();
        // How creatures live as data: what they do, where they appear and the air they breathe.
        CoreCreatureBehaviours.declare();
        Creatures.listen(modEventBus);
        // The chunks machines keep loaded for their owners, under the game's own forced-chunk file.
        modEventBus.addListener(RegisterTicketControllersEvent.class, ChunkLoaders::onRegisterControllers);
        AwayOwners.listen();
        // What each part of the Core answers on the mod's bus and the game's, declared here one class at a time
        // rather than found by a scan for annotations, so what runs is what this list says.
        Advancements.listen();
        AudioNetwork.listen(modEventBus);
        Bandwidth.listen();
        BigPayloads.listen(modEventBus);
        CableBreaking.listen();
        ClampMeter.listen(modEventBus);
        HazardDevices.listen(modEventBus);
        FireSafety.listen(modEventBus);
        NetworkProbe.listen(modEventBus);
        SignalProbe.listen(modEventBus);
        PhysicsTool.listen(modEventBus);
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
        TornSheets.listen(modEventBus);
        ManualLecterns.listen(modEventBus);
        HeatPorts.listen(modEventBus);
        BlockConductivity.listen(modEventBus);
        MechanicalPorts.listen(modEventBus);
        Conveyors.listen(modEventBus);
        NetworkMembers.listen();
        PeripheralReconciler.listen();
        CoreEnergy.listen(modEventBus);
        ConfigSync.listen(modEventBus);
        CoreParts.listen(modEventBus);
        CoreStates.listen();
        CostMap.listen();
        DataRegistries.listen(modEventBus);
        DiagnosticReports.listen();
        DimensionEffects.listen();
        DayClocks.listen();
        FieldCapabilities.listen(modEventBus);
        CoreGrids.listen();
        // The series' data network keeps the grid of data cables with itself.
        DataGrids.declare();
        GuideGifts.listen();
        Holograms.listen(modEventBus);
        InspectWatch.listen(modEventBus);
        ThingTests.listen(modEventBus);
        StudioServices.listen(modEventBus);
        // The studio's logs kept from the start on both sides, and the MCP server of a dedicated server.
        StudioLogs.listen();
        McpOnServer.listen(modEventBus);
        JsCoreDataGenerators.listen(modEventBus);
        JsTechTab.listen(modEventBus);
        KeyActions.listen(modEventBus);
        CatalogueItems.listen(modEventBus);
        MaterialLiquids.listen(modEventBus);
        MaterialUses.listen(modEventBus);
        // After the materials' uses, so the Core's own blocks of metal are there to find.
        Variants.listen(modEventBus);
        SimplePath.register(modEventBus);
        DataConditions.register(modEventBus);
        UnifiedDrops.register(modEventBus);
        LootChanges.register(modEventBus);
        RecipeChoices.listen(modEventBus);
        MediaNetwork.listen(modEventBus);
        MediaServerEvents.listen();
        ModelAnimations.listen(modEventBus);
        Notifications.listen(modEventBus);
        LevelStructures.listen();
        OwnedProtection.listen();
        Protection.listen();
        RuntimeDimensions.listen();
        DimensionTravel.listen(modEventBus);
        RuntimePacks.listen(modEventBus);
        Sections.listen();
        CommandRoot.listen();
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
                () -> ModLoader.postEvent(new RegisterOperationTypesEvent(OPERATIONS))));
        // Every cable is registered by now: two lines given one lane stop the game here, not in a world.
        modEventBus.addListener(FMLCommonSetupEvent.class, event -> CoreCables.checkLanes());
        modEventBus.addListener(FMLLoadCompleteEvent.class, event -> event.enqueueWork(() -> {
            OPERATIONS.freeze();
            StateTables.freeze();
            Eras.freeze();
            Knowledges.freeze();
        }));
        // Every mod is loaded by now: the log says which of the Core's systems step aside for which.
        modEventBus.addListener(FMLLoadCompleteEvent.class, event -> StepAside.report());
    }
}
