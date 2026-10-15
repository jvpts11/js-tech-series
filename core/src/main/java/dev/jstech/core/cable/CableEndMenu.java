/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.grid.EndMode;
import dev.jstech.core.grid.EndRouter;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SyncedValue;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The menu of one end of a line, opened on a cable block for the wire in one lane and the face it meets: the player's
 * inventory, the end as the server last said it, and the end's settings, each a button the server answers. A stack
 * dropped on a filter slot is the one the player carries, which the server already knows; a number typed in a field
 * comes as its own payload ({@link EndNumberPayload}).
 */
public final class CableEndMenu extends CoreMenu {

    private final CableBlockEntity cable;
    private final Lane lane;
    private final Direction face;
    private final SyncedValue<CableEnd> shown;
    /* What each filter slot was last set from, so a right-click goes round that thing's tags and mod. */
    private final String[] origins = new String[CableEndLayout.GHOSTS];

    /** The buttons: a mode, the priority and the rate a step or ten, the filter's mode, a channel, a filter slot. */
    public static final int FIRST_MODE = 0;
    public static final int PRIORITY_DOWN = 4;
    public static final int PRIORITY_UP = 5;
    public static final int PRIORITY_DOWN_TEN = 6;
    public static final int PRIORITY_UP_TEN = 7;
    public static final int RATE_DOWN = 8;
    public static final int RATE_UP = 9;
    public static final int RATE_DOWN_TEN = 10;
    public static final int RATE_UP_TEN = 11;
    public static final int FILTER_MODE = 12;
    public static final int FIRST_CHANNEL = 16;
    /** A filter slot clicked: set from the stack carried, or cleared when nothing is. */
    public static final int FIRST_GHOST = 32;
    /** A filter slot right-clicked: the next kind of rule. */
    public static final int FIRST_CYCLE = 48;
    /** A filter slot scrolled: more or less to keep, a step or many. */
    public static final int FIRST_MORE = 64;
    public static final int FIRST_LESS = 80;
    public static final int FIRST_MUCH_MORE = 96;
    public static final int FIRST_MUCH_LESS = 112;
    /** The steps of the rate and the amounts kept: a tick's items or millibuckets, and many at once. */
    public static final long ITEM_STEP = 1L;
    public static final long FLUID_STEP = 10L;
    public static final long ITEMS_KEPT_STEP = 1L;
    public static final long ITEMS_KEPT_MANY = 16L;
    public static final long FLUID_KEPT_STEP = 100L;
    public static final long FLUID_KEPT_MANY = 1_000L;
    /** The most a rate or an amount kept is set to. */
    public static final long MOST = 1_000_000_000L;
    private static final int TEN = 10;

    public CableEndMenu(final int containerId, final Inventory inventory, final CableBlockEntity cable,
                        final Lane lane, final Direction face) {
        super(EndSettings.MENU.get(), containerId, inventory, MenuValidity.blockEntity(cable)
                .and(player -> cable.wireIn(lane) != null));
        this.cable = cable;
        this.lane = lane;
        this.face = face;
        final PlayerSlots player = playerInventory(inventory, new GuiLayout.SlotPosition(CableEndLayout.INVENTORY_X,
                CableEndLayout.INVENTORY_Y));
        shiftClick(player.main(), player.hotbar());
        shiftClick(player.hotbar(), player.main());
        this.shown = synced("end", CableEnd.STREAM_CODEC, cable.end(lane, face), () -> cable.end(lane, face));
        final List<EndMode> modes = EndMode.round();
        for (int k = 0; k < modes.size(); k++) {
            final EndMode mode = modes.get(k);
            button(FIRST_MODE + k, who -> change(end -> end.withMode(mode)));
        }
        button(PRIORITY_DOWN, who -> change(end -> end.withPriority(priority(end.priority() - 1))));
        button(PRIORITY_UP, who -> change(end -> end.withPriority(priority(end.priority() + 1))));
        button(PRIORITY_DOWN_TEN, who -> change(end -> end.withPriority(priority(end.priority() - TEN))));
        button(PRIORITY_UP_TEN, who -> change(end -> end.withPriority(priority(end.priority() + TEN))));
        button(RATE_DOWN, who -> change(end -> end.withRate(amount(end.rate() - rateStep()))));
        button(RATE_UP, who -> change(end -> end.withRate(amount(end.rate() + rateStep()))));
        button(RATE_DOWN_TEN, who -> change(end -> end.withRate(amount(end.rate() - rateStep() * TEN))));
        button(RATE_UP_TEN, who -> change(end -> end.withRate(amount(end.rate() + rateStep() * TEN))));
        button(FILTER_MODE, who -> change(end -> end.withFilter(end.filter().withMode(
                end.filter().mode() == ItemFilter.Mode.ONLY ? ItemFilter.Mode.ALL_BUT : ItemFilter.Mode.ONLY))));
        for (int k = 0; k < EndRouter.CHANNELS; k++) {
            final int channel = k;
            button(FIRST_CHANNEL + k, who -> change(end -> end.withChannel(channel)));
        }
        for (int k = 0; k < CableEndLayout.GHOSTS; k++) {
            final int slot = k;
            button(FIRST_GHOST + k, setGhost(slot));
            button(FIRST_CYCLE + k, who -> cycle(slot));
            button(FIRST_MORE + k, who -> keep(slot, keptStep(false)));
            button(FIRST_LESS + k, who -> keep(slot, -keptStep(false)));
            button(FIRST_MUCH_MORE + k, who -> keep(slot, keptStep(true)));
            button(FIRST_MUCH_LESS + k, who -> keep(slot, -keptStep(true)));
        }
    }

    public CableEndMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, CableBlockEntity.class),
                lane(buf.readUtf()), Direction.from3DDataValue(buf.readVarInt()));
    }

    /** The cable block it is open on. */
    public BlockPos pos() {
        return this.cable.getBlockPos();
    }

    /** The face of the end it sets. */
    public Direction face() {
        return this.face;
    }

    /** The wire whose end it sets, or null once it is taken out. */
    public @Nullable Wire wire() {
        return this.cable.wireIn(this.lane);
    }

    /** What the wire carries; the fluid grid's for a wire taken out. */
    public GridKind grid() {
        final Wire wire = wire();
        return wire == null ? GridKind.FLUID : wire.type().grid();
    }

    /** The end as the server last said it. */
    public CableEnd end() {
        return this.shown.get();
    }

    /** Sets the priority or the rate a player typed, on the server. */
    public void typed(final boolean rate, final long value) {
        change(end -> rate ? end.withRate(amount(value)) : end.withPriority(priority(value)));
    }

    /* Changes the end on the server, as the change says; nothing once the wire is gone. */
    private void change(final UnaryOperator<CableEnd> how) {
        final Wire wire = wire();
        if (wire != null && !onClient()) {
            this.cable.setEnd(wire.type(), this.face, how.apply(this.cable.end(this.lane, this.face)));
        }
    }

    /* The lane the server wrote, which the client knows by the same name. */
    private static Lane lane(final String name) {
        final Lane lane = Lane.byName(name);
        if (lane == null) {
            throw new IllegalStateException("a line's end was opened in a lane the client does not know: " + name);
        }
        return lane;
    }

    /* A filter slot clicked with the stack the player carries: the rule it makes, or the slot cleared. */
    private Consumer<Player> setGhost(final int slot) {
        return player -> {
            final ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                this.origins[slot] = null;
                change(end -> end.withFilter(end.filter().without(slot)));
                return;
            }
            final Optional<String> id = EndFilters.idFor(grid(), carried);
            if (id.isEmpty()) {
                player.displayClientMessage(GameText.component(EndTexts.NOT_CARRIED), true);
                return;
            }
            change(end -> {
                final List<ItemFilter.Rule> rules = end.filter().rules();
                final long kept = slot < rules.size() ? rules.get(slot).amount() : 0L;
                final ItemFilter.Rule rule = new ItemFilter.Fuzzy(id.get(), kept);
                final int at = Math.min(slot, rules.size());
                this.origins[at] = id.get();
                // The first rule of an empty filter names what passes, as a player dropping it means, and only such
                // a filter keeps an amount in the block.
                final ItemFilter filter = rules.isEmpty() ? end.filter().withMode(ItemFilter.Mode.ONLY)
                        : end.filter();
                return end.withFilter(filter.withRule(at, rule));
            });
        };
    }

    /* The next kind of rule in a filter slot: the thing, its tags, its mod. */
    private void cycle(final int slot) {
        change(end -> {
            final List<ItemFilter.Rule> rules = end.filter().rules();
            if (slot >= rules.size()) {
                return end;
            }
            final ItemFilter.Rule next = EndFilters.next(grid(), rules.get(slot),
                    Optional.ofNullable(this.origins[slot]));
            return end.withFilter(end.filter().withRule(slot, next));
        });
    }

    /* More or less of what a filter slot names to keep in the block. */
    private void keep(final int slot, final long by) {
        change(end -> {
            final List<ItemFilter.Rule> rules = end.filter().rules();
            if (slot >= rules.size()) {
                return end;
            }
            final ItemFilter.Rule rule = rules.get(slot);
            return end.withFilter(end.filter().withRule(slot, rule.withAmount(amount(rule.amount() + by))));
        });
    }

    private long rateStep() {
        return grid() == GridKind.ITEM ? ITEM_STEP : FLUID_STEP;
    }

    private long keptStep(final boolean many) {
        if (grid() == GridKind.ITEM) {
            return many ? ITEMS_KEPT_MANY : ITEMS_KEPT_STEP;
        }
        return many ? FLUID_KEPT_MANY : FLUID_KEPT_STEP;
    }

    private static int priority(final long value) {
        return (int) Math.max(CableEnd.LOWEST, Math.min(CableEnd.HIGHEST, value));
    }

    private static long amount(final long value) {
        return Math.max(0L, Math.min(MOST, value));
    }
}
