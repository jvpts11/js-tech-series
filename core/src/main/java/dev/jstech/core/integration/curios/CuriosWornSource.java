/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.curios;

import dev.jstech.core.worn.IWornSource;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

/** What an entity wears in Curios' slots. */
final class CuriosWornSource implements IWornSource {

    @Override
    public void collect(final LivingEntity entity, final List<ItemStack> out) {
        CuriosApi.getCuriosInventory(entity).ifPresent(inventory -> {
            final IItemHandler equipped = inventory.getEquippedCurios();
            for (int slot = 0; slot < equipped.getSlots(); slot++) {
                final ItemStack stack = equipped.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    out.add(stack);
                }
            }
        });
    }
}
