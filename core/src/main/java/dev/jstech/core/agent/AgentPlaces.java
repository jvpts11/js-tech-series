/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.agent;

import com.mojang.logging.LogUtils;
import dev.jstech.core.JsCore;
import dev.jstech.core.multiblock.IStructureTerminal;
import dev.jstech.core.multiblock.LevelStructures;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * Where things are, as an agent is told: each mod hands the places of its own in the player's dimension, near and
 * far, an {@link IPlaceSource} declared once in its constructor, and the agent is told each with its distance and its
 * bearing from the player. The Core tells the player's respawn point, the world's spawn and every formed multiblock
 * loaded. A source that fails is left out, and said in the log.
 */
public final class AgentPlaces {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<Declared> SOURCES = new CopyOnWriteArrayList<>();
    /* The middle of a block, where a place at a block is. */
    private static final double MIDDLE = 0.5;

    private AgentPlaces() {
    }

    /** Declares the places the mod {@code modid} tells an agent of. */
    public static void declare(final String modid, final IPlaceSource source) {
        SOURCES.add(new Declared(modid, source));
    }

    /** Every place each mod tells of in {@code level}, the dimension {@code player} is in, with the mod's id. */
    public static List<Found> of(final ServerLevel level, final ServerPlayer player) {
        final List<Found> out = new ArrayList<>();
        for (final Declared each : SOURCES) {
            final List<Found> its = new ArrayList<>();
            try {
                each.source().places(level, player, place -> its.add(new Found(each.modid(), place)));
            } catch (final RuntimeException failed) {
                LOGGER.warn("The places of {} for an agent failed", each.modid(), failed);
                continue;
            }
            out.addAll(its);
        }
        return out;
    }

    /** Declares the Core's own places, once, from its constructor. */
    @ApiStatus.Internal
    public static void declareCore() {
        declare(JsCore.MODID, AgentPlaces::corePlaces);
    }

    /* The player's respawn point and the world's spawn, where they are in this dimension, and each formed structure. */
    private static void corePlaces(final ServerLevel level, final ServerPlayer player,
                                   final Consumer<AgentPlace> out) {
        final BlockPos respawn = player.getRespawnPosition();
        if (respawn != null && player.getRespawnDimension().equals(level.dimension())) {
            out.accept(new AgentPlace("respawn", "", Vec3.atBottomCenterOf(respawn)));
        }
        out.accept(new AgentPlace("world_spawn", "", Vec3.atBottomCenterOf(level.getSharedSpawnPos())));
        for (final BlockPos terminal : List.copyOf(LevelStructures.of(level).terminals())) {
            if (level.isLoaded(terminal) && level.getBlockEntity(terminal) instanceof IStructureTerminal formed
                    && formed.formedOrientation().isPresent()) {
                out.accept(new AgentPlace("multiblock", BuiltInRegistries.BLOCK.getKey(level.getBlockState(terminal)
                        .getBlock()).toString(), new Vec3(terminal.getX() + MIDDLE, terminal.getY() + MIDDLE,
                        terminal.getZ() + MIDDLE)));
            }
        }
    }

    /**
     * A place a mod told of.
     *
     * @param modid the mod
     * @param place the place
     */
    public record Found(String modid, AgentPlace place) {
    }

    /* A source, and the mod that declared it. */
    private record Declared(String modid, IPlaceSource source) {

        Declared {
            Objects.requireNonNull(modid, "modid");
            Objects.requireNonNull(source, "source");
        }
    }
}
