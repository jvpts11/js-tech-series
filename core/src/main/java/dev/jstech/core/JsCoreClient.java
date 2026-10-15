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
import dev.jstech.core.client.cable.CableColours;
import dev.jstech.core.client.config.CoreConfigScreen;
import dev.jstech.core.client.data.DataRegistryClient;
import dev.jstech.core.client.developer.LiveReloadClient;
import dev.jstech.core.client.diagnostic.DiagnosticsClient;
import dev.jstech.core.client.fluid.FluidsClient;
import dev.jstech.core.client.font.CoreFontClient;
import dev.jstech.core.client.gui.component.Grounds;
import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.hologram.HologramView;
import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.client.input.MenuSwapCursor;
import dev.jstech.core.client.item.ItemStateClient;
import dev.jstech.core.client.live.LiveScreens;
import dev.jstech.core.client.live.TubeFilter;
import dev.jstech.core.client.model.CoreModels;
import dev.jstech.core.client.motion.CoreMotionClient;
import dev.jstech.core.client.motion.GreyFilter;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.client.network.TransferClient;
import dev.jstech.core.client.palette.CorePaletteClient;
import dev.jstech.core.client.state.ClientStateEvents;
import dev.jstech.core.client.vehicle.VehicleControls;
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
        CableColours.listen(modEventBus);
        ClientStateEvents.listen();
        CoreFontClient.listen(modEventBus);
        CoreModels.listen(modEventBus);
        CoreMotionClient.listen(modEventBus);
        CorePaletteClient.listen(modEventBus);
        DataRegistryClient.listen();
        DiagnosticsClient.listen();
        FluidsClient.listen(modEventBus);
        GreyFilter.listen(modEventBus);
        Grounds.listen();
        GuideClient.listen(modEventBus);
        HologramView.listen();
        HudElements.listen(modEventBus);
        ItemStateClient.listen(modEventBus);
        KeyActionsClient.listen(modEventBus);
        LiveScreens.listen();
        MediaClientEvents.listen();
        MenuSwapCursor.listen();
        MotionClock.listen();
        SoundDirector.listen();
        SoundMixerEntry.listen();
        TransferClient.listen();
        TubeFilter.listen(modEventBus);
        VehicleControls.listen(modEventBus);
        // In a development run, the mods' assets reloaded as they change.
        LiveReloadClient.listen();
    }
}
