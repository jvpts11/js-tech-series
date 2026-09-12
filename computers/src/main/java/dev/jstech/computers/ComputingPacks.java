/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.integration.computercraft.ComputerCraftIntegration;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * The data packs this mod brings with it.
 *
 * <p>Only one so far, and it holds one file: the agent a ComputerCraft computer runs to answer this
 * network. It is a pack rather than anything of ours because that is how their computers are given
 * files, and it is only offered when they are installed to answer it.
 */
@EventBusSubscriber(modid = JsComputers.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ComputingPacks {

    private ComputingPacks() {
    }

    @SubscribeEvent
    public static void addPacks(final AddPackFindersEvent event) {
        ComputerCraftIntegration.addAgentPack(event);
    }
}
