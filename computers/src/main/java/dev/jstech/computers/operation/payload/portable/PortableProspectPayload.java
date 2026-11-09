/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what a portable's prospecting app finds round it (computers/docs/design/wireless.md), as J's
 * Core's world kit tells an instrument: each vein near by what it is made of most, which way it lies and how strongly
 * it shows, never where it is.
 *
 * @param device the device's id
 * @param veins  the veins found, the strongest first
 */
public record PortableProspectPayload(UUID device, List<Vein> veins) implements CustomPacketPayload {

    /** The most veins an app lists. */
    public static final int MOST_VEINS = 6;

    public static final CustomPacketPayload.Type<PortableProspectPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "portable_prospect"));

    public static final StreamCodec<ByteBuf, PortableProspectPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PortableProspectPayload::device,
            Vein.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_VEINS)), PortableProspectPayload::veins,
            PortableProspectPayload::new);

    public PortableProspectPayload {
        veins = List.copyOf(veins.subList(0, Math.min(MOST_VEINS, veins.size())));
    }

    @Override
    public CustomPacketPayload.Type<PortableProspectPayload> type() {
        return TYPE;
    }

    /**
     * A vein the app found.
     *
     * @param name     what it is made of most
     * @param colour   that material's colour, {@code 0xRRGGBB}
     * @param bearing  which way it lies, in degrees clockwise from north
     * @param strength how strongly it shows, from 0 to 1
     */
    public record Vein(Text name, int colour, float bearing, float strength) {

        public static final StreamCodec<ByteBuf, Vein> STREAM_CODEC = StreamCodec.composite(
                TextCodecs.STREAM_CODEC, Vein::name,
                ByteBufCodecs.INT, Vein::colour,
                ByteBufCodecs.FLOAT, Vein::bearing,
                ByteBufCodecs.FLOAT, Vein::strength,
                Vein::new);
    }
}
