/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.cylinder;

import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import java.util.Optional;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The Gas Cylinder in a hand. Used on a block that gives gas, a machine's outlet or a tank, it fills with that block's
 * gas; used while sneaking on a block that takes gas, it empties into it. It carries one gas at a time, so with gas
 * inside it fills only from the same gas. It acts before the block does what it does with a hand, as a bucket does, so
 * a machine's screen does not open in its stead.
 */
public final class GasCylinderItem extends Item {

    public GasCylinderItem(final Properties properties) {
        super(properties);
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
        final Optional<MatterStack> offered;
        if (held.isPresent()) {
            final long given = block.extract(held.get().material(), room, true);
            offered = given > 0 ? Optional.of(held.get().withAmount(given)) : Optional.empty();
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
