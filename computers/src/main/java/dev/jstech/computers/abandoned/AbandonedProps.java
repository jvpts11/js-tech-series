/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.computers.block.FacingShapes;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The fittings of the abandoned buildings as the decor block draws them (computers/docs/design/survival.md,
 * "Abandoned buildings"): the boards on the walls, the lights under the ceilings, the signs, a van's wheels, a dish.
 * Each part is one block's share of a fitting, its model drawn in the mod's resources; this table, written with the
 * models, holds what the game needs of each part besides its look: the light it gives and the boxes it is made of, in
 * sixteenths of the block as it stands facing north.
 */
public final class AbandonedProps {

    private static final String TABLE = "props.json";
    private static final int[] LIGHT;
    private static final double[][][] BOXES;
    private static final VoxelShape[][] SHAPES;
    private static final int TURNS = 4;
    private static final int BOX_SIZE = 6;

    static {
        final JsonArray parts = read().getAsJsonArray("parts");
        LIGHT = new int[parts.size()];
        BOXES = new double[parts.size()][][];
        for (int i = 0; i < parts.size(); i++) {
            final JsonObject part = parts.get(i).getAsJsonObject();
            LIGHT[i] = part.get("light").getAsInt();
            final JsonArray boxes = part.getAsJsonArray("boxes");
            BOXES[i] = new double[boxes.size()][];
            for (int b = 0; b < boxes.size(); b++) {
                final JsonArray box = boxes.get(b).getAsJsonArray();
                BOXES[i][b] = new double[BOX_SIZE];
                for (int k = 0; k < BOX_SIZE; k++) {
                    BOXES[i][b][k] = box.get(k).getAsDouble();
                }
            }
        }
        SHAPES = new VoxelShape[parts.size()][TURNS];
    }

    private AbandonedProps() {
    }

    /** How many parts there are. */
    public static int count() {
        return LIGHT.length;
    }

    /** The light {@code part} gives: a ceiling light's, a lit sign's; 0 for most. */
    public static int light(final int part) {
        return LIGHT[part];
    }

    /** The shape of {@code part} standing facing {@code facing}, made once and kept. */
    public static VoxelShape shape(final int part, final Direction facing) {
        final int turn = facing.get2DDataValue();
        VoxelShape shape = SHAPES[part][turn];
        if (shape == null) {
            shape = Shapes.empty();
            for (final double[] box : BOXES[part]) {
                shape = Shapes.or(shape, FacingShapes.box(box, facing));
            }
            shape = shape.optimize();
            SHAPES[part][turn] = shape;
        }
        return shape;
    }

    private static JsonObject read() {
        try (InputStream in = AbandonedProps.class.getResourceAsStream(TABLE)) {
            if (in == null) {
                throw new IllegalStateException("the abandoned buildings' fittings table " + TABLE + " is missing");
            }
            final JsonElement json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return json.getAsJsonObject();
        } catch (final IOException e) {
            throw new UncheckedIOException("reading the abandoned buildings' fittings table", e);
        }
    }
}
