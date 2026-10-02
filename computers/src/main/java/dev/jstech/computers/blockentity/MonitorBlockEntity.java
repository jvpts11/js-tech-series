/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.peripheral.IPeripheralEndpoint;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.PeripheralLink;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * BlockEntity for a Monitor: a {@code COMPUTING} {@link IPeripheralEndpoint} that displays a computer's interface.
 * Its screen lights a moment after its computer starts running and goes dark the moment it stops; the block's
 * {@code LIT} state follows whether it is lit.
 */
public class MonitorBlockEntity extends SyncedBlockEntity implements IPeripheralEndpoint {

    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING,
            PeripheralLinks.COMPUTING);
    private final BoolField lit = fields().flag("Lit", false).save();
    /** The terminal tab the player last used on this screen, which it reopens on. */
    private final IntField lastTab = fields().integer("LastTab", ComputerTerminalMenu.TAB_NETWORK).save();
    private int bootTicks;
    /*
     * The machine this screen is currently showing on someone else's behalf: a Remote Control session puts a REMOTE
     * computer on this monitor, so for as long as it lasts the screen answers for that machine and not for the one
     * its cable is linked to. Transient by nature, a session does not outlive the window it was opened in.
     */
    @Nullable
    private BlockPos remoteSession;

    private static final int BOOT_DELAY = 20;
    private static final int BATTLESTATION_MONITORS = 4;

    public MonitorBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.MONITOR_BE.get(), pos, state);
        fields().mirror(MonitorBlock.LIT, lit::get);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final MonitorBlockEntity monitor) {
        if (level instanceof ServerLevel server) {
            monitor.tick(server);
        }
    }

    @Override
    public PeripheralCableType cableType() {
        return link.cableType();
    }

    /** A screen takes a video output of its computer. */
    @Override
    public PortKind portKind() {
        return PortKind.VIDEO;
    }

    @Override
    public Optional<Long> linkedOwner() {
        return link.linkedOwner();
    }

    @Override
    public void onOwnerLinked(final long ownerPos) {
        link.linked(ownerPos);
    }

    @Override
    public void onOwnerUnlinked() {
        link.unlinked();
    }

    @Nullable
    public BlockPos ownerPos() {
        return link.ownerPos();
    }

    /** Starts (or ends, with {@code null}) a remote session showing {@code machine} on this screen. */
    public void setRemoteSession(@Nullable final BlockPos machine) {
        this.remoteSession = machine;
    }

    /** The machine a remote session is showing here, or null when the screen is showing its own. */
    @Nullable
    public BlockPos remoteSession() {
        return remoteSession;
    }

    /**
     * Whether this monitor legitimately shows {@code machine}: either its cable links to it, or a remote session put
     * it there. The menus validate through this, so a taken-over screen is not torn down for showing a computer that
     * is not its own.
     */
    public boolean shows(final BlockPos machine) {
        return machine != null
                && (machine.equals(ownerPos()) || machine.equals(remoteSession));
    }

    public int lastTab() {
        return lastTab.get();
    }

    public void setLastTab(final int tab) {
        if (tab >= 0) {
            lastTab.set(tab);
        }
    }

    private void tick(final ServerLevel level) {
        final boolean wasLinked = link.linkedOwner().isPresent();
        link.tick(level, worldPosition);
        if (!wasLinked && link.linkedOwner().isPresent()) {
            reportBattlestation(level);
        }
        updateScreen(level);
    }

    /* A fourth monitor on one computer is a battlestation, earned by whoever put this one up. */
    private void reportBattlestation(final ServerLevel level) {
        final BlockPos owner = link.ownerPos();
        if (owner == null || !(level.getBlockEntity(owner) instanceof IPeripheralOwner linkedTo)) {
            return;
        }
        int monitors = 0;
        for (final long endpoint : linkedTo.linkedEndpoints()) {
            if (level.getBlockEntity(BlockPos.of(endpoint)) instanceof MonitorBlockEntity) {
                monitors++;
            }
        }
        if (monitors >= BATTLESTATION_MONITORS) {
            JscEvents.awardOperator(this, JscEvents.BATTLESTATION);
        }
    }

    private void updateScreen(final ServerLevel level) {
        // Lit only when the linked computer is actively running, not just linked but powered off.
        final BlockPos owner = link.ownerPos();
        if (owner != null && !level.isLoaded(owner)) {
            return; // a computer whose chunk is away is not asked, nor loaded back; the screen stays as it is
        }
        final boolean computerRunning = owner != null
                && level.getBlockEntity(owner) instanceof IOsHost host
                && host.isRunning();
        if (!computerRunning) {
            bootTicks = 0;
            if (lit.get()) {
                lit.set(false);
                // A picture tube is heard going dark; a flat panel, from the Transition on, goes dark without a sound.
                if (getBlockState().getBlock() instanceof MonitorBlock monitor
                        && monitor.era().isAtMost(HardwareEra.LEGACY)) {
                    Audio.at(level, worldPosition, ComputingSounds.MONITOR_POWER_OFF);
                }
            }
            return;
        }
        if (lit.get()) {
            return; // already booted onto the desktop
        }
        if (++bootTicks >= BOOT_DELAY) {
            lit.set(true);
            Audio.at(level, worldPosition, ComputingSounds.MONITOR_POWER_ON);
            bootTicks = 0;
        }
    }
}
