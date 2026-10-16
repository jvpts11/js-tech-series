/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core;

import dev.jstech.core.client.audio.AlertSigns;
import dev.jstech.core.client.audio.AudioClientSetup;
import dev.jstech.core.client.audio.AudioDebugLines;
import dev.jstech.core.client.audio.AudioKeys;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.audio.SoundDirector;
import dev.jstech.core.client.audio.media.MediaClientEvents;
import dev.jstech.core.client.audio.mixer.SoundMixerEntry;
import dev.jstech.core.client.cable.CableClientEvents;
import dev.jstech.core.client.cable.CableEndScreen;
import dev.jstech.core.client.cable.CableColours;
import dev.jstech.core.client.cable.CableDragClient;
import dev.jstech.core.client.cable.CableFlowRenderer;
import dev.jstech.core.client.conveyor.ConveyorRenderer;
import dev.jstech.core.client.cable.FlowSightClient;
import dev.jstech.core.client.config.ConfigChoosers;
import dev.jstech.core.client.config.ConfigClient;
import dev.jstech.core.client.config.CoreConfigScreen;
import dev.jstech.core.client.connect.ConnectedPacks;
import dev.jstech.core.client.data.DataRegistryClient;
import dev.jstech.core.client.developer.LiveReloadClient;
import dev.jstech.core.client.diagnostic.DiagnosticsClient;
import dev.jstech.core.client.fluid.FluidsClient;
import dev.jstech.core.client.font.CoreFontClient;
import dev.jstech.core.client.gui.component.Grounds;
import dev.jstech.core.client.gui.data.ControlElementBuilders;
import dev.jstech.core.client.gui.data.CoreElementBuilders;
import dev.jstech.core.client.gui.data.ScreenFiles;
import dev.jstech.core.client.gui.style.StyleChooser;
import dev.jstech.core.client.gui.style.StyleLoader;
import dev.jstech.core.client.gui.tooltip.ClientRichLine;
import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.heat.HeatGlowClient;
import dev.jstech.core.client.heat.ThermalViewClient;
import dev.jstech.core.client.hologram.HologramView;
import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.client.input.MenuSwapCursor;
import dev.jstech.core.client.item.ItemStateClient;
import dev.jstech.core.client.light.LightingClient;
import dev.jstech.core.client.live.LiveScreens;
import dev.jstech.core.client.live.TubeFilter;
import dev.jstech.core.client.machine.MachineScreen;
import dev.jstech.core.client.machine.MachineWrenchClient;
import dev.jstech.core.client.material.MaterialArt;
import dev.jstech.core.client.material.MaterialTooltips;
import dev.jstech.core.client.model.CoreModels;
import dev.jstech.core.client.model.PartMotions;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitClock;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.multiblock.FormedPartsClient;
import dev.jstech.core.client.motion.CoreMotionClient;
import dev.jstech.core.client.motion.GreyFilter;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.client.network.TransferClient;
import dev.jstech.core.client.notification.NotificationsClient;
import dev.jstech.core.client.overhead.OverheadWireRenderer;
import dev.jstech.core.client.palette.CorePaletteClient;
import dev.jstech.core.client.state.ClientStateEvents;
import dev.jstech.core.client.vehicle.VehicleControls;
import dev.jstech.core.gui.style.StyleSettings;
import dev.jstech.core.jstechseries.energy.client.ClampMeterClient;
import dev.jstech.core.jstechseries.energy.client.EnergyConvertersClient;
import dev.jstech.core.jstechseries.builder.client.BuilderClient;
import dev.jstech.core.jstechseries.projector.client.ProjectorClient;
import dev.jstech.core.jstechseries.probe.client.NetworkProbeClient;
import dev.jstech.core.jstechseries.probe.client.SignalProbeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * The Core's start on a player's game: its settings on the Core's own settings screen, reached from the mods list,
 * each setting under the name and the tooltip its language file gives it, and what each part of the Core answers only
 * on a player's game.
 */
@Mod(value = JsCore.MODID, dist = Dist.CLIENT)
public final class JsCoreClient {

    public JsCoreClient(final IEventBus modEventBus, final ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, CoreConfigScreen::new);
        // What each part of the Core answers on a player's game, on the mod's bus and the game's, declared here one
        // class at a time rather than found by a scan for annotations, so what runs is what this list says.
        AlertSigns.listen(modEventBus);
        AudioClientSetup.listen(modEventBus);
        AudioDebugLines.listen(modEventBus);
        AudioKeys.listen(modEventBus);
        AudioMixer.listen();
        CableClientEvents.listen();
        CableDragClient.listen();
        ClampMeterClient.listen(modEventBus);
        CableColours.listen(modEventBus);
        CableEndScreen.listen(modEventBus);
        CableFlowRenderer.listen(modEventBus);
        ConveyorRenderer.listen(modEventBus);
        FlowSightClient.listen(modEventBus);
        ClientStateEvents.listen();
        EnergyConvertersClient.listen(modEventBus);
        ProjectorClient.listen(modEventBus);
        BuilderClient.listen(modEventBus);
        ConfigClient.listen();
        ConnectedPacks.listen(modEventBus);
        CoreFontClient.listen(modEventBus);
        CoreModels.listen(modEventBus);
        KitModels.listen(modEventBus);
        KitAnimations.listen();
        KitClock.listen();
        FormedPartsClient.listen(modEventBus);
        CoreMotionClient.listen(modEventBus);
        CorePaletteClient.listen(modEventBus);
        DataRegistryClient.listen();
        DiagnosticsClient.listen();
        FluidsClient.listen(modEventBus);
        GreyFilter.listen(modEventBus);
        Grounds.listen();
        GuideClient.listen(modEventBus);
        HeatGlowClient.listen();
        ThermalViewClient.listen(modEventBus);
        HologramView.listen();
        HudElements.listen(modEventBus);
        ItemStateClient.listen(modEventBus);
        KeyActionsClient.listen(modEventBus);
        LightingClient.listen(modEventBus);
        LiveScreens.listen();
        MachineScreen.listen(modEventBus);
        MachineWrenchClient.listen(modEventBus);
        MaterialTooltips.listen();
        MediaClientEvents.listen();
        MenuSwapCursor.listen();
        MotionClock.listen();
        NetworkProbeClient.listen(modEventBus);
        SignalProbeClient.listen(modEventBus);
        NotificationsClient.listen(modEventBus);
        OverheadWireRenderer.listen();
        PartMotions.listen();
        ScreenFiles.listen(modEventBus);
        SoundDirector.listen();
        SoundMixerEntry.listen();
        StyleLoader.listen(modEventBus);
        TransferClient.listen();
        TubeFilter.listen(modEventBus);
        VehicleControls.listen(modEventBus);
        // In a development run, the mods' assets reloaded as they change.
        LiveReloadClient.listen();
        // What draws each of the Core's kinds of screen component, a rich line of a tooltip, and the chooser of the
        // player's styles.
        CoreElementBuilders.register();
        ControlElementBuilders.register();
        ClientRichLine.listen(modEventBus);
        ConfigChoosers.register(StyleSettings.CHOOSER, new StyleChooser());
        // How the things of the materials the installed mods use look, drawn as the resources load.
        MaterialArt.declare();
    }
}
