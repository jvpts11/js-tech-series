/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.energy.EnergyCharge;
import dev.jstech.core.energy.EnergyFaults;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.EnergyTooltips;
import dev.jstech.core.look.IDescribed;
import dev.jstech.core.text.Text;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * The Lead-Acid Battery Bank: 50 MJ of J's Energy at low voltage, taking and giving through every face, its charge on
 * its gauge. While the series speaks FE, it speaks FE too.
 */
public final class BatteryBankBlockEntity extends SyncedBlockEntity implements IDescribed {

    private final EnergyStore store;
    private final PartField energyField;

    public BatteryBankBlockEntity(final BlockPos pos, final BlockState state) {
        super(SharedEnergy.BATTERY_BE.get(), pos, state);
        this.store = new EnergyStore("Energy", JsEnergy.ENERGY, JsEnergy.LOW, SharedEnergy.BATTERY_CAPACITY,
                SharedEnergy.BATTERY_RATE, SharedEnergy.BATTERY_RATE, this::storeChanged);
        this.energyField = fields().part("Energy", this.store).save().toClient();
        this.store.onFault(what -> {
            if (this.level instanceof ServerLevel server) {
                EnergyFaults.strike(server, this.worldPosition, what);
            }
        });
        fields().mirror(BatteryBankBlock.CHARGE, () -> EnergyCharge.level(this.store.stored(), this.store.capacity(),
                BatteryBankBlock.STEPS));
    }

    /** Its store. */
    public EnergyStore store() {
        return this.store;
    }

    /** What the game's FE capability sees of it: its store, while the series speaks FE; nothing otherwise. */
    public @Nullable IEnergyStorage fe() {
        return JsEnergy.speaksFe() ? this.store.asFe() : null;
    }

    @Override
    public void describe(final List<Text> lines, final boolean details) {
        lines.addAll(EnergyTooltips.store(this.store, this.level == null ? 0L : this.level.getGameTime()));
    }

    /* What the store holds changed: it is saved and sent. */
    private void storeChanged() {
        this.energyField.changed();
    }
}
