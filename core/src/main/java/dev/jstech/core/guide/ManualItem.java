/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.Objects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A manual a player holds: using it opens it, at its cover. */
public class ManualItem extends Item {

    private final String manual;

    /**
     * A manual item.
     *
     * @param manual the id of the manual it opens, {@code namespace:path}
     */
    public ManualItem(final Properties properties, final String manual) {
        super(properties.stacksTo(1));
        this.manual = Objects.requireNonNull(manual, "manual");
    }

    /** The id of the manual it opens. */
    public String manual() {
        return this.manual;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        if (level.isClientSide()) {
            GuideHooks.open(this.manual, "");
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
