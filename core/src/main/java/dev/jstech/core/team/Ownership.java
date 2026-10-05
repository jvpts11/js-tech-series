/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.team;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Whose a thing is and who else may use it: a machine its placer owns, shared with nobody, with the owner's team or
 * with everyone. The team is asked of {@link CoreTeams} each time, so a player who joins or leaves the owner's team
 * gains or loses the use of what the team shares at once. An operator with the permission to pass by owners may use
 * anything.
 *
 * @param owner  the player who owns it
 * @param access who else may use it
 */
@TextHolder
public record Ownership(UUID owner, Access access) {

    /** What a player told they may not use something reads. */
    public static final TextKey DENIED = TextKey.of("jscore.ownership.denied", "This belongs to %s.");

    public static final Codec<Ownership> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Ownership::owner),
            StableCodecs.byName(Access.class).optionalFieldOf("access", Access.PRIVATE).forGetter(Ownership::access))
            .apply(instance, Ownership::new));

    public static final StreamCodec<ByteBuf, Ownership> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Ownership::owner,
            StableCodecs.byId(Access.class, Access.PRIVATE), Ownership::access,
            Ownership::new);

    public Ownership {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(access, "access");
    }

    /** Owned by {@code owner} alone. */
    public static Ownership of(final UUID owner) {
        return new Ownership(owner, Access.PRIVATE);
    }

    /** The same owner, letting in whom {@code next} lets in. */
    public Ownership withAccess(final Access next) {
        return new Ownership(owner, next);
    }

    /** Whether {@code player} may use the thing. */
    public boolean mayUse(final ServerPlayer player) {
        if (player.getUUID().equals(owner) || CorePermissions.mayPassOwners(player)) {
            return true;
        }
        return switch (access) {
            case PUBLIC -> true;
            case TEAM -> sameTeam(player.server, player.getUUID());
            case PRIVATE -> false;
        };
    }

    /** Whether {@code player}, online or not, is on the owner's team: the owner always is. */
    public boolean sameTeam(final MinecraftServer server, final UUID player) {
        return player.equals(owner) || CoreTeams.teamOf(server, player).equals(CoreTeams.teamOf(server, owner));
    }

    /** What a player who may not use the thing is told: whose it is, by the name the server knows. */
    public Text denial(final MinecraftServer server) {
        return DENIED.with(ownerName(server));
    }

    /** The owner's name as the server last knew it, or their id when it never did. */
    public String ownerName(final MinecraftServer server) {
        final ServerPlayer online = server.getPlayerList().getPlayer(owner);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        return server.getProfileCache() == null ? owner.toString()
                : server.getProfileCache().get(owner).map(profile -> profile.getName()).orElse(owner.toString());
    }
}
