/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.menu.CraftingComputerMenu;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.network.IRearFacingDataPort;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.IPeripheralConnectable;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.BlockDrops;
import dev.jstech.core.util.BlockEntityTickers;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Crafting Computer block: an ATX-class computer that executes recipes for the network.
 */
public class CraftingComputerBlock extends HorizontalDirectionalBlock
        implements EntityBlock, IRearFacingDataPort, IPeripheralConnectable, IEraChassisBlock {

    public static final MapCodec<CraftingComputerBlock> CODEC = simpleCodec(CraftingComputerBlock::new);

    public CraftingComputerBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /**
     * The hardware era this Crafting Computer belongs to. It selects the block's skin and gates which
     * board the machine accepts: only a board of this era (and of the era's form factor) installs. The
     * base block is the Standard era; the Vintage and Legacy variants override this.
     */
    public HardwareEra era() {
        return HardwareEra.STANDARD;
    }

    @Override
    public HardwareEra chassisEra() {
        return era();
    }

    @Override
    protected MapCodec<? extends CraftingComputerBlock> codec() {
        return CODEC;
    }

    @Override
    public PeripheralCableType peripheralType() {
        return PeripheralCableType.COMPUTING;
    }

    @Override
    public Set<DataTier> acceptedCableTiers() {
        /*
         * Data via Ethernet (rear port, through a Personal Router to the backbone) plus the crafting cable
         * that runs to the Crafting Switches.
         */
        return Set.of(DataTier.T1_ETHERNET, DataTier.CRAFTING);
    }

    @Override
    public boolean connectsOnFace(final BlockState state,
                                  final Direction face, final DataTier tier) {
        /*
         * The crafting cable attaches on any face (the machine-delivery search walks out of all six);
         * the data cable keeps the rear-only port.
         */
        return tier == DataTier.CRAFTING || connectsOnFace(state, face);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(
            final BlockState state, final Level level, final BlockPos pos,
            final Player player,
            final BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CraftingComputerBlockEntity computer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new CraftingComputerMenu(
                                    id, inventory, computer),
                            Component.translatable("block.jsc.crafting_computer")),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof CraftingComputerBlockEntity computer) {
            computer.onBroken(serverLevel); // drop this computer's network-node registration
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state,
                                        final Player player) {
        // Spill the installed hardware so a broken Crafting Computer never destroys its components.
        if (level instanceof ServerLevel serverLevel
                && !player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof CraftingComputerBlockEntity computer) {
            BlockDrops.spill(serverLevel, pos, computer.getHardware());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new CraftingComputerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return BlockEntityTickers.create(type, ComputingModule.CRAFTING_COMPUTER_BE.get(),
                CraftingComputerBlockEntity::serverTick);
    }

}
