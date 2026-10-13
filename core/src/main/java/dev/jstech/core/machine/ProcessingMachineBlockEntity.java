/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.look.IDescribed;
import dev.jstech.core.look.LookTexts;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A machine that works its inputs into its outputs: while what its inputs hold makes something and its outputs have
 * room for it, it spends energy each tick and, once the work's time has run, takes the inputs and puts out the result.
 * Its slots are its inputs, then its outputs, then its upgrades; its tanks its fluid inputs, then its fluid outputs.
 * Pipes put into its inputs and its upgrade slots and take from anywhere; only the machine puts into its outputs.
 *
 * <p>What its inputs make is each machine's own: most ask the recipes of their {@link ProcessingKind}, with
 * {@link #byKind}, and a machine that works the game's own recipes, a furnace, asks those instead.
 */
public abstract class ProcessingMachineBlockEntity extends MachineBlockEntity implements IDescribed {

    private final Layout layout;
    private final int defaultEnergyPerTick;
    private final IntField progress;
    private final IntField maxProgress;
    private final List<FieldFluidTank> inputTanks = new ArrayList<>();
    private final List<FieldFluidTank> outputTanks = new ArrayList<>();
    /* The work in hand last tick; empty after a load, when the saved progress is taken as belonging to it. */
    private Processing lastWork;

    protected ProcessingMachineBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                           final Layout layout, final int energyCapacity, final int energyMaxReceive,
                                           final int defaultEnergyPerTick) {
        super(type, pos, state, layout.slots(), energyCapacity, energyMaxReceive, 0);
        this.layout = layout;
        this.defaultEnergyPerTick = defaultEnergyPerTick;
        this.progress = fields().integer("Progress", 0).save().toMenu();
        this.maxProgress = fields().integer("MaxProgress", 0).toMenu();
        for (int i = 0; i < layout.inputTanks(); i++) {
            inputTanks.add(fields().fluid("FluidIn" + i, layout.tankCapacity()).save().toClient().exposed());
        }
        for (int i = 0; i < layout.outputTanks(); i++) {
            outputTanks.add(fields().fluid("FluidOut" + i, layout.tankCapacity()).save().toClient().exposed()
                    .outputOnly());
        }
        getInventory().accepts((slot, stack) -> layout.isInput(slot)
                || layout.isUpgrade(slot) && stack.getItem() instanceof IUpgrade);
    }

    /**
     * How a processing machine's slots and tanks are laid out.
     *
     * @param inputs       the input slots
     * @param outputs      the output slots
     * @param upgrades     the upgrade slots
     * @param inputTanks   the fluid input tanks
     * @param outputTanks  the fluid output tanks
     * @param tankCapacity what each tank holds, in millibuckets
     */
    public record Layout(int inputs, int outputs, int upgrades, int inputTanks, int outputTanks, int tankCapacity) {

        public Layout {
            if (inputs < 0 || outputs < 0 || upgrades < 0 || inputTanks < 0 || outputTanks < 0 || tankCapacity < 0) {
                throw new IllegalArgumentException("a machine has no fewer than no slots");
            }
        }

        /** Items only: so many inputs and outputs, no upgrades, no tanks. */
        public static Layout items(final int inputs, final int outputs) {
            return new Layout(inputs, outputs, 0, 0, 0, 0);
        }

        /** The same, with so many upgrade slots. */
        public Layout withUpgrades(final int slots) {
            return new Layout(inputs, outputs, slots, inputTanks, outputTanks, tankCapacity);
        }

        /** The same, with so many tanks in and out, each holding {@code capacity} millibuckets. */
        public Layout withTanks(final int in, final int out, final int capacity) {
            return new Layout(inputs, outputs, upgrades, in, out, capacity);
        }

        public int slots() {
            return inputs + outputs + upgrades;
        }

        public int firstOutput() {
            return inputs;
        }

        public int firstUpgrade() {
            return inputs + outputs;
        }

        public boolean isInput(final int slot) {
            return slot >= 0 && slot < inputs;
        }

        public boolean isOutput(final int slot) {
            return slot >= inputs && slot < inputs + outputs;
        }

        public boolean isUpgrade(final int slot) {
            return slot >= inputs + outputs && slot < slots();
        }
    }

    /**
     * One piece of work: what it takes from which slots and tanks, what it makes, in how long, at what energy a tick.
     *
     * @param takes         how many items each input slot gives up
     * @param fluidTakes    how many millibuckets each input tank gives up
     * @param outputs       what it makes
     * @param fluidOutputs  the fluids it makes
     * @param ticks         how long it takes, before upgrades
     * @param energyPerTick what it spends each tick, before upgrades; 0 for the machine's own
     */
    public record Processing(List<Take> takes, List<Take> fluidTakes, List<ItemStack> outputs,
                             List<FluidStack> fluidOutputs, int ticks, int energyPerTick) {

        public Processing {
            takes = List.copyOf(takes);
            fluidTakes = List.copyOf(fluidTakes);
            outputs = outputs.stream().map(ItemStack::copy).toList();
            fluidOutputs = fluidOutputs.stream().map(FluidStack::copy).toList();
        }

        /**
         * Whether {@code other} is the same piece of work. The stacks are compared by value because every tick builds
         * its work anew, so the records themselves never compare equal.
         */
        public boolean sameWork(final Processing other) {
            if (this.ticks != other.ticks || this.energyPerTick != other.energyPerTick
                    || !this.takes.equals(other.takes) || !this.fluidTakes.equals(other.fluidTakes)
                    || !ItemStack.listMatches(this.outputs, other.outputs)
                    || this.fluidOutputs.size() != other.fluidOutputs.size()) {
                return false;
            }
            for (int i = 0; i < this.fluidOutputs.size(); i++) {
                if (!FluidStack.matches(this.fluidOutputs.get(i), other.fluidOutputs.get(i))) {
                    return false;
                }
            }
            return true;
        }

        /** One item taken from {@code slot} to make {@code output} in {@code ticks}, at the machine's energy. */
        public static Processing single(final int slot, final ItemStack output, final int ticks) {
            return new Processing(List.of(new Take(slot, 1)), List.of(), List.of(output), List.of(), ticks, 0);
        }

        /** What {@code recipe} does with {@code input}, or empty when the input no longer holds what it takes. */
        public static Optional<Processing> of(final ProcessingRecipe recipe, final ProcessingInput input) {
            final int[] slots = recipe.slotsFor(input);
            final int[] tanks = recipe.tanksFor(input);
            if (slots == null || tanks == null) {
                return Optional.empty();
            }
            final List<Take> takes = new ArrayList<>();
            for (int i = 0; i < slots.length; i++) {
                takes.add(new Take(slots[i], recipe.inputs().get(i).count()));
            }
            final List<Take> fluidTakes = new ArrayList<>();
            for (int i = 0; i < tanks.length; i++) {
                fluidTakes.add(new Take(tanks[i], recipe.fluidInputs().get(i).amount()));
            }
            return Optional.of(new Processing(takes, fluidTakes, recipe.outputs(), recipe.fluidOutputs(),
                    recipe.ticks(), recipe.energyPerTick()));
        }
    }

    /**
     * So much taken from a slot or a tank.
     *
     * @param index  the input slot or input tank, counted from the first input
     * @param amount how many items or millibuckets
     */
    public record Take(int index, int amount) {
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final ProcessingMachineBlockEntity machine) {
        machine.tick(level);
    }

    public Layout layout() {
        return layout;
    }

    public int getProgress() {
        return progress.get();
    }

    public int getMaxProgress() {
        return maxProgress.get();
    }

    /** The fluid input tank {@code index}. */
    public FieldFluidTank inputTank(final int index) {
        return inputTanks.get(index);
    }

    /** The fluid output tank {@code index}. */
    public FieldFluidTank outputTank(final int index) {
        return outputTanks.get(index);
    }

    /** What the upgrades in the upgrade slots do together. */
    public UpgradeEffect upgradeEffect() {
        final List<UpgradeEffect.Counted> each = new ArrayList<>();
        for (int slot = layout.firstUpgrade(); slot < layout.slots(); slot++) {
            final ItemStack stack = getInventory().getStackInSlot(slot);
            if (stack.getItem() instanceof IUpgrade upgrade) {
                each.add(new UpgradeEffect.Counted(upgrade.effect(stack), stack.getCount()));
            }
        }
        return UpgradeEffect.combine(each);
    }

    /** What the machine's inputs hold, as its recipes read them. */
    public ProcessingInput input() {
        final List<ItemStack> items = new ArrayList<>(layout.inputs());
        for (int slot = 0; slot < layout.inputs(); slot++) {
            items.add(getInventory().getStackInSlot(slot));
        }
        final List<FluidStack> fluids = new ArrayList<>(inputTanks.size());
        for (final FieldFluidTank tank : inputTanks) {
            fluids.add(tank.getFluid());
        }
        return new ProcessingInput(items, fluids);
    }

    /** How far through its piece of work the machine is, or that it stands idle. */
    @Override
    public void describe(final List<Text> lines, final boolean details) {
        final int max = maxProgress.get();
        final int done = progress.get();
        lines.add(done > 0 && max > 0 ? LookTexts.WORKING.with((int) Math.min(100L, done * 100L / max))
                : LookTexts.IDLE.text());
    }

    /** What the inputs make, and in how long; empty when they make nothing. */
    protected abstract Optional<Processing> process(Level level, ProcessingInput input);

    /** What the inputs make by the recipes of {@code kind}: the usual answer to {@link #process}. */
    protected static Optional<Processing> byKind(final Level level, final ProcessingKind kind,
                                                 final ProcessingInput input) {
        return kind.find(level, input).flatMap(holder -> Processing.of(holder.value(), input));
    }

    private void tick(final Level level) {
        final ProcessingInput input = input();
        final Optional<Processing> found = input.isEmpty() ? Optional.empty() : process(level, input);
        if (found.isEmpty() || !fits(found.get())) {
            progress.set(0);
            lastWork = null;
            return;
        }
        final Processing work = found.get();
        if (lastWork != null && !lastWork.sameWork(work)) {
            // The inputs went from one recipe to another with no idle tick between; the old energy was spent on
            // other work.
            progress.set(0);
        }
        lastWork = work;
        final UpgradeEffect effect = upgradeEffect();
        maxProgress.set(effect.ticks(work.ticks()));
        final long perTick = effect.energyPerTick(work.energyPerTick() > 0 ? work.energyPerTick()
                : defaultEnergyPerTick);
        if (perTick > Integer.MAX_VALUE || perTick > 0 && !getEnergy().consume((int) perTick)) {
            return;
        }
        progress.add(1);
        if (progress.get() >= maxProgress.get()) {
            finish(work);
            progress.set(0);
            lastWork = null;
        }
    }

    /* Takes the inputs and puts out what they made. */
    private void finish(final Processing work) {
        for (final Take take : work.takes()) {
            getInventory().extractItem(take.index(), take.amount(), false);
        }
        for (final Take take : work.fluidTakes()) {
            inputTanks.get(take.index()).drain(take.amount(), IFluidHandler.FluidAction.EXECUTE);
        }
        for (final ItemStack output : work.outputs()) {
            ItemStack left = output.copy();
            for (int slot = layout.firstOutput(); slot < layout.firstUpgrade() && !left.isEmpty(); slot++) {
                left = putInto(slot, left);
            }
        }
        for (final FluidStack output : work.fluidOutputs()) {
            int left = output.getAmount();
            for (final FieldFluidTank tank : outputTanks) {
                if (left <= 0) {
                    break;
                }
                left -= tank.fillInside(output.copyWithAmount(left), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /* Puts what it can of {@code stack} into the output slot, and gives back what is left. */
    private ItemStack putInto(final int slot, final ItemStack stack) {
        final ItemStack held = getInventory().getStackInSlot(slot);
        final int moved = room(held, stack, slotLimit(slot, stack));
        if (moved <= 0) {
            return stack;
        }
        getInventory().setStackInSlot(slot, held.isEmpty() ? stack.copyWithCount(moved)
                : held.copyWithCount(held.getCount() + moved));
        return stack.copyWithCount(stack.getCount() - moved);
    }

    /* The most of {@code stack} an output slot holds: its own limit, and the stack's size. */
    private int slotLimit(final int slot, final ItemStack stack) {
        return Math.min(getInventory().getSlotLimit(slot), stack.getMaxStackSize());
    }

    /* How many of {@code incoming} fit on {@code held} up to {@code limit}; the insert and the dry run share it. */
    private static int room(final ItemStack held, final ItemStack incoming, final int limit) {
        if (held.isEmpty()) {
            return Math.min(limit, incoming.getCount());
        }
        if (!ItemStack.isSameItemSameComponents(held, incoming)) {
            return 0;
        }
        return Math.max(0, Math.min(limit - held.getCount(), incoming.getCount()));
    }

    /* How many millibuckets of {@code incoming} fit in a tank holding {@code held} up to {@code capacity}. */
    private static int fluidRoom(final FluidStack held, final FluidStack incoming, final int capacity) {
        if (held.isEmpty()) {
            return Math.min(capacity, incoming.getAmount());
        }
        if (!FluidStack.isSameFluidSameComponents(held, incoming)) {
            return 0;
        }
        return Math.max(0, Math.min(capacity - held.getAmount(), incoming.getAmount()));
    }

    /* Whether every output, item and fluid, finds room, as if all of them were put out at once. */
    private boolean fits(final Processing work) {
        final List<ItemStack> slots = new ArrayList<>();
        for (int slot = layout.firstOutput(); slot < layout.firstUpgrade(); slot++) {
            slots.add(getInventory().getStackInSlot(slot).copy());
        }
        for (final ItemStack output : work.outputs()) {
            int left = output.getCount();
            for (int i = 0; i < slots.size() && left > 0; i++) {
                final ItemStack held = slots.get(i);
                final int limit = slotLimit(layout.firstOutput() + i, output);
                final int moved = room(held, output.copyWithCount(left), limit);
                if (moved <= 0) {
                    continue;
                }
                if (held.isEmpty()) {
                    slots.set(i, output.copyWithCount(moved));
                } else {
                    held.grow(moved);
                }
                left -= moved;
            }
            if (left > 0) {
                return false;
            }
        }
        final List<FluidStack> tanks = new ArrayList<>();
        for (final FieldFluidTank tank : outputTanks) {
            tanks.add(tank.getFluid().copy());
        }
        for (final FluidStack output : work.fluidOutputs()) {
            int left = output.getAmount();
            for (int i = 0; i < tanks.size() && left > 0; i++) {
                final FluidStack held = tanks.get(i);
                final int moved = fluidRoom(held, output.copyWithAmount(left), outputTanks.get(i).getCapacity());
                if (moved <= 0) {
                    continue;
                }
                if (held.isEmpty()) {
                    tanks.set(i, output.copyWithAmount(moved));
                } else {
                    held.grow(moved);
                }
                left -= moved;
            }
            if (left > 0) {
                return false;
            }
        }
        return true;
    }
}
