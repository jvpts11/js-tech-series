/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The working part of a {@link TestMachineBlock}: its input slots, an output slot and a by-product slot, and a recipe
 * made every few ticks while its inputs are there and its outputs have room. A kiln takes its one input through any
 * face; a mixer takes its first input only through its west face and its second only through its north face, as a
 * real machine with sided inputs does. Outputs leave through any face; inputs never do.
 */
public final class TestMachineBlockEntity extends BlockEntity {

    private final ItemStackHandler inventory = new ItemStackHandler(MOST_SLOTS) {
        @Override
        protected void onContentsChanged(final int slot) {
            setChanged();
        }
    };
    private int ticksPerItem = DEFAULT_TICKS;
    private boolean held;
    private int progress;
    private long made;
    /* Where it pushes what it makes, as a machine that ejects into a chest beside it does; null keeps it. */
    @Nullable
    private Direction ejectTo;

    /** How many ticks one recipe takes unless a test sets another pace. */
    public static final int DEFAULT_TICKS = 4;
    private static final int MOST_SLOTS = 4;

    public TestMachineBlockEntity(final BlockPos pos, final BlockState state) {
        super(TestMachines.MACHINE.get(), pos, state);
    }

    /** What the machine makes each recipe from, and through which faces each input goes in. */
    public enum Kind {
        KILN(1, List.of(
                new Recipe(List.of(Items.COBBLESTONE), new ItemStack(Items.STONE), ItemStack.EMPTY),
                new Recipe(List.of(Items.RAW_IRON), new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY),
                // A second way to the same ingot, for two recipes with one output.
                new Recipe(List.of(Items.IRON_ORE), new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY),
                new Recipe(List.of(Items.RAW_COPPER), new ItemStack(Items.COPPER_INGOT), ItemStack.EMPTY),
                new Recipe(List.of(Items.SAND), new ItemStack(Items.GLASS), ItemStack.EMPTY),
                new Recipe(List.of(Items.OAK_LOG), new ItemStack(Items.CHARCOAL), ItemStack.EMPTY),
                new Recipe(List.of(Items.CLAY_BALL), new ItemStack(Items.BRICK), ItemStack.EMPTY),
                // A recipe with a by-product no pattern declares, for the unexpected outputs.
                new Recipe(List.of(Items.GRAVEL), new ItemStack(Items.FLINT), new ItemStack(Items.SAND)))),
        MIXER(2, List.of(
                new Recipe(List.of(Items.DIRT, Items.GRAVEL), new ItemStack(Items.COARSE_DIRT, 2), ItemStack.EMPTY),
                new Recipe(List.of(Items.SAND, Items.CLAY_BALL), new ItemStack(Items.TERRACOTTA, 2),
                        ItemStack.EMPTY),
                // The bone meal comes back out, as a catalyst does: an output that is also an input.
                new Recipe(List.of(Items.DIRT, Items.BONE_MEAL), new ItemStack(Items.GRASS_BLOCK),
                        new ItemStack(Items.BONE_MEAL))));

        private final int inputs;
        private final List<Recipe> recipes;

        Kind(final int inputs, final List<Recipe> recipes) {
            this.inputs = inputs;
            this.recipes = recipes;
        }

        /** How many input slots it has. */
        public int inputs() {
            return inputs;
        }

        /** The input slot an item put in through {@code side} goes to, or -1 when that face takes none. */
        public int slotFor(@Nullable final Direction side) {
            if (this == KILN) {
                return 0;
            }
            return side == Direction.WEST ? 0 : side == Direction.NORTH ? 1 : -1;
        }

        /** Whether {@code item} is a recipe's input in slot {@code slot}. */
        public boolean takes(final int slot, final Item item) {
            for (final Recipe recipe : recipes) {
                if (slot < recipe.inputs().size() && recipe.inputs().get(slot) == item) {
                    return true;
                }
            }
            return false;
        }

        @Nullable
        private Recipe match(final ItemStackHandler inventory) {
            for (final Recipe recipe : recipes) {
                boolean all = true;
                for (int i = 0; i < recipe.inputs().size(); i++) {
                    all &= inventory.getStackInSlot(i).is(recipe.inputs().get(i));
                }
                if (all) {
                    return recipe;
                }
            }
            return null;
        }
    }

    /** One recipe: an item of each input, made into an output and, for some, a by-product. */
    public record Recipe(List<Item> inputs, ItemStack output, ItemStack byProduct) {
    }

    /** The machine as seen through {@code side}: what goes in there, and its outputs. */
    public IItemHandler handler(@Nullable final Direction side) {
        return new Face(side);
    }

    /** What it makes and where its inputs go in. */
    public Kind kind() {
        return getBlockState().getBlock() instanceof TestMachineBlock block ? block.kind() : Kind.KILN;
    }

    /** What sits in input slot {@code index}. */
    public ItemStack input(final int index) {
        return inventory.getStackInSlot(index);
    }

    /** What sits in the output slot. */
    public ItemStack output() {
        return inventory.getStackInSlot(outputSlot());
    }

    /** What sits in the by-product slot. */
    public ItemStack byProduct() {
        return inventory.getStackInSlot(outputSlot() + 1);
    }

    /** Puts {@code stack} straight into the output slot, as if the machine had made it before anyone watched. */
    public void preload(final ItemStack stack) {
        inventory.setStackInSlot(outputSlot(), stack);
    }

    /** How many recipes it has made. */
    public long made() {
        return made;
    }

    /** Sets how many ticks one recipe takes, at least one. */
    public void setTicksPerItem(final int ticks) {
        this.ticksPerItem = Math.max(1, ticks);
        setChanged();
    }

    /** Holds the machine still, as one out of power is, or lets it work again. */
    public void setHeld(final boolean held) {
        this.held = held;
        setChanged();
    }

    /** Pushes what it makes into the block on {@code side} every tick, or keeps it with null. */
    public void setEjectTo(@Nullable final Direction side) {
        this.ejectTo = side;
        setChanged();
    }

    /** One server tick: what it made goes out where it ejects, and a recipe's worth of progress is made. */
    public void serverTick() {
        ejectAll();
        if (held) {
            return;
        }
        final Recipe recipe = kind().match(inventory);
        if (recipe == null || !fits(outputSlot(), recipe.output()) || !fits(outputSlot() + 1, recipe.byProduct())) {
            progress = 0;
            return;
        }
        if (++progress < ticksPerItem) {
            return;
        }
        progress = 0;
        for (int i = 0; i < recipe.inputs().size(); i++) {
            inventory.extractItem(i, 1, false);
        }
        inventory.insertItem(outputSlot(), recipe.output().copy(), false);
        if (!recipe.byProduct().isEmpty()) {
            inventory.insertItem(outputSlot() + 1, recipe.byProduct().copy(), false);
        }
        made++;
        // Out at once, in the tick it was made, so a bus beside the machine never sees it first.
        ejectAll();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("TicksPerItem", ticksPerItem);
        tag.putBoolean("Held", held);
        tag.putInt("Progress", progress);
        tag.putLong("Made", made);
        if (ejectTo != null) {
            tag.putByte("EjectTo", (byte) ejectTo.get3DDataValue());
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        ticksPerItem = tag.contains("TicksPerItem") ? Math.max(1, tag.getInt("TicksPerItem")) : DEFAULT_TICKS;
        held = tag.getBoolean("Held");
        progress = tag.getInt("Progress");
        made = tag.getLong("Made");
        ejectTo = tag.contains("EjectTo") ? Direction.from3DDataValue(tag.getByte("EjectTo")) : null;
    }

    private int outputSlot() {
        return kind().inputs();
    }

    private void ejectAll() {
        if (ejectTo != null) {
            eject(outputSlot());
            eject(outputSlot() + 1);
        }
    }

    private void eject(final int slot) {
        final ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty() || level == null || ejectTo == null) {
            return;
        }
        final IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK,
                worldPosition.relative(ejectTo), ejectTo.getOpposite());
        if (target == null) {
            return;
        }
        final ItemStack left = ItemHandlerHelper.insertItem(target, stack.copy(), false);
        inventory.setStackInSlot(slot, left);
    }

    private boolean fits(final int slot, final ItemStack stack) {
        return stack.isEmpty() || inventory.insertItem(slot, stack.copy(), true).isEmpty();
    }

    /* The machine through one face: inputs go in only where that face takes them, outputs leave anywhere. */
    private final class Face implements IItemHandler {

        @Nullable
        private final Direction side;

        private Face(@Nullable final Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return outputSlot() + 2;
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return slot < outputSlot() ? ItemStack.EMPTY : inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return slot < outputSlot() && slot == kind().slotFor(side) && kind().takes(slot, stack.getItem());
        }
    }
}
