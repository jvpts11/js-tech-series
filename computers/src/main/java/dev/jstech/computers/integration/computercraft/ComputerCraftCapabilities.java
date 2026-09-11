/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import dan200.computercraft.api.network.wired.WiredElementCapability;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.jstech.computers.ComputingModule;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * The Gateway's front face as ComputerCraft sees it: the peripheral a computer or a wired modem against
 * that face wraps, and the wired-network node a networking cable against it joins. Nothing on the other
 * five faces.
 */
final class ComputerCraftCapabilities {

    private ComputerCraftCapabilities() {
    }

    static void register(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), ComputingModule.NETWORK_GATEWAY_BE.get(),
                (be, side) -> be.bridge() instanceof GatewayBridge bridge && (side == null || side == be.facing())
                        ? bridge.peripheral() : null);
        event.registerBlockEntity(WiredElementCapability.get(), ComputingModule.NETWORK_GATEWAY_BE.get(),
                (be, side) -> be.bridge() instanceof GatewayBridge bridge && (side == null || side == be.facing())
                        ? bridge.element() : null);
    }
}
