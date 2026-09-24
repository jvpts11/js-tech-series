/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.menu.PatternEncoderMenu;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.IPeripheralConnectable;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Pattern Encoder block: the burner a computer's Pattern Studio sends finished recipe files to. One block
 * per hardware era, each writing the media of its day. A click with a disc the era accepts puts it in the bay,
 * a sneak-click takes it out (unless a job holds it), and a plain click opens the bay's small panel.
 */
@TextHolder
public class PatternEncoderBlock extends HorizontalDirectionalBlock implements EntityBlock, IPeripheralConnectable,
        IEraChassisBlock {

    private final HardwareEra era;

    public static final MapCodec<PatternEncoderBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(b -> b.era)
    ).apply(i, PatternEncoderBlock::new));

    private static final TextKey WRITING_WAIT = TextKey.of("jsc.pattern_encoder.writing_wait",
            "The encoder is writing - wait for it to finish.");
    private static final TextKey BAY_FULL = TextKey.of("jsc.pattern_encoder.bay_full",
            "The bay already holds a disc - sneak-click to eject it.");
    private static final TextKey READ_ONLY = TextKey.of("jsc.pattern_encoder.read_only",
            "That disc is read-only and cannot be written.");
    private static final TextKey VINTAGE_ONLY =
            TextKey.of("jsc.pattern_encoder.vintage_only", "A Vintage encoder writes floppy disks only.");
    private static final TextKey LEGACY_ONLY =
            TextKey.of("jsc.pattern_encoder.legacy_only", "A Legacy encoder writes CDs only.");
    private static final TextKey STANDARD_ONLY = TextKey.of("jsc.pattern_encoder.standard_only",
            "This encoder writes DVDs, CDs and USB sticks, not that.");

    public PatternEncoderBlock(final Properties properties, final HardwareEra era) {
        super(properties);
        this.era = era;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** The era of this encoder, which decides which media it writes. */
    public HardwareEra era() {
        return era;
    }

    @Override
    public HardwareEra chassisEra() {
        return era;
    }

    @Override
    protected MapCodec<PatternEncoderBlock> codec() {
        return CODEC;
    }

    @Override
    public PeripheralCableType peripheralType() {
        return PeripheralCableType.COMPUTING;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The body is one model drawn by the block entity; the block itself paints nothing over it. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /*
     * The body fills its block in every era, so the shape is the plain cube: nothing to walk through or
     * stand on beyond the block itself.
     */
    @Override
    protected VoxelShape getShape(
            final BlockState state, final BlockGetter level, final BlockPos pos,
            final CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack heldStack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PatternEncoderBlockEntity encoder)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player.isShiftKeyDown()) {
            if (encoder.locked()) {
                player.displayClientMessage(GameText.component(WRITING_WAIT), true);
                return ItemInteractionResult.SUCCESS;
            }
            final ItemStack ejected = encoder.ejectMedia();
            if (!ejected.isEmpty() && !player.addItem(ejected)) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ejected);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (encoder.acceptsMedia(heldStack)) {
            final ItemStack left = encoder.insertMedia(heldStack.copyWithCount(1));
            if (left.isEmpty()) {
                heldStack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
            player.displayClientMessage(GameText.component(BAY_FULL), true);
            return ItemInteractionResult.SUCCESS;
        }
        if (heldStack.getItem() instanceof MediaItem) {
            // A refused disc says why; a silent click reads as a broken block.
            player.displayClientMessage(GameText.component(refusal(heldStack)), true);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** What to tell a player whose disc the bay refused. */
    private TextKey refusal(final ItemStack held) {
        if (held.getItem() instanceof FormattedMediaItem fmt
                && !fmt.writable()) {
            return READ_ONLY;
        }
        return switch (era) {
            case VINTAGE -> VINTAGE_ONLY;
            case LEGACY -> LEGACY_ONLY;
            default -> STANDARD_ONLY;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof PatternEncoderBlockEntity encoder) {
            if (player.isShiftKeyDown()) {
                if (encoder.locked()) {
                    player.displayClientMessage(GameText.component(WRITING_WAIT), true);
                    return InteractionResult.sidedSuccess(false);
                }
                final ItemStack ejected = encoder.ejectMedia();
                if (!ejected.isEmpty() && !player.addItem(ejected)) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ejected);
                }
                return InteractionResult.sidedSuccess(false);
            }
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new PatternEncoderMenu(
                                    id, inventory, encoder),
                            Component.translatable(getDescriptionId())),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PatternEncoderBlockEntity encoder) {
            encoder.dropContents(level, pos);
            if (level instanceof ServerLevel serverLevel) {
                encoder.unlink(serverLevel); // free the computer's endpoint slot
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new PatternEncoderBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ComputingModule.PATTERN_ENCODER_BE.get()
                ? (lvl, pos, st, be) -> PatternEncoderBlockEntity.serverTick(lvl, pos, st, (PatternEncoderBlockEntity) be)
                : null;
    }
}
