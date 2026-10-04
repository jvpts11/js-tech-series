/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: one change made in the open bus window, to the bus that window is on. What the change is decides
 * what its numbers and its text mean; the server takes it only where the bus's era can be set to it.
 *
 * @param op    the change, one of the constants here
 * @param slot  the filter slot, the condition, the tag or the kind it is about
 * @param value the new value, or how far to move it
 * @param text  the tag, the item or the bus it names
 */
public record BusEditPayload(int op, int slot, long value, String text) implements CustomPacketPayload {

    /** The carried item into filter slot {@code slot}, or the slot emptied when nothing is carried. */
    public static final int FILTER_SLOT = 0;
    /** The carried item into the first empty filter slot. */
    public static final int ADD_ITEM = 1;
    /** Only these (0) or all but these (1). */
    public static final int EXCLUDE = 2;
    /** The keep moved by {@code value}. */
    public static final int KEEP = 3;
    /** The max moved by {@code value}. */
    public static final int MAX = 4;
    /** The keep of filter slot {@code slot}'s item moved by {@code value}. */
    public static final int ITEM_KEEP = 5;
    /** The max of filter slot {@code slot}'s item moved by {@code value}. */
    public static final int ITEM_MAX = 6;
    /** The priority moved by {@code value}. */
    public static final int PRIORITY = 7;
    /** Continuous (0) or on demand (1). */
    public static final int MODE = 8;
    /** On (1) or off (0). */
    public static final int POWER = 9;
    /** Exact (0) or loose (1). */
    public static final int MATCH = 10;
    /** The tag {@code text} listed. */
    public static final int ADD_TAG = 11;
    /** Tag {@code slot} taken off. */
    public static final int REMOVE_TAG = 12;
    /**
     * A condition of kind {@code slot}: a stock of {@code text} under {@code value}; the hours, {@code value} being
     * the hour it starts times 24 plus the hour it ends; or after the bus named {@code text}.
     */
    public static final int ADD_CONDITION = 13;
    /** Condition {@code slot} taken off. */
    public static final int REMOVE_CONDITION = 14;
    /** An External Storage Bus read and written (0), read only (1) or written only (2). */
    public static final int ACCESS = 15;
    /** A Receiving Bus tied by hand to the interface its window lists at {@code slot}, or untied from it. */
    public static final int TIE = 16;

    /** The longest text a change names: a tag, an item's id, a bus's name. */
    public static final int MAX_TEXT = 64;

    public static final CustomPacketPayload.Type<BusEditPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "bus_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BusEditPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BusEditPayload::op,
            ByteBufCodecs.VAR_INT, BusEditPayload::slot,
            ByteBufCodecs.VAR_LONG, BusEditPayload::value,
            ByteBufCodecs.stringUtf8(MAX_TEXT), BusEditPayload::text,
            BusEditPayload::new);

    public BusEditPayload {
        text = text == null ? "" : text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
    }

    /** A change that names nothing. */
    public static BusEditPayload of(final int op, final int slot, final long value) {
        return new BusEditPayload(op, slot, value, "");
    }

    @Override
    public CustomPacketPayload.Type<BusEditPayload> type() {
        return TYPE;
    }
}
