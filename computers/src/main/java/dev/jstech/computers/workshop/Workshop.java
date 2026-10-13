/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.ValueField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * What the personal-use cards keep in a computer and do with it: the crafting grid, the furnace's input and output,
 * the item on the enchanting card and the two on the anvil card, in one handler saved with the computer and dropped
 * when it is broken. The items come from the player's cursor and go back to it as a container's slots do; the
 * crafting grid, the enchanting item and the anvil's two go back to the player when the program's window closes, as
 * a table gives back what was left on it. The furnace keeps its own and goes on smelting while the computer is on.
 *
 * <p>The rules are the blocks' own: the game's crafting recipes, its smelting recipes, the three offers of an
 * enchanting table with every bookshelf around it, and the anvil's result and cost, through the game's anvil logic
 * (an anvil that is nowhere, so the card never wears). Only the price and the pace differ ({@link WorkshopRates}).
 */
public final class Workshop {

    private final FieldItemHandler slots;
    private final ValueField<Integer> progress;
    private final ValueField<Float> experience;
    private final ValueField<String> anvilName;
    /** Remembers the last smelting recipe found, so a furnace whose input stays the same does not scan them all. */
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> furnaceCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);

    /** The nine cells of the crafting grid start here, row by row. */
    public static final int GRID = 0;
    public static final int GRID_SIZE = 9;
    public static final int FURNACE_IN = 9;
    public static final int FURNACE_OUT = 10;
    public static final int ENCHANT_ITEM = 11;
    public static final int ANVIL_LEFT = 12;
    public static final int ANVIL_RIGHT = 13;
    public static final int SLOTS = 14;
    /** The longest name the anvil card gives, the anvil's own limit. */
    public static final int MAX_NAME = AnvilMenu.MAX_NAME_LENGTH;
    private static final int OFFERS = 3;

    public Workshop(final FieldItemHandler slots, final ValueField<Integer> progress,
                    final ValueField<Float> experience, final ValueField<String> anvilName) {
        this.slots = slots;
        this.progress = progress;
        this.experience = experience;
        this.anvilName = anvilName;
    }

    /** One of the three offers: the level a player needs, the enchantment it names as its clue, what it costs. */
    public record Offer(int required, Component clue, int levels) {
    }

    /** What the anvil card would make of its two items now, what an anvil would ask and what the card asks. */
    public record AnvilResult(ItemStack result, int anvilLevels, int levels) {
    }

    /** What the anvil made, and what is left of the two items it was given. */
    public record AnvilTake(ItemStack made, ItemStack left, ItemStack right) {
    }

    /** What one item smelts into, the ticks a furnace takes over it and the experience it earns. */
    public record Smelt(ItemStack result, int cookTicks, float experience) {
    }

    /** The item in slot {@code slot}. */
    public ItemStack slot(final int slot) {
        return slots.getStackInSlot(slot);
    }

    /** Every slot, in order. */
    public List<ItemStack> contents() {
        final List<ItemStack> out = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            out.add(slots.getStackInSlot(i).copy());
        }
        return out;
    }

    /** Puts {@code stack} in slot {@code slot} as it is, for a test or a restore. */
    public void put(final int slot, final ItemStack stack) {
        slots.setStackInSlot(slot, stack);
    }

    /** Puts as much of {@code stack} in slot {@code slot} as fits there now, and gives back the rest. */
    public ItemStack offer(final int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack here = slots.getStackInSlot(slot);
        if (!here.isEmpty() && !ItemStack.isSameItemSameComponents(here, stack)) {
            return stack;
        }
        final int room = Math.min(limit(slot), stack.getMaxStackSize()) - here.getCount();
        final int put = Math.max(0, Math.min(room, stack.getCount()));
        if (put == 0) {
            return stack;
        }
        slots.setStackInSlot(slot, here.isEmpty() ? stack.copyWithCount(put) : here.copyWithCount(here.getCount()
                + put));
        return stack.getCount() == put ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - put);
    }

    /** The name the anvil card gives the item on it. */
    public String anvilName() {
        final String name = anvilName.get();
        return name == null ? "" : name;
    }

    public void setAnvilName(final String name) {
        anvilName.set(name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name);
    }

    /**
     * A click on slot {@code slot} with the menu's cursor, as a container slot takes one: the left button picks up,
     * puts down, merges or swaps the whole stack, the right one picks up half or puts down one. The furnace's output
     * is only taken from, and pays out the experience its smelting earned.
     */
    public void click(final ServerPlayer player, final AbstractContainerMenu menu, final int slot, final int button) {
        if (slot < 0 || slot >= SLOTS) {
            return;
        }
        final ItemStack held = menu.getCarried();
        final ItemStack here = slots.getStackInSlot(slot);
        if (slot == FURNACE_OUT) {
            takeOutput(player, menu, held, here);
            return;
        }
        if (held.isEmpty()) {
            if (here.isEmpty()) {
                return;
            }
            final int take = button == 1 ? (here.getCount() + 1) / 2 : here.getCount();
            menu.setCarried(here.split(take));
            slots.setStackInSlot(slot, here);
        } else if (accepts(player.serverLevel(), slot, held)) {
            final int room = Math.min(limit(slot), held.getMaxStackSize());
            if (here.isEmpty() || ItemStack.isSameItemSameComponents(here, held)) {
                final int put = Math.min(button == 1 ? 1 : held.getCount(), room - here.getCount());
                if (put > 0) {
                    final ItemStack next = here.isEmpty() ? held.copyWithCount(put) : here.copyWithCount(
                            here.getCount() + put);
                    held.shrink(put);
                    menu.setCarried(held);
                    slots.setStackInSlot(slot, next);
                }
            } else if (held.getCount() <= room) {
                menu.setCarried(here.copy());
                slots.setStackInSlot(slot, held.copy());
            }
            if (slot == ANVIL_LEFT && anvilName().isEmpty()) {
                setAnvilName(slots.getStackInSlot(ANVIL_LEFT).getHoverName().getString());
            }
        }
        if (slot == ANVIL_LEFT && slots.getStackInSlot(ANVIL_LEFT).isEmpty()) {
            setAnvilName("");
        }
        if (slot == FURNACE_IN) {
            progress.set(0);
        }
    }

    /** Whether slot {@code slot} takes {@code stack}: the furnace what smelts, the enchanting card what takes it. */
    public boolean accepts(final Level level, final int slot, final ItemStack stack) {
        return switch (slot) {
            case FURNACE_IN -> furnaceRecipe(level, stack) != null;
            case FURNACE_OUT -> false;
            case ENCHANT_ITEM -> stack.isEnchantable() || stack.is(Items.BOOK);
            default -> true;
        };
    }

    /** Gives the grid, the enchanting item and the anvil's two back to {@code player}, as a closed table does. */
    public void giveBack(final Player player) {
        for (int i = GRID; i < GRID + GRID_SIZE; i++) {
            giveBackSlot(player, i);
        }
        giveBackSlot(player, ENCHANT_ITEM);
        giveBackSlot(player, ANVIL_LEFT);
        giveBackSlot(player, ANVIL_RIGHT);
        setAnvilName("");
    }

    /** Empties the crafting grid into {@code player}'s inventory. */
    public void clearGrid(final Player player) {
        for (int i = GRID; i < GRID + GRID_SIZE; i++) {
            giveBackSlot(player, i);
        }
    }

    // crafting

    /** The recipe the grid makes now, or null. */
    @Nullable
    public RecipeHolder<CraftingRecipe> recipe(final Level level) {
        final CraftingInput input = CraftingInput.of(3, 3, grid());
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
    }

    /** What the grid makes now, or an empty stack. */
    public ItemStack craftResult(final Level level) {
        final RecipeHolder<CraftingRecipe> recipe = recipe(level);
        return recipe == null ? ItemStack.EMPTY
                : recipe.value().assemble(CraftingInput.of(3, 3, grid()), level.registryAccess());
    }

    /**
     * How many more times the grid could be filled again from {@code player}'s inventory with what it holds now:
     * for each kind of item the grid uses, what the inventory has of it over what one filling takes.
     */
    public int moreFromInventory(final Player player) {
        final List<ItemStack> grid = grid();
        int most = Integer.MAX_VALUE;
        boolean any = false;
        for (final ItemStack cell : grid) {
            if (cell.isEmpty()) {
                continue;
            }
            any = true;
            final int need = needed(grid, cell);
            most = Math.min(most, count(player, cell) / need);
        }
        return any ? most : 0;
    }

    /**
     * Crafts once, or with {@code all} as many times as the grid and the player's inventory allow, refilling the grid
     * from the inventory between crafts. What is made goes to the player's inventory, or at their feet when it is
     * full; containers a recipe leaves behind stay in the grid or go to the player. Returns how many crafts were made.
     */
    public int craft(final ServerPlayer player, final boolean all) {
        final Level level = player.level();
        int made = 0;
        /*
         * No run can need more crafts than the fullest cell holds plus the refills the inventory can make. The bound
         * stops a recipe whose leftover is its own ingredient, which puts the item straight back into the cell, so
         * the grid never empties and the refill never has anything to fetch.
         */
        final int limit = all ? mostInCell() + moreFromInventory(player) + 1 : 1;
        while (made < limit) {
            final RecipeHolder<CraftingRecipe> recipe = recipe(level);
            if (recipe == null) {
                break;
            }
            final List<ItemStack> pattern = grid();
            final CraftingInput.Positioned positioned = CraftingInput.ofPositioned(3, 3, pattern);
            final ItemStack result = recipe.value().assemble(positioned.input(), level.registryAccess());
            if (result.isEmpty()) {
                break;
            }
            final NonNullList<ItemStack> left = recipe.value().getRemainingItems(positioned.input());
            consumeGrid(player, positioned, left);
            result.onCraftedBy(level, player, result.getCount());
            player.awardStat(Stats.ITEM_CRAFTED.get(result.getItem()), result.getCount());
            give(player, result);
            made++;
            if (!all || !refill(player, pattern)) {
                break;
            }
        }
        return made;
    }

    // smelting

    /**
     * One tick of the furnace at {@code speed} times a furnace's pace: the input smelts into the output while there is
     * room for it. Returns true on the tick the last of the input is done.
     */
    public boolean tickFurnace(final ServerLevel level, final int speed) {
        final ItemStack input = slots.getStackInSlot(FURNACE_IN);
        final RecipeHolder<? extends AbstractCookingRecipe> recipe = input.isEmpty() ? null
                : furnaceRecipe(level, input);
        if (recipe == null || speed <= 0) {
            if (progressTicks() != 0) {
                progress.set(0);
            }
            return false;
        }
        final ItemStack made = recipe.value().assemble(new SingleRecipeInput(input), level.registryAccess());
        final ItemStack output = slots.getStackInSlot(FURNACE_OUT);
        if (!hasRoomFor(output, made)) {
            return false;
        }
        final int needed = WorkshopRates.ticksPerItem(recipe.value().getCookingTime(), speed);
        final int now = progressTicks() + 1;
        if (now < needed) {
            progress.set(now);
            return false;
        }
        progress.set(0);
        input.shrink(1);
        slots.setStackInSlot(FURNACE_IN, input);
        slots.setStackInSlot(FURNACE_OUT, output.isEmpty() ? made.copy() : output.copyWithCount(
                output.getCount() + made.getCount()));
        experience.set(experienceEarned() + recipe.value().getExperience());
        return input.isEmpty();
    }

    /** How far the item now in the furnace is, in ticks. */
    public int progressTicks() {
        return ticksOf(progress);
    }

    /** The ticks one item of the furnace's input takes at {@code speed}, or 0 with nothing that smelts in it. */
    public int ticksPerItem(final Level level, final int speed) {
        final ItemStack input = slots.getStackInSlot(FURNACE_IN);
        final RecipeHolder<? extends AbstractCookingRecipe> recipe = input.isEmpty() ? null
                : furnaceRecipe(level, input);
        return recipe == null ? 0 : WorkshopRates.ticksPerItem(recipe.value().getCookingTime(), speed);
    }

    /** What the furnace's input smelts into, or an empty stack. */
    public ItemStack smeltsInto(final Level level) {
        final ItemStack input = slots.getStackInSlot(FURNACE_IN);
        final RecipeHolder<? extends AbstractCookingRecipe> recipe = input.isEmpty() ? null
                : furnaceRecipe(level, input);
        return recipe == null ? ItemStack.EMPTY
                : recipe.value().assemble(new SingleRecipeInput(input), level.registryAccess());
    }

    /** The experience the furnace's smelting earned and nobody has taken yet. */
    public float experienceEarned() {
        final Float earned = experience.get();
        return earned == null ? 0f : earned;
    }

    /**
     * Adds to the experience the furnace has earned, which goes to whoever next takes its output, as a furnace keeps
     * what a hopper's smelting earned for the next player.
     */
    public void addExperience(final float earned) {
        experience.set(experienceEarned() + earned);
    }

    /** What {@code input} smelts into, the ticks a furnace takes and what it earns; null for what does not smelt. */
    @Nullable
    public static Smelt smelt(final Level level, final ItemStack input) {
        return smeltOf(level, input, input.isEmpty() ? null : smelting(level, input));
    }

    /**
     * Like {@link #smelt}, but finds the recipe through this card's cache, for the network's smelting that runs on
     * every tick while a card holds the same item.
     */
    @Nullable
    public Smelt smeltCached(final Level level, final ItemStack input) {
        return smeltOf(level, input, input.isEmpty() ? null : furnaceRecipe(level, input));
    }

    /**
     * Whether {@code made} fits on {@code output}: it is the same item and the stack has room, or the output is empty.
     * The one rule the furnace and the network's smelting share.
     */
    public static boolean hasRoomFor(final ItemStack output, final ItemStack made) {
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, made)
                && output.getCount() + made.getCount() <= output.getMaxStackSize();
    }

    /** The ticks a smelting progress field holds, 0 when it holds nothing yet. */
    public static int ticksOf(final ValueField<Integer> progress) {
        final Integer ticks = progress.get();
        return ticks == null ? 0 : ticks;
    }

    // enchanting

    /**
     * The three offers for the item on the card, as a table with every bookshelf around it would make them for
     * {@code player}: none for an item that takes no enchantment.
     */
    public List<Offer> offers(final ServerPlayer player) {
        return offersFor(player, slots.getStackInSlot(ENCHANT_ITEM));
    }

    /**
     * The three offers for {@code item}, as a table with every bookshelf around it would make them for
     * {@code player}: none for an item that takes no enchantment.
     */
    public static List<Offer> offersFor(final ServerPlayer player, final ItemStack item) {
        if (item.isEmpty() || !item.isEnchantable()) {
            return List.of();
        }
        final RandomSource random = RandomSource.create();
        final int[] costs = costs(player, item, random);
        final List<Offer> offers = new ArrayList<>(OFFERS);
        for (int i = 0; i < OFFERS; i++) {
            if (costs[i] <= 0) {
                offers.add(new Offer(0, Component.empty(), WorkshopRates.enchantLevels(i)));
                continue;
            }
            final List<EnchantmentInstance> list = enchantments(player, item, i, costs[i], random);
            offers.add(new Offer(costs[i], clueOf(list, random), WorkshopRates.enchantLevels(i)));
        }
        return offers;
    }

    /**
     * Enchants the item on the card with offer {@code offer} for {@code player}: the levels the card asks, no lapis.
     * False when the offer is not there or the player cannot pay it.
     */
    public boolean enchant(final ServerPlayer player, final int offer) {
        final ItemStack enchanted = enchanted(player, slots.getStackInSlot(ENCHANT_ITEM), offer);
        if (enchanted.isEmpty()) {
            return false;
        }
        slots.setStackInSlot(ENCHANT_ITEM, enchanted);
        return true;
    }

    /**
     * {@code item} enchanted with offer {@code offer} for {@code player}, who pays the levels the card asks and no
     * lapis; an empty stack, with nothing paid, when the offer is not there or the player cannot pay it.
     */
    public static ItemStack enchanted(final ServerPlayer player, final ItemStack item, final int offer) {
        if (offer < 0 || offer >= OFFERS || item.isEmpty() || !item.isEnchantable()) {
            return ItemStack.EMPTY;
        }
        final RandomSource random = RandomSource.create();
        final int[] costs = costs(player, item, random);
        final int levels = WorkshopRates.enchantLevels(offer);
        if (costs[offer] <= 0 || !player.getAbilities().instabuild
                && (player.experienceLevel < levels || player.experienceLevel < costs[offer])) {
            return ItemStack.EMPTY;
        }
        final List<EnchantmentInstance> list = enchantments(player, item, offer, costs[offer], random);
        if (list.isEmpty()) {
            return ItemStack.EMPTY;
        }
        player.onEnchantmentPerformed(item, levels);
        final ItemStack enchanted = item.getItem().applyEnchantments(item, list);
        player.awardStat(Stats.ENCHANT_ITEM);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS,
                1.0F, player.getRandom().nextFloat() * 0.1F + 0.9F);
        return enchanted;
    }

    // the anvil

    /** What the two items on the anvil card make now, with the name it gives, and the levels it asks. */
    public AnvilResult anvil(final ServerPlayer player) {
        return anvilFor(player, slots.getStackInSlot(ANVIL_LEFT), slots.getStackInSlot(ANVIL_RIGHT), anvilName());
    }

    /** What an anvil would make of {@code left} and {@code right} with {@code name}, and what the card asks. */
    public static AnvilResult anvilFor(final ServerPlayer player, final ItemStack left, final ItemStack right,
                                       final String name) {
        final AnvilMenu menu = anvilMenu(player, left, right, name);
        final ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem().copy();
        final int cost = menu.getCost();
        return new AnvilResult(result, cost, WorkshopRates.anvilLevels(cost));
    }

    /**
     * Takes what the anvil card makes into {@code player}'s inventory and charges the card's levels: the anvil's own
     * logic consumes the two items exactly as an anvil does. False when there is nothing to take or the player cannot
     * pay.
     */
    public boolean takeAnvil(final ServerPlayer player) {
        final AnvilTake take = takeAnvilFor(player, slots.getStackInSlot(ANVIL_LEFT),
                slots.getStackInSlot(ANVIL_RIGHT), anvilName());
        if (take == null) {
            return false;
        }
        slots.setStackInSlot(ANVIL_LEFT, take.left());
        slots.setStackInSlot(ANVIL_RIGHT, take.right());
        setAnvilName("");
        give(player, take.made());
        return true;
    }

    /**
     * What an anvil makes of {@code left} and {@code right} with {@code name}, {@code player} paying the card's
     * levels, and what is left of the two; null, with nothing paid, when it makes nothing or the player cannot pay.
     */
    @Nullable
    public static AnvilTake takeAnvilFor(final ServerPlayer player, final ItemStack left, final ItemStack right,
                                         final String name) {
        final AnvilMenu menu = anvilMenu(player, left, right, name);
        final ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        final int cost = menu.getCost();
        final int levels = WorkshopRates.anvilLevels(cost);
        final boolean free = player.getAbilities().instabuild;
        if (result.isEmpty() || cost <= 0 || !free && player.experienceLevel < levels) {
            return null;
        }
        final ItemStack made = result.copy();
        // The anvil's take charges its full cost: the levels it asks over the card's are handed over first.
        if (!free) {
            player.giveExperienceLevels(cost - levels);
        }
        menu.getSlot(AnvilMenu.RESULT_SLOT).onTake(player, made);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F,
                player.getRandom().nextFloat() * 0.1F + 0.9F);
        return new AnvilTake(made, menu.getSlot(AnvilMenu.INPUT_SLOT).getItem().copy(),
                menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem().copy());
    }

    /* The game's anvil over two items and a name, at no place, so nothing it does can wear a block. */
    private static AnvilMenu anvilMenu(final ServerPlayer player, final ItemStack left, final ItemStack right,
                                       final String name) {
        final AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(left.copy());
        menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(right.copy());
        menu.setItemName(name);
        menu.createResult();
        return menu;
    }

    private void takeOutput(final ServerPlayer player, final AbstractContainerMenu menu, final ItemStack held,
                            final ItemStack here) {
        if (here.isEmpty()) {
            return;
        }
        if (held.isEmpty()) {
            menu.setCarried(here.copy());
        } else if (ItemStack.isSameItemSameComponents(held, here)
                && held.getCount() + here.getCount() <= held.getMaxStackSize()) {
            held.grow(here.getCount());
            menu.setCarried(held);
        } else {
            return;
        }
        slots.setStackInSlot(FURNACE_OUT, ItemStack.EMPTY);
        payExperience(player);
    }

    /* The experience the smelting earned, as orbs at the player, the fraction left by chance as a furnace does. */
    private void payExperience(final ServerPlayer player) {
        final float earned = experienceEarned();
        experience.set(0f);
        int whole = Mth.floor(earned);
        if (player.getRandom().nextFloat() < Mth.frac(earned)) {
            whole++;
        }
        if (whole > 0) {
            ExperienceOrb.award(player.serverLevel(), player.position(), whole);
        }
    }

    private int limit(final int slot) {
        return slot == ENCHANT_ITEM ? 1 : 64;
    }

    private void giveBackSlot(final Player player, final int slot) {
        final ItemStack stack = slots.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            slots.setStackInSlot(slot, ItemStack.EMPTY);
            give(player, stack);
        }
    }

    private static void give(final Player player, final ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private int mostInCell() {
        int most = 0;
        for (int i = GRID; i < GRID + GRID_SIZE; i++) {
            most = Math.max(most, slots.getStackInSlot(i).getCount());
        }
        return most;
    }

    private List<ItemStack> grid() {
        final List<ItemStack> grid = new ArrayList<>(GRID_SIZE);
        for (int i = GRID; i < GRID + GRID_SIZE; i++) {
            grid.add(slots.getStackInSlot(i).copy());
        }
        return grid;
    }

    /* One of each used cell is spent; a container the recipe leaves goes back into its cell, or to the player. */
    private void consumeGrid(final Player player, final CraftingInput.Positioned positioned,
                             final NonNullList<ItemStack> left) {
        final CraftingInput input = positioned.input();
        for (int row = 0; row < input.height(); row++) {
            for (int col = 0; col < input.width(); col++) {
                final int cell = GRID + col + positioned.left() + (row + positioned.top()) * 3;
                final ItemStack here = slots.getStackInSlot(cell);
                if (!here.isEmpty()) {
                    here.shrink(1);
                    slots.setStackInSlot(cell, here);
                }
                final ItemStack rest = left.get(col + row * input.width());
                if (rest.isEmpty()) {
                    continue;
                }
                final ItemStack now = slots.getStackInSlot(cell);
                if (now.isEmpty()) {
                    slots.setStackInSlot(cell, rest);
                } else if (ItemStack.isSameItemSameComponents(now, rest)) {
                    slots.setStackInSlot(cell, now.copyWithCount(now.getCount() + rest.getCount()));
                } else {
                    give(player, rest);
                }
            }
        }
    }

    /* Fills the cells the last craft emptied from the player's inventory with the same items; false if it cannot. */
    private boolean refill(final Player player, final List<ItemStack> pattern) {
        for (int i = 0; i < GRID_SIZE; i++) {
            final ItemStack wanted = pattern.get(i);
            if (wanted.isEmpty() || !slots.getStackInSlot(GRID + i).isEmpty()) {
                continue;
            }
            final ItemStack taken = takeFrom(player, wanted);
            if (taken.isEmpty()) {
                return false;
            }
            slots.setStackInSlot(GRID + i, taken);
        }
        return true;
    }

    private static ItemStack takeFrom(final Player player, final ItemStack like) {
        final var inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            final ItemStack stack = inventory.items.get(i);
            if (ItemStack.isSameItemSameComponents(stack, like)) {
                final ItemStack one = stack.split(1);
                inventory.setChanged();
                return one;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int needed(final List<ItemStack> grid, final ItemStack like) {
        int need = 0;
        for (final ItemStack cell : grid) {
            if (ItemStack.isSameItemSameComponents(cell, like)) {
                need++;
            }
        }
        return Math.max(1, need);
    }

    private static int count(final Player player, final ItemStack like) {
        int have = 0;
        for (final ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(stack, like)) {
                have += stack.getCount();
            }
        }
        return have;
    }

    @Nullable
    private static Smelt smeltOf(final Level level, final ItemStack input,
                                 @Nullable final RecipeHolder<? extends AbstractCookingRecipe> recipe) {
        return recipe == null ? null : new Smelt(recipe.value().assemble(new SingleRecipeInput(input),
                level.registryAccess()), recipe.value().getCookingTime(), recipe.value().getExperience());
    }

    @Nullable
    private RecipeHolder<SmeltingRecipe> furnaceRecipe(final Level level, final ItemStack stack) {
        return furnaceCheck.getRecipeFor(new SingleRecipeInput(stack), level).orElse(null);
    }

    @Nullable
    private static RecipeHolder<? extends AbstractCookingRecipe> smelting(final Level level, final ItemStack stack) {
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
                .orElse(null);
    }

    /* The three levels a table with every bookshelf asks for the item, from the player's enchanting seed. */
    private static int[] costs(final Player player, final ItemStack item, final RandomSource random) {
        random.setSeed(player.getEnchantmentSeed());
        final int[] costs = new int[OFFERS];
        for (int i = 0; i < OFFERS; i++) {
            costs[i] = EnchantmentHelper.getEnchantmentCost(random, i, WorkshopRates.BOOKSHELVES, item);
            if (costs[i] < i + 1) {
                costs[i] = 0;
            }
        }
        return costs;
    }

    /* What offer {@code offer} puts on the item, drawn as a table draws it from the player's seed. */
    private static List<EnchantmentInstance> enchantments(final Player player, final ItemStack item, final int offer,
                                                          final int cost, final RandomSource random) {
        random.setSeed(player.getEnchantmentSeed() + offer);
        final Optional<HolderSet.Named<Enchantment>> table = player.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (table.isEmpty()) {
            return List.of();
        }
        final List<EnchantmentInstance> list = new ArrayList<>(EnchantmentHelper.selectEnchantment(random, item, cost,
                table.get().stream()));
        if (item.is(Items.BOOK) && list.size() > 1) {
            list.remove(random.nextInt(list.size()));
        }
        return list;
    }

    /* The clue a table shows for an offer: one of its enchantments, picked by the same draw a table makes. */
    private static Component clueOf(final List<EnchantmentInstance> list, final RandomSource random) {
        if (list.isEmpty()) {
            return Component.empty();
        }
        final EnchantmentInstance shown = list.get(random.nextInt(list.size()));
        final Holder<Enchantment> enchantment = shown.enchantment;
        return Enchantment.getFullname(enchantment, shown.level);
    }
}
