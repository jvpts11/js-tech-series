/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.menu.CommandPromptMenu;
import dev.jstech.computers.menu.DesktopMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * The players looking at one machine through its desktop or its prompt.
 *
 * <p>Their menus say when they open and when they close, so a tick never walks the level's players to
 * find them. The first look after the machine loads does walk them, once, because somebody may already
 * have been standing at it when it came back.
 */
final class Viewers {

    private final BlockPos host;
    private final List<ServerPlayer> watching = new ArrayList<>();
    private boolean known;

    Viewers(final BlockPos host) {
        this.host = host;
    }

    /**
     * Every player with this machine's console on screen.
     *
     * <p>The list is the machine's own, kept on the server thread: read it, do not keep it.
     */
    List<ServerPlayer> at(final ServerLevel level) {
        if (!this.known) {
            this.known = true;
            for (final ServerPlayer player : level.players()) {
                if (!(player instanceof FakePlayer) && !this.watching.contains(player)) {
                    this.watching.add(player);
                }
            }
        }
        for (int i = this.watching.size() - 1; i >= 0; i--) {
            final ServerPlayer player = this.watching.get(i);
            if (player.isRemoved() || player.level() != level || !shows(player)) {
                this.watching.remove(i);
            }
        }
        return this.watching;
    }

    /** A player opened this machine's desktop or prompt. */
    void opened(final ServerPlayer viewer) {
        if (!this.watching.contains(viewer)) {
            this.watching.add(viewer);
        }
    }

    /** A player closed this machine's desktop or prompt. */
    void closed(final ServerPlayer viewer) {
        this.watching.remove(viewer);
    }

    /** Whether that player's open menu is this machine's prompt or desktop. */
    private boolean shows(final ServerPlayer player) {
        return (player.containerMenu instanceof CommandPromptMenu prompt && this.host.equals(prompt.hostPos()))
                || (player.containerMenu instanceof DesktopMenu desk && this.host.equals(desk.hostPos()));
    }
}
