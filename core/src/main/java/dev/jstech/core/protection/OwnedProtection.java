/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.protection;

import dev.jstech.core.multiblock.LevelStructures;
import dev.jstech.core.team.IOwned;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.text.GameText;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * What another player owns, kept from those its owner does not let: a block whose block entity is owned is broken only
 * by its owner and those trusted to break it, and so is every block of a formed multiblock whose controller is owned;
 * it is used only by those its access or its trust lets in; an owned entity the same, attacking it standing for
 * breaking it. Each is written to the audit log when the server keeps one.
 */
public final class OwnedProtection {

    private OwnedProtection() {
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(BlockEvent.BreakEvent.class, OwnedProtection::onBreak);
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, OwnedProtection::onUseBlock);
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.EntityInteract.class, OwnedProtection::onUseEntity);
        NeoForge.EVENT_BUS.addListener(AttackEntityEvent.class, OwnedProtection::onAttack);
    }

    private static void onBreak(final BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockEntity block = level.getBlockEntity(event.getPos());
        if (!(block instanceof IOwned)) {
            // A block of a formed multiblock is its controller's, whatever block it is.
            final BlockPos controller = LevelStructures.of(level).controllerHolding(event.getPos());
            if (controller != null && level.isLoaded(controller)) {
                block = level.getBlockEntity(controller);
            }
        }
        if (block instanceof IOwned owned && owned.ownership() != null) {
            final Ownership ownership = owned.ownership();
            if (!ownership.mayBreak(player)) {
                event.setCanceled(true);
                player.displayClientMessage(GameText.component(Ownership.BREAK_DENIED.with(
                        ownership.ownerName(level.getServer()))), true);
                return;
            }
            AuditLog.record(level, event.getPos(), player.getUUID(), AuditLog.Kind.BREAK,
                    BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()), ownership.owner());
        }
    }

    private static void onUseBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        final BlockPos pos = event.getPos();
        if (level.getBlockEntity(pos) instanceof IOwned owned && owned.ownership() != null) {
            final Ownership ownership = owned.ownership();
            if (!ownership.mayOpen(player)) {
                event.setCanceled(true);
                player.displayClientMessage(GameText.component(ownership.denial(level.getServer())), true);
                return;
            }
            AuditLog.record(level, pos, player.getUUID(), AuditLog.Kind.USE,
                    BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()), ownership.owner());
        }
    }

    private static void onUseEntity(final PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level
                && !allowed(player, level, event.getTarget(), false)) {
            event.setCanceled(true);
        }
    }

    private static void onAttack(final AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level
                && !allowed(player, level, event.getTarget(), true)) {
            event.setCanceled(true);
        }
    }

    /* Whether {@code player} may use, or break by attacking, an entity; written down when they may. */
    private static boolean allowed(final ServerPlayer player, final ServerLevel level, final Entity target,
                                   final boolean attack) {
        if (!(target instanceof IOwned owned) || owned.ownership() == null) {
            return true;
        }
        final Ownership ownership = owned.ownership();
        if (attack ? !ownership.mayBreak(player) : !ownership.mayOpen(player)) {
            player.displayClientMessage(GameText.component(attack ? Ownership.BREAK_DENIED.with(
                    ownership.ownerName(level.getServer())) : ownership.denial(level.getServer())), true);
            return false;
        }
        AuditLog.record(level, target.blockPosition(), player.getUUID(), attack ? AuditLog.Kind.BREAK
                : AuditLog.Kind.USE, BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()), ownership.owner());
        return true;
    }
}
