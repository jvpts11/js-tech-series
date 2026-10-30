/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.pad;

import dev.jstech.core.blockentity.FieldEnergyStorage;
import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.space.api.Propellant;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.TankLoad;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A Pad Service Port as its block entity: where the pad's lines come up. Its water, piped in, and the energy for its
 * pumps feed the deluge, poured over the pad at ignition to dampen the sound and spare the concrete; without a full
 * load of both there is no deluge. Its propellant lines, piped in from the depot, fuel the rocket standing on the pad
 * over it, tank by tank, each to its propellant's mix, its pumps spending energy for every kilogram; the rocket's
 * flight computer keeps what its tanks hold till it flies. Its lines only take: nothing is drawn back out of it.
 */
public final class PadServicePortBlockEntity extends SyncedBlockEntity {

    private final FieldFluidTank water;
    private final FieldFluidTank oxygen;
    private final FieldFluidTank methane;
    private final FieldEnergyStorage energy;
    private final Lines lines = new Lines();
    private @Nullable BlockPos computer;
    private int wait;

    /** The water a deluge pours, in millibuckets, and the energy its pumps spend doing it, in joules. */
    public static final int DELUGE_WATER = 64_000;
    public static final int DELUGE_ENERGY = 400_000;
    /** How much each propellant line holds, in millibuckets. */
    public static final int LINE = 32_000;
    /** How fast it fuels a rocket, in kilograms of each liquid a tick, and the energy its pumps spend on a kilogram. */
    public static final double FUEL_RATE = 10.0;
    public static final int JOULES_A_KILOGRAM = 20;
    /** How far round it, and how far over it, a launcher's deck is looked for; how far up its rocket's computer. */
    public static final int REACH = 16;
    public static final int OVER = 12;
    public static final int TALLEST = 96;
    /* How far from the rocket's axis its flight computer may be; how many ticks between looks for a rocket. */
    private static final int AXIS_ROUND = 4;
    private static final int LOOK = 40;
    /* How fast its pumps' store fills, in joules a tick. */
    private static final int ENERGY_IN = 20_000;

    public PadServicePortBlockEntity(final BlockPos pos, final BlockState state) {
        super(PadBlocks.PORT_ENTITY.get(), pos, state);
        this.water = fields().fluid("Water", DELUGE_WATER).save().toClient();
        this.water.setValidator(stack -> stack.is(FluidTags.WATER));
        this.oxygen = fields().fluid("Oxygen", LINE).save().toClient();
        this.oxygen.setValidator(stack -> stack.getFluid() == SkeletonPropellants.LIQUID_OXYGEN.get());
        this.methane = fields().fluid("Methane", LINE).save().toClient();
        this.methane.setValidator(stack -> stack.getFluid() == SkeletonPropellants.LIQUID_METHANE.get());
        this.energy = fields().energy("Energy", DELUGE_ENERGY, ENERGY_IN, 0).save().toClient().exposed();
    }

    /** The water it holds for the deluge, in millibuckets. */
    public int water() {
        return this.water.getFluidAmount();
    }

    /** The liquid oxygen and the liquid methane its lines hold, in millibuckets. */
    public int oxygen() {
        return this.oxygen.getFluidAmount();
    }

    public int methane() {
        return this.methane.getFluidAmount();
    }

    /** The energy its pumps hold, in joules. */
    public int energy() {
        return this.energy.getEnergyStored();
    }

    /** What pipes reach on any of its faces: its water and its propellant lines, which only take. */
    public IFluidHandler lines() {
        return this.lines;
    }

    /** Whether it holds a whole deluge: its water and its pumps' energy. */
    public boolean ready() {
        return water() >= DELUGE_WATER && energy() >= DELUGE_ENERGY;
    }

    /** Pours the deluge, spending its water and its pumps' energy; false, pouring nothing, where it is not ready. */
    public boolean pour() {
        if (!ready()) {
            return false;
        }
        this.water.drain(DELUGE_WATER, IFluidHandler.FluidAction.EXECUTE);
        this.energy.consume(DELUGE_ENERGY);
        setChanged();
        return true;
    }

    /** Fuels the rocket standing on the pad over it, if one does, as much as a tick allows. */
    public void tick() {
        if (!(this.level instanceof ServerLevel server) || this.oxygen.isEmpty() && this.methane.isEmpty()) {
            return;
        }
        if (this.computer == null || !(server.getBlockEntity(this.computer) instanceof FlightComputerBlockEntity)) {
            if (--this.wait > 0) {
                return;
            }
            this.wait = LOOK;
            this.computer = find(server);
        }
        if (this.computer != null && server.getBlockEntity(this.computer) instanceof FlightComputerBlockEntity rocket) {
            fuel(server, rocket);
        }
    }

    /* Fills each of the rocket's tanks toward its propellant's mix, a tick's worth of each liquid. */
    private void fuel(final ServerLevel server, final FlightComputerBlockEntity rocket) {
        final Optional<RocketDesign> design = rocket.rocket();
        if (design.isEmpty()) {
            return;
        }
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(server);
        final List<RocketStack.Section> sections = design.get().stack().sections();
        for (int k = 0; k < sections.size(); k++) {
            final RocketStack.Part placed = sections.get(k).part();
            final RocketPart part = parts.get(placed.id());
            if (part == null || part.propellant() <= 0) {
                continue;
            }
            final double capacity = part.propellant() * (part.stretches() ? placed.layers() : 1);
            final ResourceLocation id = burnedBy(parts, sections, k);
            final Optional<Propellant> propellant = Propellants.get(server, id);
            if (propellant.isEmpty()) {
                continue;
            }
            TankLoad load = rocket.tanks().getOrDefault(k, new TankLoad(id, Map.of()));
            boolean changed = false;
            for (final Propellant.Component component : propellant.get().components()) {
                final FieldFluidTank line = lineFor(component.material());
                if (line == null || line.isEmpty()) {
                    continue;
                }
                final double held = load.kilograms().getOrDefault(component.material(), 0.0);
                final double kilograms = pumped(server, line, capacity * component.share() - held);
                if (kilograms > 0) {
                    load = load.with(component.material(), held + kilograms);
                    changed = true;
                }
            }
            if (changed) {
                rocket.load(k, load);
            }
        }
    }

    /* Pumps up to {@code wanted} kilograms out of {@code line}, a tick's worth at most; how many it pumped. */
    private double pumped(final ServerLevel server, final FieldFluidTank line, final double wanted) {
        final Optional<Material> material = MaterialLiquids.materialOf(line.getFluid().getFluid());
        if (wanted <= 0 || material.isEmpty()) {
            return 0;
        }
        final double litre = Thermal.of(material.get(), false).litreKilograms();
        if (!(litre > 0)) {
            return 0;
        }
        final double affordable = (double) energy() / JOULES_A_KILOGRAM;
        final int millibuckets = (int) Math.min(line.getFluidAmount(),
                Math.floor(Math.min(Math.min(wanted, FUEL_RATE), affordable) / litre));
        if (millibuckets <= 0) {
            return 0;
        }
        line.drain(millibuckets, IFluidHandler.FluidAction.EXECUTE);
        final double kilograms = millibuckets * litre;
        this.energy.consume((int) Math.ceil(kilograms * JOULES_A_KILOGRAM));
        setChanged();
        return kilograms;
    }

    /* The propellant the tank at {@code section} feeds: its stage's engine's, the nearest under it, else over it. */
    private static ResourceLocation burnedBy(final Map<ResourceLocation, RocketPart> parts,
                                             final List<RocketStack.Section> sections, final int section) {
        for (int k = section; k >= 0; k--) {
            final RocketPart part = parts.get(sections.get(k).part().id());
            if (part != null && part.engine().isPresent()) {
                return part.engine().get().propellant();
            }
        }
        for (int k = section + 1; k < sections.size(); k++) {
            final RocketPart part = parts.get(sections.get(k).part().id());
            if (part != null && part.engine().isPresent()) {
                return part.engine().get().propellant();
            }
        }
        return SkeletonPropellants.METHALOX;
    }

    /* Its line for the liquid of {@code material}, if it has one. */
    private @Nullable FieldFluidTank lineFor(final ResourceLocation material) {
        for (final FieldFluidTank line : List.of(this.oxygen, this.methane)) {
            final Fluid fluid = line == this.oxygen ? SkeletonPropellants.LIQUID_OXYGEN.get()
                    : SkeletonPropellants.LIQUID_METHANE.get();
            if (MaterialLiquids.materialOf(fluid).map(found -> found.id().equals(material)).orElse(false)) {
                return line;
            }
        }
        return null;
    }

    /* The flight computer of the rocket on the launcher over the pad by it, if one stands there. */
    private @Nullable BlockPos find(final ServerLevel server) {
        final BlockPos here = getBlockPos();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= OVER; dy++) {
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dz = -REACH; dz <= REACH; dz++) {
                    at.set(here.getX() + dx, here.getY() + dy, here.getZ() + dz);
                    if (server.getBlockEntity(at) instanceof LauncherDeckBlockEntity arm && arm.deck().isPresent()) {
                        return computerOver(server, arm.getBlockPos().offset(arm.deck().get().axis()));
                    }
                }
            }
        }
        return null;
    }

    /* The flight computer keeping a rocket's plan round the axis rising from {@code axis}. */
    private static @Nullable BlockPos computerOver(final ServerLevel server, final BlockPos axis) {
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= TALLEST; dy++) {
            for (int dx = -AXIS_ROUND; dx <= AXIS_ROUND; dx++) {
                for (int dz = -AXIS_ROUND; dz <= AXIS_ROUND; dz++) {
                    at.set(axis.getX() + dx, axis.getY() + dy, axis.getZ() + dz);
                    if (server.getBlockEntity(at) instanceof FlightComputerBlockEntity rocket
                            && rocket.rocket().isPresent()) {
                        return at.immutable();
                    }
                }
            }
        }
        return null;
    }

    /* Its lines as pipes reach them: each liquid into its own, nothing out. */
    private final class Lines implements IFluidHandler {

        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            return line(tank).getFluid();
        }

        @Override
        public int getTankCapacity(final int tank) {
            return line(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(final int tank, final FluidStack stack) {
            return line(tank).isFluidValid(stack);
        }

        @Override
        public int fill(final FluidStack resource, final FluidAction action) {
            for (final FieldFluidTank line : List.of(water, oxygen, methane)) {
                if (line.isFluidValid(resource)) {
                    final int filled = line.fill(resource, action);
                    if (filled > 0 && action.execute()) {
                        setChanged();
                    }
                    return filled;
                }
            }
            return 0;
        }

        @Override
        public FluidStack drain(final FluidStack resource, final FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(final int maxDrain, final FluidAction action) {
            return FluidStack.EMPTY;
        }

        private FieldFluidTank line(final int tank) {
            return tank == 0 ? water : tank == 1 ? oxygen : methane;
        }
    }
}
