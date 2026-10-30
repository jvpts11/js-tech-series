/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launcher;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A hold-down arm of a mobile launcher, as its block entity: the one at the corner of the opening behind and left of
 * the tower draws its whole deck once it is formed, as the approved launcher draws it, so it keeps the deck's measure,
 * worked out as it forms and told to the players who see it; the others keep nothing.
 */
public final class LauncherDeckBlockEntity extends SyncedBlockEntity {

    private final ValueField<Deck> deck;

    public LauncherDeckBlockEntity(final BlockPos pos, final BlockState state) {
        super(LauncherBlocks.HOLD_DOWN_ENTITY.get(), pos, state);
        this.deck = fields().nullable("Deck", Deck.CODEC).save().toClient();
    }

    /** The deck it draws, if it is the one that draws it and the deck is formed. */
    public Optional<Deck> deck() {
        return Optional.ofNullable(this.deck.get());
    }

    /** Keeps {@code to} as the deck it draws, or none. */
    public void deck(final @Nullable Deck to) {
        this.deck.set(to);
        setChanged();
    }

    /**
     * A launcher's deck, as its drawing hold-down arm measured it.
     *
     * @param side   the way to the tower, the deck's length along it
     * @param open   how many blocks the opening reaches each side of the rocket's axis
     * @param axis   where the rocket's axis meets the deck's top, from this block
     * @param back   how many blocks the deck reaches behind the axis, away from the tower
     * @param front  how many it reaches before it, toward and past the tower
     * @param across how many it reaches each side of it
     */
    public record Deck(Direction side, int open, BlockPos axis, int back, int front, int across) {

        public static final Codec<Deck> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Direction.CODEC.fieldOf("side").forGetter(Deck::side),
                Codec.INT.fieldOf("open").forGetter(Deck::open),
                BlockPos.CODEC.fieldOf("axis").forGetter(Deck::axis),
                Codec.INT.fieldOf("back").forGetter(Deck::back),
                Codec.INT.fieldOf("front").forGetter(Deck::front),
                Codec.INT.fieldOf("across").forGetter(Deck::across)
        ).apply(instance, Deck::new));
    }
}
