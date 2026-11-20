/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.plant;

import com.mojang.serialization.MapCodec;
import dev.jstech.computers.block.ServerRackStructure;
import dev.jstech.core.bigblock.AbstractBigBlock;
import dev.jstech.core.bigblock.BigBlockFootprint;
import dev.jstech.core.bigblock.IBigBlockGeometry;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.util.BlockEntityTickers;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The CDU, a big block placed whole as the Supercomputer Rack is (computers/docs/design/temperature.md): the rack's
 * own 2 by 3 by 2 cabinet, its pumps and its plate exchanger in its bay. The origin, the block the player places, is
 * its bottom block at the front right as one faces its front; the rest are its parts ({@link CduPartBlock}); its water
 * ports are in its roof and its energy at its back ({@link CduBlockEntity}).
 */
public class CduBlock extends AbstractBigBlock {

    public static final MapCodec<CduBlock> CODEC = simpleCodec(CduBlock::new);
    private static final IBigBlockGeometry FOOTPRINT = new BigBlockFootprint(ServerRackStructure.PATTERN);

    public CduBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** Opens the CDU's machine screen for {@code player}, from its origin or any of its parts. */
    public static void open(final ServerPlayer player, final BlockPos origin, final CduBlockEntity cdu) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MachineMenu(id, inventory, cdu),
                cdu.getBlockState().getBlock().getName()), buf -> buf.writeBlockPos(origin));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction facing = context.getHorizontalDirection().getOpposite();
        final Level level = context.getLevel();
        final List<BlockPos> obstructed = getObstructedBlocks(level, context.getClickedPos(), facing);
        if (!obstructed.isEmpty()) {
            // No room for the cabinet: nothing is placed, and what is in the way is shown.
            spawnMisplaceParticles(level, obstructed);
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new CduBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        return level.isClientSide() ? null : BlockEntityTickers.create(type, CoolingPlant.CDU_BE.get(),
                MachineBlockEntity::serverTick);
    }

    @Override
    protected MapCodec<? extends CduBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected IBigBlockGeometry geometry() {
        return FOOTPRINT;
    }

    @Override
    protected boolean isOwnPart(final BlockState state) {
        return state.getBlock() instanceof CduPartBlock;
    }

    @Override
    protected boolean isOwnOrigin(final BlockState state) {
        return state.getBlock() instanceof CduBlock;
    }

    @Override
    protected BlockState partStateFor(final BlockPos origin, final Direction facing, final BlockPos part,
                                      final BlockState originState) {
        return CoolingPlant.CDU_PART.get().defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected void dropContents(final ServerLevel level, final BlockPos origin) {
        Block.popResource(level, origin, new ItemStack(asItem()));
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CduBlockEntity cdu) {
            open(serverPlayer, pos, cdu);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
