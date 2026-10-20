/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.YardLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a player reach the blocks of a moving structure: every block a player uses, breaks, opens or keeps a screen of
 * is checked to be within their reach, and the game measures it to the block's place, out in the yard; so a chest on
 * a deck would close the tick after it opened. A block of a structure is measured where it is in the world; any other
 * block as before.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "canInteractWithBlock", at = @At("HEAD"), cancellable = true)
    private void jscore$reachStructureBlocks(final BlockPos pos, final double distance,
                                             final CallbackInfoReturnable<Boolean> info) {
        if (!YardLayout.isYardBlock(pos.getX())) {
            return;
        }
        final Player player = (Player) (Object) this;
        final IStructureBody body = MovingStructures.at(player.level(), pos);
        if (body == null) {
            info.setReturnValue(false);
            return;
        }
        final StructureFrame frame = body.frame();
        final Vec3 eye = player.getEyePosition();
        final Vec3 seen = new Vec3(frame.yardX(eye.x, eye.y, eye.z), frame.yardY(eye.x, eye.y, eye.z),
                frame.yardZ(eye.x, eye.y, eye.z));
        final double reach = player.blockInteractionRange() + distance;
        info.setReturnValue(new AABB(pos).distanceToSqr(seen) < reach * reach);
    }
}
