/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.depot;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.room.RoomVent;
import dev.jstech.space.rocket.SkeletonPropellants;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A Transfer Pump as its block entity: the propellant depot's own. It looks now and then for the depot it stands
 * against and forms it whole, or lets it fall apart; it holds the sphere's liquid, one propellant's, as much as a ball
 * as wide as the sphere holds, taken from and given to the pipes on its faces, on to the pad's service port; a sphere
 * that falls apart lets its liquid boil away. A depot of liquid oxygen also gives its oxygen, boiled back to gas, to
 * an Oxygen Bottle held to its pump.
 */
public final class TransferPumpBlockEntity extends SyncedBlockEntity {

    private final ValueField<DepotLayout.Depot> depot;
    private final FieldFluidTank tank;
    private final Gas gas = new Gas();
    private int wait;

    /* How many ticks between looks for its depot; oxygen's mass, kilograms a mole. */
    private static final int LOOK = 40;
    private static final double OXYGEN_KILOGRAMS_A_MOLE = 0.032;

    public TransferPumpBlockEntity(final BlockPos pos, final BlockState state) {
        super(DepotBlocks.PUMP_ENTITY.get(), pos, state);
        // Its depot as it found it, its places counted from the pump.
        this.depot = fields().nullable("Depot", DepotLayout.Depot.CODEC).save().toClient();
        this.tank = fields().fluid("Propellant", 0).save().toClient().exposed();
        this.tank.setValidator(stack -> this.depot.get() != null
                && (stack.getFluid() == SkeletonPropellants.LIQUID_OXYGEN.get()
                || stack.getFluid() == SkeletonPropellants.LIQUID_METHANE.get()));
        fields().whenLoaded(level -> this.tank.setCapacity(capacity()));
    }

    /** Its depot as it found it, its places counted from the pump; none while it is not whole. */
    public Optional<DepotLayout.Depot> depot() {
        return Optional.ofNullable(this.depot.get());
    }

    /** What its sphere holds, in millibuckets of the liquid it holds. */
    public int amount() {
        return this.tank.getFluidAmount();
    }

    /** The liquid its sphere holds, if any; and the gas it gives, oxygen boiled back off a sphere of liquid oxygen. */
    public IFluidHandler liquid() {
        return this.tank;
    }

    public IMatterHandler gas() {
        return this.gas;
    }

    /** Looks for its depot now and then. */
    public void tick() {
        if (this.level == null || --this.wait > 0) {
            return;
        }
        this.wait = LOOK;
        look(this.level);
    }

    /* Forms its depot if it has become whole, or lets it go if it has not; a broken sphere's liquid boils away. */
    private void look(final Level level) {
        final DepotLayout.Depot now = DepotLayout.find(level, getBlockPos()).orElse(null);
        final DepotLayout.Depot was = this.depot.get() == null ? null : at(this.depot.get());
        if (now == null ? was == null : now.equals(was)) {
            return;
        }
        if (was != null) {
            form(level, was, false);
        }
        if (now != null) {
            form(level, now, true);
        }
        this.depot.set(now == null ? null : now.from(getBlockPos()));
        this.tank.setCapacity(capacity());
        if (now == null) {
            this.tank.setFluid(FluidStack.EMPTY);
        } else if (this.tank.getFluidAmount() > this.tank.getCapacity()) {
            this.tank.drain(this.tank.getFluidAmount() - this.tank.getCapacity(), IFluidHandler.FluidAction.EXECUTE);
        }
        setChanged();
    }

    private void form(final Level level, final DepotLayout.Depot depot, final boolean formed) {
        for (final BlockPos at : DepotLayout.pieces(level, depot)) {
            final BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof DepotPieceBlock && state.getValue(DepotPieceBlock.FORMED) != formed) {
                level.setBlock(at, state.setValue(DepotPieceBlock.FORMED, formed), Block.UPDATE_CLIENTS);
            }
        }
    }

    /* Its depot counted from the world's origin again. */
    private DepotLayout.Depot at(final DepotLayout.Depot kept) {
        return new DepotLayout.Depot(kept.low().offset(getBlockPos()), kept.edge(), kept.legs());
    }

    private int capacity() {
        return this.depot.get() == null ? 0 : this.depot.get().capacity();
    }

    /* How many kilograms a millibucket of its liquid weighs, if it holds liquid oxygen. */
    private OptionalDouble oxygen() {
        if (this.tank.getFluid().getFluid() != SkeletonPropellants.LIQUID_OXYGEN.get()) {
            return OptionalDouble.empty();
        }
        final Optional<Material> material = MaterialLiquids.materialOf(this.tank.getFluid().getFluid());
        final double litre = material.map(found -> Thermal.of(found, false).litreKilograms()).orElse(Double.NaN);
        return litre > 0 ? OptionalDouble.of(litre) : OptionalDouble.empty();
    }

    /* Its oxygen as gas: the liquid it holds boiled back off, a millibucket of gas a mole's 22.4 litres. */
    private final class Gas implements IMatterHandler {

        @Override
        public MatterState state() {
            return MatterState.GAS;
        }

        @Override
        public int tanks() {
            return 1;
        }

        @Override
        public Optional<MatterStack> stack(final int tank) {
            final Optional<Material> material = Materials.byId(AirMix.OXYGEN);
            final OptionalDouble litre = oxygen();
            return litre.isEmpty() ? Optional.empty()
                    : material.map(found -> new MatterStack(found, gasOf(amount(), litre.getAsDouble())));
        }

        @Override
        public long capacity(final int tank) {
            final OptionalDouble litre = oxygen();
            return litre.isEmpty() ? 0 : gasOf(TransferPumpBlockEntity.this.tank.getCapacity(), litre.getAsDouble());
        }

        @Override
        public long insert(final MatterStack stack, final boolean simulate) {
            return 0;
        }

        @Override
        public long extract(final Material material, final long amount, final boolean simulate) {
            final OptionalDouble litre = oxygen();
            if (litre.isEmpty() || !material.id().equals(AirMix.OXYGEN) || amount <= 0) {
                return 0;
            }
            final double kilograms = amount / RoomVent.MILLIBUCKETS_PER_MOLE * OXYGEN_KILOGRAMS_A_MOLE;
            final int liquid = (int) Math.min(amount(), Math.ceil(kilograms / litre.getAsDouble()));
            if (!simulate && liquid > 0) {
                TransferPumpBlockEntity.this.tank.drain(liquid, IFluidHandler.FluidAction.EXECUTE);
                setChanged();
            }
            return Math.min(amount, gasOf(liquid, litre.getAsDouble()));
        }

        @Override
        public Optional<MatterStack> extractAny(final long amount, final boolean simulate) {
            final Optional<Material> material = Materials.byId(AirMix.OXYGEN);
            if (material.isEmpty()) {
                return Optional.empty();
            }
            final long taken = extract(material.get(), amount, simulate);
            return taken > 0 ? Optional.of(new MatterStack(material.get(), taken)) : Optional.empty();
        }

        /* How many millibuckets of gas {@code liquid} millibuckets of liquid oxygen boil to. */
        private long gasOf(final int liquid, final double litre) {
            return (long) Math.floor(liquid * litre / OXYGEN_KILOGRAMS_A_MOLE * RoomVent.MILLIBUCKETS_PER_MOLE);
        }
    }
}
