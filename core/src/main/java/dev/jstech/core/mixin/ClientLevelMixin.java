/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.YardLayout;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Plays the sounds of a moving structure's blocks where the blocks are: a door, a chest, a block broken or set down,
 * a note block. The game plays a block's sound at its place, out in the yard, too far for anyone to hear; a sound of
 * the yard is played where its block is in the world. Every other sound plays as before.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @WrapMethod(method = "playSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V")
    private void jscore$soundWhereTheBlockIs(final double x, final double y, final double z, final SoundEvent sound,
                                             final SoundSource source, final float volume, final float pitch,
                                             final boolean delayed, final long seed, final Operation<Void> original) {
        if (YardLayout.isYard(x)) {
            final Vec3 there = MovingStructures.inWorld((ClientLevel) (Object) this, new Vec3(x, y, z));
            original.call(there.x, there.y, there.z, sound, source, volume, pitch, delayed, seed);
            return;
        }
        original.call(x, y, z, sound, source, volume, pitch, delayed, seed);
    }
}
