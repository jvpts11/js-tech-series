/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/**
 * What an Item Port's screen shows of the ship its dock builds: its name, how many of its blocks are laid of how many
 * in all, and each block it still lacks, with what the dock and the port have of it.
 *
 * @param ship   the ship's name; empty while nothing is being built
 * @param laid   how many of its blocks are laid
 * @param blocks how many blocks it has in all
 * @param rows   each block it still lacks, the ones laid first first
 */
public record ItemPortView(String ship, int laid, int blocks, List<Row> rows) {

    /** Nothing being built. */
    public static final ItemPortView NONE = new ItemPortView("", 0, 0, List.of());
    /* The most rows sent, well over the kinds of block a ship is built of. */
    private static final int MOST_ROWS = 256;
    private static final int LONGEST_NAME = 64;
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemPortView> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(LONGEST_NAME), ItemPortView::ship,
            ByteBufCodecs.VAR_INT, ItemPortView::laid,
            ByteBufCodecs.VAR_INT, ItemPortView::blocks,
            Row.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_ROWS)), ItemPortView::rows,
            ItemPortView::new);

    public ItemPortView {
        Objects.requireNonNull(ship, "ship");
        rows = List.copyOf(rows);
    }

    /** Whether a ship is being built. */
    public boolean building() {
        return !this.ship.isEmpty();
    }

    /** Whether the port takes {@code item}: the ship lacks more of it than the dock and the port have. */
    public boolean takes(final Item item) {
        for (final Row row : this.rows) {
            if (row.item() == item) {
                return row.have() < row.lacking();
            }
        }
        return false;
    }

    /**
     * A block the ship still lacks.
     *
     * @param item    the block's item
     * @param have    how many the dock and the port have of it
     * @param lacking how many more of it the ship takes before it is whole
     */
    public record Row(Item item, int have, int lacking) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM), Row::item,
                ByteBufCodecs.VAR_INT, Row::have,
                ByteBufCodecs.VAR_INT, Row::lacking,
                Row::new);

        /** Whether the dock and the port have all of it the ship lacks. */
        public boolean enough() {
            return this.have >= this.lacking;
        }
    }
}
