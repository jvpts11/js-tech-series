/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.jstech.core.moving.StructureCollisions;
import dev.jstech.core.moving.StructureFooting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a player walk a moving deck on a server. The game takes the place a player's game says it moved to, checked
 * against where the server has them; on a deck that moves, the player's game and the server see the deck a moment
 * apart, so the player's game says instead where on the deck they stand ({@code StructureFootingPayload}) and the
 * server puts them there on its own deck; a vehicle the player drives on a deck is put where its driver's game says it
 * stands the same way. And the game sends away a player off the ground too long, counting only the world's blocks: one
 * standing on a deck stands on blocks. A player not on a structure moves as before.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @WrapOperation(method = "handleMovePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;getX(D)D"))
    private double jscore$standOnTheDeckX(final ServerboundMovePlayerPacket packet, final double otherwise,
                                          final Operation<Double> original) {
        final Vec3 deck = packet.hasPosition() ? StructureFooting.reportedPlace(player) : null;
        return deck != null ? deck.x : original.call(packet, otherwise);
    }

    @WrapOperation(method = "handleMovePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;getY(D)D"))
    private double jscore$standOnTheDeckY(final ServerboundMovePlayerPacket packet, final double otherwise,
                                          final Operation<Double> original) {
        final Vec3 deck = packet.hasPosition() ? StructureFooting.reportedPlace(player) : null;
        return deck != null ? deck.y : original.call(packet, otherwise);
    }

    @WrapOperation(method = "handleMovePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;getZ(D)D"))
    private double jscore$standOnTheDeckZ(final ServerboundMovePlayerPacket packet, final double otherwise,
                                          final Operation<Double> original) {
        final Vec3 deck = packet.hasPosition() ? StructureFooting.reportedPlace(player) : null;
        return deck != null ? deck.z : original.call(packet, otherwise);
    }

    @WrapOperation(method = "handleMoveVehicle", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ServerboundMoveVehiclePacket;getX()D"))
    private double jscore$driveOnTheDeckX(final ServerboundMoveVehiclePacket packet,
                                          final Operation<Double> original) {
        final Vec3 deck = StructureFooting.reportedVehiclePlace(player);
        return deck != null ? deck.x : original.call(packet);
    }

    @WrapOperation(method = "handleMoveVehicle", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ServerboundMoveVehiclePacket;getY()D"))
    private double jscore$driveOnTheDeckY(final ServerboundMoveVehiclePacket packet,
                                          final Operation<Double> original) {
        final Vec3 deck = StructureFooting.reportedVehiclePlace(player);
        return deck != null ? deck.y : original.call(packet);
    }

    @WrapOperation(method = "handleMoveVehicle", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ServerboundMoveVehiclePacket;getZ()D"))
    private double jscore$driveOnTheDeckZ(final ServerboundMoveVehiclePacket packet,
                                          final Operation<Double> original) {
        final Vec3 deck = StructureFooting.reportedVehiclePlace(player);
        return deck != null ? deck.z : original.call(packet);
    }

    /*
     * A player standing on a deck is put where their game says they stand on it: the game's check that their move
     * met no block it had not met already compares the deck's blocks as the server has them with a move their game
     * made against the deck as it drew it a moment apart, and would send them back for a hair's difference.
     */
    @Inject(method = "isPlayerCollidingWithAnythingNew", at = @At("HEAD"), cancellable = true)
    private void jscore$deckMovesAreTheirGames(final LevelReader level, final AABB box, final double x,
                                               final double y, final double z,
                                               final CallbackInfoReturnable<Boolean> info) {
        if (StructureFooting.reportedPlace(player) != null) {
            info.setReturnValue(false);
        }
    }

    @Inject(method = "noBlocksAround", at = @At("RETURN"), cancellable = true)
    private void jscore$deckIsGround(final Entity entity, final CallbackInfoReturnable<Boolean> info) {
        if (!info.getReturnValueZ()) {
            return;
        }
        final AABB below = entity.getBoundingBox().inflate(0.0625).expandTowards(0, -0.55, 0);
        if (StructureFooting.standingOn(entity) != null
                || StructureCollisions.overlaps(entity.level(), entity, below)) {
            info.setReturnValue(false);
        }
    }
}
