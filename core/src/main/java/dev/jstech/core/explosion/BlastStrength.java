/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * The overpressure a block of the world stands before a blast breaks it, in kilopascals: a data map of blocks ({@code
 * data/<namespace>/data_maps/block/blast_strength.json}) a mod or a data pack fills, a window's pane at 2, planks at
 * 80, stone at 200, concrete at 700, a blast door far more. A block the map says nothing of stands what its resistance
 * to the game's explosions says, through {@link BlastMath#standsFromResistance}.
 *
 * <pre>{@code
 * { "values": { "mymod:blast_door": 50000, "#c:glass_panes": 2 } }
 * }</pre>
 */
public final class BlastStrength {

    /** Each block's strength against a blast, in kilopascals. */
    public static final DataMapType<Block, Double> STRENGTH = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "blast_strength"), Registries.BLOCK,
            Codec.doubleRange(0.0, 1.0e12)).build();

    private BlastStrength() {
    }

    /** Hands the mod's bus the data map, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(STRENGTH));
    }

    /** What the block {@code state} at {@code pos} stands of {@code explosion}, in kilopascals. */
    public static double of(final BlockGetter level, final BlockPos pos, final BlockState state,
                            final Explosion explosion) {
        final Double mapped = state.getBlockHolder().getData(STRENGTH);
        if (mapped != null) {
            return mapped;
        }
        return BlastMath.standsFromResistance(state.getExplosionResistance(level, pos, explosion));
    }
}
