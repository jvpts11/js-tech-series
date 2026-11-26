/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.cylinder;

import dev.jstech.core.item.ItemGasTank;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.material.Material;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * The Gas Cylinder in a hand. Used on a block that gives gas, a machine's outlet or a tank, it fills with that block's
 * gas; used while sneaking on a block that takes gas, it empties into it. It carries one gas at a time, so with gas
 * inside it fills only from the same gas, and empty but marked for a gas ({@link GasMarking}) only from that one. It
 * acts before the block does what it does with a hand, as a bucket does, so a machine's screen does not open in its
 * stead.
 */
public final class GasCylinderItem extends Item {

    public GasCylinderItem(final Properties properties) {
        super(properties);
    }

    /**
     * Used in the air while empty: opens the list of the Core's gases to mark it for one, or, sneaking, takes its mark
     * off. A gas had no name in a pattern until a machine had made it into a cylinder.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        final IMatterHandler tank = stack.getCapability(MatterCapabilities.GAS_ITEM);
        final boolean inTheAir = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE).getType()
                == HitResult.Type.MISS;
        if (!inTheAir || tank == null || tank.stack(0).isPresent()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.isSecondaryUseActive()) {
            if (ItemGasTank.markOf(stack).isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }
            if (!level.isClientSide()) {
                ItemGasTank.mark(stack, Optional.empty());
            }
        } else if (level.isClientSide()) {
            GasMarking.open(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> tooltip,
                                final TooltipFlag flag) {
        if (stack.get(ItemStates.GAS.get()) == null && ItemGasTank.markOf(stack).isPresent()) {
            tooltip.add(GameText.component(CylinderTexts.FILLS_ONLY).copy().withStyle(ChatFormatting.GRAY));
            tooltip.add(GameText.component(CylinderTexts.CLEAR).copy().withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        final Level level = context.getLevel();
        final IMatterHandler block = level.getCapability(MatterCapabilities.GAS, context.getClickedPos(),
                context.getClickedFace());
        final IMatterHandler tank = stack.getCapability(MatterCapabilities.GAS_ITEM);
        if (block == null || tank == null || tank.tanks() == 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (context.isSecondaryUseActive()) {
                emptyInto(tank, block);
            } else {
                fillFrom(tank, block);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Fills {@code tank} from {@code block}: with the gas it holds, or any gas the block gives when empty. */
    public static long fillFrom(final IMatterHandler tank, final IMatterHandler block) {
        final Optional<MatterStack> held = tank.stack(0);
        final long room = tank.capacity(0) - held.map(MatterStack::amount).orElse(0L);
        if (room <= 0) {
            return 0L;
        }
        // An empty cylinder marked for a gas fills with that gas alone, as a full one with its own.
        final Optional<Material> wanted = held.map(MatterStack::material)
                .or(() -> tank instanceof ItemGasTank item ? item.mark() : Optional.empty());
        final Optional<MatterStack> offered;
        if (wanted.isPresent()) {
            final long given = block.extract(wanted.get(), room, true);
            offered = given > 0 ? Optional.of(new MatterStack(wanted.get(), given)) : Optional.empty();
        } else {
            offered = block.extractAny(room, true);
        }
        if (offered.isEmpty()) {
            return 0L;
        }
        final long fits = tank.insert(offered.get(), true);
        final long taken = fits > 0 ? block.extract(offered.get().material(), fits, false) : 0L;
        return taken > 0 ? tank.insert(offered.get().withAmount(taken), false) : 0L;
    }

    /** Empties {@code tank} into {@code block}, as much of its gas as the block takes. */
    public static long emptyInto(final IMatterHandler tank, final IMatterHandler block) {
        final Optional<MatterStack> held = tank.stack(0);
        if (held.isEmpty()) {
            return 0L;
        }
        final long fits = block.insert(held.get(), true);
        final long taken = fits > 0 ? tank.extract(held.get().material(), fits, false) : 0L;
        return taken > 0 ? block.insert(held.get().withAmount(taken), false) : 0L;
    }
}
