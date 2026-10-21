/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What a vehicle holds: the fuel its engine burns, a liquid in its tank or a gas pressed into it, the charge of its
 * battery, its boot and its cargo tanks. The fuel tank takes only what its engine burns. What an engine burns is drawn
 * by the joule, a millibucket at a time with what is left over carried to the next draw, so nothing is lost or made by
 * rounding.
 */
public final class VehicleHolds {

    private final FluidTank liquid;
    private final ItemStackHandler boot;
    private final List<FluidTank> cargo = new ArrayList<>();
    private final Runnable onChange;
    /* The gas in its fuel tank; none while it has no tank. */
    private @Nullable MatterTank gas;
    private int charge;
    private int chargeCapacity;
    private double owed;
    private List<ResourceLocation> fuels = List.of();
    private @Nullable Level level;

    /* A gas fuel is pressed into its tank at two hundred times the open air's pressure. */
    private static final long GAS_PRESSURE = 200;
    private static final String LIQUID = "liquid";
    private static final String GAS = "gas";
    private static final String CHARGE = "charge";
    private static final String OWED = "owed";
    private static final String BOOT = "boot";
    private static final String CARGO = "cargo";

    VehicleHolds(final VehicleDef def, final Runnable onChange) {
        this.onChange = onChange;
        this.liquid = new FluidTank(0, this::burns) {
            @Override
            protected void onContentsChanged() {
                onChange.run();
            }
        };
        this.boot = new ItemStackHandler(def.boot()) {
            @Override
            protected void onContentsChanged(final int slot) {
                onChange.run();
            }
        };
        for (final VehicleDef.CargoTank tank : def.cargo()) {
            this.cargo.add(new FluidTank(tank.capacity(), stack -> tank.holds().map(stack::is).orElse(true)) {
                @Override
                protected void onContentsChanged() {
                    onChange.run();
                }
            });
        }
    }

    /** Fits what it holds to what its parts now are: the fuels its engine burns, its tank's and battery's sizes. */
    void fit(final Level on, final VehicleBuild build) {
        this.level = on;
        this.fuels = build.fuels();
        this.liquid.setCapacity(build.tank());
        if (this.liquid.getFluidAmount() > build.tank()) {
            this.liquid.drain(this.liquid.getFluidAmount() - build.tank(), IFluidHandler.FluidAction.EXECUTE);
        }
        final long volume = this.gas == null ? 0 : this.gas.volume();
        if (volume != build.tank()) {
            // A tank of another size holds what the old one did, as far as it can; none at all spills it.
            final Optional<MatterStack> held = this.gas == null ? Optional.empty() : this.gas.held();
            this.gas = build.tank() <= 0 ? null : new MatterTank(MatterState.GAS, build.tank(), GAS_PRESSURE,
                    onChange);
            if (this.gas != null) {
                final MatterTank fresh = this.gas;
                held.ifPresent(stack -> fresh.insert(stack, false));
            }
        }
        this.chargeCapacity = build.battery();
        this.charge = Math.min(this.charge, this.chargeCapacity);
    }

    /** The liquid in its fuel tank. */
    public FluidTank liquid() {
        return liquid;
    }

    /** The gas pressed into its fuel tank, or none while it has no tank. */
    public Optional<MatterTank> gas() {
        return Optional.ofNullable(gas);
    }

    /** Its boot. */
    public ItemStackHandler boot() {
        return boot;
    }

    /** Its cargo tanks. */
    public List<FluidTank> cargo() {
        return List.copyOf(cargo);
    }

    /** The charge of its battery, in joules. */
    public int charge() {
        return charge;
    }

    /** What its battery holds at most, in joules. */
    public int chargeCapacity() {
        return chargeCapacity;
    }

    /** Charges its battery by up to {@code joules}; what it took. */
    public int receiveCharge(final int joules, final boolean simulate) {
        final int taken = Math.max(0, Math.min(joules, chargeCapacity - charge));
        if (!simulate && taken > 0) {
            charge += taken;
            onChange.run();
        }
        return taken;
    }

    /** How full its fuel is, from nought to 1: its battery's for an electric engine. */
    public float fullness(final boolean electric) {
        if (electric) {
            return chargeCapacity <= 0 ? 0 : (float) charge / chargeCapacity;
        }
        if (liquid.getCapacity() > 0 && !liquid.isEmpty()) {
            return (float) liquid.getFluidAmount() / liquid.getCapacity();
        }
        if (gas == null) {
            return 0;
        }
        final long capacity = gas.capacity();
        return gas.held().map(stack -> (float) stack.amount() / capacity).orElse(0F);
    }

    /**
     * Draws {@code joules} from what the engine runs on, its battery for an electric one; whether there was enough.
     * With not enough, it draws nothing.
     */
    public boolean draw(final double joules, final boolean electric) {
        if (joules <= 0) {
            return true;
        }
        if (electric) {
            final double wanted = joules + owed;
            final int whole = (int) Math.floor(wanted);
            if (whole > charge) {
                return false;
            }
            charge -= whole;
            owed = wanted - whole;
            if (whole > 0) {
                onChange.run();
            }
            return true;
        }
        final Optional<Fuel> fuel = heldFuel();
        if (fuel.isEmpty()) {
            return false;
        }
        final double wanted = joules / fuel.get().joulesPerMb() + owed;
        final int whole = (int) Math.floor(wanted);
        if (!liquid.isEmpty()) {
            if (whole > liquid.getFluidAmount()) {
                return false;
            }
            liquid.drain(whole, IFluidHandler.FluidAction.EXECUTE);
        } else {
            final Optional<MatterStack> held = gas == null ? Optional.empty() : gas.held();
            if (gas == null || held.isEmpty() || whole > held.get().amount()) {
                return false;
            }
            gas.extract(held.get().material(), whole, false);
        }
        owed = wanted - whole;
        return true;
    }

    /** The fuel it holds, of those its engine burns, or none. */
    public Optional<Fuel> heldFuel() {
        if (level == null) {
            return Optional.empty();
        }
        final Level on = level;
        if (!liquid.isEmpty()) {
            return Fuels.liquidAmong(on, fuels, liquid.getFluid().getFluid()).flatMap(id -> Fuels.get(on, id));
        }
        if (gas == null) {
            return Optional.empty();
        }
        return gas.held().flatMap(stack -> Fuels.gasAmong(on, fuels, stack.material().id()))
                .flatMap(id -> Fuels.get(on, id));
    }

    /** Takes the gas {@code stack} into the fuel tank, when its engine burns it and the tank holds no liquid. */
    public long fillGas(final MatterStack stack, final boolean simulate) {
        if (level == null || gas == null || !liquid.isEmpty()
                || Fuels.gasAmong(level, fuels, stack.material().id()).isEmpty()) {
            return 0;
        }
        return gas.insert(stack, simulate);
    }

    /** Its fuel tank and cargo tanks as one handler, the fuel tank first, for pipes and pumps. */
    public IFluidHandler fluids() {
        return new Fluids();
    }

    void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put(LIQUID, liquid.writeToNBT(registries, new CompoundTag()));
        if (gas != null) {
            final CompoundTag gasTag = new CompoundTag();
            gas.save(gasTag);
            tag.put(GAS, gasTag);
        }
        tag.putInt(CHARGE, charge);
        tag.putDouble(OWED, owed);
        tag.put(BOOT, boot.serializeNBT(registries));
        final ListTag tanks = new ListTag();
        for (final FluidTank tank : cargo) {
            tanks.add(tank.writeToNBT(registries, new CompoundTag()));
        }
        tag.put(CARGO, tanks);
    }

    void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        liquid.readFromNBT(registries, tag.getCompound(LIQUID));
        if (gas != null && tag.contains(GAS)) {
            gas.load(tag.getCompound(GAS));
        }
        charge = Math.max(0, tag.getInt(CHARGE));
        owed = tag.getDouble(OWED);
        if (tag.contains(BOOT)) {
            final ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound(BOOT));
            for (int slot = 0; slot < Math.min(saved.getSlots(), boot.getSlots()); slot++) {
                boot.setStackInSlot(slot, saved.getStackInSlot(slot));
            }
        }
        final ListTag tanks = tag.getList(CARGO, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(tanks.size(), cargo.size()); i++) {
            cargo.get(i).readFromNBT(registries, tanks.getCompound(i));
        }
    }

    /* Whether the fuel tank takes {@code stack}: a liquid its engine burns. */
    private boolean burns(final FluidStack stack) {
        return level != null && Fuels.liquidAmong(level, fuels, stack.getFluid()).isPresent();
    }

    /* The fuel tank and the cargo tanks, one after another. */
    private final class Fluids implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1 + cargo.size();
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            return tankAt(tank).getFluid();
        }

        @Override
        public int getTankCapacity(final int tank) {
            return tankAt(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(final int tank, final FluidStack stack) {
            return tankAt(tank).isFluidValid(stack);
        }

        @Override
        public int fill(final FluidStack resource, final FluidAction action) {
            int filled = 0;
            FluidStack left = resource.copy();
            for (int tank = 0; tank < getTanks() && !left.isEmpty(); tank++) {
                final int into = tankAt(tank).fill(left, action);
                filled += into;
                left = left.copyWithAmount(left.getAmount() - into);
            }
            return filled;
        }

        @Override
        public FluidStack drain(final FluidStack resource, final FluidAction action) {
            for (final FluidTank tank : cargo) {
                final FluidStack drained = tank.drain(resource, action);
                if (!drained.isEmpty()) {
                    return drained;
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(final int amount, final FluidAction action) {
            for (final FluidTank tank : cargo) {
                final FluidStack drained = tank.drain(amount, action);
                if (!drained.isEmpty()) {
                    return drained;
                }
            }
            return FluidStack.EMPTY;
        }

        private FluidTank tankAt(final int tank) {
            return tank == 0 ? liquid : cargo.get(tank - 1);
        }
    }
}
