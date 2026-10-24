/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.transit;

import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.LineGroup;
import dev.jstech.core.grid.LineGroups;
import dev.jstech.core.inventory.ItemFilters;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * A station of a pneumatic post, on a line of tubes of its mod's: it has an address, and what is put into it (by a
 * hopper, a machine, a pipe of items) it sends in a capsule to the station of the address its routes give that item,
 * the first route whose filter lets it through, or the address it sends everything else to; what it has no address
 * for, it does not take. It sends one capsule as often as its rules let, carrying at most their stacks, to the nearest
 * station of that address its tubes join; the capsule takes time by the distance ({@link CapsuleTransit}). What arrives
 * it unloads into the block it faces, keeping what does not fit until it does. Its mod may make it spend something to
 * send ({@link #readyToSend}, {@link #sent}), as J's Industrial's stations spend compressed air.
 *
 * <pre>{@code
 * public static final GridKind TUBES = GridKind.declare(rl("capsule_tube"), GridPolicies.GROUP);
 * // its type, made with (pos, state) -> new CapsuleStationBlockEntity(STATION_BE.get(), pos, state, TUBES,
 * //         new CapsuleRules(4, 20, 20.0)), ticked by CapsuleStationBlockEntity::serverTick, and in the mod's
 * // RegisterCapabilitiesEvent:
 * CapsuleStationBlockEntity.register(event, STATION_BE.get());
 * }</pre>
 */
public class CapsuleStationBlockEntity extends SyncedBlockEntity implements LineGroups.IGroupMember {

    private final GridKind tubes;
    private final CapsuleRules rules;
    private final IntField address;
    private final ValueField<List<CapsuleRoute>> routes;
    private final IntField otherwise;
    private final FieldItemHandler outgoing;
    private final FieldItemHandler incoming;
    private final LongField nextSend;

    /** The address of no station: a station of it receives nothing, a route never goes to it. */
    public static final int NO_ADDRESS = 0;
    /** How often a station unloads what arrived into the block it faces, in ticks. */
    public static final int UNLOADS_EVERY = 10;

    /** A station on the tubes of {@code tubes}, a grid that groups the stations its runs join, under {@code rules}. */
    public CapsuleStationBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                     final GridKind tubes, final CapsuleRules rules) {
        super(type, pos, state);
        this.tubes = Objects.requireNonNull(tubes, "tubes");
        this.rules = Objects.requireNonNull(rules, "rules");
        this.address = fields().integer("Address", NO_ADDRESS).save().toClient();
        this.routes = fields().value("Routes", CapsuleRoute.CODEC.listOf(), List.<CapsuleRoute>of()).save();
        this.otherwise = fields().integer("Otherwise", NO_ADDRESS).save();
        this.outgoing = fields().items("Outgoing", rules.stacks()).save().dropsWhenBroken();
        this.incoming = fields().items("Incoming", rules.stacks()).save().dropsWhenBroken();
        this.nextSend = fields().longInteger("NextSend", 0L).save();
    }

    /**
     * Gives stations of {@code type} their faces: every face takes what the station has an address for and gives
     * what arrived; and the tubes find them on every face.
     */
    public static <T extends CapsuleStationBlockEntity> void register(final RegisterCapabilitiesEvent event,
                                                                      final BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (station, side) -> station.items());
        event.registerBlockEntity(LineGroups.MEMBER, type, (station, side) -> station);
    }

    /** One server tick of {@code station}, the ticker its block is ticked by: it unloads, then sends. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final CapsuleStationBlockEntity station) {
        if (level instanceof ServerLevel server) {
            final long now = server.getGameTime();
            if (Math.floorMod(now + pos.asLong(), UNLOADS_EVERY) == 0) {
                station.unload(server);
            }
            if (now >= station.nextSend.get()) {
                station.send(server, now);
            }
        }
    }

    /** Its rules. */
    public CapsuleRules rules() {
        return this.rules;
    }

    /** Its address; {@link #NO_ADDRESS} for none. */
    public int address() {
        return this.address.get();
    }

    /** Gives it the address {@code to}; {@link #NO_ADDRESS} for none. */
    public void setAddress(final int to) {
        this.address.set(Math.max(NO_ADDRESS, to));
    }

    /** Its routes, the first that lets an item through sending it. */
    public List<CapsuleRoute> routes() {
        return this.routes.get();
    }

    /** Sets its routes. */
    public void setRoutes(final List<CapsuleRoute> to) {
        this.routes.set(List.copyOf(to));
    }

    /** The address it sends what no route lets through to; {@link #NO_ADDRESS} for none, which it does not take. */
    public int otherwise() {
        return this.otherwise.get();
    }

    /** Sends what no route lets through to {@code to}; {@link #NO_ADDRESS} to take none of it. */
    public void setOtherwise(final int to) {
        this.otherwise.set(Math.max(NO_ADDRESS, to));
    }

    /** What waits to be sent. */
    public FieldItemHandler outgoing() {
        return this.outgoing;
    }

    /** What arrived and waits to be unloaded. */
    public FieldItemHandler incoming() {
        return this.incoming;
    }

    /**
     * What the game's item capability sees of it on every face: the slots of what waits to be sent, which take only
     * what it has an address for, then those of what arrived, which give.
     */
    public IItemHandler items() {
        return new Face();
    }

    /** The address it sends {@code stack} to: its first route that lets it through, else what it sends the rest to. */
    public OptionalInt addressOf(final ItemStack stack) {
        for (final CapsuleRoute route : this.routes.get()) {
            if (ItemFilters.allows(route.filter(), stack)) {
                return OptionalInt.of(route.address());
            }
        }
        return this.otherwise.get() == NO_ADDRESS ? OptionalInt.empty() : OptionalInt.of(this.otherwise.get());
    }

    /** Takes in a capsule's {@code items}, all of them, if it has room for each; whether it did. */
    public boolean receive(final List<ItemStack> items) {
        int empty = 0;
        for (int slot = 0; slot < this.incoming.getSlots(); slot++) {
            if (this.incoming.getStackInSlot(slot).isEmpty()) {
                empty++;
            }
        }
        if (empty < items.size()) {
            return false;
        }
        int slot = 0;
        for (final ItemStack stack : items) {
            while (!this.incoming.getStackInSlot(slot).isEmpty()) {
                slot++;
            }
            this.incoming.setStackInSlot(slot, stack.copy());
        }
        return true;
    }

    /** The face it unloads into the block on: the one it faces. */
    public Direction front() {
        return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    /** It joined a run of tubes: it looks for its stations as it sends, so it keeps nothing of it. */
    @Override
    public void joined(final LineGroup group) {
        // The group is asked for as each capsule leaves.
    }

    /** It left its run of tubes. */
    @Override
    public void left(final GridKind kind) {
        // The group is asked for as each capsule leaves.
    }

    /**
     * Whether it may send a capsule of {@code stacks} stacks now: always, unless its mod says it needs something to,
     * as compressed air.
     */
    protected boolean readyToSend(final ServerLevel level, final int stacks) {
        return true;
    }

    /** It sent {@code capsule}: its mod spends what sending costs. */
    protected void sent(final ServerLevel level, final Capsule capsule) {
        // Sending costs nothing unless its mod says.
    }

    /* Unloads what arrived into the block it faces, keeping what does not fit. */
    private void unload(final ServerLevel level) {
        final BlockPos ahead = this.worldPosition.relative(front());
        IItemHandler target = null;
        for (int slot = 0; slot < this.incoming.getSlots(); slot++) {
            final ItemStack stack = this.incoming.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (target == null) {
                if (!Loaded.at(level, ahead)) {
                    return;
                }
                target = level.getCapability(Capabilities.ItemHandler.BLOCK, ahead, front().getOpposite());
                if (target == null) {
                    return;
                }
            }
            this.incoming.setStackInSlot(slot, ItemHandlerHelper.insertItemStacked(target, stack, false));
        }
    }

    /* Sends a capsule of what waits for the first address, if a station of it is on its tubes and it may. */
    private void send(final ServerLevel level, final long now) {
        int first = -1;
        for (int slot = 0; slot < this.outgoing.getSlots() && first < 0; slot++) {
            if (!this.outgoing.getStackInSlot(slot).isEmpty()) {
                first = slot;
            }
        }
        if (first < 0) {
            return;
        }
        this.nextSend.set(now + this.rules.every());
        final OptionalInt to = addressOf(this.outgoing.getStackInSlot(first));
        if (to.isEmpty()) {
            return;
        }
        final Optional<BlockPos> station = nearest(level, to.getAsInt());
        if (station.isEmpty()) {
            return;
        }
        final List<Integer> slots = new ArrayList<>();
        for (int slot = first; slot < this.outgoing.getSlots() && slots.size() < this.rules.stacks(); slot++) {
            final ItemStack stack = this.outgoing.getStackInSlot(slot);
            if (!stack.isEmpty() && addressOf(stack).equals(to)) {
                slots.add(slot);
            }
        }
        if (!readyToSend(level, slots.size())) {
            return;
        }
        final List<ItemStack> items = new ArrayList<>(slots.size());
        for (final int slot : slots) {
            items.add(this.outgoing.getStackInSlot(slot).copy());
            this.outgoing.setStackInSlot(slot, ItemStack.EMPTY);
        }
        final Capsule capsule = new Capsule(this.worldPosition, station.get(), items,
                now + this.rules.ticksFor(this.worldPosition.distManhattan(station.get())));
        CapsuleTransit.send(level, capsule);
        sent(level, capsule);
    }

    /* The nearest station of {@code to} its tubes join, other than itself. */
    private Optional<BlockPos> nearest(final ServerLevel level, final int to) {
        final Optional<LineGroup> group = LineGroups.of(level, this.tubes)
                .flatMap(groups -> groups.groupOf(this.worldPosition));
        if (group.isEmpty()) {
            return Optional.empty();
        }
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (final BlockPos member : group.get().members()) {
            if (member.equals(this.worldPosition) || !Loaded.at(level, member)) {
                continue;
            }
            final int distance = member.distManhattan(this.worldPosition);
            if (distance < bestDistance && level.getBlockEntity(member) instanceof CapsuleStationBlockEntity other
                    && other.address() == to) {
                best = member;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    /* Its face: the slots of what waits to be sent, which take what it has an address for, then those of what came. */
    private final class Face implements IItemHandler {

        @Override
        public int getSlots() {
            return outgoing.getSlots() + incoming.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return slot < outgoing.getSlots() ? outgoing.getStackInSlot(slot)
                    : incoming.getStackInSlot(slot - outgoing.getSlots());
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            return isItemValid(slot, stack) ? outgoing.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return slot < outgoing.getSlots() ? ItemStack.EMPTY
                    : incoming.extractItem(slot - outgoing.getSlots(), amount, simulate);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return slot < outgoing.getSlots() ? outgoing.getSlotLimit(slot)
                    : incoming.getSlotLimit(slot - outgoing.getSlots());
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return slot < outgoing.getSlots() && addressOf(stack).isPresent();
        }
    }
}
