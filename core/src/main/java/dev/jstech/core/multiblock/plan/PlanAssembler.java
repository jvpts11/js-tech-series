/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.plan;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.multiblock.BuildBalance;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A machine that lays the blocks of a planned build by itself, one at a time at the pace a builder places them
 * ({@link BuildBalance#blocksPerSecond}), from the blocks it holds: a crane over a rocket in its hangar, robot arms
 * over a ship in its dock. What it holds comes in through its {@link #inbox()}, which takes only the blocks the plan
 * still wants, and no more of each than the plan wants, from whatever puts items in: a hopper, a pipe, another mod's
 * exporter, a network. What the plan stops wanting, as a player lays a block by hand or a new plan takes the old one's
 * place, goes back out through the same inbox to whatever pulls from it. It lays the lowest missing block it holds
 * first, layer by layer, where nothing stands in the way and no protection stops it, as the world's own machine.
 *
 * <p>Its owner declares it among its fields, which saves what it holds, ticks it on the server, and tells it which
 * build to lay ({@link #build}). With nothing it can lay, it sleeps until blocks come in or the plan changes
 * ({@link #wake}), looking again once a second for a place something stood in.
 */
public final class PlanAssembler implements IFieldPart {

    private final BlockPos by;
    private final Runnable onChange;
    private final Map<Item, Integer> held = new HashMap<>();
    /* What it holds by kind, in the order the kinds came in: the inbox's places. */
    private final List<Item> kinds = new ArrayList<>();
    private final Inbox inbox = new Inbox();
    private @Nullable PlannedBuild build;
    private int credit;
    private int sleep;
    private boolean waiting;

    /** Where what it holds is saved. */
    public static final String KEY = "Held";
    private static final Codec<Map<Item, Integer>> HELD = Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(),
            Codec.intRange(1, Integer.MAX_VALUE));
    private static final int TICKS_PER_SECOND = 20;
    /* How long it sleeps with nothing it can lay before looking again, for a place something stood in. */
    private static final int SLEEP = TICKS_PER_SECOND;

    /**
     * An assembler for the machine at {@code by}, which places its blocks as the world's own machine there; it runs
     * {@code onChange} each time what it holds changes, for its owner to save.
     */
    public PlanAssembler(final BlockPos by, final Runnable onChange) {
        this.by = by.immutable();
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    /**
     * Lays {@code to} from now on, or nothing while it is null; what it holds stays held, and what the new build does
     * not want may be pulled out of the inbox.
     */
    public void build(final @Nullable PlannedBuild to) {
        this.build = to;
        this.waiting = false;
        wake();
    }

    /** The build it lays, if it has one. */
    public Optional<PlannedBuild> build() {
        return Optional.ofNullable(this.build);
    }

    /** Wakes it to look for a block to lay on its next tick: its plan's counts changed. */
    public void wake() {
        this.sleep = 0;
    }

    /** Where blocks for the build go in, and what it no longer wants comes out: a port passes this on. */
    public IItemHandler inbox() {
        return this.inbox;
    }

    /** How many of {@code item} it holds. */
    public int held(final Item item) {
        return this.held.getOrDefault(item, 0);
    }

    /** Everything it holds, by item. */
    public Map<Item, Integer> held() {
        return Map.copyOf(this.held);
    }

    /**
     * What it waits for: each block item its build still wants beyond what it holds, and how many more, in the order
     * the places wanting them come to be laid; nothing without a build.
     */
    public Map<Item, Integer> needs() {
        final PlannedBuild now = this.build;
        final Map<Item, Integer> out = new LinkedHashMap<>();
        if (now == null) {
            return out;
        }
        for (final Map.Entry<ResourceLocation, Integer> wanted : now.wanted().entrySet()) {
            final Item item = BuiltInRegistries.BLOCK.get(wanted.getKey()).asItem();
            final int more = wanted.getValue() - held(item);
            if (item != Items.AIR && more > 0) {
                out.put(item, more);
            }
        }
        return out;
    }

    /** Whether it waits for blocks: its build is not done, and it found nothing it could lay when it last looked. */
    public boolean waiting() {
        final PlannedBuild now = this.build;
        return this.waiting && now != null && !now.complete();
    }

    /**
     * Lays as many blocks as the pace gives this tick, on the server, each the lowest missing one of its build it
     * holds and can set down.
     *
     * @return how many it laid
     */
    public int tick(final ServerLevel level) {
        final PlannedBuild now = this.build;
        if (now == null || now.complete()) {
            this.credit = 0;
            return 0;
        }
        if (this.sleep > 0) {
            this.sleep--;
            return 0;
        }
        final int pace = BuildBalance.blocksPerSecond();
        this.credit = Math.min(this.credit + pace, Math.max(TICKS_PER_SECOND, pace));
        int laid = 0;
        while (this.credit >= TICKS_PER_SECOND) {
            final Optional<PlanSpot> next = now.next(spot -> canLay(level, now, spot));
            if (next.isEmpty()) {
                this.waiting = true;
                this.sleep = SLEEP;
                return laid;
            }
            this.waiting = false;
            this.credit -= TICKS_PER_SECOND;
            lay(level, now, next.get());
            laid++;
        }
        return laid;
    }

    /** Drops everything it holds at {@code at}, as its machine is broken. */
    public void spill(final Level level, final BlockPos at) {
        for (final Map.Entry<Item, Integer> kept : this.held.entrySet()) {
            int left = kept.getValue();
            final int most = new ItemStack(kept.getKey()).getMaxStackSize();
            while (left > 0) {
                final int count = Math.min(left, most);
                Containers.dropItemStack(level, at.getX(), at.getY(), at.getZ(), new ItemStack(kept.getKey(), count));
                left -= count;
            }
        }
        this.held.clear();
        this.kinds.clear();
        this.onChange.run();
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        HELD.encodeStart(NbtOps.INSTANCE, Map.copyOf(this.held)).result().ifPresent(kept -> tag.put(KEY, kept));
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.held.clear();
        this.kinds.clear();
        if (tag.contains(KEY)) {
            HELD.parse(NbtOps.INSTANCE, tag.get(KEY)).result().ifPresent(kept -> kept.forEach(this::add));
        }
    }

    /* How many more of {@code item} its build takes than it holds; none for an item that is no block. */
    private int room(final Item item) {
        final PlannedBuild now = this.build;
        final Block block = Block.byItem(item);
        if (now == null || block == Blocks.AIR) {
            return 0;
        }
        return Math.max(0, now.wanted(BuiltInRegistries.BLOCK.getKey(block)) - held(item));
    }

    /* How many of {@code item} it holds that its build does not want: all of them, without a build. */
    private int surplus(final Item item) {
        final PlannedBuild now = this.build;
        final int has = held(item);
        if (now == null) {
            return has;
        }
        return Math.max(0, has - now.wanted(BuiltInRegistries.BLOCK.getKey(Block.byItem(item))));
    }

    /* Whether it can lay the block wanted at {@code spot} now: it holds one, and nothing keeps it from going there. */
    private boolean canLay(final ServerLevel level, final PlannedBuild now, final PlanSpot spot) {
        final Block block = BuiltInRegistries.BLOCK.get(spot.block());
        if (held(block.asItem()) <= 0 || block.asItem() == Items.AIR) {
            return false;
        }
        final BlockPos at = now.where(spot);
        if (!level.isLoaded(at) || !level.getBlockState(at).canBeReplaced()) {
            return false;
        }
        final BlockState placing = block.defaultBlockState();
        return placing.canSurvive(level, at) && level.isUnobstructed(placing, at, CollisionContext.empty())
                && Protection.may(Actor.world(this.by), ProtectedAction.PLACE, level, at);
    }

    /* Sets the block wanted at {@code spot} down, as a player placing it would be heard, and takes it from the hold. */
    private void lay(final ServerLevel level, final PlannedBuild now, final PlanSpot spot) {
        final Block block = BuiltInRegistries.BLOCK.get(spot.block());
        final BlockPos at = now.where(spot);
        final BlockState placing = block.defaultBlockState();
        take(block.asItem());
        // Its neighbours told, the world's index of structures tells the build its place is filled.
        level.setBlock(at, placing, Block.UPDATE_ALL);
        final SoundType sound = placing.getSoundType(level, at, null);
        level.playSound(null, at, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, at, GameEvent.Context.of(placing));
    }

    /* Holds {@code count} more of {@code item}. */
    private void add(final Item item, final int count) {
        if (this.held.merge(item, count, Integer::sum) == count) {
            this.kinds.add(item);
        }
    }

    /* Takes one {@code item} from the hold. */
    private void take(final Item item) {
        takeSome(item, 1);
    }

    /* Takes {@code count} of {@code item} from the hold, letting go of a kind it has no more of. */
    private void takeSome(final Item item, final int count) {
        final int left = held(item) - count;
        if (left > 0) {
            this.held.put(item, left);
        } else {
            this.held.remove(item);
            this.kinds.remove(item);
        }
        this.onChange.run();
    }

    /*
     * The hold as an item handler: a place for each kind it holds, which takes more of that kind and gives back what
     * the build does not want of it, and one empty place more, which takes any block the build wants.
     */
    private final class Inbox implements IItemHandler {

        @Override
        public int getSlots() {
            return kinds.size() + 1;
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            final Item item = itemIn(slot);
            if (item == null) {
                return ItemStack.EMPTY;
            }
            final ItemStack one = new ItemStack(item);
            return one.copyWithCount(Math.min(held(item), one.getMaxStackSize()));
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            final Item in = itemIn(slot);
            // A block with data of its own, a named one, is not one the plan wants.
            if (stack.isEmpty() || in != null && in != stack.getItem() || !stack.getComponentsPatch().isEmpty()) {
                return stack;
            }
            final int taking = Math.min(room(stack.getItem()), stack.getCount());
            if (taking <= 0) {
                return stack;
            }
            if (!simulate) {
                add(stack.getItem(), taking);
                wake();
                onChange.run();
            }
            return stack.copyWithCount(stack.getCount() - taking);
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            final Item item = itemIn(slot);
            if (item == null || amount <= 0) {
                return ItemStack.EMPTY;
            }
            final ItemStack one = new ItemStack(item);
            final int out = Math.min(Math.min(amount, one.getMaxStackSize()), surplus(item));
            if (out <= 0) {
                return ItemStack.EMPTY;
            }
            if (!simulate) {
                takeSome(item, out);
            }
            return one.copyWithCount(out);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            final Item in = itemIn(slot);
            return (in == null || in == stack.getItem()) && room(stack.getItem()) > 0;
        }

        /* The kind held at {@code slot}; none for the empty place, or a place past the end. */
        private @Nullable Item itemIn(final int slot) {
            return slot >= 0 && slot < kinds.size() ? kinds.get(slot) : null;
        }
    }
}
