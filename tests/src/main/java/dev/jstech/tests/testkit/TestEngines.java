/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.engine.EngineDef;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

/**
 * A Network Operations Engine registered the way another mod registers one, through the computers' register event,
 * so the engine tests can install one that is not the series' own and swap to it. It offers none of the extras.
 */
public final class TestEngines {

    /** The plain engine's package, which is also its id. */
    public static final ResourceLocation PLAIN = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "plain_engine");

    private TestEngines() {
    }

    /** Registers the plain engine and its package while the game loads. */
    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener(ComputersRegisterEvent.class, event -> {
            event.program(ProgramSpec.of(PLAIN, "plainengine", false, EnumSet.allOf(Platform.class), 8,
                            ProgramKind.SERVICE, 0, HostScope.MAINFRAME)
                    .named("Plain Engine")
                    .described("An engine with nothing past what every engine answers."));
            final Map<HardwareEra, String> versions = new EnumMap<>(HardwareEra.class);
            for (final HardwareEra era : HardwareEra.values()) {
                versions.put(era, "1.0");
            }
            event.engine(new EngineDef(PLAIN, "Plain", versions, Set.of()));
        });
    }
}
