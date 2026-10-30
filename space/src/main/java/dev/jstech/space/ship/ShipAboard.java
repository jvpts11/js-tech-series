/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.shape.FormedShapes;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;

/**
 * A small ship that has left its dock, as its flight console keeps it, for the console to draw its parts aboard
 * wherever the ship goes: its design, where the block of its nose stands from the console, how its frame is turned,
 * and which of its parts are formed, by their numbers, the spine's from the nose first.
 *
 * @param design its design
 * @param nose   where the block of its nose on its centre line at its hull's floor stands, from the console
 * @param turn   how its frame is turned, by its place among {@link FormedShapes#ORIENTATIONS}
 * @param formed its parts formed
 */
public record ShipAboard(ShipDesign design, BlockPos nose, int turn, List<Integer> formed) {

    /** As a console keeps it. */
    public static final Codec<ShipAboard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ShipDesign.CODEC.fieldOf("design").forGetter(ShipAboard::design),
            BlockPos.CODEC.fieldOf("nose").forGetter(ShipAboard::nose),
            Codec.intRange(0, FormedShapes.ORIENTATIONS.size() - 1).fieldOf("turn").forGetter(ShipAboard::turn),
            Codec.INT.listOf().fieldOf("formed").forGetter(ShipAboard::formed)
    ).apply(instance, ShipAboard::new));

    public ShipAboard {
        Objects.requireNonNull(design, "design");
        nose = nose.immutable();
        formed = List.copyOf(formed);
    }

    /** How its frame is turned. */
    public Orientation orientation() {
        return FormedShapes.ORIENTATIONS.get(this.turn);
    }
}
