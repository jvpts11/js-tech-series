/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.IProtectionAdapter;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.team.CoreTeams;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Who may do what at a place, as every mod asking the Core's protections hears it: the player asking, someone of
 * their team, a player they name (or the nearest other one) and that player's machines, which are weighed as their
 * owner, online or not; and what each protection answered, with the claim the place lies in and the server's rule on
 * hurting other players.
 */
final class AccessService implements IStudioService {

    static final String NAME = "access";
    /** The actions asked of the place, the same question every mod asks. */
    static final List<ProtectedAction> ASKED = List.of(ProtectedAction.BREAK, ProtectedAction.PLACE,
            ProtectedAction.USE, ProtectedAction.EXPLODE, ProtectedAction.BURN, ProtectedAction.FLOW,
            ProtectedAction.MOVE, ProtectedAction.SHOOT);
    /* How far the nearest other player is looked for when nobody is named. */
    private static final double NEAREST = 64;

    @Override
    public void answer(final ServerPlayer player, final CompoundTag args, final Consumer<CompoundTag> reply) {
        final ServerLevel level = player.serverLevel();
        final BlockPos pos = BlockPos.of(args.getLong(StudioKeys.POS));
        final MinecraftServer server = level.getServer();
        final List<Weighed> weighed = new ArrayList<>();
        weighed.add(new Weighed(StudioKeys.YOU, player.getScoreboardName(), Actor.of(player)));
        teammate(server, player).ifPresent(mate -> weighed.add(new Weighed(StudioKeys.TEAM, name(server, mate),
                actorOf(server, mate))));
        final Optional<UUID> other = named(server, player, args.getString(StudioKeys.WHO));
        other.ifPresent(who -> {
            weighed.add(new Weighed(StudioKeys.PLAYER, name(server, who), actorOf(server, who)));
            weighed.add(new Weighed(StudioKeys.MACHINES, name(server, who), Actor.forOwner(who)));
        });
        final CompoundTag out = new CompoundTag();
        final ListTag columns = new ListTag();
        for (final Weighed one : weighed) {
            final CompoundTag column = new CompoundTag();
            column.putString(StudioKeys.KIND, one.kind());
            column.putString(StudioKeys.NAME, one.name());
            final ListTag allowed = new ListTag();
            for (final ProtectedAction action : ASKED) {
                final CompoundTag cell = new CompoundTag();
                cell.putString(StudioKeys.ACTION, action.serializedName());
                cell.putBoolean(StudioKeys.ALLOWED, Protection.may(one.actor(), action, level, pos));
                allowed.add(cell);
            }
            column.put(StudioKeys.ENTRIES, allowed);
            columns.add(column);
        }
        out.put(StudioKeys.COLUMNS, columns);
        out.put(StudioKeys.ANSWERS, answers(level, pos, weighed.getLast().actor()));
        out.putString(StudioKeys.DIMENSION, level.dimension().location().toString());
        Protection.claimOwner(level, pos).ifPresent(owner -> out.putString(StudioKeys.CLAIM, name(server, owner)));
        out.putBoolean(StudioKeys.PVP, server.isPvpAllowed());
        other.flatMap(who -> Optional.ofNullable(server.getPlayerList().getPlayer(who)))
                .ifPresent(who -> out.putBoolean(StudioKeys.MAY_HURT, Protection.mayHurt(Actor.of(who), player)));
        reply.accept(out);
    }

    /* What each protection answered of the last one weighed breaking the place: allowed, stopped or nothing said. */
    private static ListTag answers(final ServerLevel level, final BlockPos pos, final Actor actor) {
        final ListTag out = new ListTag();
        for (final Map.Entry<String, IProtectionAdapter.Verdict> verdict : Protection.verdicts(actor,
                ProtectedAction.BREAK, level, pos).entrySet()) {
            final CompoundTag one = new CompoundTag();
            one.putString(StudioKeys.NAME, verdict.getKey());
            one.putString(StudioKeys.VERDICT, verdict.getValue().name());
            out.add(one);
        }
        return out;
    }

    /* Someone of {@code player}'s team other than them, online first. */
    private static Optional<UUID> teammate(final MinecraftServer server, final ServerPlayer player) {
        final List<UUID> members = CoreTeams.membersOf(server, CoreTeams.teamOf(server, player.getUUID()));
        Optional<UUID> offline = Optional.empty();
        for (final UUID member : members) {
            if (member.equals(player.getUUID())) {
                continue;
            }
            if (server.getPlayerList().getPlayer(member) != null) {
                return Optional.of(member);
            }
            offline = offline.isPresent() ? offline : Optional.of(member);
        }
        return offline;
    }

    /* The player called {@code name}, online or known to the server; with no name, the nearest other one online. */
    private static Optional<UUID> named(final MinecraftServer server, final ServerPlayer asking, final String name) {
        if (!name.isBlank()) {
            final ServerPlayer online = server.getPlayerList().getPlayerByName(name.trim());
            if (online != null) {
                return Optional.of(online.getUUID());
            }
            return server.getProfileCache() == null ? Optional.empty()
                    : server.getProfileCache().get(name.trim()).map(profile -> profile.getId());
        }
        ServerPlayer nearest = null;
        for (final ServerPlayer one : asking.serverLevel().players()) {
            if (one != asking && one.distanceTo(asking) <= NEAREST
                    && (nearest == null || one.distanceTo(asking) < nearest.distanceTo(asking))) {
                nearest = one;
            }
        }
        return Optional.ofNullable(nearest).map(ServerPlayer::getUUID);
    }

    /* A player weighed as themselves when online, else as what acts for them. */
    private static Actor actorOf(final MinecraftServer server, final UUID player) {
        final ServerPlayer online = server.getPlayerList().getPlayer(player);
        return online != null ? Actor.of(online) : Actor.forOwner(player);
    }

    private static String name(final MinecraftServer server, final UUID player) {
        return CoreTeams.nameOf(server, player).orElse(player.toString());
    }

    /* One column: who it weighs, their name, and the actor the protections are asked about. */
    private record Weighed(String kind, String name, Actor actor) {
    }
}
