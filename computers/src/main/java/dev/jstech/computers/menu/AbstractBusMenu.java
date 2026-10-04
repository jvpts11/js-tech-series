/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.block.part.BusEdits;
import dev.jstech.computers.block.part.CraftingRouterPart;
import dev.jstech.computers.block.part.ExportBusPart;
import dev.jstech.computers.block.part.ExternalStorageBusPart;
import dev.jstech.computers.block.part.ReceivingBusPart;
import dev.jstech.computers.bus.BusAbilities;
import dev.jstech.computers.bus.BusSettings;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.computers.operation.payload.BusEditPayload;
import dev.jstech.computers.operation.payload.BusStatePayload;
import dev.jstech.computers.operation.payload.CraftingView;
import dev.jstech.computers.operation.payload.crafting.CraftingViews;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * The window of a bus (Import, Export, and the crafting Input and Receiving): its three tabs over the player's
 * inventory, which only Configure shows. Everything the window shows that is not a slot comes from the server as one
 * {@link BusStatePayload}, sent when it opens and again whenever the bus changes; everything changed in it goes back as
 * a {@link BusEditPayload}, which this menu applies to the bus it is open on. The filter's cells are drawn, not slots:
 * a click puts what the cursor carries in them, as a copy, and takes nothing from the player.
 */
public abstract class AbstractBusMenu extends CoreMenu {

    private final AbstractBusPart part;
    /* The player the window is open for. */
    private final Player owner;
    private final BlockPos cablePos;
    private final Direction face;
    private final HardwareEra era;
    private final BusLayout.Window window;
    private String busName;
    /* Whether the inventory is shown: the client's Configure tab is open. The server's copy always has it. */
    private boolean inventoryShown = true;
    /* What the window shows: on the client, the last state received; on the server, the last one sent. */
    @Nullable
    private BusStatePayload state;
    private int sinceChecked;

    /** How far a player may stand from the cable and keep this menu open, in blocks. */
    private static final double REACH_BLOCKS = 8.0;
    /** How often, in ticks, the server looks for a change to send. */
    private static final int CHECK_EVERY = 5;

    protected AbstractBusMenu(final MenuType<?> type, final int containerId, final Inventory playerInventory,
                              final AbstractBusPart part, final Level level, final Opening opening,
                              final BusLayout.Window window) {
        super(type, containerId, playerInventory, validity(level, opening.pos(), opening.face(), part));
        this.part = part;
        this.owner = playerInventory.player;
        this.cablePos = opening.pos();
        this.face = opening.face();
        this.era = opening.era();
        this.window = window;
        this.busName = opening.name();
        final GuiLayout.SlotPosition at = new GuiLayout.SlotPosition(BusLayout.INV_X,
                BusLayout.inventoryY(BusLayout.abilities(era, window), window));
        final PlayerSlots playerSlots = playerInventory(playerInventory, at, () -> inventoryShown);
        shiftClick(playerSlots.main(), playerSlots.hotbar());
        shiftClick(playerSlots.hotbar(), playerSlots.main());
    }

    /** What a bus's window is opened with, written by the server and read by the client before the menu is made. */
    public record Opening(BlockPos pos, Direction face, String name, HardwareEra era) {

        public void write(final FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeByte(face.get3DDataValue());
            buf.writeUtf(name, AbstractBusPart.MAX_NAME_LENGTH);
            buf.writeVarInt(era.level());
        }

        public static Opening read(final FriendlyByteBuf buf) {
            return new Opening(buf.readBlockPos(), Direction.from3DDataValue(buf.readByte()),
                    buf.readUtf(AbstractBusPart.MAX_NAME_LENGTH), HardwareEra.fromLevel(buf.readVarInt()));
        }

        /** The opening of the window on {@code part}, on {@code cable}'s face {@code face}. */
        public static Opening of(final CableBlockEntity cable, final Direction face, final AbstractBusPart part) {
            return new Opening(cable.getBlockPos(), face, part.name(), part.era());
        }
    }

    /** The era of the bus, which decides its rows; its window's skin comes with its state. */
    public HardwareEra era() {
        return era;
    }

    /** What kind of bus the window is for: one that moves, a crafting bus, or an External Storage Bus. */
    public BusLayout.Window window() {
        return window;
    }

    /** Whether it is a crafting part (a router or a Receiving Bus), which has its filter and what it is part of. */
    public boolean crafting() {
        return window == BusLayout.Window.ROUTER || window == BusLayout.Window.RECEIVING;
    }

    /** What the bus can be set to, by its era and kind. */
    public BusAbilities abilities() {
        return BusLayout.abilities(era, window);
    }

    /** Whether the bus sends out of the network (an Export Bus) rather than bringing in. */
    public boolean exports() {
        return part instanceof ExportBusPart;
    }

    public BlockPos cablePos() {
        return cablePos;
    }

    public Direction face() {
        return face;
    }

    public String busName() {
        return busName;
    }

    /** Updates the locally-known name after the server confirms an edit, so the field stays in sync. */
    public void setBusNameLocal(final String name) {
        this.busName = name == null ? "" : name;
    }

    /** The last state the window received, or null before the first arrives. */
    @Nullable
    public BusStatePayload state() {
        return state;
    }

    /** What the bus is set to as the window last heard, or a new bus of its era before it has heard. */
    public BusSettings settings() {
        if (state != null) {
            return state.settings();
        }
        return window == BusLayout.Window.EXTERNAL ? BusSettings.freshExternal(era) : BusSettings.fresh(era);
    }

    /** What filter slot {@code slot} lists, to draw, as the window last heard. */
    public ItemStack filterStack(final int slot) {
        final List<ItemStack> stacks = state == null ? List.of() : state.filter();
        return slot >= 0 && slot < stacks.size() ? stacks.get(slot) : ItemStack.EMPTY;
    }

    /** Takes the state the server sent; the client's copy only. */
    public void accept(final BusStatePayload received) {
        this.state = received;
        this.busName = received.settings().name();
    }

    /** Shows or hides the inventory with the tab: only Configure has it. */
    public void showInventory(final boolean shown) {
        this.inventoryShown = shown;
    }

    /** Applies a change made in the window, on the server, and sends the window what it changed. */
    public void edit(final ServerPlayer player, final BusEditPayload edit) {
        if (edit.op() == BusEditPayload.TIE) {
            if (part instanceof ReceivingBusPart bus && tie(player.serverLevel(), bus, edit.slot())) {
                send(player, true);
            }
            return;
        }
        if (BusEdits.apply(part, edit, getCarried())) {
            send(player, true);
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (onClient() || ++sinceChecked < CHECK_EVERY && state != null) {
            return;
        }
        sinceChecked = 0;
        if (owner instanceof ServerPlayer player && player.containerMenu == this) {
            send(player, false);
        }
    }

    /* Sends the bus's state when it changed since the last one sent, or {@code always}. */
    private void send(final ServerPlayer player, final boolean always) {
        final ExternalStorageBusPart external = part instanceof ExternalStorageBusPart bus ? bus : null;
        final ServerLevel level = player.serverLevel();
        // A crafting part is linked when its network reaches a Crafting Computer, and wears that computer's era.
        final boolean linked = part instanceof CraftingRouterPart ? CraftingViews.routerLinked(level, cablePos, face)
                : part instanceof ReceivingBusPart ? CraftingViews.reachesComputer(level, cablePos)
                : part.reachesNetwork();
        final HardwareEra skin = crafting() ? CraftingViews.skinOf(level, cablePos) : part.skin();
        final BusStatePayload now = new BusStatePayload(containerId, part.settings(), part.filterStacks(),
                linked, part.speed(), part.cableCarries(), skin, part.activity().entries(),
                external == null ? 0 : external.places(), external == null ? 0 : external.placesUsed(),
                craftingView(level));
        if (always || state == null || !same(state, now)) {
            state = now;
            PacketDistributor.sendToPlayer(player, now);
        }
    }

    /* What a crafting part's window shows of the network it is part of; nothing for a bus that is none. */
    private CraftingView craftingView(final ServerLevel level) {
        if (part instanceof CraftingRouterPart router) {
            return CraftingViews.router(level, cablePos, face, router);
        }
        if (part instanceof ReceivingBusPart bus) {
            return CraftingViews.receiving(level, cablePos, face, bus);
        }
        return CraftingView.NONE;
    }

    /* Ties the bus by hand to the interface its window lists at {@code index}, or unties it from it. */
    private boolean tie(final ServerLevel level, final ReceivingBusPart bus, final int index) {
        final List<UUID> ids = CraftingViews.pickIds(level, CraftingFloor.through(level, cablePos));
        if (index < 0 || index >= ids.size()) {
            return false;
        }
        final List<UUID> ties = new ArrayList<>(bus.tiedByHand());
        if (!ties.remove(ids.get(index))) {
            ties.add(ids.get(index));
        }
        bus.tieByHand(ties);
        return true;
    }

    /* Whether two states show the same; the filter's stacks follow its ids. */
    private static boolean same(final BusStatePayload a, final BusStatePayload b) {
        return a.settings().equals(b.settings()) && a.linked() == b.linked() && a.speed() == b.speed()
                && a.carries() == b.carries() && a.skin() == b.skin() && a.activity().equals(b.activity())
                && a.places() == b.places() && a.placesUsed() == b.placesUsed() && a.crafting().equals(b.crafting());
    }

    /**
     * Valid while the cable at {@code cablePos} still stands within reach and the part this menu was opened on is
     * still mounted on {@code face} of it: a part swapped out (or picked off) from under an open menu closes it,
     * rather than going on editing a bus that is no longer there.
     */
    private static Predicate<Player> validity(final Level level, final BlockPos cablePos, final Direction face,
                                              final AbstractBusPart part) {
        return MenuValidity.near(level, cablePos, REACH_BLOCKS)
                .and(player -> level.getBlockEntity(cablePos) instanceof CableBlockEntity cable
                        && cable.getPart(face) == part);
    }
}
