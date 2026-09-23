/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.advancement;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;

/**
 * Who is acting right now, for work that is asked for here and finishes somewhere else later.
 *
 * <p>An Operation typed at a terminal is taken on by the Mainframe's scheduler and settles ticks later, several
 * layers away from the payload that carried the command, and none of the layers between them knows a player.
 * Rather than hand a player through all of them, the code that does know one runs the work inside a scope, and the
 * scheduler reads the scope once, when it takes the Operation on. A payload's sender opens one; a program running on
 * its own opens one for whoever works its machine; outside any scope nobody is acting.
 *
 * <p>Scopes nest and are kept per thread, so work on another thread never sees them.
 */
public final class Acting {

    private static final UUID NOBODY = new UUID(0L, 0L);
    private static final ThreadLocal<Deque<UUID>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);

    private Acting() {
    }

    /** Runs {@code work} with {@code player} acting; a machine acting as a player is nobody. */
    public static void as(@Nullable final Player player, final Runnable work) {
        run(player == null || player instanceof FakePlayer ? NOBODY : player.getUUID(), work);
    }

    /**
     * Runs {@code work} with the player of that id acting, whether or not they are here: a saved Operation resuming
     * after a restart is still asked for by whoever asked for it.
     */
    public static void as(@Nullable final UUID player, final Runnable work) {
        run(player == null ? NOBODY : player, work);
    }

    /** Runs {@code work} with the operator of {@code machine} acting, or nobody when it has none. */
    public static void asOperatorOf(@Nullable final BlockEntity machine, final Runnable work) {
        final UUID operator = machine == null ? null : machine.getExistingDataOrNull(MachineOperators.OPERATOR);
        run(operator == null ? NOBODY : operator, work);
    }

    /** Who is acting in the innermost scope, when anybody is. */
    public static Optional<UUID> current() {
        final UUID top = SCOPES.get().peek();
        return top == null || NOBODY.equals(top) ? Optional.empty() : Optional.of(top);
    }

    private static void run(final UUID actor, final Runnable work) {
        final Deque<UUID> scopes = SCOPES.get();
        scopes.push(actor);
        try {
            work.run();
        } finally {
            scopes.pop();
        }
    }
}
