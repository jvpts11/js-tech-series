/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.multiblock.MultiblockPortBlockEntity;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.core.multiblock.plan.PlanAssembler;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * An Item Port set in a wall of a Small Shipyard: as the dock forms it is bound to the dock's terminal, and it is a
 * store, a hold of two rows of nine places a player fills by hand and a chest, a hopper, a pipe or a network fills
 * too. It takes only the blocks the ship being built still lacks, no more of each than the dock and the hold lack,
 * and passes them on to the arms as they wait for them. Its screen shows what the build still lacks; broken, its hold
 * falls where it stood.
 */
public final class ItemPortBlockEntity extends MultiblockPortBlockEntity {

    private final FieldItemHandler hold;
    /** How many places its hold has. */
    public static final int PLACES = 18;
    /* How many items go on from the hold to the arms each tick. */
    private static final int PER_TICK = 4;

    public ItemPortBlockEntity(final BlockPos pos, final BlockState state) {
        super(HangarBlocks.ITEM_PORT_ENTITY.get(), pos, state, PortKind.ITEM_INPUT);
        this.hold = fields().items("Hold", PLACES).save().dropsWhenBroken()
                .accepts((slot, stack) -> !stack.isEmpty() && room(stack.getItem()) > 0)
                .stackLimit(this::limit);
    }

    /** Its hold, which takes only what the build still lacks. */
    public IItemHandler hold() {
        return this.hold;
    }

    /** The dock it is bound to, while it is formed. */
    public Optional<ShipyardTerminalBlockEntity> dock() {
        final BlockPos terminal = terminalPos();
        if (terminal == null || this.level == null || !this.level.isLoaded(terminal)
                || !(this.level.getBlockEntity(terminal) instanceof ShipyardTerminalBlockEntity dock)
                || !dock.formed()) {
            return Optional.empty();
        }
        return Optional.of(dock);
    }

    /** How many of {@code item} its hold holds. */
    public int holds(final Item item) {
        int count = 0;
        for (int slot = 0; slot < this.hold.getSlots(); slot++) {
            final ItemStack stack = this.hold.getStackInSlot(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** How many items its hold holds in all. */
    public int held() {
        int count = 0;
        for (int slot = 0; slot < this.hold.getSlots(); slot++) {
            count += this.hold.getStackInSlot(slot).getCount();
        }
        return count;
    }

    /**
     * What the build of its dock still lacks, as its screen shows it: each block with what the dock and this hold
     * have of it and how many more the ship takes; none while nothing is being built.
     */
    public ItemPortView view() {
        final Optional<ShipyardTerminalBlockEntity> dock = dock();
        if (dock.isEmpty() || dock.get().assembler().build().isEmpty()) {
            return ItemPortView.NONE;
        }
        final PlanAssembler arms = dock.get().assembler();
        final PlannedBuild build = arms.build().get();
        final Map<Item, Integer> lacking = new LinkedHashMap<>();
        arms.needs().forEach((item, count) -> lacking.merge(item, count, Integer::sum));
        arms.held().forEach((item, count) -> lacking.merge(item, count, Integer::sum));
        final List<ItemPortView.Row> rows = new ArrayList<>();
        int lackingAll = 0;
        for (final Map.Entry<Item, Integer> each : lacking.entrySet()) {
            if (each.getValue() <= 0) {
                continue;
            }
            rows.add(new ItemPortView.Row(each.getKey(), arms.held(each.getKey()) + holds(each.getKey()),
                    each.getValue()));
            lackingAll += each.getValue();
        }
        final int total = build.plan().spots().size();
        return new ItemPortView(dock.get().shipName(), total - lackingAll, total, rows);
    }

    /** Passes on to its dock's arms what they wait for, a few at a time; on the server, every tick. */
    public void tick(final ServerLevel level) {
        final Optional<ShipyardTerminalBlockEntity> dock = dock();
        if (dock.isEmpty()) {
            return;
        }
        dock.get().portSeen(this);
        final IItemHandler inbox = dock.get().assembler().inbox();
        int left = PER_TICK;
        for (int slot = 0; slot < this.hold.getSlots() && left > 0; slot++) {
            final ItemStack stack = this.hold.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            final ItemStack moving = stack.copyWithCount(Math.min(left, stack.getCount()));
            final ItemStack back = insert(inbox, moving);
            final int taken = moving.getCount() - back.getCount();
            if (taken > 0) {
                this.hold.extractItem(slot, taken, false);
                left -= taken;
            }
        }
    }

    /* How many more of {@code item} the build takes than its arms and this hold have: what the hold may take. */
    private int room(final Item item) {
        final Optional<ShipyardTerminalBlockEntity> dock = dock();
        if (dock.isEmpty()) {
            return 0;
        }
        return dock.get().assembler().needs().getOrDefault(item, 0) - holds(item);
    }

    /* The most of {@code stack}'s item place {@code slot} holds: what it holds of it and what the build lacks more. */
    private int limit(final int slot, final ItemStack stack) {
        final ItemStack there = this.hold.getStackInSlot(slot);
        return (there.is(stack.getItem()) ? there.getCount() : 0) + Math.max(0, room(stack.getItem()));
    }

    /* Puts {@code stack} into {@code into} wherever it goes, what it would not take given back. */
    private static ItemStack insert(final IItemHandler into, final ItemStack stack) {
        ItemStack left = stack;
        for (int slot = 0; slot < into.getSlots() && !left.isEmpty(); slot++) {
            left = into.insertItem(slot, left, false);
        }
        return left;
    }
}
