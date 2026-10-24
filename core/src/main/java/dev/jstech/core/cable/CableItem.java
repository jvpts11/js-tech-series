/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.blockentity.ClientUpdates;
import dev.jstech.core.overhead.OverheadLines;
import dev.jstech.core.text.GameText;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The item that lays a cable. Used on a cable block it lays its wire there, when the block takes it (its lane is free
 * and neither it nor what is there never shares a block); otherwise, or while sneaking, it lays a new cable block
 * against the face clicked, holding its wire alone. The item of a line that hangs, used on a block with insulators,
 * hangs its wire from them instead ({@link OverheadLines}).
 */
public final class CableItem extends Item {

    private final Supplier<CableType> type;

    public CableItem(final Properties properties, final Supplier<CableType> type) {
        super(properties);
        this.type = Objects.requireNonNull(type, "type");
    }

    /** The cable it lays. */
    public CableType type() {
        return this.type.get();
    }

    /**
     * What the cable is for, and the era it belongs to in that era's colour, as its declaration says them; then how
     * far one run of it reaches, read from the cable, and what starts a new run, so a player knows where a run needs
     * one before it goes dark.
     */
    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> tooltip,
                                final TooltipFlag flag) {
        final CableType cable = type();
        cable.what().ifPresent(what -> tooltip.add(GameText.component(what).withStyle(ChatFormatting.GRAY)));
        cable.step().ifPresent(step -> tooltip.add(GameText.component(step.named())
                .withStyle(style -> style.withColor(step.colour()))));
        tooltip.add(GameText.component(cable.range() > 0 ? CableTexts.REACH.with(cable.range())
                : CableTexts.ANY_LENGTH.with()).withStyle(ChatFormatting.GRAY));
        cable.renewal().ifPresent(renewal -> tooltip.add(GameText.component(renewal)
                .withStyle(ChatFormatting.DARK_GRAY)));
    }

    /**
     * Lays a wire of {@code type} at {@code pos}: into the cable block there, or into a new one when the place is
     * free to build in. On the server; false when it could not. A new block holds its wire before its neighbours hear
     * of it, so a cable beside it joins it the first time it looks.
     */
    public static boolean lay(final Level level, final BlockPos pos, final CableType type) {
        final Wire wire = Wire.of(type);
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            return cable.lay(wire);
        }
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        // Laying the wire tells the neighbours (CableBlockEntity.lay); placed without, they would look at an empty one.
        if (!level.setBlock(pos, CoreCables.BLOCK.get().defaultBlockState(), Block.UPDATE_CLIENTS)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable && cable.lay(wire)) {
            return true;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        return false;
    }

    /**
     * Shows the players watching the blocks {@code changed} those blocks and the cables beside them at once, as they
     * are now, rather than at the end of the tick: after a player's own hand laid or took out wires, so a block and
     * what it joins appear together, as a block a player places does. Each block is sent once.
     */
    public static void showNow(final ServerLevel level, final Collection<BlockPos> changed) {
        final Set<BlockPos> shown = new LinkedHashSet<>();
        for (final BlockPos pos : changed) {
            shown.add(pos.immutable());
            for (final Direction face : Direction.values()) {
                final BlockPos next = pos.relative(face);
                if (level.getBlockState(next).getBlock() instanceof CableBlock) {
                    shown.add(next);
                }
            }
        }
        for (final BlockPos pos : shown) {
            ClientUpdates.sendNow(level, pos);
        }
    }

    /**
     * Where a click on {@code hit} with a cable of {@code type} in {@code hand} lays its wire, the same on the player's
     * side as on the server's: into the cable block clicked, when it takes the wire and the player is not sneaking;
     * otherwise where a block would be placed, the clicked block itself when it gives way (a snow layer, grass), the
     * one against the clicked face when not. None where nothing can be laid.
     */
    public static Optional<BlockPos> landing(final Level level, final @Nullable Player player,
                                             final InteractionHand hand, final BlockHitResult hit,
                                             final CableType type) {
        final BlockPos clicked = hit.getBlockPos();
        final boolean sneaking = player != null && player.isSecondaryUseActive();
        if (!sneaking && level.getBlockEntity(clicked) instanceof CableBlockEntity cable
                && cable.takes(Wire.of(type))) {
            return Optional.of(clicked);
        }
        final BlockPlaceContext place = new BlockPlaceContext(level, player, hand,
                player == null ? ItemStack.EMPTY : player.getItemInHand(hand), hit);
        return place.canPlace() ? Optional.of(place.getClickedPos()) : Optional.empty();
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos clicked = context.getClickedPos();
        final Player player = context.getPlayer();
        // A line that hangs is hung from a block's insulators, rather than laid against it.
        if (type().hangs() && level.getCapability(OverheadLines.ANCHORS, clicked, null) != null) {
            return OverheadLines.useOn(context, type());
        }
        final Optional<BlockPos> landing = landing(level, player, context.getHand(), new BlockHitResult(
                context.getClickLocation(), context.getClickedFace(), clicked, context.isInside()), type());
        if (landing.isEmpty()) {
            return InteractionResult.FAIL;
        }
        final BlockPos target = landing.get();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!lay(level, target, type())) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            showNow(server, List.of(target));
        }
        final BlockState state = level.getBlockState(target);
        final SoundType sound = type().sound();
        level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, target, GameEvent.Context.of(player, state));
        context.getItemInHand().consume(1, player);
        return InteractionResult.CONSUME;
    }
}
