/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A session on a monitor that is not a system: the self-test, the boot manager, the firmware setup, a system
 * coming up, an installer, and the switch that says which machine of a rack a monitor shows.
 *
 * <p>Every one of these used to be a bare screen the server pushed at a player with a packet, which made them
 * a second kind of computer screen with none of the things the first kind gets for free. They wore the newest
 * machine's monitor whatever machine they were on, because nothing told them the era. A recipe viewer never
 * appeared beside them, because a viewer sits beside a menu. And the server was never told when one closed,
 * so a player who walked away was still counted as watching and the next page of an installer was put in
 * front of them in the middle of a field.
 *
 * <p>So they are menus now, like every other computer screen. It holds no slots: what is on the glass keeps
 * arriving by its own packet, and this carries only who is being looked at, on which monitor, and which of
 * the sessions it is, so the client knows which screen to open and the server knows who is still watching.
 */
public class MonitorSessionMenu extends AbstractContainerMenu {

    private final BlockPos monitorPos;
    private final BlockPos hostPos;
    @Nullable
    private final HardwareEra era;
    private final Phase phase;

    /** How far from the glass a session survives, the vanilla reach a container is closed past. */
    private static final double REACH_SQUARED = 64.0;

    public MonitorSessionMenu(final int containerId, final Inventory playerInventory, final BlockPos monitorPos,
                              final BlockPos hostPos, @Nullable final HardwareEra era, final Phase phase) {
        super(ComputingModule.MONITOR_SESSION_MENU.get(), containerId);
        this.monitorPos = monitorPos;
        this.hostPos = hostPos;
        this.era = era;
        this.phase = phase;
    }

    public static MonitorSessionMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                 final RegistryFriendlyByteBuf buf) {
        final BlockPos monitor = buf.readBlockPos();
        final BlockPos host = buf.readBlockPos();
        final HardwareEra era = HardwareEra.find(buf.readVarInt());
        final Phase phase = Phase.byId(buf.readVarInt());
        return new MonitorSessionMenu(containerId, playerInventory, monitor, host, era, phase);
    }

    /** Writes what the client rebuilds this from: the two positions, the machine's era, and which session. */
    public static void writeOpenBuffer(final RegistryFriendlyByteBuf buf, final BlockPos monitorPos,
                                       final BlockPos hostPos, @Nullable final HardwareEra era,
                                       final Phase phase) {
        buf.writeBlockPos(monitorPos);
        buf.writeBlockPos(hostPos);
        buf.writeVarInt(era == null ? -1 : era.id());
        buf.writeVarInt(phase.id());
    }

    public BlockPos monitorPos() {
        return this.monitorPos;
    }

    public BlockPos hostPos() {
        return this.hostPos;
    }

    /** The machine's generation, which is the monitor its screen wears and the skin its setup is drawn in. */
    @Nullable
    public HardwareEra hardwareEra() {
        return this.era;
    }

    public Phase phase() {
        return this.phase;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }

    /**
     * Open while the machine is still a machine and the player is still at the glass showing it.
     *
     * <p>Reach is measured at the monitor, not at the computer, because that is where the player is standing;
     * a machine opened at itself has no monitor to stand at, so the machine is the place. Nothing here asks
     * what the machine is doing: a self-test that ends, a system that comes up and an installer that finishes
     * all hand over to the next screen themselves, and closing this on them would take the monitor dark in
     * between.
     */
    @Override
    public boolean stillValid(final Player player) {
        final Level level = player.level();
        if (!(level.getBlockEntity(this.hostPos) instanceof IOsHost)) {
            return false;
        }
        final BlockPos standingAt = this.monitorPos.equals(this.hostPos) ? this.hostPos : this.monitorPos;
        if (player.distanceToSqr(standingAt.getX() + 0.5, standingAt.getY() + 0.5, standingAt.getZ() + 0.5)
                > REACH_SQUARED) {
            return false;
        }
        /*
         * A monitor switched to another machine of the same rack is no longer showing this one, and what is
         * drawn here belongs to a machine the player is not looking at any more.
         */
        return this.monitorPos.equals(this.hostPos)
                || !(level.getBlockEntity(this.monitorPos) instanceof MonitorBlockEntity monitor)
                || monitor.shows(this.hostPos);
    }

    /**
     * Which session a monitor is showing, which is what decides the screen the client opens.
     *
     * <p>Each says which number it goes by rather than being read by its place in this list, so the order
     * they are written in here is nobody's business but the reader's.
     */
    public enum Phase implements IStableId {

        /** The power-on self-test, and the failure it ends on when there is nothing to boot. */
        POST(0),

        /** The boot manager, listing every disk that carries a system. */
        BOOT_MENU(1),

        /** A system coming up, between the self-test ending and the desktop or the prompt opening. */
        SYSTEM_BOOT(2),

        /** The firmware setup: boot order, hardware, storage. */
        FIRMWARE(3),

        /** A system's own installer, on the page it has reached. */
        INSTALLER(4),

        /** The plain copy of a system onto a disk, and what it ends on, finished or refused. */
        INSTALL_PROGRESS(5),

        /** The switch that says which machine of a rack this monitor shows. */
        KVM(6);

        private static final StableIds<Phase> IDS = StableIds.of(Phase.class);

        private final int id;

        Phase(final int id) {
            this.id = id;
        }

        /** The phase that declares {@code id}; an id no phase declares reads as the self-test. */
        public static Phase byId(final int id) {
            return IDS.byId(id, POST);
        }

        @Override
        public int id() {
            return this.id;
        }
    }
}
