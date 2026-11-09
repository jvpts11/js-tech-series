/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.wireless.WirelessStandard;
import dev.jstech.core.id.StableCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The link a portable is on while an access point reaches it, as its item carries it for its tooltip and its system:
 * the network's short id, the access point's name, and the Wi-Fi the link goes at, the older of the device's and the
 * access point's.
 */
public record PortableLink(String network, String accessPoint, WirelessStandard speed) {

    /** The longest name either end goes by. */
    public static final int MOST_NAME = 32;
    public static final Codec<PortableLink> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("network").forGetter(PortableLink::network),
            Codec.STRING.fieldOf("access_point").forGetter(PortableLink::accessPoint),
            StableCodecs.byName(WirelessStandard.class).fieldOf("speed").forGetter(PortableLink::speed)
    ).apply(i, PortableLink::new));
    public static final StreamCodec<ByteBuf, PortableLink> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MOST_NAME), PortableLink::network,
            ByteBufCodecs.stringUtf8(MOST_NAME), PortableLink::accessPoint,
            ByteBufCodecs.stringUtf8(MOST_NAME).map(WirelessStandard::byName, WirelessStandard::serializedName),
            PortableLink::speed,
            PortableLink::new);
}
