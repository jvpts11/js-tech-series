/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fluid;

import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.machine.IMachineParameters;
import dev.jstech.core.machine.MachineParameter;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.signal.ISignalOutput;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.signal.Signals;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A valve on a line of liquid or of gas, a block between two runs of it: what reaches its back from the line behind it
 * goes out of its front into the line ahead, never the other way, as much as its rate a tick, while its rule opens it
 * ({@link IValveRule}: always, as a check valve; by hand; by a redstone signal). One may let a single fluid or gas
 * through, a filter valve sending each of a shared run to its machine; and every valve counts what has passed it, the
 * flow a comparator reads of it, as a flow meter shows. Its mod gives it a block that faces whoever placed it, its type
 * and its capabilities ({@link #register}).
 *
 * <pre>{@code
 * public static final BlockEntry<DeviceBlock> CHECK_VALVE = CONTENT.block("check_valve",
 *         Device.of(() -> MyContent.CHECK_VALVE_BE.get()).ticks(LineValveBlockEntity::serverTick)::block)...;
 * // its type, made with (pos, state) -> new LineValveBlockEntity(CHECK_VALVE_BE.get(), pos, state,
 * //         MatterState.LIQUID, 200, IValveRule.ALWAYS), and in the mod's RegisterCapabilitiesEvent:
 * LineValveBlockEntity.register(event, CHECK_VALVE_BE.get());
 * }</pre>
 */
@TextHolder
public class LineValveBlockEntity extends SyncedBlockEntity implements ISignalOutput, IMachineParameters {

    private final MatterState state;
    private final long rate;
    private final IValveRule rule;
    private final BoolField byHand;
    private final ValueField<Optional<ResourceLocation>> filter;
    private final LongField passed;
    private final FieldFluidTank liquid;
    private final MatterTank gas;
    private boolean openNow;
    private long passing;
    private long lastTick;
    private @Nullable List<MachineParameter> parameters;

    public static final TextKey FLOW = TextKey.of("jscore.valve.flow", "Flow");
    public static final TextKey PER_TICK = TextKey.of("jscore.valve.per_tick", "mB a tick");
    public static final TextKey OPEN = TextKey.of("jscore.valve.open", "Open");

    /**
     * A valve of a line of {@code state}, a liquid or a gas, letting through at most {@code rate} millibuckets a tick
     * while {@code rule} opens it.
     */
    public LineValveBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState blockState,
                                final MatterState state, final long rate, final IValveRule rule) {
        super(type, pos, blockState);
        if (state != MatterState.LIQUID && state != MatterState.GAS) {
            throw new IllegalArgumentException("a valve is on a line of liquid or of gas, not of " + state);
        }
        if (rate <= 0 || rate > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("a valve lets through some millibuckets a tick, not " + rate);
        }
        this.state = state;
        this.rate = rate;
        this.rule = Objects.requireNonNull(rule, "rule");
        this.byHand = fields().flag("Open", false).save().toClient();
        this.filter = fields().value("Filter", ResourceLocation.CODEC.optionalFieldOf("id").codec(),
                Optional.<ResourceLocation>empty()).save().toClient();
        this.passed = fields().longInteger("Passed", 0L).save();
        this.liquid = fields().fluid("Liquid", (int) rate).save();
        this.gas = new MatterTank(MatterState.GAS, rate, 1, this::setChanged);
        fields().part("Gas", IFieldPart.of((tag, registries) -> this.gas.save(tag),
                (tag, registries) -> this.gas.load(tag))).save();
    }

    /** Gives valves of {@code type} their faces: the line behind fills them, the line ahead draws on them. */
    public static <T extends LineValveBlockEntity> void register(final RegisterCapabilitiesEvent event,
                                                                 final BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, LineValveBlockEntity::fluid);
        MatterCapabilities.block(MatterState.GAS).ifPresent(capability -> event.registerBlockEntity(capability, type,
                LineValveBlockEntity::matter));
        event.registerBlockEntity(Signals.OUTPUT, type, (valve, side) -> valve);
        event.registerBlockEntity(IMachineParameters.CAPABILITY, type, (valve, side) -> valve);
    }

    /** One server tick of {@code valve}, the ticker its block is ticked by: whether it is open now, and its flow. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final LineValveBlockEntity valve) {
        if (level instanceof ServerLevel server) {
            valve.openNow = valve.rule.open(server, valve);
            valve.lastTick = valve.passing;
            valve.passing = 0L;
        }
    }

    /** Whether a player has opened it by hand. */
    public boolean openByHand() {
        return this.byHand.get();
    }

    /** Opens it by hand, or shuts it, as a player turns its wheel. */
    public void turn() {
        this.byHand.set(!this.byHand.get());
    }

    /** Whether it lets through now, as its rule last said. */
    public boolean open() {
        return this.openNow;
    }

    /** What it lets through alone, a fluid's or a material's id; none for everything. */
    public Optional<ResourceLocation> filter() {
        return this.filter.get();
    }

    /** Lets through only {@code id}, a fluid's or a material's, or everything for none. */
    public void setFilter(final Optional<ResourceLocation> id) {
        this.filter.set(Objects.requireNonNull(id, "id"));
    }

    /** What has passed it in all, in millibuckets. */
    public long passed() {
        return this.passed.get();
    }

    /** What passed it in the last tick, in millibuckets. */
    public long flow() {
        return this.lastTick;
    }

    /** The face its line ahead meets: the one it faces. */
    public Direction front() {
        return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    /** What the game's fluid capability sees of it on {@code side}: its back takes, its front gives. */
    public @Nullable IFluidHandler fluid(final @Nullable Direction side) {
        if (this.state != MatterState.LIQUID || side == null) {
            return null;
        }
        if (side == front()) {
            return new Ahead();
        }
        return side == front().getOpposite() ? new Behind() : null;
    }

    /** What the Core's gas capability sees of it on {@code side}: its back takes, its front gives. */
    public @Nullable IMatterHandler matter(final @Nullable Direction side) {
        if (this.state != MatterState.GAS || side == null) {
            return null;
        }
        if (side == front()) {
            return new GasAhead();
        }
        return side == front().getOpposite() ? new GasBehind() : null;
    }

    /** Its flow, which a comparator reads: from nothing to its rate a tick. */
    @Override
    public List<Reading> readings() {
        return List.of(new Reading(FLOW, this.lastTick, 0, this.rate, PER_TICK));
    }

    /**
     * What a controller reads of it: whether it lets through, and its flow. A valve opened by hand is opened and shut
     * from outside as by its wheel; one that follows a signal, or is always open, is only read.
     */
    @Override
    public List<MachineParameter> parameters() {
        if (this.parameters == null) {
            final MachineParameter open = this.rule == IValveRule.BY_HAND
                    ? MachineParameter.set("open", OPEN, null, 0, 1, () -> this.openNow ? 1 : 0,
                    value -> this.byHand.set(value >= 0.5))
                    : MachineParameter.read("open", OPEN, null, 0, 1, () -> this.openNow ? 1 : 0);
            this.parameters = List.of(open, MachineParameter.read("flow", FLOW, PER_TICK, 0, this.rate,
                    () -> this.lastTick));
        }
        return this.parameters;
    }

    private boolean passes(final ResourceLocation id) {
        return this.filter.get().map(id::equals).orElse(true);
    }

    private void counted(final long amount) {
        if (amount > 0) {
            this.passing += amount;
            this.passed.set(this.passed.get() + amount);
        }
    }

    /* Its back, which the line behind it fills while it is open. */
    private final class Behind implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            return liquid.getFluid();
        }

        @Override
        public int getTankCapacity(final int tank) {
            return liquid.getCapacity();
        }

        @Override
        public boolean isFluidValid(final int tank, final FluidStack stack) {
            return openNow && passes(BuiltInRegistries.FLUID.getKey(stack.getFluid()));
        }

        @Override
        public int fill(final FluidStack resource, final FluidAction action) {
            return isFluidValid(0, resource) ? liquid.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(final FluidStack resource, final FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(final int maxDrain, final FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    /* Its front, which the line ahead draws on. */
    private final class Ahead implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            return liquid.getFluid();
        }

        @Override
        public int getTankCapacity(final int tank) {
            return liquid.getCapacity();
        }

        @Override
        public boolean isFluidValid(final int tank, final FluidStack stack) {
            return false;
        }

        @Override
        public int fill(final FluidStack resource, final FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(final FluidStack resource, final FluidAction action) {
            final FluidStack out = liquid.drain(resource, action);
            if (action.execute()) {
                counted(out.getAmount());
            }
            return out;
        }

        @Override
        public FluidStack drain(final int maxDrain, final FluidAction action) {
            final FluidStack out = liquid.drain(maxDrain, action);
            if (action.execute()) {
                counted(out.getAmount());
            }
            return out;
        }
    }

    /* Its back, of gas. */
    private final class GasBehind implements IMatterHandler {

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
            return gas.stack(tank);
        }

        @Override
        public long capacity(final int tank) {
            return gas.capacity(tank);
        }

        @Override
        public long insert(final MatterStack stack, final boolean simulate) {
            return openNow && passes(stack.material().id()) ? gas.insert(stack, simulate) : 0L;
        }

        @Override
        public long extract(final Material material, final long amount, final boolean simulate) {
            return 0L;
        }

        @Override
        public Optional<MatterStack> extractAny(final long amount, final boolean simulate) {
            return Optional.empty();
        }
    }

    /* Its front, of gas. */
    private final class GasAhead implements IMatterHandler {

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
            return gas.stack(tank);
        }

        @Override
        public long capacity(final int tank) {
            return gas.capacity(tank);
        }

        @Override
        public long insert(final MatterStack stack, final boolean simulate) {
            return 0L;
        }

        @Override
        public long extract(final Material material, final long amount, final boolean simulate) {
            final long out = gas.extract(material, amount, simulate);
            if (!simulate) {
                counted(out);
            }
            return out;
        }

        @Override
        public Optional<MatterStack> extractAny(final long amount, final boolean simulate) {
            final Optional<MatterStack> out = gas.extractAny(amount, simulate);
            if (!simulate) {
                out.ifPresent(taken -> counted(taken.amount()));
            }
            return out;
        }
    }
}
