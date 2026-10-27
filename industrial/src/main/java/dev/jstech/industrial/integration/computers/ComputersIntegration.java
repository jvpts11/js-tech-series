/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.optional.OptionalMod;
import java.util.function.BiConsumer;
import net.neoforged.bus.api.IEventBus;

/**
 * The work with J's Computers, when it is installed beside this mod: the Industrial Controller Computer of each era,
 * the Fieldbus Cards it reads machines with, and the industrial control cable it reads them over. This class is the
 * way in, behind the guard; it names nothing of J's Computers, so nothing of it is loaded when it is not installed.
 */
public final class ComputersIntegration {

    /** J's Computers. */
    public static final OptionalMod COMPUTERS = OptionalMod.of("jsc");

    private ComputersIntegration() {
    }

    /** Declares what comes with J's Computers, when it is installed; from the mod's constructor. */
    public static void declare(final IEventBus modEventBus) {
        COMPUTERS.ifLoaded(() -> () -> IndustrialControllers.declare(modEventBus));
    }

    /** Draws what comes with J's Computers on a player's game, when it is installed; from the client entry point. */
    public static void client(final IEventBus modEventBus) {
        COMPUTERS.ifLoaded(() -> () -> IndustrialControllersClient.listen(modEventBus));
    }

    /** Hands the language file the names of what comes with J's Computers, when it is installed; from the datagen. */
    public static void names(final BiConsumer<String, String> add) {
        COMPUTERS.ifLoaded(() -> () -> MachineMonitor.names(add));
    }

    /**
     * Writes the pages of what comes with J's Computers into the mod's chapter, in {@code section}, when it is
     * installed; a game without it leaves them out of its manuals.
     */
    public static void pages(final ModGuide guide, final ModGuide.SectionRef section) {
        COMPUTERS.ifLoaded(() -> () -> ControllerPages.declare(guide, section));
    }
}
