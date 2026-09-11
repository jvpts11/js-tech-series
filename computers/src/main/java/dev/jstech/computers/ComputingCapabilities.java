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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Exposes the Computing module's block capabilities.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class ComputingCapabilities {

    private ComputingCapabilities() {
    }

    @SubscribeEvent
    public static void register(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ComputingModule.TANK_BE.get(),
                (be, side) -> be.fluidHandler());
        // The Gateway's buffer, for the chest, hopper or turtle on its sides, top or bottom.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ComputingModule.NETWORK_GATEWAY_BE.get(),
                (be, side) -> be.bufferFor(side));
        // The Gateway's ComputerCraft face, only when there is a ComputerCraft to face.
        ComputerCraftIntegration.registerCapabilities(event);
    }
}
