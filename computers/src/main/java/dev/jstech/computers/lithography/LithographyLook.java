/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.lithography;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What the lithography program shows of one Lithography Station its computer works for: the station, what it is
 * doing, its wafer and the layer it exposes, what its computer holds for it, its liquids and gases, what stands in its
 * mask slot, the masks it writes for that computer, and the mask it is writing. Read off the station on the server, the
 * same for the program's text screen and its window.
 *
 * @param stations        how many stations the computer works for; none leaves the rest empty
 * @param shown           which of them this is, from nought
 * @param station         the station: its era and its light
 * @param status          what it is doing: idle, writing a mask, or exposing a layer of how many
 * @param wafer           the chip the wafer it exposes is for, or none
 * @param exposing        whether it is exposing a layer
 * @param layerPermille   how far through that layer it is, in thousandths
 * @param layerSeconds    how many seconds that layer has left
 * @param lithoMb         what its computer holds for it, in megabytes; 0 while it holds nothing
 * @param supplies        its liquids and gases, with how much of each it holds
 * @param slot            what stands in its mask slot: a Mask Blank, a mask, or nothing
 * @param blank           whether a Mask Blank is in it, to be written on
 * @param masked          whether a mask is in it, to expose through
 * @param picks          the masks it writes for that computer, in the order of the program's lists
 * @param writing         the mask it is writing, by what the program calls it, or nothing
 * @param writingPermille how far through the writing it is, in thousandths
 * @param writingSeconds  how many seconds the writing has left
 * @param moulder         whether the machine is an Injection Moulder, whose rows tell its plastic, the part it moulds
 *                        and its mould slot, and whose picks are the moulds it cuts on a Mould Blank
 */
public record LithographyLook(int stations, int shown, Text station, Text status, Text wafer, boolean exposing,
                              int layerPermille, int layerSeconds, int lithoMb, List<Supply> supplies, Text slot,
                              boolean blank, boolean masked, List<Pick> picks, Text writing, int writingPermille,
                              int writingSeconds, boolean moulder) {

    /* The most of each list a look carries: far over what a station has. */
    private static final int MOST_SUPPLIES = 8;
    private static final int MOST_PICKS = 256;
    private static final int MOST_KIND = 32;

    /** A computer no station works for. */
    public static final LithographyLook NONE = new LithographyLook(0, 0, Text.EMPTY, Text.EMPTY, Text.EMPTY, false,
            0, 0, 0, List.of(), Text.EMPTY, false, false, List.of(), Text.EMPTY, 0, 0, false);

    public static final StreamCodec<RegistryFriendlyByteBuf, LithographyLook> STREAM_CODEC = StreamCodec.of(
            (buf, look) -> {
                buf.writeVarInt(look.stations()).writeVarInt(look.shown());
                TextCodecs.STREAM_CODEC.encode(buf, look.station());
                TextCodecs.STREAM_CODEC.encode(buf, look.status());
                TextCodecs.STREAM_CODEC.encode(buf, look.wafer());
                buf.writeBoolean(look.exposing()).writeVarInt(look.layerPermille()).writeVarInt(look.layerSeconds())
                        .writeVarInt(look.lithoMb());
                Supply.LIST_CODEC.encode(buf, look.supplies());
                TextCodecs.STREAM_CODEC.encode(buf, look.slot());
                buf.writeBoolean(look.blank()).writeBoolean(look.masked());
                Pick.LIST_CODEC.encode(buf, look.picks());
                TextCodecs.STREAM_CODEC.encode(buf, look.writing());
                buf.writeVarInt(look.writingPermille()).writeVarInt(look.writingSeconds());
                buf.writeBoolean(look.moulder());
            },
            buf -> new LithographyLook(buf.readVarInt(), buf.readVarInt(), TextCodecs.STREAM_CODEC.decode(buf),
                    TextCodecs.STREAM_CODEC.decode(buf), TextCodecs.STREAM_CODEC.decode(buf), buf.readBoolean(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), Supply.LIST_CODEC.decode(buf),
                    TextCodecs.STREAM_CODEC.decode(buf), buf.readBoolean(), buf.readBoolean(),
                    Pick.LIST_CODEC.decode(buf),
                    TextCodecs.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

    public LithographyLook {
        supplies = List.copyOf(supplies);
        picks = List.copyOf(picks);
    }

    /** Whether it is writing a mask. */
    public boolean writes() {
        return !writing.isEmpty();
    }

    /**
     * One liquid or gas a station takes.
     *
     * @param name   what it is
     * @param amount how much of it the station holds, in millibuckets
     */
    public record Supply(Text name, long amount) {

        static final StreamCodec<RegistryFriendlyByteBuf, Supply> CODEC = StreamCodec.composite(
                TextCodecs.STREAM_CODEC, Supply::name, ByteBufCodecs.VAR_LONG, Supply::amount, Supply::new);
        static final StreamCodec<RegistryFriendlyByteBuf, List<Supply>> LIST_CODEC =
                CODEC.apply(ByteBufCodecs.list(MOST_SUPPLIES));
    }

    /**
     * One mask a station writes.
     *
     * @param kind the program's list it is on, by the kind's name
     * @param mask the mask, by its item's id
     * @param name what the program calls it
     */
    public record Pick(String kind, ResourceLocation mask, Text name) {

        static final StreamCodec<RegistryFriendlyByteBuf, Pick> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(MOST_KIND), Pick::kind, ResourceLocation.STREAM_CODEC, Pick::mask,
                TextCodecs.STREAM_CODEC, Pick::name, Pick::new);
        static final StreamCodec<RegistryFriendlyByteBuf, List<Pick>> LIST_CODEC =
                CODEC.apply(ByteBufCodecs.list(MOST_PICKS));
    }
}
