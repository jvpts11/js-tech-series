/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.research;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.data.DeclaredData;
import dev.jstech.core.progression.KnowledgeGate;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.ServerState;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The discoveries of every mod, declared while the game loads ({@link Discovery#builder}), and what each costs: its
 * declared cost unless a data pack's file of its id, under {@code data/<namespace>/jstech/research/}, says otherwise.
 *
 * <p>A discovery exists only where everything it asks for does: one that names a kind of data no installed mod
 * declares, or comes after knowledge no installed mod gives, is not there to research, and nothing it opens is
 * locked by it either.
 *
 * <p>A mod may turn its research off with a setting of its own ({@link #gate}): then none of its discoveries locks
 * anything and none is granted. The server sends every player which mods have turned it off, so the recipe viewers
 * and the manuals show no lock either.
 */
public final class Discoveries {

    private static final Map<ResourceLocation, Discovery> DECLARED = new LinkedHashMap<>();
    private static final DeclaredData<ResearchCost> COSTS = new DeclaredData<>(id("research_costs"), "jstech/research",
            ResearchCost.CODEC);
    /* Each mod's setting saying whether its research locks what its discoveries open, by its namespace. */
    private static final Map<String, BooleanSupplier> GATES = new ConcurrentHashMap<>();
    /* The mods whose research is turned off, as the server last worked out and sent every player. */
    private static final ServerState<List<String>> OFF = CoreState.<List<String>>builder(id("research_off"),
                    Codec.STRING.listOf(), List.of())
            .fileName("jstech_research_off")
            .synced(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
            .server();
    private static final int TICKS_A_SECOND = 20;

    private Discoveries() {
    }

    /** Registers what sends the gates to the players and opens what they leave open, from the Core's constructor. */
    @ApiStatus.Internal
    public static void register() {
        CoreStates.register(OFF);
        KnowledgeGate.openWhen(Discoveries::open);
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, Discoveries::onServerTick);
    }

    /** The discovery of {@code id}, if one is declared. */
    public static synchronized Optional<Discovery> byId(final ResourceLocation id) {
        return Optional.ofNullable(DECLARED.get(id));
    }

    /** Every discovery declared, in the order the mods declared them, whether or not it exists here. */
    public static synchronized List<Discovery> all() {
        return List.copyOf(DECLARED.values());
    }

    /** Every discovery there is to research on the side {@code level} is on, in the order they were declared. */
    public static List<Discovery> existing(final Level level) {
        final List<Discovery> there = new ArrayList<>();
        for (final Discovery discovery : all()) {
            if (exists(level, discovery)) {
                there.add(discovery);
            }
        }
        return there;
    }

    /**
     * Whether {@code discovery} is there to research on the side {@code level} is on: it has a cost, every kind of
     * data the cost names is declared, and everything it comes after is knowledge some installed mod gives.
     */
    public static boolean exists(final Level level, final Discovery discovery) {
        final Optional<ResearchCost> cost = cost(level, discovery.id());
        if (cost.isEmpty()) {
            return false;
        }
        for (final ResourceLocation kind : cost.get().data().keySet()) {
            if (DataKinds.byId(kind).isEmpty()) {
                return false;
            }
        }
        for (final ResourceLocation known : discovery.after()) {
            if (Knowledges.byId(known).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** What the discovery {@code id} costs on the side {@code level} is on: a pack's file, else its declaration. */
    public static Optional<ResearchCost> cost(final Level level, final ResourceLocation id) {
        return COSTS.get(level, id);
    }

    /** Every cost declared in code, by the discovery's id, for the data generation to write as files. */
    public static Map<ResourceLocation, ResearchCost> declaredCosts() {
        return COSTS.declared();
    }

    /**
     * Has {@code required} say whether the research of the mod {@code modid} locks what its discoveries open, from the
     * mod's constructor: its own setting. Off, nothing that asks for one of them is locked, and none is granted.
     */
    public static void gate(final String modid, final BooleanSupplier required) {
        GATES.put(Objects.requireNonNull(modid, "modid"), Objects.requireNonNull(required, "required"));
    }

    /**
     * Whether the research of the mod {@code modid} locks what its discoveries open: on the server by its setting, on a
     * player's game as the server last said. A mod that declared no gate always locks.
     */
    public static boolean required(final @Nullable MinecraftServer server, final String modid) {
        if (server == null) {
            return !OFF.client().contains(modid);
        }
        final BooleanSupplier gate = GATES.get(modid);
        return gate == null || gate.getAsBoolean();
    }

    static synchronized Discovery declare(final Discovery discovery, final ResearchCost cost) {
        if (DECLARED.putIfAbsent(discovery.id(), discovery) != null) {
            throw DeclarationError.of(discovery.id().getNamespace(), "the discovery " + discovery.id(),
                    "is declared twice", "give one of the two another id");
        }
        Knowledges.declare(discovery.knowledge());
        COSTS.declare(discovery.id(), cost);
        return discovery;
    }

    /* Whether the knowledge {@code id} is a discovery whose mod turned its research off. */
    private static boolean open(final @Nullable MinecraftServer server, final ResourceLocation id) {
        return byId(id).isPresent() && !required(server, id.getNamespace());
    }

    /* Once a second, which mods have turned their research off, sent to every player when it changes. */
    private static void onServerTick(final ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % TICKS_A_SECOND != 0 || GATES.isEmpty()) {
            return;
        }
        final TreeSet<String> off = new TreeSet<>();
        GATES.forEach((modid, required) -> {
            if (!required.getAsBoolean()) {
                off.add(modid);
            }
        });
        OFF.set(event.getServer(), List.copyOf(off));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
