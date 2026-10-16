/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What a Structure Projector shows and how, as its screen sets it: the multiblock chosen, its turn (0 north to 3
 * west), whether it is mirrored, up to which layer it is shown (0 for all of them), whether only what is missing is
 * shown, and whether it shows at all.
 *
 * @param chosen      the multiblock shown, by id; none for nothing chosen yet
 * @param turn        its turn, a quarter clockwise for each, seen from above
 * @param mirrored    whether it is mirrored, for a multiblock that allows it
 * @param layer       the highest layer shown, from 1 at the bottom; 0 for every layer
 * @param missingOnly whether only the places still empty are shown
 * @param on          whether it shows anything
 */
public record ProjectorSettings(Optional<ResourceLocation> chosen, int turn, boolean mirrored, int layer,
                                boolean missingOnly, boolean on) {

    /** A projector as placed: nothing chosen, facing north, every layer, on. */
    public static final ProjectorSettings FRESH = new ProjectorSettings(Optional.empty(), 0, false, 0, false, true);
    /** How the settings travel from the screen to the server. */
    public static final StreamCodec<RegistryFriendlyByteBuf, ProjectorSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), ProjectorSettings::chosen,
            ByteBufCodecs.VAR_INT, ProjectorSettings::turn,
            ByteBufCodecs.BOOL, ProjectorSettings::mirrored,
            ByteBufCodecs.VAR_INT, ProjectorSettings::layer,
            ByteBufCodecs.BOOL, ProjectorSettings::missingOnly,
            ByteBufCodecs.BOOL, ProjectorSettings::on,
            ProjectorSettings::new);
    /** The turns there are. */
    public static final int TURNS = 4;

    public ProjectorSettings {
        turn = Math.floorMod(turn, TURNS);
        layer = Math.max(0, layer);
    }

    /** The same, showing {@code id}, from its first layer to its last again. */
    public ProjectorSettings choosing(final ResourceLocation id) {
        return new ProjectorSettings(Optional.of(id), this.turn, this.mirrored, 0, this.missingOnly, this.on);
    }
}
