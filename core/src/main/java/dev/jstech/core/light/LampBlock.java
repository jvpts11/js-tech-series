/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jetbrains.annotations.Nullable;

/**
 * A lamp of the lighting kit: it gives the light of its kind ({@link LightKind}, by id from {@link LightKinds}),
 * facing the way it was placed against, which a spotlight shines. Its light is the game's own, set by its block
 * entity ({@link LampBlockEntity}) through NeoForge's light of block entities, so it turns on and off, flickers and
 * dims with no state of its own, and counts for everything light counts for. Its model and recipe are its mod's.
 *
 * <pre>{@code
 * BlockEntry<LampBlock> ALARM = CONTENT.block("alarm_lamp", properties -> new LampBlock(properties,
 *         MyLights.ALARM, () -> LAMP.get())).named("Alarm Lamp").register();
 * DeferredHolder<BlockEntityType<?>, BlockEntityType<LampBlockEntity>> LAMP = CONTENT.blockEntity("lamp",
 *         (pos, state) -> new LampBlockEntity(LAMP.get(), pos, state), ALARM, FLOODLIGHT);
 * }</pre>
 */
public class LampBlock extends DirectionalBlock implements EntityBlock {

    private final ResourceLocation kind;
    private final Supplier<? extends BlockEntityType<? extends LampBlockEntity>> type;
    private final MapCodec<LampBlock> codec;

    public LampBlock(final Properties properties, final ResourceLocation kind,
                     final Supplier<? extends BlockEntityType<? extends LampBlockEntity>> type) {
        super(properties);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.type = Objects.requireNonNull(type, "type");
        this.codec = simpleCodec(made -> new LampBlock(made, kind, type));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    /** The id of its kind of light. */
    public ResourceLocation kind() {
        return this.kind;
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public boolean hasDynamicLightEmission(final BlockState state) {
        return true;
    }

    @Override
    public int getLightEmission(final BlockState state, final BlockGetter level, final BlockPos pos) {
        final AuxiliaryLightManager lights = level.getAuxLightManager(pos);
        return lights == null ? 0 : lights.getLightAt(pos);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return this.type.get().create(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                          final BlockEntityType<T> type) {
        if (level.isClientSide() || type != this.type.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<LampBlockEntity>) LampBlockEntity::serverTick;
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                   final Block neighbour, final BlockPos neighbourPos, final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbour, neighbourPos, movedByPiston);
        if (level.getBlockEntity(pos) instanceof LampBlockEntity lamp) {
            lamp.hear(level);
        }
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
                            final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LampBlockEntity lamp) {
            lamp.goDark(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
