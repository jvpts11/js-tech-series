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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Whose a thing is and who else may do what with it: a machine its placer owns, shared with nobody, with the owner's
 * team or with everyone, and beyond that a list of players the owner trusts by name, each to use it, to set it up as
 * well, or to break it as well. The team is asked of {@link CoreTeams} each time, so a player who joins or leaves the
 * owner's team gains or loses the use of what the team shares at once. An operator, or whoever the server's permission
 * mod grants it to, may pass by the owner ({@link CorePermissions}).
 *
 * @param owner   the player who owns it
 * @param access  who else may use it
 * @param trusted the players the owner trusts by name, and how far
 */
@TextHolder
public record Ownership(UUID owner, Access access, Map<UUID, Trust> trusted) {

    /** What a player told they may not use something reads. */
    public static final TextKey DENIED = TextKey.of("jscore.ownership.denied", "This belongs to %s.");
    /** What a player told they may not break something reads. */
    public static final TextKey BREAK_DENIED = TextKey.of("jscore.ownership.break_denied",
            "This belongs to %s, who has not trusted you to break it.");

    /** The most players one thing trusts, more than any owner names. */
    public static final int MOST_TRUSTED = 256;

    public static final Codec<Ownership> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Ownership::owner),
            StableCodecs.byName(Access.class).optionalFieldOf("access", Access.PRIVATE).forGetter(Ownership::access),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, StableCodecs.byName(Trust.class)).optionalFieldOf("trusted",
                    Map.of()).forGetter(Ownership::trusted))
            .apply(instance, Ownership::new));

    public static final StreamCodec<ByteBuf, Ownership> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Ownership::owner,
            StableCodecs.byId(Access.class, Access.PRIVATE), Ownership::access,
            ByteBufCodecs.map(LinkedHashMap::new, UUIDUtil.STREAM_CODEC, StableCodecs.byId(Trust.class, Trust.USE),
                    MOST_TRUSTED), Ownership::trusted,
            Ownership::new);

    public Ownership {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(access, "access");
        trusted = Collections.unmodifiableMap(new LinkedHashMap<>(trusted));
    }

    /** Owned by {@code owner} alone. */
    public static Ownership of(final UUID owner) {
        return new Ownership(owner, Access.PRIVATE, Map.of());
    }

    /** The same owner and trust, letting in whom {@code next} lets in. */
    public Ownership withAccess(final Access next) {
        return new Ownership(owner, next, trusted);
    }

    /** The same, trusting {@code player} as far as {@code level}. */
    public Ownership withTrust(final UUID player, final Trust level) {
        final Map<UUID, Trust> more = new LinkedHashMap<>(trusted);
        more.put(player, level);
        return new Ownership(owner, access, more);
    }

    /** The same, no longer trusting {@code player} by name. */
    public Ownership withoutTrust(final UUID player) {
        final Map<UUID, Trust> fewer = new LinkedHashMap<>(trusted);
        fewer.remove(player);
        return new Ownership(owner, access, fewer);
    }

    /** How far the owner trusts {@code player} by name, or none. */
    public Optional<Trust> trustOf(final UUID player) {
        return Optional.ofNullable(trusted.get(player));
    }

    /** Whether {@code player} may use the thing: its owner, whoever its access or its trust lets in, an operator. */
    public boolean mayUse(final ServerPlayer player) {
        if (player.getUUID().equals(owner) || trusted(player.getUUID(), Trust.USE)
                || CorePermissions.has(player, CorePermissions.USE_OTHERS)) {
            return true;
        }
        return switch (access) {
            case PUBLIC -> true;
            case TEAM -> sameTeam(player.server, player.getUUID());
            case PRIVATE -> false;
        };
    }

    /** Whether {@code player} may open the thing's screen: whoever may use it, and whoever may open others'. */
    public boolean mayOpen(final ServerPlayer player) {
        return mayUse(player) || CorePermissions.has(player, CorePermissions.OPEN_OTHERS);
    }

    /** Whether {@code player} may change how the thing is set up: its owner and whom the owner trusts to. */
    public boolean maySetUp(final ServerPlayer player) {
        return player.getUUID().equals(owner) || trusted(player.getUUID(), Trust.SET_UP)
                || CorePermissions.has(player, CorePermissions.SET_UP_OTHERS);
    }

    /** Whether {@code player} may break the thing: its owner and whom the owner trusts to. */
    public boolean mayBreak(final ServerPlayer player) {
        return player.getUUID().equals(owner) || trusted(player.getUUID(), Trust.BREAK)
                || CorePermissions.has(player, CorePermissions.BREAK_OTHERS);
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
        return CoreTeams.nameOf(server, owner).orElse(owner.toString());
    }

    private boolean trusted(final UUID player, final Trust level) {
        final Trust held = trusted.get(player);
        return held != null && held.holds(level);
    }
}
