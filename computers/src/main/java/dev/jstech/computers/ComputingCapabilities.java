/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.integration.computercraft.ComputerCraftIntegration;
import dev.jstech.core.signal.Signals;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Exposes the Computing module's block capabilities that differ by side; an inventory, energy store or tank offered
 * on every side is declared as an exposed field of its block entity instead.
 */
public final class ComputingCapabilities {

    private ComputingCapabilities() {
    }

    /** Hands the mod's bus what this answers, once, from the mod's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, ComputingCapabilities::register);
    }

    public static void register(final RegisterCapabilitiesEvent event) {
        // The Gateway's buffer, for the chest, hopper or turtle on its sides, top or bottom.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ComputingModule.NETWORK_GATEWAY_BE.get(),
                (be, side) -> be.bufferFor(side));
        // The Gateway's ComputerCraft face, only when there is a ComputerCraft to face.
        ComputerCraftIntegration.registerCapabilities(event);
        // A Redstone Interface says what it reads or emits to whatever reads the Core's signals.
        Signals.offer(event, ComputingModule.REDSTONE_INTERFACE_BE.get());
    }
}
