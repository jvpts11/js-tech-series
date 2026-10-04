/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.CraftingRouterPart;
import dev.jstech.computers.storage.StorageKey;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Which router of an interface's own cable carries each input of a pattern: the one chosen for it by hand, while it is
 * still on the cable; else a router whose filter lists the input; else a router with nothing in its filter, which
 * takes anything. An input none of these carries has no way into the machine, and the pattern cannot run there.
 */
public final class InterfaceRoutes {

    private InterfaceRoutes() {
    }

    /** How a router came to carry an input. */
    public enum Why {
        /** It was chosen for the input by hand or by a program. */
        CHOSEN,
        /** Its filter lists the input. */
        FILTER,
        /** Its filter is empty, so it takes anything. */
        ANY,
        /** No router carries it. */
        NONE
    }

    /** The router carrying an input and why, or {@link Why#NONE} with no router. */
    public record Route(@Nullable CraftingFloor.Site router, Why why) {

        public static final Route NONE = new Route(null, Why.NONE);
    }

    /** The router of {@code reach} that carries {@code input} of {@code held}. */
    public static Route routeFor(final ServerLevel level, final CraftingInterfacePart.HeldPattern held,
                                 final StorageKey input, final CraftingFloor.Reach reach) {
        final UUID chosen = held.routes().get(input.id());
        if (chosen != null) {
            for (final CraftingFloor.Site site : reach.routers()) {
                final CraftingRouterPart router = site.part(level, CraftingRouterPart.class);
                if (router != null && router.id().equals(chosen)) {
                    return new Route(site, Why.CHOSEN);
                }
            }
        }
        CraftingFloor.Site wildcard = null;
        for (final CraftingFloor.Site site : reach.routers()) {
            final CraftingRouterPart router = site.part(level, CraftingRouterPart.class);
            if (router == null) {
                continue;
            }
            final List<StorageKey> listed = router.filterKeys();
            if (listed.contains(input)) {
                return new Route(site, Why.FILTER);
            }
            if (listed.isEmpty() && wildcard == null) {
                wildcard = site;
            }
        }
        return wildcard == null ? Route.NONE : new Route(wildcard, Why.ANY);
    }

    /**
     * Whether {@code pattern} can run on the interface: it feeds a machine, and through routers every input has one
     * to go through.
     */
    public static boolean runs(final ServerLevel level, final CraftingInterfacePart.HeldPattern held,
                               final ProcessingPattern pattern, final CraftingFloor.Reach reach) {
        if (reach.machine() == null) {
            return false;
        }
        if (reach.mode() != CraftingFloor.Mode.CABLE) {
            return true;
        }
        for (final ProcessingPattern.ProcessingInput in : pattern.inputs()) {
            if (routeFor(level, held, in.key(), reach).router() == null) {
                return false;
            }
        }
        return true;
    }
}
