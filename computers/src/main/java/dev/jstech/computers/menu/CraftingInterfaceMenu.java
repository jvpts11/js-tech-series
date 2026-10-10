/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.CraftingRouterPart;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.payload.CraftingInterfaceEditPayload;
import dev.jstech.computers.operation.payload.CraftingInterfaceStatePayload;
import dev.jstech.computers.operation.payload.InterfaceView;
import dev.jstech.computers.operation.payload.crafting.CraftingViews;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The window of a Crafting Interface: its three tabs, with no inventory. Everything it shows comes from the server as
 * one {@link CraftingInterfaceStatePayload}, sent when it opens and again whenever the interface changes; everything
 * changed in it goes back as a {@link CraftingInterfaceEditPayload}, which this menu applies to the interface it is
 * open on.
 */
public class CraftingInterfaceMenu extends CoreMenu {

    private final CraftingInterfacePart part;
    /* The player the window is open for. */
    private final Player owner;
    private final BlockPos cablePos;
    private final Direction face;
    private final HardwareEra era;
    private String interfaceName;
    /* What the window shows: on the client, the last state received; on the server, the last one sent. */
    @Nullable
    private InterfaceView state;
    private int sinceChecked;

    /** How often, in ticks, the server looks for a change to send. */
    private static final int CHECK_EVERY = 5;

    public CraftingInterfaceMenu(final int containerId, final Inventory inventory, final CableBlockEntity cable,
                                 final Direction face, final CraftingInterfacePart part) {
        this(containerId, inventory, part, cable.getLevel(), Opening.of(cable, face, part));
    }

    private CraftingInterfaceMenu(final int containerId, final Inventory inventory, final CraftingInterfacePart part,
                                  final Level level, final Opening opening) {
        super(ComputingMenus.CRAFTING_INTERFACE_MENU.get(), containerId, inventory,
                validity(level, opening.pos(), opening.face(), part));
        this.part = part;
        this.owner = inventory.player;
        this.cablePos = opening.pos();
        this.face = opening.face();
        this.era = opening.era();
        this.interfaceName = opening.name();
    }

    /** The menu on the player's game, made from what the server wrote when it opened. */
    public static CraftingInterfaceMenu fromNetwork(final int containerId, final Inventory inventory,
                                                    final RegistryFriendlyByteBuf buf) {
        final Opening opening = Opening.read(buf);
        final Level level = inventory.player.level();
        final CraftingInterfacePart part = level.getBlockEntity(opening.pos()) instanceof CableBlockEntity cable
                && cable.getPart(opening.face()) instanceof CraftingInterfacePart real ? real
                : new CraftingInterfacePart(opening.era());
        return new CraftingInterfaceMenu(containerId, inventory, part, level, opening);
    }

    /** What an interface's window is opened with, written by the server and read by the client first. */
    public record Opening(BlockPos pos, Direction face, String name, HardwareEra era) {

        public void write(final FriendlyByteBuf buf) {
            new AbstractBusMenu.Opening(pos, face, name, era).write(buf, CraftingInterfacePart.MAX_NAME_LENGTH);
        }

        public static Opening read(final FriendlyByteBuf buf) {
            final AbstractBusMenu.Opening read =
                    AbstractBusMenu.Opening.read(buf, CraftingInterfacePart.MAX_NAME_LENGTH);
            return new Opening(read.pos(), read.face(), read.name(), read.era());
        }

        /** The opening of the window on {@code part}, on {@code cable}'s face {@code face}. */
        public static Opening of(final CableBlockEntity cable, final Direction face,
                                 final CraftingInterfacePart part) {
            return new Opening(cable.getBlockPos(), face, part.name(), part.era());
        }
    }

    public BlockPos cablePos() {
        return cablePos;
    }

    public Direction face() {
        return face;
    }

    /** The interface's era, which says how many patterns it holds. */
    public HardwareEra era() {
        return era;
    }

    public String interfaceName() {
        return interfaceName;
    }

    /** The last state the window received, or null before the first arrives. */
    @Nullable
    public InterfaceView state() {
        return state;
    }

    /** Takes the state the server sent; the client's copy only. */
    public void accept(final InterfaceView received) {
        this.state = received;
        this.interfaceName = received.name();
    }

    /** Applies a change made in the window, on the server, and sends the window what it changed. */
    public void edit(final ServerPlayer player, final CraftingInterfaceEditPayload edit) {
        final boolean taken = switch (edit.action()) {
            case CraftingInterfaceEditPayload.NAME -> {
                part.setName(edit.text());
                yield true;
            }
            case CraftingInterfaceEditPayload.MODE -> part.setExclusive(edit.value() != 0, "");
            case CraftingInterfaceEditPayload.STATE -> part.setPaused(edit.value() != 0, "");
            case CraftingInterfaceEditPayload.JOBS -> part.setMaxJobs(edit.value(), "");
            case CraftingInterfaceEditPayload.ROUTE -> route(player.serverLevel(), edit);
            default -> false;
        };
        if (taken) {
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

    /* Routes an input of a pattern through a router of the interface's own cable, or back to the default. */
    private boolean route(final ServerLevel level, final CraftingInterfaceEditPayload edit) {
        if (edit.pattern() < 0 || edit.pattern() >= part.patterns().size()) {
            return false;
        }
        final CraftingInterfacePart.HeldPattern held = part.patterns().get(edit.pattern());
        final List<StorageKey> inputs = new ArrayList<>();
        held.recipe().proc().ifPresent(pattern -> addInputs(pattern, inputs));
        held.recipe().multi().ifPresent(multi -> multi.stages().forEach(stage -> stage.proc()
                .ifPresent(pattern -> addInputs(pattern, inputs))));
        if (edit.input() < 0 || edit.input() >= inputs.size()) {
            return false;
        }
        final CraftingFloor.Reach reach = CraftingFloor.through(level, cablePos)
                .reach(new CraftingFloor.Site(cablePos, face));
        UUID router = null;
        if (edit.value() >= 0 && edit.value() < reach.routers().size()) {
            final CraftingRouterPart routerPart = reach.routers().get(edit.value()).part(level,
                    CraftingRouterPart.class);
            if (routerPart == null) {
                return false;
            }
            router = routerPart.id();
        }
        return part.route(edit.pattern(), inputs.get(edit.input()), router, "");
    }

    private static void addInputs(final ProcessingPattern pattern, final List<StorageKey> into) {
        for (final ProcessingPattern.ProcessingInput in : pattern.inputs()) {
            if (!into.contains(in.key())) {
                into.add(in.key());
            }
        }
    }

    /* Sends the interface's state when it changed since the last one sent, or {@code always}. */
    private void send(final ServerPlayer player, final boolean always) {
        final InterfaceView now = CraftingViews.of(player.serverLevel(), cablePos, face, part);
        if (always || state == null || !state.equals(now)) {
            state = now;
            PacketDistributor.sendToPlayer(player, new CraftingInterfaceStatePayload(containerId, now));
        }
    }

    /*
     * Valid while the cable still stands within reach and the interface this menu was opened on is still on its face:
     * an interface taken off from under an open menu closes it.
     */
    private static Predicate<Player> validity(final Level level, final BlockPos cablePos, final Direction face,
                                              final CraftingInterfacePart part) {
        return AbstractBusMenu.validity(level, cablePos, face, part);
    }
}
