/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.call;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.ApiStatus;

/**
 * What the server does when a player's screen makes a call: handed the menu the player has open, already checked to
 * be of the call's kind and still in reach, the player, and what the screen sent.
 *
 * @param <M> the kind of menu the call is made from
 * @param <A> what the screen sends with it
 */
@ApiStatus.Experimental
@FunctionalInterface
public interface IMenuCallHandler<M extends AbstractContainerMenu, A> {

    void handle(M menu, ServerPlayer player, A argument);
}
