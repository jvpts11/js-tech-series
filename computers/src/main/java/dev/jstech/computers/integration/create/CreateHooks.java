/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.create;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries;
import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries.BlockEntityTransformer;
import dev.jstech.core.machine.IMachineParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The side of J's Computers' work with Create that names Create's classes, loaded only when Create is met: the check
 * Create asks before it moves a block, what is done to a computer it sets down, and the machine parameters every
 * kinetic block of Create's, or of a mod built on it, gives the industrial control cable.
 */
final class CreateHooks {

    /* What is done to a computer's block entity when a contraption sets it down, whichever way it turned on the way. */
    private static final BlockEntityTransformer SET_DOWN = (block, transform) -> Contraptions.setDown(block);

    private CreateHooks() {
    }

    /** Asks Create to check J's Computers' machines before it moves them, and gives its kinetic blocks their values. */
    static void start(final IEventBus modBus) {
        BlockMovementChecks.registerMovementAllowedCheck(CreateHooks::allowed);
        MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.registerProvider(CreateHooks::transformerOf);
        modBus.addListener(RegisterCapabilitiesEvent.class, CreateHooks::capabilities);
    }

    private static CheckResult allowed(final BlockState state, final Level level, final BlockPos pos) {
        return Contraptions.mayCarry(state, level, pos) ? CheckResult.PASS : CheckResult.FAIL;
    }

    /*
     * Only a computer's kind: Create turns a block entity of any other kind as it always does, which a transformer
     * given for it would take the place of.
     */
    @Nullable
    private static BlockEntityTransformer transformerOf(final BlockEntityType<?> kind) {
        return Contraptions.keepsComputer(kind) ? SET_DOWN : null;
    }

    /*
     * Every kind of block entity may be one of Create's kinetic blocks (a mod built on Create adds its own): each is
     * asked, and only a kinetic one answers.
     */
    private static void capabilities(final RegisterCapabilitiesEvent event) {
        for (final BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            register(event, type);
        }
    }

    private static <T extends BlockEntity> void register(final RegisterCapabilitiesEvent event,
                                                         final BlockEntityType<T> type) {
        event.registerBlockEntity(IMachineParameters.CAPABILITY, type, (block, side) -> KineticParameters.of(block));
    }
}
