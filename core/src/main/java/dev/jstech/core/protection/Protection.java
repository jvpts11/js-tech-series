/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.protection;

import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.IOwned;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.team.ScoreboardTeams;
import dev.jstech.core.util.DeclarationError;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The one question every mod of the series asks before it changes the world: may this actor do this here? A robot
 * breaking a block, a machine placing one, a tool working on the move, a moving structure, an explosion, a fire, a
 * fluid or something shot asks it, and every protection installed answers: the game's spawn protection, FTB Chunks and
 * Open Parties and Claims when they are there, and any a mod registers. One that stops it stops it; the rest only let
 * it through.
 *
 * <pre>{@code
 * if (!Protection.may(Actor.forOwner(owner), ProtectedAction.BREAK, level, target)) {
 *     return Status.FAILED;
 * }
 * }</pre>
 *
 * <p>Damage between players asks {@link #mayHurt}: the server's PvP rule, the claim the victim stands in, and whether
 * the two are on one team.
 */
public final class Protection {

    private static final Logger LOGGER = CoreLogs.TEAMS.logger();
    /**
     * The protections asked, by the name each was registered under, in the order they were: replaced whole on each
     * registration, so the question, asked often, reads it without a lock.
     */
    private static volatile Map<String, IProtectionAdapter> adapters = Map.of();

    private Protection() {
    }

    /**
     * Registers a protection under {@code name}, from a mod's constructor or setup.
     *
     * @throws DeclarationError when one of that name is registered already
     */
    public static synchronized void register(final String name, final IProtectionAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        if (adapters.containsKey(name)) {
            throw DeclarationError.of(null, "the protection " + name, "is registered twice",
                    "register it once; a mod's protection takes a name of its own");
        }
        final Map<String, IProtectionAdapter> more = new LinkedHashMap<>(adapters);
        more.put(name, adapter);
        adapters = Collections.unmodifiableMap(more);
    }

    /** The names of the protections asked, in the order they are. */
    public static List<String> names() {
        return List.copyOf(adapters.keySet());
    }

    /** Whether {@code actor} may do {@code action} to {@code pos} in {@code level}: no protection stops it. */
    public static boolean may(final Actor actor, final ProtectedAction action, final ServerLevel level,
                              final BlockPos pos) {
        return stoppedBy(actor, action, level, pos).isEmpty();
    }

    /** The name of the protection that stops {@code actor} doing {@code action} there, or none when none does. */
    public static Optional<String> stoppedBy(final Actor actor, final ProtectedAction action, final ServerLevel level,
                                             final BlockPos pos) {
        for (final Map.Entry<String, IProtectionAdapter> entry : adapters.entrySet()) {
            if (entry.getValue().ask(actor, action, level, pos) == IProtectionAdapter.Verdict.DENY) {
                LOGGER.debug("{} stops {} {} at {} in {}", entry.getKey(), actor, action.serializedName(), pos,
                        level.dimension().location());
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /**
     * What each protection answers for {@code actor} doing {@code action} to {@code pos} in {@code level}, in the order
     * they are asked: who allows it, who stops it and who says nothing, for whoever wants to see why.
     */
    public static Map<String, IProtectionAdapter.Verdict> verdicts(final Actor actor, final ProtectedAction action,
                                                                    final ServerLevel level, final BlockPos pos) {
        final Map<String, IProtectionAdapter.Verdict> out = new LinkedHashMap<>();
        adapters.forEach((name, adapter) -> out.put(name, adapter.ask(actor, action, level, pos)));
        return out;
    }

    /**
     * Who owns the claim {@code pos} of {@code level} lies in, player or team, as the first protection to know says.
     */
    public static Optional<UUID> claimOwner(final ServerLevel level, final BlockPos pos) {
        for (final IProtectionAdapter adapter : adapters.values()) {
            final Optional<UUID> owner = adapter.claimOwner(level, pos);
            if (owner.isPresent()) {
                return owner;
            }
        }
        return Optional.empty();
    }

    /**
     * Whether {@code pos} and {@code origin} lie in the same claim, as far as {@code adapter} knows: the world's own
     * work (a fire, a fluid, an explosion with nobody behind it) may cross into no claim. A place in no claim takes it
     * from anywhere.
     */
    public static boolean sameClaim(final IProtectionAdapter adapter, final ServerLevel level,
                                    @Nullable final BlockPos origin, final BlockPos pos) {
        final Optional<UUID> target = adapter.claimOwner(level, pos);
        if (target.isEmpty()) {
            return true;
        }
        return origin != null && target.equals(adapter.claimOwner(level, origin));
    }

    /**
     * Whether {@code attacker} may hurt {@code victim}: the world always may, and a player themselves; anyone else only
     * where the server allows PvP, where the claim the victim stands in does, and when the two are not on one team or
     * the server lets teammates hurt each other.
     */
    public static boolean mayHurt(final Actor attacker, final ServerPlayer victim) {
        final Optional<UUID> who = attacker.who();
        if (who.isEmpty() || who.get().equals(victim.getUUID())) {
            return true;
        }
        final MinecraftServer server = victim.server;
        if (!server.isPvpAllowed()) {
            return false;
        }
        if (!teammatesMayHurt(victim)
                && CoreTeams.teamOf(server, who.get()).equals(CoreTeams.teamOf(server, victim.getUUID()))) {
            return false;
        }
        return may(attacker, ProtectedAction.HURT, victim.serverLevel(), victim.blockPosition());
    }

    /** Who is behind {@code source}: a player, the owner of what dealt it, or the world. */
    public static Actor actorOf(final DamageSource source) {
        final Entity entity = source.getEntity();
        if (entity instanceof ServerPlayer player) {
            return Actor.of(player);
        }
        if (entity instanceof IOwned owned) {
            final Ownership ownership = owned.ownership();
            if (ownership != null) {
                return Actor.forOwner(ownership.owner());
            }
        }
        return Actor.world(entity == null ? null : entity.blockPosition());
    }

    /** Hands the game's bus what this answers, and the Core's own protections, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        register("minecraft:spawn", new SpawnProtection());
        NeoForge.EVENT_BUS.addListener(LivingIncomingDamageEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer victim && !mayHurt(actorOf(event.getSource()), victim)) {
                event.setCanceled(true);
            }
        });
    }

    /*
     * Whether players on one team may hurt each other: as the server's setting says, or, while the teams are the
     * game's scoreboard teams, as the victim's team says of its own friendly fire, as the game does.
     */
    private static boolean teammatesMayHurt(final ServerPlayer victim) {
        if (ProtectionBalance.teamDamage()) {
            return true;
        }
        if (CoreTeams.source() == ScoreboardTeams.INSTANCE) {
            final PlayerTeam team = victim.getTeam();
            return team == null || team.isAllowFriendlyFire();
        }
        return false;
    }
}
