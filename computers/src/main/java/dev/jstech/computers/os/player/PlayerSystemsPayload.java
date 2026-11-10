/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.player;

import dev.jstech.computers.os.OsDef;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client: the systems and desktops the world's players have built, so the client names, boots and draws them
 * as the server does. Sent when a player joins and whenever one is built; the desktops come without their files, which
 * a client reads off the machine's disk when it draws them.
 *
 * @param systems  every player's system, as the installers see it
 * @param desktops every player's desktop, its look and its programs
 */
public record PlayerSystemsPayload(List<OsDef> systems, List<PlayerDesktop> desktops) implements CustomPacketPayload {

    /** The most systems and desktops a world tells a client of, far more than players build. */
    public static final int MOST = 1024;

    public static final CustomPacketPayload.Type<PlayerSystemsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "player_systems"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSystemsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(OsDef.CODEC).apply(ByteBufCodecs.list(MOST)), PlayerSystemsPayload::systems,
                    ByteBufCodecs.fromCodec(PlayerCodecs.DESKTOP).apply(ByteBufCodecs.list(MOST)),
                    PlayerSystemsPayload::desktops,
                    PlayerSystemsPayload::new);

    public PlayerSystemsPayload {
        systems = List.copyOf(systems.size() > MOST ? systems.subList(0, MOST) : systems);
        desktops = List.copyOf(desktops.size() > MOST ? desktops.subList(0, MOST) : desktops);
    }

    @Override
    public CustomPacketPayload.Type<PlayerSystemsPayload> type() {
        return TYPE;
    }
}
