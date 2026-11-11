/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.AsicBlockEntity;
import dev.jstech.computers.menu.AsicMenu;
import dev.jstech.computers.teracoin.AsicModel;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLines;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.util.BlockEntityTickers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * An ASIC (computers/docs/design/teracoin.md): Blockwright's mining machine of its era, one block, its fans at either
 * end. It takes the data cable of its era or an earlier one on any face, and its energy there too; a click opens its
 * window, where its payout address and its pool are set.
 */
public class AsicBlock extends HorizontalDirectionalBlock implements EntityBlock, IFaceConnector,
        IEraChassisBlock {

    private final AsicModel model;
    private final FacePorts ports;

    public static final MapCodec<AsicBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            Codec.STRING.xmap(AsicModel::byName, AsicModel::name).fieldOf("model").forGetter(AsicBlock::model)
    ).apply(i, AsicBlock::new));
    /*
     * Its chassis on its feet with its fans at either end, as its model stands: a little narrower than the block across
     * its front, lower than its top; turned a quarter when it faces east or west.
     */
    private static final VoxelShape ALONG_Z = Block.box(1.5, 0.0, 0.5, 14.5, 12.5, 15.5);
    private static final VoxelShape ALONG_X = Block.box(0.5, 0.0, 1.5, 15.5, 12.5, 14.5);

    public AsicBlock(final Properties properties, final AsicModel model) {
        super(properties);
        this.model = model;
        this.ports = FacePorts.builder().port(FaceRule.EVERY, DataLines.upTo(model.era(), DataLine.ACCESS)).build();
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** The model it is: its hashrate, its draw, its hashboards. */
    public AsicModel model() {
        return model;
    }

    @Override
    public Era chassisEra() {
        return model.era();
    }

    @Override
    public FacePorts ports() {
        return ports;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new AsicBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        return level.isClientSide() ? null : BlockEntityTickers.create(type, ComputingModule.ASIC_BE.get(),
                AsicBlockEntity::serverTick);
    }

    @Override
    protected MapCodec<? extends AsicBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /* The machine is one model drawn by its block entity; the block itself paints nothing over it. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ALONG_Z : ALONG_X;
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AsicBlockEntity asic) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new AsicMenu(id, inventory, asic),
                    getName()), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
