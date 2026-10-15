/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.protection;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * The game's own spawn protection, as a dedicated server keeps it: around the world's spawn, within the server's
 * radius, only an operator changes or uses the blocks, and only while the server has operators at all. A machine acting
 * for its owner is weighed as that owner, online or not; the world's own work is left to the game.
 */
final class SpawnProtection implements IProtectionAdapter {

    /** A name for an owner the server never knew by one, enough for its list of operators, which goes by id. */
    private static final String UNNAMED = "";

    @Override
    public Verdict ask(final Actor actor, final ProtectedAction action, final ServerLevel level, final BlockPos pos) {
        final MinecraftServer server = level.getServer();
        return switch (actor) {
            case Actor.Person person -> server.isUnderSpawnProtection(level, pos, person.player()) ? Verdict.DENY
                    : Verdict.ABSTAIN;
            case Actor.ForOwner owner -> protects(server, level, pos, owner.owner()) ? Verdict.DENY : Verdict.ABSTAIN;
            case Actor.World ignored -> Verdict.ABSTAIN;
        };
    }

    /* The dedicated server's rule, for a player who need not be online. */
    private static boolean protects(final MinecraftServer server, final ServerLevel level, final BlockPos pos,
                                    final UUID player) {
        if (!server.isDedicatedServer() || level.dimension() != Level.OVERWORLD
                || server.getPlayerList().getOps().isEmpty() || server.getSpawnProtectionRadius() <= 0
                || server.getPlayerList().isOp(new GameProfile(player, UNNAMED))) {
            return false;
        }
        final BlockPos spawn = level.getSharedSpawnPos();
        final int reach = Math.max(Math.abs(pos.getX() - spawn.getX()), Math.abs(pos.getZ() - spawn.getZ()));
        return reach <= server.getSpawnProtectionRadius();
    }
}
