/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * A marker on the maps: a point, or an area of chunks, in a layer, with a label. Shown in the map mods the player has
 * (JourneyMap, Xaero's Minimap and World Map, FTB Chunks) and in the maps of the series' own mods.
 *
 * @param id        its id, unique among its owner's markers
 * @param layer     the layer it is in
 * @param dimension the dimension it is in
 * @param pos       where it is, for a point; the middle of an area
 * @param area      the chunks it covers, for an area; empty for a point
 * @param label     what it says
 * @param colour    its colour, {@code 0xRRGGBB}, or -1 for its layer's
 */
public record Marker(String id, ResourceLocation layer, ResourceKey<Level> dimension, BlockPos pos,
                     List<ChunkPos> area, Component label, int colour) {

    /** The colour that says to take the layer's. */
    public static final int LAYERS = -1;
    private static final Codec<ChunkPos> CHUNK = Codec.INT.listOf(2, 2).xmap(
            xz -> new ChunkPos(xz.get(0), xz.get(1)), chunk -> List.of(chunk.x, chunk.z));
    public static final Codec<Marker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Marker::id),
            ResourceLocation.CODEC.fieldOf("layer").forGetter(Marker::layer),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Marker::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(Marker::pos),
            CHUNK.listOf().optionalFieldOf("area", List.of()).forGetter(Marker::area),
            ComponentSerialization.CODEC.fieldOf("label").forGetter(Marker::label),
            Codec.INT.optionalFieldOf("colour", LAYERS).forGetter(Marker::colour))
            .apply(instance, Marker::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Marker> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public Marker {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(label, "label");
        pos = pos.immutable();
        area = List.copyOf(area);
    }

    /** A point at {@code pos}. */
    public static Marker point(final String id, final ResourceLocation layer, final ResourceKey<Level> dimension,
                               final BlockPos pos, final Component label) {
        return new Marker(id, layer, dimension, pos, List.of(), label, LAYERS);
    }

    /** An area over {@code chunks}, its marker at their middle. */
    public static Marker area(final String id, final ResourceLocation layer, final ResourceKey<Level> dimension,
                              final List<ChunkPos> chunks, final Component label) {
        long x = 0;
        long z = 0;
        for (final ChunkPos chunk : chunks) {
            x += chunk.getMiddleBlockX();
            z += chunk.getMiddleBlockZ();
        }
        final int count = Math.max(1, chunks.size());
        return new Marker(id, layer, dimension, new BlockPos((int) (x / count), 64, (int) (z / count)), chunks,
                label, LAYERS);
    }

    /** Whether it is an area rather than a point. */
    public boolean isArea() {
        return !this.area.isEmpty();
    }

    /** The same marker in {@code rgb}. */
    public Marker coloured(final int rgb) {
        return new Marker(this.id, this.layer, this.dimension, this.pos, this.area, this.label, rgb);
    }

    /** Its colour, {@code 0xRRGGBB}: its own, or its layer's. */
    public int colourIn(final MarkerLayer of) {
        return this.colour == LAYERS ? of.colour() : this.colour;
    }
}
