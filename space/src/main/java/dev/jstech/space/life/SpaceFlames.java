/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.life;

import dev.jstech.core.room.Flames;
import dev.jstech.core.room.IFlame;
import dev.jstech.space.JsSpace;
import dev.jstech.space.config.SpaceServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The game's open flames as J's Space has them: torches, lanterns, candles and campfires, the block tag
 * {@code jsspace:flames} a data pack may add to, burn the air's oxygen as fire does and go out where it holds too
 * little for a fire, in a vacuum, in the thin air of Mars, in a room whose air runs short, while flames need air in
 * J's Space's settings. Put out, a candle or a campfire is left unlit, and a torch or a lantern, which the game has no
 * unlit form of, falls as an item with a puff of smoke. J's Core puts out the fires and the furnaces itself.
 */
public final class SpaceFlames implements IFlame {

    /** The blocks that burn an open flame. */
    public static final TagKey<Block> FLAMES = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "flames"));
    /* The smoke a flame put out gives off. */
    private static final int SMOKE = 6;
    private static final double SMOKE_SPREAD = 0.15;
    private static final double SMOKE_SPEED = 0.01;
    private static final double MIDDLE = 0.5;

    private SpaceFlames() {
    }

    /** Declares the flames, once, from the mod's constructor. */
    public static void declare() {
        Flames.declare(new SpaceFlames());
    }

    @Override
    public boolean burning(final BlockState state) {
        return state.is(FLAMES) && (!state.hasProperty(BlockStateProperties.LIT)
                || state.getValue(BlockStateProperties.LIT)) && SpaceServerConfig.flamesNeedAir();
    }

    @Override
    public void putOut(final ServerLevel level, final BlockPos pos, final BlockState state) {
        if (state.getBlock() instanceof AbstractCandleBlock) {
            // The game's own way, with the candle's own sound and smoke.
            AbstractCandleBlock.extinguish(null, state, level, pos);
            return;
        }
        if (state.getBlock() instanceof CampfireBlock) {
            CampfireBlock.dowse(null, level, pos, state);
            level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
        } else if (state.hasProperty(BlockStateProperties.LIT)) {
            level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LIT, false));
        } else {
            level.destroyBlock(pos, true);
        }
        level.levelEvent(LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + MIDDLE, pos.getY() + MIDDLE, pos.getZ() + MIDDLE, SMOKE,
                SMOKE_SPREAD, SMOKE_SPREAD, SMOKE_SPREAD, SMOKE_SPEED);
    }
}
