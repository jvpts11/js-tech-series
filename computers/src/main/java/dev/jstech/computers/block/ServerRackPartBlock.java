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
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerRackPartBlockEntity;
import dev.jstech.computers.menu.ServerRackMenu;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.multiblock.AbstractMultiblockControllerBlock;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.peripheral.PeripheralLine;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A structural part of the Server Rack, one of the 11 non-controller blocks of the 2x3x2 cabinet.
 */
public class ServerRackPartBlock extends Block implements EntityBlock, IFaceConnector {

    public static final MapCodec<ServerRackPartBlock> CODEC = simpleCodec(ServerRackPartBlock::new);

    public static final BooleanProperty TOP = BooleanProperty.create("top");

    public static final BooleanProperty FRONT = BooleanProperty.create("front");
    /**
     * Whether this part belongs to a Supercomputer Rack. The parts are one shared block, and a model
     * cannot ask the controller what cabinet it is, so the controller stamps the cabinet type on each
     * part it raises, and that is what lets the whole cabinet wear one livery, not just its base block.
     */
    public static final BooleanProperty COMPUTE = BooleanProperty.create("compute");

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    /*
     * The backbone and the compute fabric on the back, every era's: a part does not know its cabinet's age, so the
     * cabinet's own block is where the age is held to. A rack is on the backbone; the access line is for the small
     * computers, which reach it through a router.
     */
    private static final FacePorts PORTS = FacePorts.builder()
            .port(FaceRule.BACK, DataLines.upTo(HardwareEra.ADVANCED, DataLine.BACKBONE, DataLine.HPC))
            .port(FaceRule.EVERY, PeripheralLine.of(HardwareEra.ADVANCED))
            .build();

    public ServerRackPartBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(TOP, false)
                .setValue(FRONT, false)
                .setValue(COMPUTE, false)
                .setValue(FACING, Direction.NORTH)
                .setValue(ServerRackBlock.BAYS, 0)
                .setValue(OpticalPort.OPTICAL, false));
    }

    /** A part is collision and a link back to the controller; the cabinet model is drawn from there. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected MapCodec<ServerRackPartBlock> codec() {
        return CODEC;
    }

    @Override
    public FacePorts ports() {
        return PORTS;
    }

    /*
     * A compute cabinet is on the high-compute fabric only; a server cabinet takes every data tier but that one, and
     * both only through the cabinet's back, where the cabinet links its cables. The cable's rendered nub and the
     * cabinet's own link follow this same rule, so a data cable on a supercomputer cabinet neither shows a
     * connection nor makes one. The fibre only when a server in the cabinet holds an Optical Network Card.
     */
    @Override
    public boolean accepts(final BlockState state, final Direction face, final Connection offered) {
        return IFaceConnector.super.accepts(state, face, offered)
                && state.getValue(COMPUTE) == (DataLines.of(offered.line()) == DataLine.HPC)
                && OpticalPort.admits(state, offered);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, FRONT, COMPUTE, FACING, ServerRackBlock.BAYS, OpticalPort.OPTICAL);
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.sidedSuccess(true);
        }
        if (level.getBlockEntity(pos) instanceof ServerRackPartBlockEntity part
                && part.controllerPos() != null
                && level.getBlockEntity(part.controllerPos()) instanceof ServerRackBlockEntity rack) {
            // The part knows its cabinet; the row under the crosshair is resolved against the controller.
            return ServerRackBlock.mountFromHand(rack, part.controllerPos(), pos, stack, player, hit);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof ServerRackPartBlockEntity part
                && part.controllerPos() != null
                && level.getBlockEntity(part.controllerPos()) instanceof ServerRackBlockEntity rack) {
            final BlockPos controllerPos = part.controllerPos();
            MachineOperators.note(rack, player);
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ServerRackMenu(id, inv, rack),
                    // The cabinet names itself; every era and the compute cabinet have their own.
                    rack.getBlockState().getBlock().getName()),
                    buf -> buf.writeBlockPos(controllerPos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * A part has no item of its own, so the middle mouse button used to pick nothing at all from eleven of
     * the cabinet's twelve blocks. It answers with what the cabinet would: the machine under the crosshair,
     * or the cabinet itself.
     */
    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target,
                                       final LevelReader level, final BlockPos pos,
                                       final Player player) {
        if (level.getBlockEntity(pos) instanceof ServerRackPartBlockEntity part && part.controllerPos() != null
                && level.getBlockEntity(part.controllerPos()) instanceof ServerRackBlockEntity rack
                && rack.getBlockState().getBlock() instanceof ServerRackBlock cabinet) {
            return ServerRackBlock.pickFrom(rack, part.controllerPos(), pos, target, cabinet.asItem());
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state,
                                        final Player player) {
        // Drops happen here (not in dissolve) so creative mode never spills items.
        if (level instanceof ServerLevel serverLevel && !player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof ServerRackPartBlockEntity part
                && part.controllerPos() != null
                && level.getBlockState(part.controllerPos()).getBlock()
                        instanceof AbstractMultiblockControllerBlock controller) {
            controller.dropContentsExternally(serverLevel, part.controllerPos());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof ServerRackPartBlockEntity part
                && part.controllerPos() != null) {
            /*
             * The controller is still present when a part is broken; read its facing
             * so the whole cabinet dissolves. (Re-entrant calls are guarded.)
             */
            final BlockState controller = level.getBlockState(part.controllerPos());
            if (controller.getBlock()
                    instanceof AbstractMultiblockControllerBlock owner) {
                owner.dissolve(serverLevel, part.controllerPos(),
                        controller.getValue(HorizontalDirectionalBlock.FACING));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ServerRackPartBlockEntity(pos, state);
    }
}
