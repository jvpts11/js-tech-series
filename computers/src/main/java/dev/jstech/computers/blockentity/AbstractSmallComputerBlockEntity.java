/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A small computer, one block: the Personal Computer, the Crafting Computer and the Cluster Management Computer. Its
 * case is drawn by its block entity, from the model its block names: the tower of its age, or in the later ages the
 * one of three cases it comes in.
 */
public abstract class AbstractSmallComputerBlockEntity extends AbstractComputerBlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);

    protected AbstractSmallComputerBlockEntity(final BlockEntityType<?> type, final BlockPos pos,
                                               final BlockState state, final ComputerHardwareLayout layout) {
        super(type, pos, state, layout);
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        // The case stands still: nothing in it moves yet.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }
}
