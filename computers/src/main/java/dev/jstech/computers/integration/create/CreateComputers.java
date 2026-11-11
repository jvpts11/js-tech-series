/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.create;

import dev.jstech.core.integration.PinnedVersions;
import dev.jstech.core.integration.create.CreateIntegration;
import net.neoforged.bus.api.IEventBus;

/**
 * J's Computers with Create (computers/docs/design/implementation.md, "Create"), behind its guard: computers ride
 * Create's contraptions and come down shut down, or stay where they are while they do what they cannot lose, and the
 * industrial control cable reads Create's kinetic networks into the {@code create_kinetics} table and sets a Rotation
 * Speed Controller.
 * It starts only on the versions of Create J's Core works with it on; on another it stays off, as J's Core's own work
 * with Create does, and J's Core's log says so.
 */
public final class CreateComputers {

    private static volatile boolean on;

    private CreateComputers() {
    }

    /** Whether J's Computers works with Create: Create is installed, of a version the work is tested against. */
    public static boolean on() {
        return on;
    }

    /** Starts the work with Create from the mod's constructor; a no-op without Create. */
    public static void bootstrap(final IEventBus modBus) {
        if (!CreateIntegration.isLoaded() || !PinnedVersions.covers(CreateIntegration.MOD, CreateIntegration.VERSIONS)) {
            return;
        }
        on = CreateIntegration.MOD.ifLoaded(() -> () -> CreateHooks.start(modBus));
        if (on) {
            KineticTable.declare();
        }
    }
}
