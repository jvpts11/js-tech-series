/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.tool;

import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.item.ItemStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Tools on a player's game: the mode key held a moment opens the ring of the item's modes, a tap passes to the next as
 * before; the HUD shows the item's name with its mode and what it works on, and the charge and heat under the
 * crosshair; icons carry their bars and the mode's letter; and the world shows what a tool will mine or place.
 */
public final class ToolClient {

    /** How long the mode key is held before the ring opens, in ticks: a press shorter is a tap. */
    public static final int HOLD_TICKS = 6;

    private ToolClient() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> KeyActionsClient.onHold(CoreKeys.CHANGE_ITEM_MODE,
                HOLD_TICKS, ToolClient::openRing));
        modBus.addListener(RegisterGuiLayersEvent.class, ToolHud::onRegisterLayers);
        modBus.addListener(RegisterItemDecorationsEvent.class, ToolIconBars::onRegister);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, ToolHud::onTick);
        NeoForge.EVENT_BUS.addListener(RenderGuiLayerEvent.Pre.class, ToolHud::onLayer);
        NeoForge.EVENT_BUS.addListener(RenderHighlightEvent.Block.class, ToolPreviews::onHighlight);
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, ToolPreviews::onStage);
    }

    /** Opens the ring of the modes of the item in the main hand, when it has modes and no screen is open. */
    public static void openRing() {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player != null && minecraft.screen == null && ItemStates.of(player.getMainHandItem()).hasModes()) {
            minecraft.setScreen(new ModeRadialScreen(player.getMainHandItem()));
        }
    }
}
