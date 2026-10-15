/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.ftbchunks;

import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.property.PrivacyMode;
import dev.ftb.mods.ftbteams.api.property.PrivacyProperty;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.IProtectionAdapter;
import dev.jstech.core.protection.ProtectedAction;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

/**
 * FTB Chunks' claims, asked by the Core's protection: a player in person as FTB Chunks asks it of its own players; a
 * machine acting for its owner as that owner, online or not, by the claiming team's block edit or interaction mode
 * (its members, its allies too, or everyone) and the owner's bypass; the world's own work only inside the claim it
 * comes from, an explosion only where the team lets explosions in; and damage between players only where the team
 * allows PvP.
 */
final class FtbChunksProtection implements IProtectionAdapter {

    @Override
    public Verdict ask(final Actor actor, final ProtectedAction action, final ServerLevel level, final BlockPos pos) {
        final ClaimedChunk chunk = claimAt(level, pos);
        if (chunk == null) {
            return Verdict.ABSTAIN;
        }
        final ChunkTeamData team = chunk.getTeamData();
        if (action == ProtectedAction.HURT) {
            return team.allowPVP() ? Verdict.ABSTAIN : Verdict.DENY;
        }
        return switch (actor) {
            case Actor.Person person -> FTBChunksAPI.api().getManager().shouldPreventInteraction(person.player(),
                    InteractionHand.MAIN_HAND, pos, protectionOf(action), null) ? Verdict.DENY : Verdict.ALLOW;
            case Actor.ForOwner owner -> letsIn(team, owner.owner(), action) ? Verdict.ALLOW : Verdict.DENY;
            case Actor.World world -> worldMay(team, action, level, world.origin());
        };
    }

    @Override
    public Optional<UUID> claimOwner(final ServerLevel level, final BlockPos pos) {
        final ClaimedChunk chunk = claimAt(level, pos);
        return chunk == null ? Optional.empty() : Optional.of(chunk.getTeamData().getTeam().getId());
    }

    @Nullable
    private static ClaimedChunk claimAt(final ServerLevel level, final BlockPos pos) {
        if (!FTBChunksAPI.api().isManagerLoaded()) {
            return null;
        }
        return FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level, pos));
    }

    /* Whether the claiming team lets {@code player} do {@code action}, by its mode and the player's bypass. */
    private static boolean letsIn(final ChunkTeamData team, final UUID player, final ProtectedAction action) {
        final ClaimedChunkManager manager = team.getManager();
        if (manager.getBypassProtection(player)) {
            return true;
        }
        final PrivacyProperty property = action == ProtectedAction.USE ? FTBChunksProperties.BLOCK_INTERACT_MODE
                : FTBChunksProperties.BLOCK_EDIT_MODE;
        final PrivacyMode mode = team.getTeam().getProperty(property);
        return switch (mode) {
            case PUBLIC -> true;
            case ALLIES -> team.isTeamMember(player) || team.isAlly(player);
            case PRIVATE -> team.isTeamMember(player);
        };
    }

    /* An explosion goes where the team lets one; the world's other work stays inside the claim it comes from. */
    private Verdict worldMay(final ChunkTeamData team, final ProtectedAction action, final ServerLevel level,
                             @Nullable final BlockPos origin) {
        if (action == ProtectedAction.EXPLODE) {
            return team.canExplosionsDamageTerrain() ? Verdict.ALLOW : Verdict.DENY;
        }
        return origin != null && claimOwner(level, origin).equals(Optional.of(team.getTeam().getId()))
                ? Verdict.ALLOW : Verdict.DENY;
    }

    private static Protection protectionOf(final ProtectedAction action) {
        return action == ProtectedAction.USE ? Protection.INTERACT_BLOCK : Protection.EDIT_BLOCK;
    }
}
