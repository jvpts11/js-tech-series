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
import com.simibubi.create.content.contraptions.StructureTransform;
import dev.jstech.core.bigblock.BigBlockPartBlockEntity;
import dev.jstech.core.machine.IMachineParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The side of J's Computers' work with Create that names Create's classes, loaded only when Create is met: the checks
 * Create asks before it moves a block, what is done to J's Computers' block entities it sets down, and the machine
 * parameters every kinetic block of Create's, or of a mod built on it, gives the industrial control cable.
 */
final class CreateHooks {

    /*
     * What is done to one of J's Computers' block entities when a contraption sets it down, whichever way it turned on
     * the way: a part of a big machine turns its way to the origin as its blocks turned, and a computer comes down off.
     */
    private static final BlockEntityTransformer SET_DOWN = (block, transform) -> {
        if (block instanceof BigBlockPartBlockEntity part) {
            part.turn(way -> turned(transform, way));
        }
        Contraptions.setDown(block);
    };

    private CreateHooks() {
    }

    /** Asks Create to check J's Computers' machines before it moves them, and gives its kinetic blocks their values. */
    static void start(final IEventBus modBus) {
        BlockMovementChecks.registerMovementAllowedCheck(CreateHooks::allowed);
        BlockMovementChecks.registerAttachedCheck(CreateHooks::attached);
        MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.registerProvider(CreateHooks::transformerOf);
        modBus.addListener(RegisterCapabilitiesEvent.class, CreateHooks::capabilities);
    }

    private static CheckResult allowed(final BlockState state, final Level level, final BlockPos pos) {
        return Contraptions.mayCarry(state, level, pos) ? CheckResult.PASS : CheckResult.FAIL;
    }

    /* A block of a big machine holds on to the machine's other blocks, so a contraption takes the machine whole. */
    private static CheckResult attached(final BlockState state, final Level level, final BlockPos pos,
                                        final Direction towards) {
        return Contraptions.sameMachine(level, pos, pos.relative(towards)) ? CheckResult.SUCCESS : CheckResult.PASS;
    }

    /*
     * A computer's kind, any mod's, and J's Computers' own: none of these is a block entity Create turns itself, so the
     * transformer takes nothing from it, and every other kind Create turns as it always does.
     */
    @Nullable
    private static BlockEntityTransformer transformerOf(final BlockEntityType<?> kind) {
        return Contraptions.setsDown(kind) ? SET_DOWN : null;
    }

    /*
     * The way {@code way} once the contraption turned it: where the block that far off went, from where the block at
     * the start went. Both are turned about the same centre, so the difference is the way turned, mirrored too.
     */
    private static Vec3i turned(final StructureTransform transform, final Vec3i way) {
        return transform.applyWithoutOffset(new BlockPos(way)).subtract(transform.applyWithoutOffset(BlockPos.ZERO));
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
