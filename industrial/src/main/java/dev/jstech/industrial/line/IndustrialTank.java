/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.line;

import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.fluid.ReliefValveBlockEntity;
import dev.jstech.core.heat.BoilOff;
import dev.jstech.core.inspect.InspectTexts;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineParameter;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.multiblock.IStructureShape;
import dev.jstech.core.multiblock.ShapeMatch;
import dev.jstech.industrial.blockentity.TankBlockEntity;
import dev.jstech.industrial.multiblock.IndustrialMultiblock;
import dev.jstech.industrial.multiblock.SizeGives;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A tank of J's Industrial, the terminal of a multiblock of a size the player chooses, as its {@link TankRating} says
 * (industrial/docs/design/fluids-and-gases.md): the room inside its walls is its volume, a block a cubic metre, a
 * thousand millibuckets, and a gas tank holds its volume times its pressure. It holds one content, refuses what it is
 * not made for and, full, refuses more; a comparator reads how full it is; a cold liquid in it boils off as the heat of
 * the place leaks through its walls, faster the less they are insulated. Its contents stay while its walls stand apart,
 * and the tank fills again only once it stands whole.
 */
public abstract class IndustrialTank extends MachineBlockEntity {

    private final TankRating rating;
    private final SizeGives size;
    private final LongField inside;
    private final FieldFluidTank liquid;
    private MarkedTank gas;
    private @Nullable BoilOff boilOff;
    private final int[] sizedFor = new int[3];
    private long lastBoiled;
    private boolean sized;

    private static final double TICKS_A_SECOND = 20.0;

    /** A tank of {@code rating}, the terminal of {@code multiblock} of {@code shape}, whose size gives room inside. */
    protected IndustrialTank(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                             final TankRating rating, final IndustrialMultiblock multiblock,
                             final IStructureShape shape) {
        super(type, pos, state, 0, MachineEnergy.fe(0, 0, 0));
        this.rating = Objects.requireNonNull(rating, "rating");
        this.size = multiblock.size();
        this.inside = fields().longInteger("Inside", 0L).save();
        this.liquid = fields().fluid("Tank", 0).save().toClient();
        this.liquid.setValidator(stack -> rating.state() == MatterState.LIQUID && rating.holds(stack.getFluid()));
        this.gas = new MarkedTank(rating, 0L, this::setChanged);
        fields().part("Gas", IFieldPart.of((tag, registries) -> this.gas.save(tag), this::loadGas)).save();
        formsWith(multiblock.structure(shape));
    }

    /** What it says of itself. */
    public TankRating rating() {
        return this.rating;
    }

    /** What it holds at most now, in millibuckets: its room inside, as it last stood whole. */
    public long capacity() {
        return this.rating.capacity(this.inside.get());
    }

    /** Its liquid. */
    public FieldFluidTank liquid() {
        return this.liquid;
    }

    /** Its gas. */
    public MatterTank gas() {
        return this.gas;
    }

    /** What a machine does not: it takes no energy. */
    @Override
    public boolean takesEnergy() {
        return false;
    }

    /**
     * A tick of it: sized again to its room inside when its walls changed, and a cold liquid in it boiled off for the
     * time since it last was; it rests, as a tank only waits.
     */
    @Override
    protected MachineStatus work(final ServerLevel level) {
        final MachineStructure structure = structure();
        final Optional<ShapeMatch.Formed> formed = structure == null ? Optional.empty() : structure.structure();
        formed.ifPresent(found -> resize(found, structure.size()));
        final long now = level.getGameTime();
        if (this.boilOff != null && !this.liquid.isEmpty() && now > this.lastBoiled) {
            this.boilOff.vent(level, this.worldPosition, this.liquid, null,
                    this.lastBoiled == 0L ? 0.0 : (now - this.lastBoiled) / TICKS_A_SECOND);
        }
        this.lastBoiled = now;
        return MachineStatus.WAITING;
    }

    @Override
    protected @Nullable IFluidHandler fluidsIn() {
        return this.rating.state() == MatterState.LIQUID ? this.liquid : null;
    }

    @Override
    protected @Nullable IFluidHandler fluidsOut() {
        return fluidsIn();
    }

    @Override
    protected @Nullable IMatterHandler matterHandler(final MatterState state) {
        return state == MatterState.GAS && this.rating.state() == MatterState.GAS ? this.gas : null;
    }

    /** How full it is, which a comparator reads, whichever it is set to read. */
    @Override
    protected double inputShare() {
        final long capacity = capacity();
        if (capacity <= 0) {
            return 0.0;
        }
        return Math.min(1.0, held() / (double) capacity);
    }

    @Override
    protected double outputShare() {
        return inputShare();
    }

    /** What a controller reads of it besides a machine's own: what it holds, and a gas tank's pressure. */
    @Override
    protected void declareParameters(final List<MachineParameter> declared) {
        declared.add(MachineParameter.read("amount", TankBlockEntity.AMOUNT, TankBlockEntity.MILLIBUCKETS, 0,
                Math.max(1, capacity()), this::held));
        if (this.rating.state() == MatterState.GAS) {
            declared.add(MachineParameter.read("pressure", InspectTexts.PRESSURE, ReliefValveBlockEntity.ATMOSPHERES,
                    0, Math.max(1, this.gas.pressure()), () -> this.gas.pressureNow(0)));
        }
    }

    /* What it holds now, in millibuckets, of its liquid or its gas. */
    private long held() {
        return this.rating.state() == MatterState.LIQUID ? this.liquid.getFluidAmount()
                : this.gas.held().map(MatterStack::amount).orElse(0L);
    }

    /* Sizes it to the room inside {@code formed}, of {@code outer} blocks along x, y and z, keeping what it holds. */
    private void resize(final ShapeMatch.Formed formed, final int[] outer) {
        final long room = this.size.blocksInside(formed);
        // Loaded, it is sized again once: its liquid's capacity is not saved with it.
        if (room != this.inside.get() || !this.sized) {
            this.sized = true;
            this.inside.set(room);
            this.liquid.setCapacity((int) Math.min(Integer.MAX_VALUE, this.rating.capacity(room)));
            final Optional<MatterStack> held = this.gas.held();
            this.gas = new MarkedTank(this.rating, room * TankRating.CUBIC_METRE, this::setChanged);
            held.ifPresent(this.gas::set);
            setChanged();
        }
        if (outer[0] != this.sizedFor[0] || outer[1] != this.sizedFor[1] || outer[2] != this.sizedFor[2]) {
            System.arraycopy(outer, 0, this.sizedFor, 0, 3);
            final double surface = 2.0 * (outer[0] * outer[1] + outer[1] * outer[2] + outer[0] * outer[2]);
            this.boilOff = BoilOff.of(this.rating.insulation(), surface);
        }
    }

    /* Reads its gas back, in a tank of the room it last stood with. */
    private void loadGas(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.gas = new MarkedTank(this.rating, this.inside.get() * TankRating.CUBIC_METRE, this::setChanged);
        this.gas.load(tag);
    }

    /* A tank of gas that refuses what its tank is not made for, and everything while it has no room. */
    private static final class MarkedTank extends MatterTank {

        private final TankRating rating;
        private final boolean open;

        MarkedTank(final TankRating rating, final long volume, final Runnable onChange) {
            super(MatterState.GAS, Math.max(1L, volume), rating.pressure(), onChange);
            this.rating = rating;
            this.open = volume > 0;
        }

        @Override
        public long insert(final MatterStack stack, final boolean simulate) {
            return this.open && this.rating.holds(stack.material()) ? super.insert(stack, simulate) : 0L;
        }

        @Override
        public long push(final MatterStack stack, final long pressure, final boolean simulate) {
            return this.open && this.rating.holds(stack.material()) ? super.push(stack, pressure, simulate) : 0L;
        }
    }
}
