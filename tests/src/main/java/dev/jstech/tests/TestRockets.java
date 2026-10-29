/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.rocket.RocketStack;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A stand that shows a rocket's formed parts, as the hangar and the moving structure that carries a rocket will: it
 * holds a stack of parts and whether its legs are spread, and its renderer draws the stack's models standing on it.
 */
public final class TestRockets {

    public static final BlockEntry<DisplayBlock> DISPLAY = TestSounds.CONTENT.block("test_rocket_display",
            DisplayBlock::new).named("Test rocket display").look(IBlockLook.cubeAll("test_rocket_display")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DisplayEntity>> DISPLAY_ENTITY =
            TestSounds.CONTENT.blockEntity("test_rocket_display", DisplayEntity::new, DISPLAY);

    private TestRockets() {
    }

    /** Declares the stand, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** The stand: drawn only as the rocket it shows. */
    public static final class DisplayBlock extends BaseEntityBlock {

        private static final MapCodec<DisplayBlock> CODEC = simpleCodec(DisplayBlock::new);

        public DisplayBlock(final Properties properties) {
            super(properties.noOcclusion());
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new DisplayEntity(pos, state);
        }

        @Override
        protected RenderShape getRenderShape(final BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }
    }

    /** What the stand shows: a rocket's stack, whether its legs are spread to land, and what its cabins read. */
    public static final class DisplayEntity extends SyncedBlockEntity {

        private final ValueField<RocketStack> stack;
        private final ValueField<Boolean> legsDown;
        private final ValueField<CabinReadings> readings;

        public DisplayEntity(final BlockPos pos, final BlockState state) {
            super(DISPLAY_ENTITY.get(), pos, state);
            this.stack = fields().value("Stack", RocketStack.CODEC, new RocketStack(List.of())).save().toClient();
            this.legsDown = fields().value("LegsDown", Codec.BOOL, false).save().toClient();
            this.readings = fields().value("Readings", CabinReadings.CODEC, CabinReadings.AT_REST).save().toClient();
        }

        /** What its rocket's cabins read. */
        public CabinReadings readings() {
            final CabinReadings shown = this.readings.get();
            return shown == null ? CabinReadings.AT_REST : shown;
        }

        /** Has its rocket's cabins read {@code read}. */
        public void read(final CabinReadings read) {
            this.readings.set(read);
            setChanged();
        }

        /** The rocket it shows. */
        public RocketStack stack() {
            final RocketStack shown = this.stack.get();
            return shown == null ? new RocketStack(List.of()) : shown;
        }

        /** Whether its legs are spread. */
        public boolean legsDown() {
            return Boolean.TRUE.equals(this.legsDown.get());
        }

        /** Shows {@code rocket}, its legs spread or not. */
        public void show(final RocketStack rocket, final boolean spread) {
            this.stack.set(rocket);
            this.legsDown.set(spread);
            setChanged();
        }
    }
}
