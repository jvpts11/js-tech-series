/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.blockentity.BlockEntityFields;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * A machine's tanks of the states of matter that are neither solid nor liquid (gas, supercritical fluid, slurry,
 * plasma, exotic matter), its inputs and its outputs, each saved and sent to the players who see it. Lines put into
 * its inputs and take from its outputs; only the machine fills its outputs and empties its inputs.
 */
public final class MatterTanks {

    private final Map<MatterState, List<MatterTank>> inputs = new EnumMap<>(MatterState.class);
    private final Map<MatterState, List<MatterTank>> outputs = new EnumMap<>(MatterState.class);
    private final Map<MatterState, IMatterHandler> handlers = new EnumMap<>(MatterState.class);

    /** The tanks {@code layout} asks for, declared in {@code fields}. */
    public static MatterTanks in(final BlockEntityFields fields, final List<MatterSpec> layout) {
        final MatterTanks tanks = new MatterTanks();
        for (final MatterSpec spec : layout) {
            for (int i = 0; i < spec.inputs(); i++) {
                tanks.inputs.computeIfAbsent(spec.state(), state -> new ArrayList<>()).add(tank(fields,
                        "Matter" + spec.state().serializedName() + "In" + i, spec));
            }
            for (int i = 0; i < spec.outputs(); i++) {
                tanks.outputs.computeIfAbsent(spec.state(), state -> new ArrayList<>()).add(tank(fields,
                        "Matter" + spec.state().serializedName() + "Out" + i, spec));
            }
        }
        for (final MatterState state : MatterState.values()) {
            if (tanks.inputs.containsKey(state) || tanks.outputs.containsKey(state)) {
                tanks.handlers.put(state, tanks.new Outside(state));
            }
        }
        return tanks;
    }

    /** Its input tanks of {@code state}, in order. */
    public List<MatterTank> inputs(final MatterState state) {
        return this.inputs.getOrDefault(state, List.of());
    }

    /** Its output tanks of {@code state}, in order. */
    public List<MatterTank> outputs(final MatterState state) {
        return this.outputs.getOrDefault(state, List.of());
    }

    /** What lines see of its tanks of {@code state}: inputs to fill, outputs to empty; none when it has none. */
    public @Nullable IMatterHandler outside(final MatterState state) {
        return this.handlers.get(state);
    }

    /** Whether it has any tank at all. */
    public boolean any() {
        return !this.handlers.isEmpty();
    }

    private static MatterTank tank(final BlockEntityFields fields, final String key, final MatterSpec spec) {
        final PartField[] declared = new PartField[1];
        final MatterTank tank = new MatterTank(spec.state(), spec.volume(), spec.pressure(),
                () -> declared[0].changed());
        declared[0] = fields.part(key, new TankPart(key, tank)).save().toClient();
        return tank;
    }

    /**
     * Tanks of one state a machine has.
     *
     * @param state    the state of matter
     * @param inputs   how many input tanks
     * @param outputs  how many output tanks
     * @param volume   each tank's volume, in millibuckets
     * @param pressure the pressure each holds, in times the open air's
     */
    public record MatterSpec(MatterState state, int inputs, int outputs, long volume, long pressure) {

        public MatterSpec {
            if (state == MatterState.SOLID || state == MatterState.LIQUID) {
                throw new IllegalArgumentException("solids are items and liquids fluids, not tanks of matter");
            }
            if (inputs < 0 || outputs < 0 || volume <= 0 || pressure <= 0) {
                throw new IllegalArgumentException("tanks of " + state.serializedName() + " need a count, a volume "
                        + "and a pressure");
            }
        }
    }

    /* One tank saved under its own key, and sent as it is saved. */
    private record TankPart(String key, MatterTank tank) implements IFieldPart {

        @Override
        public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
            final CompoundTag own = new CompoundTag();
            this.tank.save(own);
            tag.put(this.key, own);
        }

        @Override
        public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
            this.tank.load(tag.getCompound(this.key));
        }

        @Override
        public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
            save(tag, registries);
        }

        @Override
        public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
            load(tag, registries);
        }
    }

    /* Its tanks of one state as a line meets them: the inputs to fill, then the outputs to empty. */
    private final class Outside implements IMatterHandler {

        private final MatterState state;

        private Outside(final MatterState state) {
            this.state = state;
        }

        @Override
        public MatterState state() {
            return this.state;
        }

        @Override
        public int tanks() {
            return inputs(this.state).size() + outputs(this.state).size();
        }

        @Override
        public Optional<MatterStack> stack(final int tank) {
            return at(tank).held();
        }

        @Override
        public long capacity(final int tank) {
            return at(tank).capacity();
        }

        @Override
        public double pressureNow(final int tank) {
            return at(tank).pressureNow();
        }

        @Override
        public long insert(final MatterStack stack, final boolean simulate) {
            long left = stack.amount();
            for (final MatterTank tank : inputs(this.state)) {
                if (left <= 0) {
                    break;
                }
                left -= tank.insert(stack.withAmount(left), simulate);
            }
            return stack.amount() - left;
        }

        @Override
        public long extract(final Material material, final long amount, final boolean simulate) {
            long taken = 0L;
            for (final MatterTank tank : outputs(this.state)) {
                if (taken >= amount) {
                    break;
                }
                taken += tank.extract(material, amount - taken, simulate);
            }
            return taken;
        }

        @Override
        public Optional<MatterStack> extractAny(final long amount, final boolean simulate) {
            for (final MatterTank tank : outputs(this.state)) {
                final Optional<MatterStack> out = tank.extractAny(amount, simulate);
                if (out.isPresent()) {
                    return out;
                }
            }
            return Optional.empty();
        }

        private MatterTank at(final int tank) {
            final List<MatterTank> in = inputs(this.state);
            return tank < in.size() ? in.get(tank) : outputs(this.state).get(tank - in.size());
        }
    }
}
