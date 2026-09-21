/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.menu.MonitorSessionMenu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
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
 * still has to be shown to come from a player the server put that machine in front of. That used to be a note
 * the server kept per player, because those screens were not menus and nothing else knew who was looking at
 * what. They are menus now, so the menu a player has open is the note, and it cannot fall out of step.
 *
 * <p>A desktop that closes tells the machine which windows it leaves open, and that word reaches the server
 * after the menu it came from has already gone. So the desktop a player closes is remembered for a moment,
 * and only for the machine it was showing.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class ScreenSessions {

    /** How long a closed desktop's last word is still taken, in ticks. */
    private static final long CLOSING_TICKS = 40;

    private record Closed(ResourceKey<Level> level, BlockPos host, long tick) {
    }

    private static final Map<UUID, Closed> CLOSED_DESKTOPS = new HashMap<>();

    private ScreenSessions() {
    }

    /**
     * Boots every player watching that machine into whatever it comes up as: what the end of a self-test does
     * to the screens that were showing it.
     *
     * <p>The watchers are gathered before any screen is opened, because opening one ends that player's plain
     * screen session and so writes to the very map this reads.
     */
    public static void bootWatchers(final ServerLevel level, final BlockPos host) {
        /*
         * Asked of the machine rather than assumed: what follows a self-test is whatever the machine is now
         * doing, and that is not always its boot target. A machine told to install something is in its
         * installer when the test ends, and going straight to the boot target dropped the player into the
         * firmware instead, with the installation waiting behind a screen nobody was shown.
         */
        eachWatcher(level, host, (player, monitor) -> MonitorBlock.openSession(player, level, monitor, host));
    }

    /**
     * Hands every player watching that machine to {@code action}, with the monitor each is watching it on.
     *
     * <p>The watchers are gathered before the first is handed over, because putting a screen in front of one ends
     * that player's plain screen session and so writes to the very map this reads.
     */
    public static void eachWatcher(final ServerLevel level, final BlockPos host,
                                   final BiConsumer<ServerPlayer, BlockPos> action) {
        final List<ServerPlayer> watching = new ArrayList<>();
        final List<BlockPos> monitors = new ArrayList<>();
        for (final ServerPlayer player : level.players()) {
            if (player.containerMenu instanceof MonitorSessionMenu session && session.hostPos().equals(host)) {
                watching.add(player);
                monitors.add(session.monitorPos());
            }
        }
        for (int at = 0; at < watching.size(); at++) {
            action.accept(watching.get(at), monitors.get(at));
        }
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
    public static void onMenuClosed(final PlayerContainerEvent.Close event) {
        if (event.getContainer() instanceof DesktopMenu desktop) {
            CLOSED_DESKTOPS.put(event.getEntity().getUUID(), new Closed(event.getEntity().level().dimension(),
                    desktop.hostPos().immutable(), event.getEntity().level().getGameTime()));
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        CLOSED_DESKTOPS.remove(event.getEntity().getUUID());
        ComputerAccess.forget(event.getEntity().getUUID());
    }
}
