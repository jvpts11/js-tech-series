/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.blockentity.CombinedTanks;
import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.energy.PartialWork;
import dev.jstech.core.heat.HeatBody;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.look.LookTexts;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * A machine that works its inputs into its outputs: while what its inputs hold makes something and its outputs have
 * room for it, byproducts and all, it spends energy each tick and, once the work's time has run, takes the inputs and
 * puts out the result. Short of energy, it spends what there is and works slower in the same proportion
 * ({@link PartialWork}); damaged, it works slower too.
 *
 * <p>Its slots are its inputs, then its outputs, then its upgrades; with several lanes, each lane's inputs, then each
 * lane's outputs. Each lane works on its own, as Mekanism's factories do, and the machine sorts what is put into its
 * inputs among its lanes by itself. Its tanks, of liquids and of the other states of matter, serve every lane. Pipes
 * put into its inputs and its upgrade slots and take from anywhere; only the machine puts into its outputs. An input
 * slot may be locked to an item, so automation never fills it with the wrong thing; and when its inputs make several
 * recipes, the player picks which.
 *
 * <p>What its inputs make is each machine's own: most ask the recipes of their {@link ProcessingKind}, with
 * {@link #byKind}, and a machine that works the game's own recipes, a furnace, asks those instead.
 */
public abstract class ProcessingMachineBlockEntity extends MachineBlockEntity {

    private final Layout layout;
    private final long defaultEnergyPerTick;
    private final MatterTanks matter;
    private final List<Lane> lanes = new ArrayList<>();
    private final List<FieldFluidTank> inputTanks = new ArrayList<>();
    private final List<FieldFluidTank> outputTanks = new ArrayList<>();
    private final ValueField<ResourceLocation> chosenRecipe;
    private final SlotLocks locks;
    /* What its upgrades do together, worked out again only when an upgrade slot changes. */
    private @Nullable UpgradeEffect effect;

    /** How often a machine of several lanes sorts its inputs among them, in ticks. */
    public static final int SORT_EVERY = 10;
    /* The slots on a row of the process window, when the lanes are too many for rows of their own. */
    private static final int ROW = 6;
    private static final String CHOSEN = "chosen_recipe";

    protected ProcessingMachineBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                           final Layout layout, final int energyCapacity, final int energyMaxReceive,
                                           final int defaultEnergyPerTick) {
        this(type, pos, state, layout, MachineEnergy.fe(energyCapacity, energyMaxReceive, 0), defaultEnergyPerTick);
    }

    /**
     * A machine of {@code layout} running on {@code energy}, spending {@code defaultEnergyPerTick} a tick on a recipe
     * that names no energy of its own.
     */
    protected ProcessingMachineBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                           final Layout layout, final MachineEnergy energy,
                                           final long defaultEnergyPerTick) {
        super(type, pos, state, layout.slots(), energy);
        this.layout = layout;
        this.defaultEnergyPerTick = defaultEnergyPerTick;
        for (int lane = 0; lane < layout.lanes(); lane++) {
            this.lanes.add(new Lane(lane));
        }
        for (int i = 0; i < layout.inputTanks(); i++) {
            inputTanks.add(fields().fluid("FluidIn" + i, layout.tankCapacity()).save().toClient().exposed());
        }
        for (int i = 0; i < layout.outputTanks(); i++) {
            outputTanks.add(fields().fluid("FluidOut" + i, layout.tankCapacity()).save().toClient().exposed()
                    .outputOnly());
        }
        this.matter = MatterTanks.in(fields(), layout.matter());
        this.chosenRecipe = fields().nullable("ChosenRecipe", ResourceLocation.CODEC).save().toClient();
        final PartField[] locksField = new PartField[1];
        this.locks = new SlotLocks(layout.firstOutput(), () -> locksField[0].changed());
        locksField[0] = fields().part("Locks", this.locks).save().toClient();
        getInventory().accepts((slot, stack) -> layout.isInput(slot) && this.locks.allows(slot, stack)
                || layout.isUpgrade(slot) && stack.getItem() instanceof IUpgrade)
                .stackLimit((slot, stack) -> layout.isUpgrade(slot) ? upgradeRoom(slot, stack)
                        : Item.ABSOLUTE_MAX_STACK_SIZE)
                .onChange(slot -> {
                    if (layout.isUpgrade(slot)) {
                        this.effect = null;
                    }
                })
                .onLoad(() -> this.effect = null);
    }

    /**
     * How a processing machine's slots and tanks are laid out.
     *
     * @param inputs       the input slots of each lane
     * @param outputs      the output slots of each lane
     * @param upgrades     the upgrade slots
     * @param inputTanks   the fluid input tanks
     * @param outputTanks  the fluid output tanks
     * @param tankCapacity what each tank holds, in millibuckets
     * @param matter       the tanks of the states of matter that are neither solid nor liquid
     * @param lanes        how many lanes work side by side, each with its own inputs and outputs
     */
    public record Layout(int inputs, int outputs, int upgrades, int inputTanks, int outputTanks, int tankCapacity,
                         List<MatterTanks.MatterSpec> matter, int lanes) {

        public Layout {
            if (inputs < 0 || outputs < 0 || upgrades < 0 || inputTanks < 0 || outputTanks < 0 || tankCapacity < 0) {
                throw new IllegalArgumentException("a machine has no fewer than no slots");
            }
            if (lanes < 1) {
                throw new IllegalArgumentException("a machine works in one lane at least");
            }
            matter = List.copyOf(matter);
        }

        /** A layout of one lane with no tanks of matter but liquids. */
        public Layout(final int inputs, final int outputs, final int upgrades, final int inputTanks,
                      final int outputTanks, final int tankCapacity) {
            this(inputs, outputs, upgrades, inputTanks, outputTanks, tankCapacity, List.of(), 1);
        }

        /** Items only: so many inputs and outputs, no upgrades, no tanks. */
        public static Layout items(final int inputs, final int outputs) {
            return new Layout(inputs, outputs, 0, 0, 0, 0);
        }

        /** The same, with so many upgrade slots. */
        public Layout withUpgrades(final int slots) {
            return new Layout(inputs, outputs, slots, inputTanks, outputTanks, tankCapacity, matter, lanes);
        }

        /** The same, with so many tanks in and out, each holding {@code capacity} millibuckets. */
        public Layout withTanks(final int in, final int out, final int capacity) {
            return new Layout(inputs, outputs, upgrades, in, out, capacity, matter, lanes);
        }

        /**
         * The same, with so many tanks of {@code state} in and out, each of {@code volume} millibuckets holding
         * {@code pressure} times the open air's pressure: a gas, a supercritical fluid, a slurry, a plasma or exotic
         * matter.
         */
        public Layout withMatter(final MatterState state, final int in, final int out, final long volume,
                                 final long pressure) {
            final List<MatterTanks.MatterSpec> more = new ArrayList<>(matter);
            more.add(new MatterTanks.MatterSpec(state, in, out, volume, pressure));
            return new Layout(inputs, outputs, upgrades, inputTanks, outputTanks, tankCapacity, more, lanes);
        }

        /** The same, working {@code count} lanes side by side, each with its own inputs and outputs. */
        public Layout withLanes(final int count) {
            return new Layout(inputs, outputs, upgrades, inputTanks, outputTanks, tankCapacity, matter, count);
        }

        public int slots() {
            return lanes * (inputs + outputs) + upgrades;
        }

        public int firstOutput() {
            return lanes * inputs;
        }

        public int firstUpgrade() {
            return lanes * (inputs + outputs);
        }

        public boolean isInput(final int slot) {
            return slot >= 0 && slot < firstOutput();
        }

        public boolean isOutput(final int slot) {
            return slot >= firstOutput() && slot < firstUpgrade();
        }

        public boolean isUpgrade(final int slot) {
            return slot >= firstUpgrade() && slot < slots();
        }

        /** The slot of input {@code index} of lane {@code lane}. */
        public int laneInput(final int lane, final int index) {
            return lane * inputs + index;
        }

        /** The slot of output {@code index} of lane {@code lane}. */
        public int laneOutput(final int lane, final int index) {
            return firstOutput() + lane * outputs + index;
        }
    }

    /**
     * One piece of work: what it takes from which slots and tanks, what it makes, in how long, at what energy a tick.
     *
     * @param takes         how many items each input slot of the lane gives up
     * @param fluidTakes    how many millibuckets each input tank gives up
     * @param outputs       what it makes
     * @param fluidOutputs  the fluids it makes
     * @param ticks         how long it takes, before upgrades
     * @param energyPerTick what it spends each tick, before upgrades; 0 for the machine's own
     * @param matterTakes   how many millibuckets each input tank of matter gives up, counted across every state's
     * @param matterOutputs the matter it makes
     * @param byproducts    what it may make besides, by chance
     */
    public record Processing(List<Take> takes, List<Take> fluidTakes, List<ItemStack> outputs,
                             List<FluidStack> fluidOutputs, int ticks, int energyPerTick, List<Take> matterTakes,
                             List<MatterStack> matterOutputs, List<Byproduct> byproducts) {

        public Processing {
            takes = List.copyOf(takes);
            fluidTakes = List.copyOf(fluidTakes);
            outputs = outputs.stream().map(ItemStack::copy).toList();
            fluidOutputs = fluidOutputs.stream().map(FluidStack::copy).toList();
            matterTakes = List.copyOf(matterTakes);
            matterOutputs = List.copyOf(matterOutputs);
            byproducts = List.copyOf(byproducts);
        }

        /** Work of items and liquids only, with no byproduct. */
        public Processing(final List<Take> takes, final List<Take> fluidTakes, final List<ItemStack> outputs,
                          final List<FluidStack> fluidOutputs, final int ticks, final int energyPerTick) {
            this(takes, fluidTakes, outputs, fluidOutputs, ticks, energyPerTick, List.of(), List.of(), List.of());
        }

        /**
         * Whether {@code other} is the same piece of work. The stacks are compared by value because every tick builds
         * its work anew, so the records themselves never compare equal.
         */
        public boolean sameWork(final Processing other) {
            if (this.ticks != other.ticks || this.energyPerTick != other.energyPerTick
                    || !this.takes.equals(other.takes) || !this.fluidTakes.equals(other.fluidTakes)
                    || !this.matterTakes.equals(other.matterTakes) || !this.matterOutputs.equals(other.matterOutputs)
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
            final int[] matterTanks = recipe.matterTanksFor(input);
            if (slots == null || tanks == null || matterTanks == null
                    || !recipe.conditions().holds(input.temperature(), input.pressure())) {
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
            final List<Take> matterTakes = new ArrayList<>();
            for (int i = 0; i < matterTanks.length; i++) {
                matterTakes.add(new Take(matterTanks[i], (int) Math.min(Integer.MAX_VALUE,
                        recipe.matterInputs().get(i).amount())));
            }
            return Optional.of(new Processing(takes, fluidTakes, recipe.outputs(), recipe.fluidOutputs(),
                    recipe.ticks(), recipe.energyPerTick(), matterTakes, recipe.matterOutputs(),
                    recipe.byproducts()));
        }
    }

    /**
     * So much taken from a slot or a tank.
     *
     * @param index  the input slot of the lane or the input tank, counted from the first
     * @param amount how many items or millibuckets
     */
    public record Take(int index, int amount) {
    }

    public Layout layout() {
        return layout;
    }

    /** How far through its work its first lane is, in ticks. */
    public int getProgress() {
        return lanes.getFirst().progress.get();
    }

    /** How long its first lane's work takes, in ticks. */
    public int getMaxProgress() {
        return lanes.getFirst().maxProgress.get();
    }

    /** How far through its work lane {@code lane} is, and how long it takes, in ticks. */
    public int progress(final int lane) {
        return lanes.get(lane).progress.get();
    }

    public int maxProgress(final int lane) {
        return lanes.get(lane).maxProgress.get();
    }

    /** How fast it worked last tick, in percent of its full speed: below a hundred while it is short of energy. */
    public int speed() {
        return lanes.getFirst().speed.get();
    }

    /** The fluid input tank {@code index}. */
    public FieldFluidTank inputTank(final int index) {
        return inputTanks.get(index);
    }

    /** The fluid output tank {@code index}. */
    public FieldFluidTank outputTank(final int index) {
        return outputTanks.get(index);
    }

    /** Its tanks of gases, supercritical fluids, slurries, plasmas and exotic matter. */
    public MatterTanks matter() {
        return matter;
    }

    /** Its input slots locked to an item. */
    public SlotLocks locks() {
        return locks;
    }

    /** The recipe the player picked, when its inputs make several; none for the first that fits. */
    public Optional<ResourceLocation> chosenRecipe() {
        return Optional.ofNullable(chosenRecipe.get());
    }

    /** Picks the recipe named {@code id} among those its inputs make; none goes back to the first that fits. */
    public void chooseRecipe(final @Nullable ResourceLocation id) {
        chosenRecipe.set(id);
    }

    /**
     * Its inputs and outputs where the standard face shows them: each lane a row of the process window, its inputs,
     * an arrow and its outputs; on rows of six in the window when they are too many for that.
     */
    @Override
    public List<FaceSlot> mainSlots() {
        final List<FaceSlot> slots = new ArrayList<>();
        if (MachineScreenLayout.fits(layout.lanes(), layout.inputs(), layout.outputs())) {
            for (int lane = 0; lane < layout.lanes(); lane++) {
                for (int i = 0; i < layout.inputs(); i++) {
                    final var at = MachineScreenLayout.laneSlot(lane, layout.lanes(), i, false, layout.inputs());
                    slots.add(new FaceSlot(layout.laneInput(lane, i), at.x(), at.y(), false));
                }
                for (int i = 0; i < layout.outputs(); i++) {
                    final var at = MachineScreenLayout.laneSlot(lane, layout.lanes(), i, true, layout.inputs());
                    slots.add(new FaceSlot(layout.laneOutput(lane, i), at.x(), at.y(), true));
                }
            }
            return slots;
        }
        for (int slot = 0; slot < layout.firstUpgrade(); slot++) {
            final int x = MachineScreenLayout.faceX(MachineScreenLayout.PROCESS_X + 6 + slot % ROW
                    * MachineScreenLayout.SLOT) + 1;
            final int y = MachineScreenLayout.faceY(MachineScreenLayout.PROCESS_Y + 1 + slot / ROW
                    * MachineScreenLayout.SLOT) + 1;
            slots.add(new FaceSlot(slot, x, y, layout.isOutput(slot)));
        }
        return slots;
    }

    /**
     * What the upgrades in the upgrade slots do together: of each upgrade, as many as fit in one machine count, the
     * first slots first.
     */
    public UpgradeEffect upgradeEffect() {
        final UpgradeEffect known = this.effect;
        if (known != null) {
            return known;
        }
        final List<UpgradeEffect.Counted> each = new ArrayList<>();
        final Map<Item, Integer> seen = new HashMap<>();
        for (int slot = layout.firstUpgrade(); slot < layout.slots(); slot++) {
            final ItemStack stack = getInventory().getStackInSlot(slot);
            if (stack.getItem() instanceof IUpgrade upgrade) {
                final int before = seen.getOrDefault(stack.getItem(), 0);
                final int counted = (int) Math.max(0L, Math.min(stack.getCount(),
                        (long) upgrade.most(stack) - before));
                seen.put(stack.getItem(), before + counted);
                each.add(new UpgradeEffect.Counted(upgrade.effect(stack), counted));
            }
        }
        final UpgradeEffect combined = UpgradeEffect.combine(each);
        this.effect = combined;
        return combined;
    }

    /** How many of {@code upgrade} its upgrade slots hold together. */
    public int upgradesOf(final Item upgrade) {
        int count = 0;
        for (int slot = layout.firstUpgrade(); slot < layout.slots(); slot++) {
            final ItemStack stack = getInventory().getStackInSlot(slot);
            if (stack.is(upgrade)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** What the first lane's inputs hold, as its recipes read them. */
    public ProcessingInput input() {
        return input(0);
    }

    /** What lane {@code lane}'s inputs hold, with the tanks every lane shares, as its recipes read them. */
    public ProcessingInput input(final int lane) {
        final List<ItemStack> items = new ArrayList<>(layout.inputs());
        for (int index = 0; index < layout.inputs(); index++) {
            items.add(getInventory().getStackInSlot(layout.laneInput(lane, index)));
        }
        final List<FluidStack> fluids = new ArrayList<>(inputTanks.size());
        for (final FieldFluidTank tank : inputTanks) {
            fluids.add(tank.getFluid());
        }
        final List<Optional<MatterStack>> held = new ArrayList<>();
        for (final MatterTank tank : matterInputs()) {
            held.add(tank.held());
        }
        return new ProcessingInput(items, fluids, held, temperature(), pressure());
    }

    /** How far through its piece of work the machine is or that it stands idle, its speed if low, and its state. */
    @Override
    public void describe(final List<Text> lines, final boolean details) {
        final int max = getMaxProgress();
        final int done = getProgress();
        lines.add(done > 0 && max > 0 ? LookTexts.WORKING.with((int) Math.min(100L, done * 100L / max))
                : LookTexts.IDLE.text());
        if (speed() < PartialWork.PERCENT) {
            lines.add(LookTexts.LOW_POWER.with(speed()));
        }
        super.describe(lines, details);
    }

    /** Items and energy, and its fluids and each state of matter it has tanks of. */
    @Override
    protected Set<MachineResource> resources() {
        final Set<MachineResource> resources = EnumSet.of(MachineResource.ITEMS, MachineResource.ENERGY);
        if (!inputTanks.isEmpty() || !outputTanks.isEmpty()) {
            resources.add(MachineResource.FLUIDS);
        }
        for (final MatterTanks.MatterSpec spec : layout.matter()) {
            MachineResource.of(spec.state()).ifPresent(resources::add);
        }
        return resources;
    }

    @Override
    protected IItemHandler itemsIn() {
        return new RangedWrapper(getInventory(), 0, layout.firstOutput());
    }

    @Override
    protected IItemHandler itemsOut() {
        return new RangedWrapper(getInventory(), layout.firstOutput(), layout.firstUpgrade());
    }

    @Override
    protected @Nullable IFluidHandler fluidsIn() {
        return inputTanks.isEmpty() ? null : new CombinedTanks(inputTanks);
    }

    @Override
    protected @Nullable IFluidHandler fluidsOut() {
        return outputTanks.isEmpty() ? null : new CombinedTanks(outputTanks);
    }

    @Override
    protected List<MatterTank> matterIn(final MatterState state) {
        return matter.inputs(state);
    }

    @Override
    protected List<MatterTank> matterOut(final MatterState state) {
        return matter.outputs(state);
    }

    @Override
    protected double progressShare() {
        final int max = getMaxProgress();
        return max <= 0 ? 0.0 : Math.min(1.0, getProgress() / (double) max);
    }

    @Override
    protected double inputShare() {
        return share(0, layout.firstOutput(), inputTanks);
    }

    @Override
    protected double outputShare() {
        return share(layout.firstOutput(), layout.firstUpgrade(), outputTanks);
    }

    /**
     * The temperature inside it, in kelvin: its casing's, for a machine that holds heat, and otherwise the air of the
     * place it stands in.
     */
    protected double temperature() {
        final HeatBody casing = heatBody();
        if (casing != null && casing.known()) {
            return casing.temperature();
        }
        return level == null ? ProcessingInput.AMBIENT_KELVIN : HeatClimate.ambient(level, worldPosition);
    }

    /** The pressure inside it, in kilopascals: the air of the place it stands in, unless it holds its own. */
    protected double pressure() {
        return level == null ? ProcessingInput.AMBIENT_KPA : HeatClimate.pressure(level, worldPosition);
    }

    /** What its upgrades do to it. */
    @Override
    protected UpgradeEffect effect() {
        return upgradeEffect();
    }

    /** Its input locks and the recipe picked, which only a machine of its kind can take. */
    @Override
    protected void writeSetup(final CompoundTag setup, final HolderLookup.Provider registries) {
        locks.save(setup, registries);
        chosenRecipe().ifPresent(id -> setup.putString(CHOSEN, id.toString()));
    }

    @Override
    protected void readSetup(final CompoundTag setup, final HolderLookup.Provider registries) {
        locks.paste(setup, registries);
        chooseRecipe(setup.contains(CHOSEN) ? ResourceLocation.tryParse(setup.getString(CHOSEN)) : null);
    }

    /** What the inputs make, and in how long; empty when they make nothing. */
    protected abstract Optional<Processing> process(Level level, ProcessingInput input);

    /**
     * What the inputs make by the recipes of {@code kind}: the usual answer to {@link #process}. When they make
     * several, the one the player picked, or the first.
     */
    protected Optional<Processing> byKind(final Level level, final ProcessingKind kind, final ProcessingInput input) {
        final List<RecipeHolder<ProcessingRecipe>> all = kind.findAll(level, input);
        if (all.isEmpty()) {
            return Optional.empty();
        }
        final ResourceLocation chosen = chosenRecipe.get();
        final RecipeHolder<ProcessingRecipe> picked = chosen == null ? all.getFirst()
                : all.stream().filter(holder -> holder.id().equals(chosen)).findFirst().orElse(all.getFirst());
        return Processing.of(picked.value(), input);
    }

    /*
     * One tick of work: each lane works on its own, and the machine is as busy as its busiest lane: working, else
     * waiting for energy, else with its output full, else waiting for input.
     */
    @Override
    protected MachineStatus work(final ServerLevel level) {
        if (lanes.size() > 1 && level.getGameTime() % SORT_EVERY == 0) {
            sortInputs();
        }
        final UpgradeEffect effect = upgradeEffect();
        final double share = health().workShare();
        MachineStatus result = MachineStatus.WAITING;
        for (final Lane lane : lanes) {
            final MachineStatus worked = lane.work(level, effect, share);
            if (busier(worked, result)) {
                result = worked;
            }
        }
        return result;
    }

    /*
     * How many of {@code stack} upgrade slot {@code slot} takes: as many as fit in one machine, less what its other
     * upgrade slots hold of it.
     */
    private int upgradeRoom(final int slot, final ItemStack stack) {
        if (!(stack.getItem() instanceof IUpgrade upgrade)) {
            return 0;
        }
        final long elsewhere = upgradesOf(stack.getItem()) - (getInventory().getStackInSlot(slot).is(stack.getItem())
                ? getInventory().getStackInSlot(slot).getCount() : 0);
        return (int) Math.max(0L, Math.min(Item.ABSOLUTE_MAX_STACK_SIZE, upgrade.most(stack) - elsewhere));
    }

    /* {@code amount} for each of {@code times}, held to what a long holds. */
    private static long saturated(final long amount, final int times) {
        return amount > Long.MAX_VALUE / Math.max(1, times) ? Long.MAX_VALUE : amount * times;
    }

    /* {@code count} for each of {@code times}, held to what an int holds. */
    private static int saturatedInt(final int count, final int times) {
        return (int) Math.min(Integer.MAX_VALUE, (long) count * times);
    }

    /* The fluids {@code times} sets make. */
    private static List<FluidStack> times(final List<FluidStack> fluids, final int times) {
        if (times == 1) {
            return fluids;
        }
        final List<FluidStack> all = new ArrayList<>(fluids.size());
        for (final FluidStack fluid : fluids) {
            all.add(fluid.copyWithAmount(saturatedInt(fluid.getAmount(), times)));
        }
        return all;
    }

    /* The matter {@code times} sets make. */
    private static List<MatterStack> timesMatter(final List<MatterStack> matter, final int times) {
        if (times == 1) {
            return matter;
        }
        final List<MatterStack> all = new ArrayList<>(matter.size());
        for (final MatterStack each : matter) {
            all.add(each.withAmount(saturated(each.amount(), times)));
        }
        return all;
    }

    /* Whether {@code a} says more of what the machine is doing than {@code b}. */
    private static boolean busier(final MachineStatus a, final MachineStatus b) {
        return rank(a) > rank(b);
    }

    private static int rank(final MachineStatus status) {
        return switch (status) {
            case WORKING -> 3;
            case NO_ENERGY -> 2;
            case OUTPUT_FULL -> 1;
            default -> 0;
        };
    }

    /* Its input tanks of matter, every state's in the order of the states, as its recipes count them. */
    private List<MatterTank> matterInputs() {
        final List<MatterTank> all = new ArrayList<>();
        for (final MatterState state : MatterState.values()) {
            all.addAll(matter.inputs(state));
        }
        return all;
    }

    /* Its output tanks of matter, every state's. */
    private List<MatterTank> matterOutputs() {
        final List<MatterTank> all = new ArrayList<>();
        for (final MatterState state : MatterState.values()) {
            all.addAll(matter.outputs(state));
        }
        return all;
    }

    /*
     * Sorts what its lanes hold of each input among them, as even as it goes, the first lanes taking what is left over:
     * only when every lane that holds anything there holds the same item, and only into lanes not locked to another.
     */
    private void sortInputs() {
        for (int index = 0; index < layout.inputs(); index++) {
            ItemStack kind = ItemStack.EMPTY;
            int total = 0;
            boolean mixed = false;
            for (int lane = 0; lane < lanes.size() && !mixed; lane++) {
                final ItemStack held = getInventory().getStackInSlot(layout.laneInput(lane, index));
                if (held.isEmpty()) {
                    continue;
                }
                if (kind.isEmpty()) {
                    kind = held;
                } else if (!ItemStack.isSameItemSameComponents(kind, held)) {
                    mixed = true;
                }
                total += held.getCount();
            }
            if (mixed || kind.isEmpty()) {
                continue;
            }
            final List<Integer> open = new ArrayList<>();
            for (int lane = 0; lane < lanes.size(); lane++) {
                final int slot = layout.laneInput(lane, index);
                if (!getInventory().getStackInSlot(slot).isEmpty() || locks.allows(slot, kind)) {
                    open.add(slot);
                }
            }
            if (open.size() < 2) {
                continue;
            }
            final ItemStack sample = kind.copy();
            final int each = total / open.size();
            int extra = total % open.size();
            for (final int slot : open) {
                final int count = Math.min(sample.getMaxStackSize(), each + (extra > 0 ? 1 : 0));
                if (extra > 0) {
                    extra--;
                }
                final ItemStack now = getInventory().getStackInSlot(slot);
                if (now.getCount() != count || count > 0 && now.isEmpty()) {
                    getInventory().setStackInSlot(slot, count <= 0 ? ItemStack.EMPTY : sample.copyWithCount(count));
                }
            }
        }
    }

    /*
     * How full the slots from {@code first} up to {@code end} and {@code tanks} are, from empty to full: the mean of
     * how full each is, so a tank of thousands of millibuckets counts no more than a slot.
     */
    private double share(final int first, final int end, final List<FieldFluidTank> tanks) {
        double sum = 0.0;
        int count = 0;
        for (int slot = first; slot < end; slot++) {
            final ItemStack stack = getInventory().getStackInSlot(slot);
            sum += stack.isEmpty() ? 0.0 : stack.getCount()
                    / (double) Math.max(1, Math.min(getInventory().getSlotLimit(slot), stack.getMaxStackSize()));
            count++;
        }
        for (final FieldFluidTank tank : tanks) {
            sum += tank.getCapacity() <= 0 ? 0.0 : tank.getFluidAmount() / (double) tank.getCapacity();
            count++;
        }
        return count == 0 ? 0.0 : Math.min(1.0, sum / count);
    }

    /* One lane of the machine: its own inputs and outputs, its own progress, its own payment. */
    private final class Lane {

        private final int index;
        private final IntField progress;
        private final IntField maxProgress;
        private final LongField paid;
        private final IntField speed;
        private final PartialWork payment = new PartialWork();
        /* The work in hand last tick; empty after a load, when the saved progress is taken as belonging to it. */
        private @Nullable Processing lastWork;

        private Lane(final int index) {
            this.index = index;
            final String suffix = index == 0 ? "" : String.valueOf(index);
            this.progress = fields().integer("Progress" + suffix, 0).save().toMenu();
            this.maxProgress = fields().integer("MaxProgress" + suffix, 0).toMenu();
            this.paid = fields().longInteger("Paid" + suffix, 0L).save();
            // Read on the server, where what a machine says to whoever looks at it is written.
            this.speed = fields().integer("Speed" + suffix, PartialWork.PERCENT);
        }

        /*
         * One tick of the lane: nothing to work, or no room for everything it makes at once, sets its progress back;
         * with no energy it waits, keeping it; otherwise it spends the tick's energy and moves on, slower in
         * proportion when short of energy and when the machine is damaged.
         */
        private MachineStatus work(final ServerLevel level, final UpgradeEffect effect, final double share) {
            final ProcessingInput input = input(this.index);
            final Optional<Processing> found = input.isEmpty() ? Optional.empty() : process(level, input);
            final int held = found.map(each -> setsHeld(each, effect)).orElse(0);
            if (held <= 0) {
                reset();
                return MachineStatus.WAITING;
            }
            final Processing work = found.get();
            final int times = timesThatFit(work, effect, held);
            if (times <= 0) {
                reset();
                return MachineStatus.OUTPUT_FULL;
            }
            if (this.lastWork != null && !this.lastWork.sameWork(work)) {
                // The inputs went from one recipe to another with no idle tick between; the old energy was spent
                // on other work.
                this.progress.set(0);
            }
            this.lastWork = work;
            this.maxProgress.set((int) Math.min(Integer.MAX_VALUE, Math.ceil(effect.ticks(work.ticks()) / share)));
            // Several recipes at once spend the energy of each, a tick.
            final long perTick = saturated(effect.energyPerTick(work.energyPerTick() > 0 ? work.energyPerTick()
                    : defaultEnergyPerTick), times);
            // Short of energy, it spends what there is and works slower in the same proportion.
            this.payment.restore(this.paid.get());
            final long spent = perTick <= 0 ? 0L : power().spend(perTick);
            drew(spent);
            final int steps = this.payment.pay(spent, perTick);
            this.paid.set(this.payment.paid());
            this.speed.set(this.payment.lastPercent());
            if (perTick > 0 && spent <= 0) {
                return MachineStatus.NO_ENERGY;
            }
            if (steps > 0) {
                this.progress.add(steps);
                if (this.progress.get() >= this.maxProgress.get()) {
                    final int made = finish(work, times, effect, level.getRandom());
                    if (made >= 0) {
                        this.progress.set(0);
                        this.lastWork = null;
                        control().finishedOne();
                        madeThings(Math.max(1, made), level.getGameTime());
                    }
                }
            }
            return MachineStatus.WORKING;
        }

        /*
         * How many sets of the work the inputs hold, at most the recipes the machine works at once: each input slot's
         * count, and each tank's fluid and matter as the upgrades spend them.
         */
        private int setsHeld(final Processing work, final UpgradeEffect effect) {
            int sets = effect.recipesAtOnce();
            for (final Take take : work.takes()) {
                sets = Math.min(sets, getInventory().getStackInSlot(layout.laneInput(this.index, take.index()))
                        .getCount() / Math.max(1, take.amount()));
            }
            for (final Take take : work.fluidTakes()) {
                final long spent = effect.spent(take.amount());
                if (spent > 0) {
                    sets = (int) Math.min(sets, inputTanks.get(take.index()).getFluidAmount() / spent);
                }
            }
            final List<MatterTank> matterIn = matterInputs();
            for (final Take take : work.matterTakes()) {
                final long spent = effect.spent(take.amount());
                if (spent > 0) {
                    sets = (int) Math.min(sets, matterIn.get(take.index()).held().map(MatterStack::amount)
                            .orElse(0L) / spent);
                }
            }
            return sets;
        }

        /* The most of {@code held} sets whose outputs, every byproduct and the upgrades' yield counted, find room. */
        private int timesThatFit(final Processing work, final UpgradeEffect effect, final int held) {
            final ProcessingOutputs outputs = outputs();
            for (int times = held; times > 0; times--) {
                if (outputs.fit(atMost(work, effect, times), times(work.fluidOutputs(), times),
                        timesMatter(work.matterOutputs(), times))) {
                    return times;
                }
            }
            return 0;
        }

        /* The most items {@code times} sets can make: their outputs with the yield, every byproduct as if each came. */
        private List<ItemStack> atMost(final Processing work, final UpgradeEffect effect, final int times) {
            final List<ItemStack> all = new ArrayList<>();
            for (final ItemStack output : work.outputs()) {
                all.add(output.copyWithCount(saturatedInt(effect.mostMade(output.getCount()), times)));
            }
            for (final Byproduct byproduct : work.byproducts()) {
                all.add(byproduct.stack().copyWithCount(saturatedInt(byproduct.stack().getCount(), times)));
            }
            return all;
        }

        private void reset() {
            this.progress.set(0);
            this.paid.set(0L);
            this.speed.set(PartialWork.PERCENT);
            this.lastWork = null;
        }

        /* Where this lane's work goes. */
        private ProcessingOutputs outputs() {
            return new ProcessingOutputs(getInventory(), layout.laneOutput(this.index, 0),
                    layout.laneOutput(this.index, 0) + layout.outputs(), outputTanks, matterOutputs());
        }

        /*
         * Takes the inputs of {@code times} sets and puts out what they made, the upgrades' yield and the byproducts
         * that came with them; how many items it made, or -1, leaving it all, when another lane took first what the
         * shared tanks held for it.
         */
        private int finish(final Processing work, final int times, final UpgradeEffect effect,
                           final RandomSource random) {
            final List<MatterTank> matterIn = matterInputs();
            for (final Take take : work.fluidTakes()) {
                if (inputTanks.get(take.index()).getFluidAmount() < saturated(effect.spent(take.amount()), times)) {
                    return -1;
                }
            }
            for (final Take take : work.matterTakes()) {
                if (matterIn.get(take.index()).held().map(MatterStack::amount).orElse(0L)
                        < saturated(effect.spent(take.amount()), times)) {
                    return -1;
                }
            }
            for (final Take take : work.takes()) {
                getInventory().extractItem(layout.laneInput(this.index, take.index()),
                        saturatedInt(take.amount(), times), false);
            }
            for (final Take take : work.fluidTakes()) {
                inputTanks.get(take.index()).drain((int) Math.min(Integer.MAX_VALUE,
                        saturated(effect.spent(take.amount()), times)), IFluidHandler.FluidAction.EXECUTE);
            }
            for (final Take take : work.matterTakes()) {
                final MatterTank tank = matterIn.get(take.index());
                final long spent = saturated(effect.spent(take.amount()), times);
                tank.held().ifPresent(held -> tank.extract(held.material(), spent, false));
            }
            final List<ItemStack> made = new ArrayList<>();
            int count = 0;
            for (final ItemStack output : work.outputs()) {
                final int many = effect.made(saturatedInt(output.getCount(), times), random.nextDouble());
                if (many > 0) {
                    made.add(output.copyWithCount(many));
                    count += many;
                }
            }
            for (final Byproduct byproduct : work.byproducts()) {
                final double chance = effect.byproductChance(byproduct.chance());
                for (int each = 0; each < times; each++) {
                    if (random.nextDouble() < chance) {
                        made.add(byproduct.stack().copy());
                    }
                }
            }
            outputs().put(made, times(work.fluidOutputs(), times), timesMatter(work.matterOutputs(), times));
            return count;
        }
    }
}
