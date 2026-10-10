/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.accessories;

import dev.jstech.core.worn.IWornSource;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * What an entity wears in Accessories' slots. Accessories held inside an accessory nest count as worn, as Accessories
 * itself treats them as equipped; its lookup cache always includes them, whatever the flag of the call.
 */
final class AccessoriesWornSource implements IWornSource {

    @Override
    public void collect(final LivingEntity entity, final List<ItemStack> out) {
        AccessoriesCapability.getOptionally(entity).ifPresent(capability -> {
            for (final SlotEntryReference entry : capability.getAllEquipped()) {
                if (!entry.stack().isEmpty()) {
                    out.add(entry.stack());
                }
            }
        });
    }
}
