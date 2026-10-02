/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.core.blockentity.BoolField;
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
    /** The left side taken off the case, showing what the player put inside; the world and the screen both show it. */
    private final BoolField sidePanelOff = fields().flag("SidePanelOff", false).save().toClient().toMenu();

    protected AbstractSmallComputerBlockEntity(final BlockEntityType<?> type, final BlockPos pos,
                                               final BlockState state, final ComputerHardwareLayout layout) {
        super(type, pos, state, layout);
    }

    /** Whether the left side is off the case. */
    public boolean sidePanelOff() {
        return sidePanelOff.get();
    }

    /** Takes the left side off the case, or puts it back. */
    public void toggleSidePanel() {
        sidePanelOff.set(!sidePanelOff.get());
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
