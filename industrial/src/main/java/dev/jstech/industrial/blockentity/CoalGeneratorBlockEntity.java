/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.blockentity;

import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.machine.FaceSlot;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.machine.IndustrialGenerator;
import dev.jstech.industrial.machine.MachineRating;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Coal Generator, the first source of energy: a small steam set with its water loop closed inside, making up to
 * 5 kW at low voltage from furnace fuel (coal, charcoal and the like). It follows its load: it burns only what the
 * grid draws, so a piece of fuel lasts as long as a furnace burns it at the generator's full power, and longer when
 * less is drawn ({@link IndustrialGenerator}).
 */
public class CoalGeneratorBlockEntity extends IndustrialGenerator {

    private final LongField fuelLeft;
    private final LongField fuelOfPiece;

    /** A T1 generator on low voltage, making up to 5 kW. */
    public static final MachineRating RATING = MachineRating.of(IndustrialTier.T1, JsEnergy.LOW, 5_000);
    public static final int FUEL_SLOT = 0;

    public CoalGeneratorBlockEntity(final BlockPos pos, final BlockState state) {
        super(IndustrialModule.COAL_GENERATOR_BE.get(), pos, state, 1, RATING);
        this.fuelLeft = fields().longInteger("FuelLeft", 0L).save().toMenu();
        this.fuelOfPiece = fields().longInteger("FuelOfPiece", 0L).save().toMenu();
        getInventory().accepts((slot, stack) -> stack.getBurnTime(null) > 0);
    }

    /** The energy a piece of {@code fuel} holds, in joules: as long as a furnace burns it, at the full 5 kW. */
    public static long joulesOf(final ItemStack fuel) {
        return fuel.getBurnTime(null) * RATING.joulesPerTick();
    }

    /** Whether a piece of fuel is lit, with energy left in it. */
    public boolean isBurning() {
        return this.fuelLeft.get() > 0;
    }

    /** The energy left in the piece of fuel lit, in joules. */
    public long fuelLeft() {
        return this.fuelLeft.get();
    }

    @Override
    public List<FaceSlot> mainSlots() {
        final GuiLayout.SlotPosition at = MachineScreenLayout.laneSlot(0, 1, 0, false, 1);
        return List.of(new FaceSlot(FUEL_SLOT, at.x(), at.y(), false));
    }

    @Override
    protected long burn(final long joules) {
        long covered = 0L;
        while (covered < joules && (this.fuelLeft.get() > 0 || lightFuel())) {
            final long taken = Math.min(joules - covered, this.fuelLeft.get());
            this.fuelLeft.set(this.fuelLeft.get() - taken);
            covered += taken;
        }
        return covered;
    }

    @Override
    protected double progressShare() {
        return this.fuelOfPiece.get() == 0 ? 0.0 : (double) this.fuelLeft.get() / this.fuelOfPiece.get();
    }

    /* Takes the next piece of fuel from its slot and lights it, giving back what it leaves (a lava bucket's bucket). */
    private boolean lightFuel() {
        final ItemStack fuel = getInventory().getStackInSlot(FUEL_SLOT);
        final long joules = joulesOf(fuel);
        if (joules <= 0) {
            return false;
        }
        final ItemStack remainder = fuel.getCraftingRemainingItem();
        getInventory().extractItem(FUEL_SLOT, 1, false);
        if (!remainder.isEmpty() && getInventory().getStackInSlot(FUEL_SLOT).isEmpty()) {
            getInventory().setStackInSlot(FUEL_SLOT, remainder);
        }
        this.fuelLeft.set(joules);
        this.fuelOfPiece.set(joules);
        return true;
    }
}
