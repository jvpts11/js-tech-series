/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.menu.DesktopMenu;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The computer screens the server opened for each player that are not container menus, and the desktop each
 * player last closed.
 *
 * <p>The firmware setup, the power-on self-test, the installer's last prompt and the KVM channel picker are
 * plain screens: the server tells the client what to show and holds no menu for them. What they send back
 * still has to be shown to come from a player the server put that machine in front of, so the server writes
 * down the one it last opened for each player. A payload from such a screen is taken only from that player,
 * for that machine, while they still stand at the monitor and the monitor still shows the machine. Opening a
 * menu, or leaving, ends it.
 *
 * <p>A desktop that closes tells the machine which windows it leaves open, and that word reaches the server
 * after the menu it came from has already gone. So the desktop a player closes is remembered for a moment,
 * and only for the machine it was showing.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class ScreenSessions {

    /** How far a player may stand from the monitor, squared: the same reach the computer menus allow. */
    private static final double REACH_SQUARED = 64.0;

    /** How long a closed desktop's last word is still taken, in ticks. */
    private static final long CLOSING_TICKS = 40;

    private record Session(ResourceKey<Level> level, BlockPos monitor, BlockPos host) {
    }

    private record Closed(ResourceKey<Level> level, BlockPos host, long tick) {
    }

    /** One open screen per player; the server thread is the only one that touches these. */
    private static final Map<UUID, Session> OPEN = new HashMap<>();
    private static final Map<UUID, Closed> CLOSED_DESKTOPS = new HashMap<>();

    private ScreenSessions() {
    }

    /** Writes down that the server opened a plain screen for this player, showing that machine on that monitor. */
    public static void opened(final ServerPlayer player, final BlockPos monitor, final BlockPos host) {
        OPEN.put(player.getUUID(), new Session(player.level().dimension(), monitor.immutable(), host.immutable()));
    }

    /** Whether this player has such a screen open on that machine and is still at the monitor showing it. */
    static boolean admits(final ServerPlayer player, final BlockPos host) {
        final Session session = OPEN.get(player.getUUID());
        if (session == null || !session.host().equals(host) || !session.level().equals(player.level().dimension())
                || player.distanceToSqr(Vec3.atCenterOf(session.monitor())) > REACH_SQUARED) {
            return false;
        }
        // A machine opened at itself has no monitor to ask; any other has to be on the screen it was opened on.
        return session.monitor().equals(host)
                || player.level().getBlockEntity(session.monitor()) instanceof MonitorBlockEntity screen
                        && screen.shows(host);
    }

    /**
     * Whether this player closed that machine's desktop a moment ago.
     *
     * <p>Reach is not asked: a desktop closes when its player walks away, and the windows it leaves open are
     * the machine's all the same.
     */
    static boolean closedDesktopOf(final ServerPlayer player, final BlockPos host) {
        final Closed closed = CLOSED_DESKTOPS.get(player.getUUID());
        return closed != null && closed.host().equals(host) && closed.level().equals(player.level().dimension())
                && player.level().getGameTime() - closed.tick() <= CLOSING_TICKS;
    }

    @SubscribeEvent
    public static void onMenuOpened(final PlayerContainerEvent.Open event) {
        OPEN.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onMenuClosed(final PlayerContainerEvent.Close event) {
        if (event.getContainer() instanceof DesktopMenu desktop) {
            CLOSED_DESKTOPS.put(event.getEntity().getUUID(), new Closed(event.getEntity().level().dimension(),
                    desktop.hostPos().immutable(), event.getEntity().level().getGameTime()));
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        OPEN.remove(event.getEntity().getUUID());
        CLOSED_DESKTOPS.remove(event.getEntity().getUUID());
        ComputerAccess.forget(event.getEntity().getUUID());
    }
}
