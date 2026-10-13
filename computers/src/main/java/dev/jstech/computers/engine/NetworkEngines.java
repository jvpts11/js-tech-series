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

    /**
     * NextgreIQL, the explicit engine, from the Legacy on: it shows the plan it made and how it ran, takes hints that
     * change it, weighs the plans it could make by what they cost, and takes rules, hints and statistics from other
     * mods into its planner.
     */
    public static final EngineDef NEXTGRE_IQL = new EngineDef(Programs.NEXTGRE_IQL, "NextgreIQL",
            Map.of(HardwareEra.LEGACY, "7.0", HardwareEra.TRANSITION, "8.3", HardwareEra.STANDARD, "9.0",
                    HardwareEra.ADVANCED, "16"),
            Set.of(EngineCapability.EXPLAIN, EngineCapability.PLANNER_HINTS, EngineCapability.EXTENSIONS));

    /**
     * Prophet YourIQL, the state-oriented engine, from the Legacy on: told what state the network is to keep, it works
     * out the Operations to get there and stay there, counting what is already on its way, and it reacts to what
     * changed rather than going over everything again.
     */
    public static final EngineDef PROPHET_YOURIQL = new EngineDef(Programs.YOURIQL, "YourIQL",
            Map.of(HardwareEra.LEGACY, "3.23", HardwareEra.TRANSITION, "5.0", HardwareEra.STANDARD, "5.6",
                    HardwareEra.ADVANCED, "8.0"),
            Set.of(EngineCapability.DECLARATIVE_STATE, EngineCapability.SUBSCRIPTIONS));

    private static final Map<ResourceLocation, INetworkEngine> ENGINES = new LinkedHashMap<>();

    private NetworkEngines() {
    }

    /**
     * Adds an engine. Its package is registered as a program like any other, with the engine kind; this says what
     * the engine is. Two engines on one id are refused, and so is one added after the loading is done.
     */
    public static void register(final EngineDef def) {
        register(new IqlCoreEngine(def));
    }

    /**
     * Adds an engine that brings its own way of planning: the series' engines past the Midsoft IQL Server, whose
     * dialects and planners are their own. The same rules hold as for {@link #register(EngineDef)}.
     */
    public static void register(final INetworkEngine engine) {
        final EngineDef def = engine.def();
        if (OsRegistry.isFrozen()) {
            JsComputers.LOGGER.warn("The engine {} was not registered: engines are only added while the game loads",
                    def.program());
            return;
        }
        if (ENGINES.containsKey(def.program())) {
            throw new IllegalStateException("An engine is already registered as " + def.program());
        }
        ENGINES.put(def.program(), engine);
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
