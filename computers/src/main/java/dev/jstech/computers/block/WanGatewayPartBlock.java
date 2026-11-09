/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import dev.jstech.computers.advancement.MachineOperators;
import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.computers.blockentity.WanGatewayPartBlockEntity;
import dev.jstech.core.bigblock.AbstractBigBlock;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLines;
import dev.jstech.core.jstechseries.peripheral.PeripheralLine;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A part of the WAN Gateway Computer, a big block: one of the 15 blocks round its origin. Every part takes the
 * backbone and the peripheral cable on any face as the origin does; the four of the bottom back row also take, on
 * their backs, the long-distance line of the gateway's era and of every earlier one, a port each.
 */
public class WanGatewayPartBlock extends HorizontalDirectionalBlock implements EntityBlock, IFaceConnector {

    public static final MapCodec<WanGatewayPartBlock> CODEC = simpleCodec(WanGatewayPartBlock::new);
    /** The era the part inherits from its origin, so the whole footprint wears one skin ({@link PartEras}). */
    public static final IntegerProperty ERA = PartEras.property();
    /** The port on the part's back, 1 to 4, or 0 for a part with none. */
    public static final IntegerProperty PORT = IntegerProperty.create("port", 0, WanGatewayStructure.PORTS);
    /* What any part may take, for the cables to know which lines it is a device of: every era's. */
    private static final FacePorts PORTS = portsOf(HardwareEra.ADVANCED, true);
    /* What a part of each era's gateway takes, by the era's place among the series' seven: without a port, with one. */
    private static final List<FacePorts> BY_ERA = byEra(false);
    private static final List<FacePorts> BY_ERA_WITH_PORT = byEra(true);

    public WanGatewayPartBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ERA, PartEras.valueFor(HardwareEra.STANDARD))
                .setValue(PORT, 0)
                .setValue(OpticalPort.OPTICAL, false));
    }

    @Override
    public FacePorts ports() {
        return PORTS;
    }

    /*
     * The cables of the gateway's own era and the earlier ones, read from the era its origin stamps on every part; the
     * fibre only while the gateway holds an Optical Network Card.
     */
    @Override
    public boolean accepts(final BlockState state, final Direction face, final Connection offered) {
        final List<FacePorts> byEra = state.getValue(PORT) > 0 ? BY_ERA_WITH_PORT : BY_ERA;
        return byEra.get(state.getValue(ERA)).accepts(state, face, offered) && OpticalPort.admits(state, offered);
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state,
                                        final Player player) {
        // The whole gateway goes with any of its blocks, so a player's break here is a break of the gateway.
        if (level instanceof ServerLevel serverLevel && origin(level, pos) instanceof BlockPos origin
                && level.getBlockState(origin).getBlock() instanceof AbstractBigBlock big) {
            big.brokenBy(serverLevel, origin, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** What the middle mouse button picks off any block of the cabinet: the gateway itself, in its own era. */
    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final LevelReader level,
                                       final BlockPos pos, final Player player) {
        final BlockPos origin = origin(level, pos);
        if (origin != null && level.getBlockState(origin).getBlock() instanceof WanGatewayBlock gateway) {
            return new ItemStack(gateway);
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new WanGatewayPartBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<WanGatewayPartBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ERA, PORT, OpticalPort.OPTICAL);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        final BlockPos origin = origin(level, pos);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && origin != null
                && level.getBlockEntity(origin) instanceof WanGatewayBlockEntity gateway) {
            MachineOperators.note(gateway, player);
            // Sneaking anywhere on the cabinet takes its service panel off, the same as on the origin.
            if (player.isShiftKeyDown()) {
                gateway.toggleServicePanel();
            } else {
                WanGatewayBlock.open(serverPlayer, origin, gateway);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
                            final boolean movedByPiston) {
        final BlockPos origin = origin(level, pos);
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel && origin != null
                && level.getBlockState(origin).getBlock() instanceof AbstractBigBlock big) {
            big.dissolve(serverLevel, origin, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /* The origin of the gateway the part at {@code pos} belongs to, or null when it has lost it. */
    @Nullable
    private static BlockPos origin(final LevelReader level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof WanGatewayPartBlockEntity part ? part.originPos() : null;
    }

    /* What a part of a gateway of {@code era} takes, with the port of the long-distance line on its back or not. */
    private static FacePorts portsOf(final Era era, final boolean port) {
        final FacePorts.Builder ports = FacePorts.builder()
                .port(FaceRule.EVERY, DataLines.upTo(era, DataLine.BACKBONE), PeripheralLine.of(era));
        if (port) {
            ports.port(FaceRule.BACK, DataLines.upTo(era, DataLine.LONG_DISTANCE));
        }
        return ports.build();
    }

    private static List<FacePorts> byEra(final boolean port) {
        final List<FacePorts> ports = new ArrayList<>();
        for (int value = 0; value <= PartEras.LAST; value++) {
            ports.add(portsOf(PartEras.eraOf(value), port));
        }
        return List.copyOf(ports);
    }
}
