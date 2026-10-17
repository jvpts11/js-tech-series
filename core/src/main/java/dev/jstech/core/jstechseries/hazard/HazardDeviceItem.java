/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import dev.jstech.core.hazard.IHazardInstrument;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A hazard device in hand: carried, it shows on the HUD; used, the Geiger counter and the dosimeter are held up full
 * size, and the map device opens the whole map. The Geiger counter is an instrument: carried, it reads the rate of the
 * series' radiation onto the player's map of hazards.
 */
public final class HazardDeviceItem extends Item implements IHazardInstrument {

    private final HazardDevice device;

    public HazardDeviceItem(final HazardDevice device, final Properties properties) {
        super(properties.stacksTo(1));
        this.device = device;
    }

    /** Which device it is. */
    public HazardDevice device() {
        return this.device;
    }

    @Override
    public Optional<ResourceLocation> reads(final ItemStack stack) {
        return this.device == HazardDevice.GEIGER_COUNTER ? Optional.of(HazardDevices.RADIATION) : Optional.empty();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        if (level.isClientSide()) {
            HazardDevices.open(this.device, hand);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
