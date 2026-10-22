/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.IProtectionAdapter;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

/**
 * A protection of the test mod, as a claim mod's would be: boxes of a test's arena claimed while the test runs, into
 * which the world's own work from outside may not come, a fluid flowing in, something shot by nobody, a structure made
 * by nobody crashing; inside its own claim it may. It is registered the first time a test claims a box.
 */
public final class TestClaims {

    /** The name the protection is registered under. */
    public static final String NAME = "jstests:claims";
    /** Who the claimed boxes belong to. */
    public static final UUID CLAIMANT = UUID.fromString("6a737465-7374-7300-0000-0000000000c1");
    private static final Set<AABB> CLAIMED = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private TestClaims() {
    }

    /** Claims {@code box}, in the world's places, until {@link #unclaim} lets it go. */
    public static void claim(final AABB box) {
        if (REGISTERED.compareAndSet(false, true)) {
            Protection.register(NAME, new Claims());
        }
        CLAIMED.add(box);
    }

    /** Lets {@code box} go. */
    public static void unclaim(final AABB box) {
        CLAIMED.remove(box);
    }

    /* Whether the middle of the block at {@code pos} is in a claimed box. */
    private static boolean claimed(final BlockPos pos) {
        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.5;
        final double z = pos.getZ() + 0.5;
        for (final AABB box : CLAIMED) {
            if (box.contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /** The claims: the world's work from outside them stays out, everyone else is left to the other protections. */
    private static final class Claims implements IProtectionAdapter {

        @Override
        public Verdict ask(final Actor actor, final ProtectedAction action, final ServerLevel level,
                           final BlockPos pos) {
            if (!claimed(pos) || !(actor instanceof Actor.World world)) {
                return Verdict.ABSTAIN;
            }
            return Protection.sameClaim(this, level, world.origin(), pos) ? Verdict.ALLOW : Verdict.DENY;
        }

        @Override
        public Optional<UUID> claimOwner(final ServerLevel level, final BlockPos pos) {
            return claimed(pos) ? Optional.of(CLAIMANT) : Optional.empty();
        }
    }
}
