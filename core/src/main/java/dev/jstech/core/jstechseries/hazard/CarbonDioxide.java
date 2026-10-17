/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.hazard.HazardBalance;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.ServerState;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The carbon dioxide in the whole world's air, in parts per million: its real effect is on the whole planet, not on a
 * chunk, so it is one number for the server, kept with the world and shown to every player. It starts at the air's
 * before industry and rises by what the mods' machines burn; what it does is left to come.
 */
@TextHolder
public final class CarbonDioxide {

    public static final TextKey SHOWN = TextKey.of("jscore.pollution.carbon_dioxide",
            "Carbon dioxide in the whole world: %s ppm");
    /** The air's before industry, in parts per million. */
    public static final double BEFORE_INDUSTRY = 280;
    /** How much one kilogram given off raises it, in parts per million: a world's air is far smaller than Earth's. */
    public static final double PER_KILOGRAM = 1.0E-7;
    /** How much there is, kept with the world and sent to every player. */
    public static final ServerState<Double> PPM = CoreState.builder(ResourceLocation.fromNamespaceAndPath(
                    JsCore.MODID, "carbon_dioxide"), Codec.DOUBLE, BEFORE_INDUSTRY)
            .synced(ByteBufCodecs.DOUBLE).server();
    /* How often what was given off since is added, so the players are not told of every puff. */
    private static final int ADDED_EVERY = 1_200;
    /* What was given off since it was last added, in parts per million; on the server's thread only. */
    private static double pending;

    private CarbonDioxide() {
    }

    /** Registers the number and hands the game's bus what adds to it, from the Core's constructor. */
    public static void declare() {
        CoreStates.register(PPM);
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (event.getServer().getTickCount() % ADDED_EVERY == 0) {
                flush(event.getServer());
            }
        });
    }

    /** Gives off {@code kilograms} of carbon dioxide into the world's air, from a machine burning; added soon. */
    public static void emit(final double kilograms) {
        if (HazardBalance.pollution() && kilograms > 0) {
            pending += kilograms * PER_KILOGRAM;
        }
    }

    /** How much there is now, in parts per million. */
    public static double ppm(final MinecraftServer server) {
        return PPM.get(server);
    }

    /** Adds what was given off since it was last added, now: what the regular adding does; for a test. */
    public static void flush(final MinecraftServer server) {
        if (pending != 0) {
            final double added = pending;
            pending = 0;
            PPM.update(server, ppm -> ppm + added);
        }
    }
}
