/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/*
 * A structure on its way to another dimension: the dimension, the plot of its yard the structure takes there, how far
 * up or down it goes to fit that dimension's height, and where its pivot arrives. It goes once that plot's chunks
 * and the land it arrives at have loaded, so nothing reads or writes a chunk that is not loaded; saved so, it goes on
 * as it loads.
 */
record StructureDeparture(ResourceKey<Level> to, int plot, int dy, double x, double y, double z) {

    static final Codec<StructureDeparture> CODEC = RecordCodecBuilder.create(departure -> departure.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("to").forGetter(StructureDeparture::to),
            Codec.INT.fieldOf("plot").forGetter(StructureDeparture::plot),
            Codec.INT.fieldOf("dy").forGetter(StructureDeparture::dy),
            Codec.DOUBLE.fieldOf("x").forGetter(StructureDeparture::x),
            Codec.DOUBLE.fieldOf("y").forGetter(StructureDeparture::y),
            Codec.DOUBLE.fieldOf("z").forGetter(StructureDeparture::z)
    ).apply(departure, StructureDeparture::new));
}
