/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a tool takes besides the block it breaks: an area of the face it breaks into, or the vein the block is part of.
 * A tool breaking one block at a time has no shape. A mode of a tool gives it one ({@link ToolSpec}), or a module in it
 * does; written in data as
 *
 * <pre>{@code
 * {"type": "area", "width": 3, "height": 3, "depth": 1}
 * {"type": "vein", "most": 64}
 * }</pre>
 */
public sealed interface MiningShape {

    /** How a shape is written in data: its type, then its numbers. */
    Codec<MiningShape> CODEC = Codec.STRING.partialDispatch("type", shape -> DataResult.success(shape.type()),
            MiningShape::codecOf);

    /** An area {@code width} across and {@code height} up the face broken into, {@code depth} into it. */
    static MiningShape area(final int width, final int height, final int depth) {
        return new Area(width, height, depth);
    }

    /** Up to {@code most} blocks of the vein the block broken is part of, the block itself among them. */
    static MiningShape vein(final int most) {
        return new Vein(most);
    }

    /** What it is called in data. */
    String type();

    /** The most blocks it takes, the one broken among them. */
    int most();

    /* How a shape of {@code type} is read, or why none is. */
    private static DataResult<MapCodec<? extends MiningShape>> codecOf(final String type) {
        if (Area.TYPE.equals(type)) {
            return DataResult.success(Area.MAP_CODEC);
        }
        if (Vein.TYPE.equals(type)) {
            return DataResult.success(Vein.MAP_CODEC);
        }
        return DataResult.error(() -> "no mining shape " + type);
    }

    /**
     * An area of the face broken into, centred on the block broken: an even size has its extra row above it or its
     * extra column to its right, as the breaker sees the face.
     *
     * @param width  how many blocks across the face
     * @param height how many blocks up the face
     * @param depth  how many blocks into it
     */
    record Area(int width, int height, int depth) implements MiningShape {

        static final String TYPE = "area";
        /* The largest side of an area: what a server would break in one blow stays near a few hundred blocks. */
        private static final int LARGEST = 15;
        static final MapCodec<Area> MAP_CODEC = RecordCodecBuilder.mapCodec(area -> area.group(
                Codec.intRange(1, LARGEST).fieldOf("width").forGetter(Area::width),
                Codec.intRange(1, LARGEST).fieldOf("height").forGetter(Area::height),
                Codec.intRange(1, LARGEST).optionalFieldOf("depth", 1).forGetter(Area::depth))
                .apply(area, Area::new));

        public Area {
            if (width < 1 || height < 1 || depth < 1 || width > LARGEST || height > LARGEST || depth > LARGEST) {
                throw new IllegalArgumentException("an area is 1 to " + LARGEST + " blocks each way, not " + width
                        + "x" + height + "x" + depth);
            }
        }

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public int most() {
            return width * height * depth;
        }
    }

    /**
     * The vein the block broken is part of: the blocks touching it, at a face, an edge or a corner, of the same block
     * or of the same ore, and those touching them, nearest first.
     *
     * @param most the most blocks it takes, the one broken among them
     */
    record Vein(int most) implements MiningShape {

        static final String TYPE = "vein";
        /* The most a vein takes: what a server would break in one blow. */
        private static final int LARGEST = 4096;
        static final MapCodec<Vein> MAP_CODEC = RecordCodecBuilder.mapCodec(vein -> vein.group(
                Codec.intRange(1, LARGEST).fieldOf("most").forGetter(Vein::most)).apply(vein, Vein::new));

        public Vein {
            if (most < 1 || most > LARGEST) {
                throw new IllegalArgumentException("a vein takes 1 to " + LARGEST + " blocks, not " + most);
            }
        }

        @Override
        public String type() {
            return TYPE;
        }
    }
}
