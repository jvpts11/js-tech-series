/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * A belt or a chain, which joins two pulleys of the Core's kit across a distance: used on one pulley, then on another
 * turning on the same axis beside it, within its reach, it joins them, and the two turn the same way, the smaller the
 * faster. A belt slips a little and loses more to friction than a chain. Breaking either pulley drops it again. The
 * item and its look are its mod's.
 *
 * <pre>{@code
 * ItemEntry<ShaftLinkItem> BELT = CONTENT.item("belt", properties -> new ShaftLinkItem(properties, false, 16))
 *         .named("Belt").register();
 * }</pre>
 */
@TextHolder
public class ShaftLinkItem extends Item {

    private final boolean chain;
    private final int reach;

    public static final TextKey STARTED = TextKey.of("jscore.shaft_link.started",
            "First pulley set: use it on the second");
    public static final TextKey JOINED = TextKey.of("jscore.shaft_link.joined", "The pulleys are joined");
    public static final TextKey TAKEN = TextKey.of("jscore.shaft_link.taken", "That pulley is joined already");
    public static final TextKey GONE = TextKey.of("jscore.shaft_link.gone", "The first pulley is gone: start again");
    public static final TextKey NOT_ALIGNED = TextKey.of("jscore.shaft_link.not_aligned",
            "Pulleys are joined side by side, turning on one axis");
    public static final TextKey TOO_FAR = TextKey.of("jscore.shaft_link.too_far", "Too far: it reaches %s blocks");

    /** The place of the first pulley, while a link is being made. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> START =
            CoreItems.CONTENT.component("shaft_link_start", BlockPos.CODEC, BlockPos.STREAM_CODEC);

    /** A belt, or a chain when {@code isChain}, that reaches {@code blocks} from pulley to pulley. */
    public ShaftLinkItem(final Properties properties, final boolean isChain, final int blocks) {
        super(properties);
        if (blocks < 1) {
            throw new IllegalArgumentException("a belt reaches a block at least, not " + blocks);
        }
        this.chain = isChain;
        this.reach = blocks;
    }

    /** Declares the component, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof MechanicalPartBlockEntity pulley) || !pulley(pulley)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        final Player player = context.getPlayer();
        final ItemStack stack = context.getItemInHand();
        if (pulley.linked() != null) {
            return told(player, TAKEN.with(), InteractionResult.FAIL);
        }
        final BlockPos start = stack.get(START.get());
        if (start == null || start.equals(pos)) {
            stack.set(START.get(), pos.immutable());
            return told(player, STARTED.with(), InteractionResult.CONSUME);
        }
        stack.remove(START.get());
        if (!(level.getBlockEntity(start) instanceof MechanicalPartBlockEntity first) || !pulley(first)
                || first.linked() != null) {
            return told(player, GONE.with(), InteractionResult.FAIL);
        }
        final Text problem = problem(first, pulley, start, pos);
        if (problem != null) {
            return told(player, problem, InteractionResult.FAIL);
        }
        first.link(pos, this.chain, BuiltInRegistries.ITEM.getKey(this));
        pulley.link(start, this.chain, BuiltInRegistries.ITEM.getKey(this));
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return told(player, JOINED.with(), InteractionResult.CONSUME);
    }

    /* What keeps the two pulleys from being joined: not side by side on one axis, or too far apart; none if nothing. */
    private @Nullable Text problem(final MechanicalPartBlockEntity first, final MechanicalPartBlockEntity second,
                                   final BlockPos from, final BlockPos to) {
        final Direction.Axis axis = first.getBlockState().getValue(RotatedPillarBlock.AXIS);
        if (axis != second.getBlockState().getValue(RotatedPillarBlock.AXIS)
                || to.subtract(from).get(axis) != 0) {
            return NOT_ALIGNED.with();
        }
        return Math.sqrt(from.distSqr(to)) > this.reach ? TOO_FAR.with(this.reach) : null;
    }

    private static boolean pulley(final MechanicalPartBlockEntity part) {
        return part.spec().kind() == MechanicalPartSpec.Kind.PULLEY
                && part.getBlockState().hasProperty(RotatedPillarBlock.AXIS);
    }

    private static InteractionResult told(@Nullable final Player player, final Text said,
                                          final InteractionResult result) {
        if (player != null) {
            player.displayClientMessage(GameText.component(said), true);
        }
        return result;
    }
}
