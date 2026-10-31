/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crew;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.team.CoreTeams;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Who may do what aboard a starship: its owner, who flies it and alone sets the roles; the role of the owner's team,
 * and of everyone else; and the players the owner names, each with a role of their own. A ship belongs to its team,
 * so a new ship's owner flies it and the owner's team is its crew; everyone else stays outside till named. The team is
 * asked of J's Core each time, so a player who joins or leaves the owner's team gains or loses its role at once.
 *
 * @param owner     the player who owns the ship
 * @param ownerName the owner's name, as the console shows it
 * @param team      the role of the owner's team
 * @param others    the role of everyone else
 * @param named     the players the owner named, in the order they were named
 */
public record ShipCrew(UUID owner, String ownerName, CrewRole team, CrewRole others, List<Member> named) {

    /** The most players one ship names. */
    public static final int MOST_NAMED = 64;
    /** The longest name a player has. */
    public static final int LONGEST_NAME = 16;

    public static final Codec<ShipCrew> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(ShipCrew::owner),
            Codec.STRING.optionalFieldOf("owner_name", "").forGetter(ShipCrew::ownerName),
            StableCodecs.byName(CrewRole.class).optionalFieldOf("team", CrewRole.CREW).forGetter(ShipCrew::team),
            StableCodecs.byName(CrewRole.class).optionalFieldOf("others", CrewRole.NONE).forGetter(ShipCrew::others),
            Member.CODEC.listOf(0, MOST_NAMED).optionalFieldOf("named", List.of()).forGetter(ShipCrew::named)
    ).apply(instance, ShipCrew::new));

    public static final StreamCodec<ByteBuf, ShipCrew> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ShipCrew::owner,
            ByteBufCodecs.stringUtf8(LONGEST_NAME), ShipCrew::ownerName,
            StableCodecs.byId(CrewRole.class, CrewRole.NONE), ShipCrew::team,
            StableCodecs.byId(CrewRole.class, CrewRole.NONE), ShipCrew::others,
            Member.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_NAMED)), ShipCrew::named,
            ShipCrew::new);

    public ShipCrew {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(others, "others");
        ownerName = ownerName.length() > LONGEST_NAME ? ownerName.substring(0, LONGEST_NAME) : ownerName;
        named = List.copyOf(named);
    }

    /** A new ship's crew: {@code owner} flies it, their team is its crew, everyone else stays outside. */
    public static ShipCrew of(final UUID owner, final String ownerName) {
        return new ShipCrew(owner, ownerName, CrewRole.CREW, CrewRole.NONE, List.of());
    }

    /**
     * {@code player}'s role aboard: the owner flies it, and so does whoever passes the locks (an operator); a player
     * named has their own role; the owner's team has the team's; everyone else the others'.
     */
    public CrewRole roleOf(final ServerPlayer player) {
        if (player.getUUID().equals(this.owner) || CorePermissions.has(player, CorePermissions.USE_OTHERS)) {
            return CrewRole.PILOT;
        }
        return roleOf(player.server, player.getUUID());
    }

    /** The role of the player {@code player}, online or not, the owner's own aside from the locks. */
    public CrewRole roleOf(final MinecraftServer server, final UUID player) {
        if (player.equals(this.owner)) {
            return CrewRole.PILOT;
        }
        for (final Member member : this.named) {
            if (member.id().equals(player)) {
                return member.role();
            }
        }
        return CoreTeams.teamOf(server, player).equals(CoreTeams.teamOf(server, this.owner)) ? this.team
                : this.others;
    }

    /** Whether {@code player} may set its roles: its owner, and whoever passes the locks. */
    public boolean maySetRoles(final ServerPlayer player) {
        return player.getUUID().equals(this.owner) || CorePermissions.has(player, CorePermissions.SET_UP_OTHERS);
    }

    /** The same, its owner's team given {@code role}. */
    public ShipCrew withTeam(final CrewRole role) {
        return new ShipCrew(this.owner, this.ownerName, role, this.others, this.named);
    }

    /** The same, everyone else given {@code role}. */
    public ShipCrew withOthers(final CrewRole role) {
        return new ShipCrew(this.owner, this.ownerName, this.team, role, this.named);
    }

    /**
     * The same, the player {@code id} named {@code name} given {@code role}: named anew at the end, or kept where they
     * stand; {@link CrewRole#NONE} takes them off. The owner is never named, and no more than {@link #MOST_NAMED}.
     */
    public ShipCrew withNamed(final UUID id, final String name, final CrewRole role) {
        if (id.equals(this.owner)) {
            return this;
        }
        final List<Member> next = new ArrayList<>();
        boolean found = false;
        for (final Member member : this.named) {
            if (member.id().equals(id)) {
                found = true;
                if (role != CrewRole.NONE) {
                    next.add(new Member(id, member.name(), role));
                }
            } else {
                next.add(member);
            }
        }
        if (!found && role != CrewRole.NONE && next.size() < MOST_NAMED) {
            next.add(new Member(id, name, role));
        }
        return new ShipCrew(this.owner, this.ownerName, this.team, this.others, next);
    }

    /**
     * A player the owner named.
     *
     * @param id   who
     * @param name their name, as the console shows it
     * @param role their role aboard
     */
    public record Member(UUID id, String name, CrewRole role) {

        public static final Codec<Member> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Member::id),
                Codec.STRING.fieldOf("name").forGetter(Member::name),
                StableCodecs.byName(CrewRole.class).fieldOf("role").forGetter(Member::role)
        ).apply(instance, Member::new));

        public static final StreamCodec<ByteBuf, Member> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Member::id,
                ByteBufCodecs.stringUtf8(LONGEST_NAME), Member::name,
                StableCodecs.byId(CrewRole.class, CrewRole.NONE), Member::role,
                Member::new);

        public Member {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(role, "role");
            name = name.length() > LONGEST_NAME ? name.substring(0, LONGEST_NAME) : name;
        }
    }
}
