/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipFinishes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A stand that shows a starship's formed parts, as the shipyard and the moving structure that carries a ship will: it
 * holds a design and the finish it is painted in, and its renderer draws the ship's models on it.
 */
public final class TestShips {

    public static final BlockEntry<DisplayBlock> DISPLAY = TestSounds.CONTENT.block("test_ship_display",
            DisplayBlock::new).named("Test ship display").look(IBlockLook.cubeAll("test_ship_display")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DisplayEntity>> DISPLAY_ENTITY =
            TestSounds.CONTENT.blockEntity("test_ship_display", DisplayEntity::new, DISPLAY);
    /* A stand showing nothing. */
    private static final ShipDesign NONE = new ShipDesign("", List.of(), List.of());

    private TestShips() {
    }

    /** Declares the stand, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** The stand: drawn only as the ship it shows. */
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

    /** What the stand shows: a ship's design and the finish it is painted in. */
    public static final class DisplayEntity extends SyncedBlockEntity {

        private final ValueField<ShipDesign> design;
        private final ValueField<ResourceLocation> finish;

        public DisplayEntity(final BlockPos pos, final BlockState state) {
            super(DISPLAY_ENTITY.get(), pos, state);
            this.design = fields().value("Design", ShipDesign.CODEC, NONE).save().toClient();
            this.finish = fields().value("Finish", ResourceLocation.CODEC, SmallShipFinishes.DEFAULT).save()
                    .toClient();
        }

        /** The ship it shows. */
        public ShipDesign design() {
            final ShipDesign shown = this.design.get();
            return shown == null ? NONE : shown;
        }

        /** The finish its ship is painted in. */
        public ResourceLocation finish() {
            final ResourceLocation shown = this.finish.get();
            return shown == null ? SmallShipFinishes.DEFAULT : shown;
        }

        /** Shows {@code ship}, painted in {@code paint}. */
        public void show(final ShipDesign ship, final ResourceLocation paint) {
            this.design.set(ship);
            this.finish.set(paint);
            setChanged();
        }
    }
}
