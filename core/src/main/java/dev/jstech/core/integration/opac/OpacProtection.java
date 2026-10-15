/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.opac;

import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.IProtectionAdapter;
import dev.jstech.core.protection.ProtectedAction;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.protection.api.IChunkProtectionAPI;

/**
 * Open Parties and Claims' claims, asked by the Core's protection: whoever the claim gives access to the chunk, a
 * player in person or the owner a machine acts for, online or not, may change and use it; the world's own work stays
 * inside the claim it comes from. Damage between players is left to Open Parties and Claims' own protection.
 */
final class OpacProtection implements IProtectionAdapter {

    @Override
    public Verdict ask(final Actor actor, final ProtectedAction action, final ServerLevel level, final BlockPos pos) {
        final IPlayerChunkClaimAPI claim = claimAt(level, pos);
        if (claim == null || action == ProtectedAction.HURT) {
            return Verdict.ABSTAIN;
        }
        final IChunkProtectionAPI protection = OpenPACServerAPI.get(level.getServer()).getChunkProtection();
        final ResourceLocation dimension = level.dimension().location();
        final int chunkX = pos.getX() >> 4;
        final int chunkZ = pos.getZ() >> 4;
        final boolean allowed = switch (actor) {
            case Actor.Person person -> protection.hasChunkAccess(person.player(), dimension, chunkX, chunkZ);
            case Actor.ForOwner owner -> protection.hasChunkAccess(owner.owner(), dimension, chunkX, chunkZ);
            case Actor.World world -> world.origin() != null
                    && claimOwner(level, world.origin()).equals(Optional.of(claim.getPlayerId()));
        };
        return allowed ? Verdict.ALLOW : Verdict.DENY;
    }

    @Override
    public Optional<UUID> claimOwner(final ServerLevel level, final BlockPos pos) {
        final IPlayerChunkClaimAPI claim = claimAt(level, pos);
        return claim == null ? Optional.empty() : Optional.of(claim.getPlayerId());
    }

    @Nullable
    private static IPlayerChunkClaimAPI claimAt(final ServerLevel level, final BlockPos pos) {
        return OpenPACServerAPI.get(level.getServer()).getServerClaimsManager().get(level.dimension().location(), pos);
    }
}
