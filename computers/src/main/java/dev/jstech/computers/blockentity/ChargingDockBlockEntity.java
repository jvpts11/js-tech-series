/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.computers.PortableItems;
import dev.jstech.computers.block.ChargingDockBlock;
import dev.jstech.computers.portable.PortableBattery;
import dev.jstech.computers.portable.PortableDevices;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.computers.portable.Portables;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * A Mobile Device Charging Dock (computers/docs/design/wireless.md): it holds one portable, switched off while it
 * stands in it, and fills its battery at its era's charger's watts from the energy behind it, taking from its line only
 * what it gives the battery. Its lamp is lit while it charges; the screen of the device shows the battery filling.
 */
public class ChargingDockBlockEntity extends SyncedBlockEntity {

    private final EnergyStore store = new EnergyStore("Energy", JsEnergy.ENERGY, JsEnergy.LOW, 0L, TAKES_A_TICK, 0L,
            this::setChanged);
    private final FieldItemHandler device = fields().items("Device", 1).save().toClient().dropsWhenBroken();
    /* Its lamp, which the client draws: whether it is charging this moment. */
    private final ValueField<Boolean> charging = fields().value("Charging", Codec.BOOL, false).toClient();
    private final TickDraw draw = new TickDraw();
    /* The share the client last saw, so it is told only when the battery's picture would change. */
    private int shownPercent = -1;

    /* The most its store takes from the line in a tick: far more than any charger gives. */
    private static final long TAKES_A_TICK = 1_000L;

    public ChargingDockBlockEntity(final BlockPos pos, final BlockState state) {
        super(PortableItems.CHARGING_DOCK_BE.get(), pos, state);
        fields().part("Energy", store).save();
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final ChargingDockBlockEntity dock) {
        if (level instanceof ServerLevel) {
            dock.tick();
        }
    }

    /** The era of the portables it holds. */
    public Era era() {
        return getBlockState().getBlock() instanceof ChargingDockBlock block ? block.era() : HardwareEra.LEGACY;
    }

    /** The portable it holds, or an empty stack. */
    public ItemStack held() {
        return device.getStackInSlot(0);
    }

    /** Whether it is filling a battery this moment, as its lamp shows. */
    public boolean isCharging() {
        return Boolean.TRUE.equals(charging.get());
    }

    /** Stands {@code stack}, a portable of its era, in it, switched off. */
    public void put(final ItemStack stack) {
        if (level instanceof ServerLevel server) {
            Portables.switchOff(server, stack);
        }
        device.setStackInSlot(0, stack);
        shownPercent = -1;
        fields().syncToClients();
    }

    /** Takes out the portable it holds, which comes on again once it is carried. */
    public ItemStack take() {
        final ItemStack taken = device.extractItem(0, 1, false);
        charging.set(false);
        fields().syncToClients();
        return taken;
    }

    /** Its energy port, behind it and nowhere else. */
    @Nullable
    public IEnergyPort energyPort(@Nullable final Direction side) {
        return side == ChargingDockBlock.cableFace(getBlockState()) ? store : null;
    }

    /** What the game's FE capability sees there: the same store, while the series speaks FE. */
    @Nullable
    public IEnergyStorage feEnergy(@Nullable final Direction side) {
        return side == ChargingDockBlock.cableFace(getBlockState()) && JsEnergy.speaksFe() ? store.asFe() : null;
    }

    /* A tick: a battery not full is given what the charger gives, while the line gives it the energy. */
    private void tick() {
        final ItemStack held = held();
        final boolean filling;
        if (held.getItem() instanceof PortableItem portable) {
            final int capacity = PortableBattery.capacity(portable.model());
            final int stored = Portables.battery(held, portable.model());
            filling = stored < capacity && draw.draw(store, PortableBattery.chargerWatts(era()));
            if (filling) {
                Portables.setBattery(held, portable.model(), stored + PortableBattery.chargedATick(era()));
                setChanged();
            }
            final int percent = PortableBattery.percent(Portables.battery(held, portable.model()), capacity);
            if (percent != shownPercent) {
                shownPercent = percent;
                fields().syncToClients();
            }
            seen(held);
        } else {
            filling = false;
        }
        if (filling != isCharging()) {
            charging.set(filling);
            fields().syncToClients();
        }
    }

    /* A device standing in its dock is not lost: it is marked so once a second. */
    private void seen(final ItemStack held) {
        final UUID id = Portables.id(held);
        if (id != null && level instanceof ServerLevel server && server.getGameTime() % Portables.LOOK_EVERY == 0) {
            PortableDevices.get(server.getServer()).seen(id, server.getGameTime());
        }
    }
}
