/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.tier.HardwareEra;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Every Network Operations Engine a Mainframe can install, by the id of the package that installs it.
 *
 * <p>Engines are added while the game loads, through the same event as programs, and the list closes with the
 * programs' when the loading is done. An engine another mod registers answers the way the Midsoft IQL Server does
 * until that mod brings a planner of its own, so a network never meets an engine that cannot do the work.
 */
public final class NetworkEngines {

    /**
     * The Midsoft IQL Server: the engine every new Mainframe ships with, running, in the version of the Mainframe's
     * age. It speaks IQL and keeps views and procedures.
     */
    public static final EngineDef MIDSOFT_IQL_SERVER = new EngineDef(Programs.IQL_ENGINE, "IQL",
            Map.of(HardwareEra.VINTAGE, "4.2", HardwareEra.LEGACY, "2000", HardwareEra.TRANSITION, "2008",
                    HardwareEra.STANDARD, "2012", HardwareEra.ADVANCED, "2022"),
            Set.of(EngineCapability.PROCEDURES_AND_VIEWS));

    private static final Map<ResourceLocation, INetworkEngine> ENGINES = new LinkedHashMap<>();

    private NetworkEngines() {
    }

    /**
     * Adds an engine. Its package is registered as a program like any other, with the engine kind; this says what
     * the engine is. Two engines on one id are refused, and so is one added after the loading is done.
     */
    public static void register(final EngineDef def) {
        if (OsRegistry.isFrozen()) {
            JsComputers.LOGGER.warn("The engine {} was not registered: engines are only added while the game loads",
                    def.program());
            return;
        }
        if (ENGINES.containsKey(def.program())) {
            throw new IllegalStateException("An engine is already registered as " + def.program());
        }
        ENGINES.put(def.program(), new IqlCoreEngine(def));
    }

    /** The engine installed by {@code program}, or {@code null} when that program installs none. */
    @Nullable
    public static INetworkEngine get(final ResourceLocation program) {
        return ENGINES.get(program);
    }

    /** What the engine installed by {@code program} is, or {@code null} when that program installs none. */
    @Nullable
    public static EngineDef def(final ResourceLocation program) {
        final INetworkEngine engine = ENGINES.get(program);
        return engine == null ? null : engine.def();
    }

    /** Whether {@code program} installs an engine. */
    public static boolean isEngine(final ResourceLocation program) {
        return ENGINES.containsKey(program);
    }

    /** Every engine, in the order they were registered. */
    public static Collection<INetworkEngine> all() {
        return Collections.unmodifiableCollection(ENGINES.values());
    }
}
