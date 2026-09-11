/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import dan200.computercraft.api.ComputerCraftAPI;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.gateway.IGatewayBridge;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Soft integration with CC: Tweaked: when the mod is present, the Network Gateway is a ComputerCraft
 * peripheral and the two families of computers talk through it. Nothing here touches a ComputerCraft class
 * unless {@link #isLoaded()} is true, so the mod runs unchanged without CC: Tweaked.
 */
public final class ComputerCraftIntegration {

    public static final String MOD_ID = "computercraft";
    /** What a Gateway says on its screen when the mod it bridges to is not there. */
    public static final String MISSING = "CC: Tweaked is not installed";

    private ComputerCraftIntegration() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /** The installed CC: Tweaked version, or an empty string when the mod is absent. */
    public static String installedVersion() {
        return isLoaded() ? version() : "";
    }

    /** The ComputerCraft side of a Gateway, or null without the mod. */
    @Nullable
    public static IGatewayBridge bridge(final NetworkGatewayBlockEntity gateway) {
        return isLoaded() ? newBridge(gateway) : null;
    }

    /** Registers the Gateway's ComputerCraft capabilities; a no-op without the mod. */
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        if (isLoaded()) {
            registerBridgeCapabilities(event);
        }
    }

    // Each kept in its own method so the classes behind it are only resolved once the mod is known to be there.
    private static String version() {
        return ComputerCraftAPI.getInstalledVersion();
    }

    private static IGatewayBridge newBridge(final NetworkGatewayBlockEntity gateway) {
        return new GatewayBridge(gateway);
    }

    private static void registerBridgeCapabilities(final RegisterCapabilitiesEvent event) {
        ComputerCraftCapabilities.register(event);
    }
}
